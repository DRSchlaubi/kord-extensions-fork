/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:OptIn(PrivilegedIntent::class, KordPreview::class)

package dev.kordex.core

import dev.kord.common.annotation.KordPreview
import dev.kord.core.Kord
import dev.kord.core.behavior.requestMembers
import dev.kord.core.event.Event
import dev.kord.core.event.UnknownEvent
import dev.kord.core.event.gateway.DisconnectEvent
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.guild.GuildCreateEvent
import dev.kord.core.event.interaction.*
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.gateway.handler.DefaultGatewayEventInterceptor
import dev.kord.core.on
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import dev.kordex.core.builders.ExtensibleBotBuilder
import dev.kordex.core.commands.application.ApplicationCommandRegistry
import dev.kordex.core.commands.chat.ChatCommandRegistry
import dev.kordex.core.components.ComponentRegistry
import dev.kordex.core.events.EventHandler
import dev.kordex.core.events.KordExEvent
import dev.kordex.core.events.extra.GuildJoinRequestDeleteEvent
import dev.kordex.core.events.extra.GuildJoinRequestUpdateEvent
import dev.kordex.core.events.extra.models.GuildJoinRequestDelete
import dev.kordex.core.events.extra.models.GuildJoinRequestUpdate
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.impl.HelpExtension
import dev.kordex.core.extensions.impl.SentryExtension
import dev.kordex.core.koin.KordExContext
import dev.kordex.core.koin.KordExKoinComponent
import dev.kordex.core.types.Lockable
import dev.kordex.core.utils.MutableStringKeyedMap
import dev.kordex.core.utils.loadModule
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.koin.core.component.inject
import org.koin.dsl.bind
import kotlin.concurrent.thread

/**
 * An extensible bot, wrapping a Kord instance.
 *
 * This is your jumping-off point. ExtensibleBot provides a system for managing extensions, commands and event
 * handlers. Either subclass ExtensibleBot or use it as-is if it suits your needs.
 *
 * You shouldn't construct this class directly - use the builder pattern via the companion object's `invoke` method:
 * `ExtensibleBot(token) { extensions { add(::MyExtension) } }`.
 *
 * @param settings Bot builder object containing the bot's settings.
 * @param token Token for connecting to Discord.
 */
public open class ExtensibleBot(
	public val settings: ExtensibleBotBuilder,
	private val token: String,
) : KordExKoinComponent, Lockable {

	override var mutex: Mutex? = Mutex()
	override var locking: Boolean = settings.membersBuilder.lockMemberRequests

	/** @suppress Meant for internal use by public inline function. **/
	public val kordRef: Kord by inject()

	/**
	 * A list of all registered event handlers.
	 */
	public open val eventHandlers: MutableList<EventHandler<out Event>> = mutableListOf()

	/**
	 * A map of the names of all loaded [Extension]s to their instances.
	 */
	public open val extensions: MutableStringKeyedMap<Extension> = mutableMapOf()

	/** @suppress **/
	public open val eventPublisher: MutableSharedFlow<Any> = MutableSharedFlow()

	/** A [Flow] representing a combined set of Kord events and Kord Extensions events. **/
	public open val events: SharedFlow<Any> = eventPublisher.asSharedFlow()

	/** @suppress **/
	public open var initialized: Boolean = false

	/** @suppress **/
	public open val logger: KLogger = KotlinLogging.logger {}

	/** @suppress **/
	public open val shutdownHook: Thread = thread(false) {
		runBlocking {
			close()
		}
	}

	/** @suppress Function that sets up the bot early on, called by the builder. **/
	public open suspend fun setup() {
		val kord = settings.kordBuilder(token) {
			cache {
				settings.cacheBuilder.builder.invoke(this, it)
			}

			defaultStrategy = settings.cacheBuilder.defaultStrategy

			if (settings.shardingBuilder != null) {
				sharding(settings.shardingBuilder!!)
			}

			enableShutdownHook = settings.hooksBuilder.kordShutdownHook

			settings.kordHooks.forEach { it() }

			gatewayEventInterceptor = DefaultGatewayEventInterceptor(
				customContextCreator = { _, _ ->
					mutableMapOf<String, Any>()
				}
			)
		}

		loadModule { single { kord } bind Kord::class }

		settings.cacheBuilder.dataCacheBuilder.invoke(kord, kord.cache)

		kord.on<Event> {
			kord.launch {
				send(this@on)
			}
		}

		addDefaultExtensions()
	}

	/** Start up the bot and log into Discord. **/
	public open suspend fun start() {
		settings.hooksBuilder.runBeforeStart(this)

		if (!initialized) registerListeners()

		@Suppress("TooGenericExceptionCaught")
		try {
			Runtime.getRuntime().addShutdownHook(shutdownHook)
		} catch (e: IllegalArgumentException) {
			logger.debug(e) { "Shutdown hook already added or thread is running." }
		} catch (e: Exception) {
			logger.warn(e) { "Unable to add shutdown hook." }
		}

		getKoin().get<Kord>().login {
			this.presence(settings.presenceBuilder)
			this.intents = Intents(settings.intentsBuilder!!)
		}
	}

	/**
	 * Stop the bot by logging out [Kord].
	 *
	 * This will leave extensions loaded and the Koin context intact, so later restarting of the bot is possible.
	 *
	 * @see close
	 **/
	public open suspend fun stop() {
		getKoin().get<Kord>().logout()
	}

	/**
	 * Stop the bot by unloading extensions, shutting down [Kord], and removing its Koin context.
	 *
	 * Restarting the bot after closing will result in undefined behaviour
	 * because the Koin context needed to start will no longer exist.
	 *
	 * You must fully rebuild closed bots to start again.
	 * Likewise, you must close previous bots before building a new one.
	 *
	 * @see stop
	 **/
	public open suspend fun close() {
		@Suppress("TooGenericExceptionCaught")
		try {
			Runtime.getRuntime().removeShutdownHook(shutdownHook)
		} catch (e: IllegalStateException) {
			logger.debug { "Shutdown in progress, unable to remove shutdown hook." }
		} catch (e: Exception) {
			logger.warn(e) { "Failed to remove shutdown hook." }
		}

		extensions.keys.forEach {
			unloadExtension(it)
		}

		getKoin().get<Kord>().shutdown()

		KordExContext.stopKoin()
	}

	/** Start up the bot and log into Discord, but launched via Kord's coroutine scope. **/
	public open fun startAsync(): Job =
		getKoin().get<Kord>().launch {
			start()
		}

	/** This function sets up all of the bot's default event listeners. **/
	public open suspend fun registerListeners() {
		val eventJson = Json {
			ignoreUnknownKeys = true
		}

		on<GuildCreateEvent> {
			withLock {  // If configured, this won't be concurrent, saving larger bots from spammy rate limits
				if (
					settings.membersBuilder.guildsToFill == null ||
					settings.membersBuilder.guildsToFill!!.contains(guild.id)
				) {
					logger.debug { "Requesting members for guild: ${guild.name}" }

					guild.requestMembers {
						presences = settings.membersBuilder.fillPresences
						requestAllMembers()
					}.collect()
				}
			}
		}

		@Suppress("TooGenericExceptionCaught")
		on<UnknownEvent> {
			try {
				val eventObj = when (name) {
					"GUILD_JOIN_REQUEST_DELETE" -> {
						val data: GuildJoinRequestDelete = eventJson.decodeFromJsonElement(this.data!!)

						GuildJoinRequestDeleteEvent(data)
					}

					"GUILD_JOIN_REQUEST_UPDATE" -> {
						val data: GuildJoinRequestUpdate = eventJson.decodeFromJsonElement(this.data!!)

						GuildJoinRequestUpdateEvent(data)
					}

					else -> null
				} ?: return@on

				send(eventObj)
			} catch (e: Exception) {
				logger.error(e) { "Failed to deserialize event: $data" }
			}
		}

		on<DisconnectEvent.DiscordCloseEvent> {
			logger.warn { "Disconnected: $closeCode" }
		}

		on<ButtonInteractionCreateEvent> {
			getKoin().get<ComponentRegistry>().handle(this)
		}

		on<SelectMenuInteractionCreateEvent> {
			getKoin().get<ComponentRegistry>().handle(this)
		}

		on<ModalSubmitInteractionCreateEvent> {
			getKoin().get<ComponentRegistry>().handle(this)
		}

		if (settings.chatCommandsBuilder.enabled) {
			on<MessageCreateEvent> {
				getKoin().get<ChatCommandRegistry>().handleEvent(this)
			}
		} else {
			logger.debug {
				"Chat command support is disabled - set `enabled` to `true` in the `chatCommands` builder" +
					" if you want to use them."
			}
		}

		if (settings.applicationCommandsBuilder.enabled) {
			on<ChatInputCommandInteractionCreateEvent> {
				getKoin().get<ApplicationCommandRegistry>().handle(this)
			}

			on<MessageCommandInteractionCreateEvent> {
				getKoin().get<ApplicationCommandRegistry>().handle(this)
			}

			on<UserCommandInteractionCreateEvent> {
				getKoin().get<ApplicationCommandRegistry>().handle(this)
			}

			on<AutoCompleteInteractionCreateEvent> {
				getKoin().get<ApplicationCommandRegistry>().handle(this)
			}

			getKoin().get<ApplicationCommandRegistry>().initialRegistration()
		} else {
			logger.debug {
				"Application command support is disabled - set `enabled` to `true` in the " +
					"`applicationCommands` builder if you want to use them."
			}
		}

		if (!initialized) {
			eventHandlers.forEach { handler ->
				handler.listenerRegistrationCallable?.invoke() ?: logger.error {
					"Event handler $handler does not have a listener registration callback. This should never happen!"
				}
			}

			initialized = true
		}
	}

	/** This function adds all of the default extensions when the bot is being set up. **/
	public open suspend fun addDefaultExtensions() {
		val extBuilder = settings.extensionsBuilder

		if (extBuilder.helpExtensionBuilder.enableBundledExtension) {
			this.addExtension(::HelpExtension)
		}

		if (extBuilder.sentryExtensionBuilder.enable && extBuilder.sentryExtensionBuilder.feedbackExtension) {
			this.addExtension(::SentryExtension)
		}
	}

	/**
	 * Subscribe to an event. You shouldn't need to use this directly, but it's here just in case.
	 *
	 * You can subscribe to any type, realistically - but this is intended to be used only with Kord
	 * [Event] subclasses, and our own [KordExEvent]s.
	 *
	 * @param T Types of event to subscribe to.
	 * @param scope Coroutine scope to run the body of your callback under.
	 * @param consumer The callback to run when the event is fired.
	 */
	public inline fun <reified T : Event> on(
		launch: Boolean = true,
		scope: CoroutineScope = kordRef,
		noinline consumer: suspend T.() -> Unit,
	): Job =
		events.buffer(Channel.UNLIMITED)
			.filterIsInstance<T>()
			.onEach {
				runCatching {
					if (launch) scope.launch { consumer(it) } else consumer(it)
				}.onFailure { logger.catching(it) }
			}.catch { logger.catching(it) }
			.launchIn(kordRef)

	/**
	 * @suppress
	 */
	public suspend inline fun send(event: Event) {
		eventPublisher.emit(event)
	}

	/**
	 * Install an [Extension] to this bot.
	 *
	 * This function will call the given builder function and store the resulting extension object, ready to be
	 * set up when the next [ReadyEvent] happens.
	 *
	 * @param builder Builder function (or extension constructor) that takes an [ExtensibleBot] instance and
	 * returns an [Extension].
	 */
	@Throws(InvalidExtensionException::class)
	public open suspend fun addExtension(builder: () -> Extension) {
		val extensionObj = builder.invoke()

		if (extensions.contains(extensionObj.name)) {
			logger.error {
				"Extension with duplicate name ${extensionObj.name} loaded - unloading previously registered extension"
			}

			unloadExtension(extensionObj.name)
		}

		extensions[extensionObj.name] = extensionObj
		loadExtension(extensionObj.name)

		if (!extensionObj.loaded) {
			logger.warn { "Failed to set up extension: ${extensionObj.name}" }
		} else {
			logger.debug { "Loaded extension: ${extensionObj.name}" }

			settings.hooksBuilder.runExtensionAdded(this, extensionObj)
		}
	}

	/**
	 * Reload an unloaded [Extension] from this bot, by name.
	 *
	 * This function **does not** create a new extension object - it simply
	 * calls its `setup()` function. Loaded extensions can
	 * be unload again by calling [unloadExtension].
	 *
	 * This function simply returns if the extension isn't found.
	 *
	 * @param extension The name of the [Extension] to unload.
	 */
	@Throws(InvalidExtensionException::class)
	public open suspend fun loadExtension(extension: String) {
		val extensionObj = extensions[extension] ?: return

		if (!extensionObj.loaded) {
			extensionObj.doSetup()
		}
	}

	/**
	 * Find the first loaded extension that is an instance of the type provided in `T`.
	 *
	 * This can be used to find an extension based on, for example, an implemented interface.
	 *
	 * @param T Types to match extensions against.
	 */
	public inline fun <reified T> findExtension(): T? =
		findExtensions<T>().firstOrNull()

	/**
	 * Find all loaded extensions that are instances of the type provided in `T`.
	 *
	 * This can be used to find extensions based on, for example, an implemented interface.
	 *
	 * @param T Types to match extensions against.
	 */
	public inline fun <reified T> findExtensions(): List<T> =
		extensions.values.filterIsInstance<T>()

	/**
	 * Unload an installed [Extension] from this bot, by name.
	 *
	 * This function **does not** remove the extension object - it simply
	 * removes its event handlers and commands. Unloaded extensions can
	 * be loaded again by calling [loadExtension].
	 *
	 * This function simply returns if the extension isn't found.
	 *
	 * @param extension The name of the [Extension] to unload.
	 */
	public open suspend fun unloadExtension(extension: String) {
		val extensionObj = extensions[extension] ?: return

		if (extensionObj.loaded) {
			extensionObj.doUnload()
		}
	}

	/**
	 * Remove an installed [Extension] from this bot, by name.
	 *
	 * This function will unload the given extension (if it's loaded), and remove the
	 * extension object from the list of registered extensions.
	 *
	 * @param extension The name of the [Extension] to unload.
	 *
	 * @suppress This is meant to be used with the module system, and isn't necessarily a user-facing API.
	 * You need to be quite careful with this!
	 */
	public open suspend fun removeExtension(extension: String) {
		unloadExtension(extension)

		extensions.remove(extension)
	}

	/**
	 * Directly register an [EventHandler] to this bot.
	 *
	 * Generally speaking, you shouldn't call this directly - instead, create an [Extension] and
	 * call the [Extension.event] function in your [Extension.setup] function.
	 *
	 * This function will throw an [EventHandlerRegistrationException] if the event handler has already been registered.
	 *
	 * @param handler The event handler to be registered.
	 * @throws EventHandlerRegistrationException Thrown if the event handler could not be registered.
	 */
	@Throws(EventHandlerRegistrationException::class)
	public inline fun <reified T : Event> addEventHandler(handler: EventHandler<T>) {
		if (eventHandlers.contains(handler)) {
			throw EventHandlerRegistrationException(
				"Event handler already registered in '${handler.extension.name}' extension."
			)
		}

		if (initialized) {
			handler.listenerRegistrationCallable?.invoke() ?: error(
				"Event handler $handler does not have a listener registration callback. This should never happen!"
			)
		}

		eventHandlers.add(handler)
	}

	/**
	 * Directly register an [EventHandler] to this bot.
	 *
	 * Generally speaking, you shouldn't call this directly - instead, create an [Extension] and
	 * call the [Extension.event] function in your [Extension.setup] function.
	 *
	 * This function will throw an [EventHandlerRegistrationException] if the event handler has already been registered.
	 *
	 * @param handler The event handler to be registered.
	 * @throws EventHandlerRegistrationException Thrown if the event handler could not be registered.
	 */
	@Throws(EventHandlerRegistrationException::class)
	public inline fun <reified T : Event> registerListenerForHandler(handler: EventHandler<T>): Job {
		return on<T>(scope = handler.coroutineScope) {
			handler.call(this)
		}
	}

	/**
	 * Directly remove a registered [EventHandler] from this bot.
	 *
	 * This function is used when extensions are unloaded, in order to clear out their event handlers.
	 * No exception is thrown if the event handler wasn't registered.
	 *
	 * @param handler The event handler to be removed.
	 */
	public open fun removeEventHandler(handler: EventHandler<out Event>): Boolean = eventHandlers.remove(handler)
}

/**
 * DSL function for creating a bot instance. This is the Kord Extensions entrypoint.
 *
 * `ExtensibleBot(token) { extensions { add(::MyExtension) } }`
 */
@Suppress("FunctionNaming")  // This is a factory function
public suspend fun ExtensibleBot(token: String, builder: suspend ExtensibleBotBuilder.() -> Unit): ExtensibleBot {
	val settings = ExtensibleBotBuilder()

	builder(settings)

	return settings.build(token)
}