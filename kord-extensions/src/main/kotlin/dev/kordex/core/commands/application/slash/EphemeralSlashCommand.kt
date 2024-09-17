/*
 * Copyrighted (Kord Extensions, 2024). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

@file:Suppress("TooGenericExceptionCaught")
@file:OptIn(KordUnsafe::class)

package dev.kordex.core.commands.application.slash

import dev.kord.common.annotation.KordUnsafe
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.message.create.InteractionResponseCreateBuilder
import dev.kordex.core.ArgumentParsingException
import dev.kordex.core.DiscordRelayedException
import dev.kordex.core.InvalidCommandException
import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.events.*
import dev.kordex.core.components.forms.ModalForm
import dev.kordex.core.extensions.Extension
import dev.kordex.core.types.FailureReason
import dev.kordex.core.utils.MutableStringKeyedMap
import dev.kordex.core.utils.getLocale

public typealias InitialEphemeralSlashResponseBuilder =
	(suspend InteractionResponseCreateBuilder.(ChatInputCommandInteractionCreateEvent) -> Unit)?

/** Ephemeral slash command. **/
public class EphemeralSlashCommand<A : Arguments, M : ModalForm>(
	extension: Extension,

	public override val arguments: (() -> A)? = null,
	public override val modal: (() -> M)? = null,
	public override val parentCommand: SlashCommand<*, *, *>? = null,
	public override val parentGroup: SlashGroup? = null,
) : SlashCommand<EphemeralSlashCommandContext<A, M>, A, M>(extension) {
	/** @suppress Internal guilder **/
	public var initialResponseBuilder: InitialEphemeralSlashResponseBuilder = null

	/** Call this to open with a response, omit it to ack instead. **/
	public fun initialResponse(body: InitialEphemeralSlashResponseBuilder) {
		initialResponseBuilder = body
	}

	override fun validate() {
		super.validate()

		if (modal != null && initialResponseBuilder != null) {
			throw InvalidCommandException(
				name,

				"You may not provide a modal builder and an initial response - pick one, not both."
			)
		}
	}

	override suspend fun call(event: ChatInputCommandInteractionCreateEvent, cache: MutableStringKeyedMap<Any>) {
		findCommand(event).run(event, cache)
	}

	override suspend fun run(event: ChatInputCommandInteractionCreateEvent, cache: MutableStringKeyedMap<Any>) {
		emitEventAsync(EphemeralSlashCommandInvocationEvent(this, event))

		try {
			if (!runChecks(event, cache)) {
				emitEventAsync(
					EphemeralSlashCommandFailedChecksEvent(
						this,
						event,
						"Checks failed without a message."
					)
				)

				return
			}
		} catch (e: DiscordRelayedException) {
			event.interaction.respondEphemeral {
				settings.failureResponseBuilder(this, e.reason, FailureReason.ProvidedCheckFailure(e))
			}

			emitEventAsync(
				EphemeralSlashCommandFailedChecksEvent(
					this,
					event,
					e.reason
				)
			)

			return
		}

		val modalObj = modal?.invoke()

		val response = if (initialResponseBuilder != null) {
			event.interaction.respondEphemeral { initialResponseBuilder!!(event) }
		} else if (modalObj != null) {
			componentRegistry.register(modalObj)

			val locale = event.getLocale()

			event.interaction.modal(
				modalObj.translateTitle(locale),
				modalObj.id
			) {
				modalObj.applyToBuilder(this, event.getLocale())
			}

			modalObj.awaitCompletion {
				componentRegistry.unregisterModal(modalObj)

				it?.deferEphemeralResponseUnsafe()
			} ?: return
		} else {
			event.interaction.deferEphemeralResponseUnsafe()
		}

		val context = EphemeralSlashCommandContext(event, this, response, cache)

		context.populate()

		firstSentryBreadcrumb(context, this)

		try {
			checkBotPerms(context)
		} catch (e: DiscordRelayedException) {
			respondText(context, e.reason, FailureReason.OwnPermissionsCheckFailure(e))

			emitEventAsync(
				EphemeralSlashCommandFailedChecksEvent(
					this,
					event,
					e.reason
				)
			)

			return
		}
		if (arguments != null) {
			try {
				val args = registry.argumentParser.parse(arguments, context)

				context.populateArgs(args)
			} catch (e: ArgumentParsingException) {
				respondText(context, e.reason, FailureReason.ArgumentParsingFailure(e))
				emitEventAsync(EphemeralSlashCommandFailedParsingEvent(this, event, e))

				return
			}
		}

		try {
			body(context, modalObj)
		} catch (t: Throwable) {
			emitEventAsync(EphemeralSlashCommandFailedWithExceptionEvent(this, event, t))

			if (t is DiscordRelayedException) {
				respondText(context, t.reason, FailureReason.RelayedFailure(t))

				return
			}

			handleError(context, t, this)

			return
		}

		emitEventAsync(EphemeralSlashCommandSucceededEvent(this, event))
	}

	override suspend fun respondText(
		context: EphemeralSlashCommandContext<A, M>,
		message: String,
		failureType: FailureReason<*>,
	) {
		context.respond { settings.failureResponseBuilder(this, message, failureType) }
	}
}
