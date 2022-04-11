/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.commands.application

import com.kotlindiscord.kord.extensions.DiscordRelayedException
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder
import com.kotlindiscord.kord.extensions.checks.types.Check
import com.kotlindiscord.kord.extensions.checks.types.CheckContext
import com.kotlindiscord.kord.extensions.commands.Command
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.any
import com.kotlindiscord.kord.extensions.utils.getLocale
import dev.kord.common.entity.ApplicationCommandType
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.RoleBehavior
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.event.interaction.InteractionCreateEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import dev.kord.common.Locale as KLocale
import java.util.*

/**
 * Abstract class representing an application command - extend this for actual implementations.
 *
 * @param extension Extension this application command belongs to.
 */
public abstract class ApplicationCommand<E : InteractionCreateEvent>(
    extension: Extension
) : Command(extension), KoinComponent {
    /** Translations provider, for retrieving translations. **/
    public val translationsProvider: TranslationsProvider by inject()
    protected val bot: ExtensibleBot by inject()

    /** Quick access to the command registry. **/
    public val registry: ApplicationCommandRegistry by inject()

    /** Bot settings object. **/
    public val settings: ExtensibleBotBuilder by inject()

    /** Kord instance, backing the ExtensibleBot. **/
    public val kord: Kord by inject()

    /** Sentry adapter, for easy access to Sentry functions. **/
    public val sentry: SentryAdapter by inject()

    /** Discord-side command type, for matching up. **/
    public abstract val type: ApplicationCommandType

    /** @suppress **/
    public open val checkList: MutableList<Check<E>> = mutableListOf()

    /** @suppress **/
    public open var guildId: Snowflake? = settings.applicationCommandsBuilder.defaultGuild

    /**
     * Whether to allow everyone to use this command by default.
     *
     * Leaving this at `true` means that your allowed roles/users sets will effectively be ignored, but your denied
     * roles/users won't be.
     *
     * This will be set to `false` automatically by the `allowX` functions, to ensure that they're applied by Discord.
     */
    public open var allowByDefault: Boolean = true

    /**
     * List of allowed role IDs. Allows take priority over disallows.
     */
    public open val allowedRoles: MutableSet<Snowflake> = mutableSetOf()

    /**
     * List of allowed invoker IDs. Allows take priority over disallows.
     */
    public open val allowedUsers: MutableSet<Snowflake> = mutableSetOf()

    /**
     * List of disallowed role IDs. Allows take priority over disallows.
     */
    public open val disallowedRoles: MutableSet<Snowflake> = mutableSetOf()

    /**
     * List of disallowed invoker IDs. Allows take priority over disallows.
     */
    public open val disallowedUsers: MutableSet<Snowflake> = mutableSetOf()

    /** Permissions required to be able to run this command. **/
    public open val requiredPerms: MutableSet<Permission> = mutableSetOf()

    /**
     * A [Localized] version of [name].
     */
    public val localizedName: Localized<String> by lazy { localize(name) }

    /**
     * Localizes a property by its [key] for this command.
     */
    public fun localize(key: String): Localized<String> {
        val default = translationsProvider.translate(
            key,
            this.resolvedBundle,
            translationsProvider.defaultLocale
        )
        val translations = bot.settings.i18nBuilder.applicationCommandLocales.associateWith { locale ->
            translationsProvider.translate(
                key,
                this.resolvedBundle,
                locale.asJavaLocale()
            )
        }

        return Localized(default, translations.toMutableMap())
    }

    /** If your bot requires permissions to be able to execute the command, add them using this function. **/
    public fun requireBotPermissions(vararg perms: Permission) {
        perms.forEach { requiredPerms.add(it) }
    }

    /** Specify a specific guild for this application command to be locked to. **/
    public open fun guild(guild: Snowflake) {
        this.guildId = guild
    }

    /** Specify a specific guild for this application command to be locked to. **/
    public open fun guild(guild: Long) {
        this.guildId = Snowflake(guild)
    }

    /** Specify a specific guild for this application command to be locked to. **/
    public open fun guild(guild: GuildBehavior) {
        this.guildId = guild.id
    }

    /** Register an allowed role, and set [allowByDefault] to `false`. **/
    public open fun allowRole(role: Snowflake): Boolean {
        allowByDefault = false

        return allowedRoles.add(role)
    }

    /** Register an allowed role, and set [allowByDefault] to `false`. **/
    public open fun allowRole(role: RoleBehavior): Boolean =
        allowRole(role.id)

    /** Register a disallowed role, and set [allowByDefault] to `false`. **/
    public open fun disallowRole(role: Snowflake): Boolean =
        disallowedRoles.add(role)

    /** Register a disallowed role, and set [allowByDefault] to `false`. **/
    public open fun disallowRole(role: RoleBehavior): Boolean =
        disallowRole(role.id)

    /** Register an allowed user, and set [allowByDefault] to `false`. **/
    public open fun allowUser(user: Snowflake): Boolean {
        allowByDefault = false

        return allowedUsers.add(user)
    }

    /** Register an allowed user, and set [allowByDefault] to `false`. **/
    public open fun allowUser(user: UserBehavior): Boolean =
        allowUser(user.id)

    /** Register a disallowed user. **/
    public open fun disallowUser(user: Snowflake): Boolean =
        disallowedUsers.add(user)

    /** Register a disallowed user. **/
    public open fun disallowUser(user: UserBehavior): Boolean =
        disallowUser(user.id)

    /**
     * Define a check which must pass for the command to be executed.
     *
     * A command may have multiple checks - all checks must pass for the command to be executed.
     * Checks will be run in the order that they're defined.
     *
     * This function can be used DSL-style with a given body, or it can be passed one or more
     * predefined functions. See the samples for more information.
     *
     * @param checks Checks to apply to this command.
     */
    public open fun check(vararg checks: Check<E>) {
        checkList.addAll(checks)
    }

    /**
     * Overloaded check function to allow for DSL syntax.
     *
     * @param check Check to apply to this command.
     */
    public open fun check(check: Check<E>) {
        checkList.add(check)
    }

    /** Called in order to execute the command. **/
    public open suspend fun doCall(event: E): Unit = withLock {
        if (!runChecks(event)) {
            return@withLock
        }

        call(event)
    }

    /** Runs standard checks that can be handled in a generic way, without worrying about subclass-specific checks. **/
    @Throws(DiscordRelayedException::class)
    public open suspend fun runStandardChecks(event: E): Boolean {
        val locale = event.getLocale()

        checkList.forEach { check ->
            val context = CheckContext(event, locale)

            check(context)

            if (!context.passed) {
                context.throwIfFailedWithMessage()

                return false
            }
        }

        // Handle discord-side perms checks, as they can't be relied on to enforce them

        val guildId = event.interaction.data.guildId.value ?: return allowByDefault
        val memberId = event.interaction.user.id

        var isAllowed = memberId in allowedUsers
        var isDenied = memberId in disallowedUsers

        if (allowedRoles.isNotEmpty()) {
            val member = GuildBehavior(guildId, kord).getMember(memberId)

            isAllowed = isAllowed || member.roles.any { it.id in allowedRoles }
        }

        if (disallowedRoles.isNotEmpty()) {
            val member = GuildBehavior(guildId, kord).getMember(memberId)

            isDenied = isDenied || member.roles.any { it.id in disallowedRoles }
        }

        return if (allowByDefault) {
            !isDenied
        } else {
            isAllowed && !isDenied
        }
    }

    /** Override this in order to implement any subclass-specific checks. **/
    @Throws(DiscordRelayedException::class)
    public open suspend fun runChecks(event: E): Boolean =
        runStandardChecks(event)

    /** Override this to implement the calling logic for your subclass. **/
    public abstract suspend fun call(event: E)
}

/**
 * Representation of a localized object.
 *
 * @property default the default translations
 * @property translations a map containing all localizations
 * @param T the type of the object
 */
public data class Localized<T>(val default: T, val translations: MutableMap<KLocale, String>)
