/*
 * Copyrighted (Kord Extensions, 2024). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

@file:OptIn(KordUnsafe::class, KordExperimental::class)

package dev.kordex.core.components

import dev.kord.common.annotation.KordExperimental
import dev.kord.common.annotation.KordUnsafe
import dev.kord.common.entity.ApplicationIntegrationType
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.MemberBehavior
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.entity.Member
import dev.kord.core.entity.Message
import dev.kord.core.event.interaction.ComponentInteractionCreateEvent
import dev.kordex.core.builders.ExtensibleBotBuilder
import dev.kordex.core.checks.channelFor
import dev.kordex.core.checks.guildFor
import dev.kordex.core.checks.userFor
import dev.kordex.core.koin.KordExKoinComponent
import dev.kordex.core.sentry.SentryContext
import dev.kordex.core.sentry.captures.SentryBreadcrumbCapture
import dev.kordex.core.types.TranslatableContext
import dev.kordex.core.utils.MutableStringKeyedMap
import jdk.internal.net.http.common.Log.channel
import org.koin.core.component.inject
import java.util.*

/**
 * Abstract class representing the execution context for a generic components.
 *
 * @param E Event type the component makes use of
 * @param component Component object that's being interacted with
 * @param event Event that triggered this execution context
 * @param cache Data cache map shared with the defined checks.
 */
public abstract class ComponentContext<E : ComponentInteractionCreateEvent>(
	public open val component: Component,
	public open val event: E,
	public open val cache: MutableStringKeyedMap<Any>,
) : KordExKoinComponent, TranslatableContext {
	/** Configured bot settings object. **/
	public val settings: ExtensibleBotBuilder by inject()

	/** Current Sentry context, containing breadcrumbs and other goodies. **/
	public val sentry: SentryContext = SentryContext()

	/** Cached locale variable, stored and retrieved by [getLocale]. **/
	public override var resolvedLocale: Locale? = null

	/** Channel this component was interacted with within. **/
	public open lateinit var channel: MessageChannelBehavior

	/** Guild this component was interacted with within, if any. **/
	public open var guild: GuildBehavior? = null

	/** Member that interacted with this component, if on a guild. **/
	public open var member: MemberBehavior? = null

	/** The message the component is attached to. **/
	public open lateinit var message: Message

	/** User that interacted with this component. **/
	public open lateinit var user: UserBehavior

	/** Called before processing, used to populate any extra variables from event data. **/
	public open suspend fun populate() {
		channel = getChannel()
		guild = getGuild()
		member = getMember()
		message = getMessage()
		user = getUser()
	}

	/** Extract channel information from event data, if that context is available. **/
	@JvmName("getChannel1")
	public fun getChannel(): MessageChannelBehavior =
		event.interaction.channel

	/** Extract guild information from event data, if that context is available. **/
	@OptIn(KordUnsafe::class, KordExperimental::class)
	@JvmName("getGuild1")
	public fun getGuild(): GuildBehavior? =
		event.interaction.data.guildId.value?.let { event.kord.unsafe.guild(it) }

	/** Extract member information from event data, if that context is available. **/
	@JvmName("getMember1")
	public fun getMember(): MemberBehavior? =
		getGuild()?.let { Member(event.interaction.data.member.value!!, event.interaction.user.data, event.kord) }

	/** Extract message information from event data, if that context is available. **/
	@JvmName("getMessage1")
	public fun getMessage(): Message =
		event.interaction.message

	/** Extract user information from event data, if that context is available. **/
	@JvmName("getUser1")
	public fun getUser(): UserBehavior =
		event.interaction.user

	/** Resolve the locale for this command context. **/
	public override suspend fun getLocale(): Locale {
		var locale: Locale? = resolvedLocale

		if (locale != null) {
			return locale
		}

		val guild = guildFor(event)
		val channel = channelFor(event)
		val user = userFor(event)

		for (resolver in settings.i18nBuilder.localeResolvers) {
			val result = resolver(guild, channel, user, event.interaction)

			if (result != null) {
				locale = result
				break
			}
		}

		resolvedLocale = locale ?: settings.i18nBuilder.defaultLocale

		return resolvedLocale!!
	}

	/**
	 * @param capture breadcrumb data will be modified to add the component context information
	 */
	public suspend fun addContextDataToBreadcrumb(capture: SentryBreadcrumbCapture) {
		if (event.interaction.authorizingIntegrationOwners.containsKey(ApplicationIntegrationType.GuildInstall)) {
			capture.channel = channel.asChannelOrNull()
		}
		capture.guild = guild?.asGuildOrNull()

		capture.data["message.id"] = message.id.toString()
	}
}
