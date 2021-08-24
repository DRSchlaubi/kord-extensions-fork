package com.kotlindiscord.kord.extensions.commands.application

import com.kotlindiscord.kord.extensions.CommandException
import com.kotlindiscord.kord.extensions.checks.types.Check
import com.kotlindiscord.kord.extensions.checks.types.CheckContext
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.i18n.TranslationsProvider
import com.kotlindiscord.kord.extensions.sentry.SentryAdapter
import com.kotlindiscord.kord.extensions.utils.getLocale
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.any
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.event.interaction.InteractionCreateEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

/**
 * Abstract class representing an application command - extend this for actual implementations.
 *
 * @param extension Extension this application command belongs to.
 */
public abstract class ApplicationCommand<E : InteractionCreateEvent>(
    public open val extension: Extension
) : KoinComponent {
    /** Translations provider, for retrieving translations. **/
    public val translationsProvider: TranslationsProvider by inject()

    /** Kord instance, backing the ExtensibleBot. **/
    public val kord: Kord by inject()

    /** Sentry adapter, for easy access to Sentry functions. **/
    public val sentry: SentryAdapter by inject()

    /** @suppress **/
    public open val checkList: MutableList<Check<E>> = mutableListOf()

    /** @suppress **/
    public open var guildId: Snowflake? = null

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

    /** Translation cache, so we don't have to look up translations every time. **/
    public open val nameTranslationCache: MutableMap<Locale, String> = mutableMapOf()

    /** Command name, shown on Discord. **/
    public lateinit var name: String

    /** Return this command's name translated for the given locale, cached as required. **/
    public open fun getTranslatedName(locale: Locale): String {
        if (!nameTranslationCache.containsKey(locale)) {
            nameTranslationCache[locale] = translationsProvider.translate(
                this.name,
                this.extension.bundle,
                locale
            ).lowercase()
        }

        return nameTranslationCache[locale]!!
    }

    /** If your bot requires permissions to be able to execute the command, add them using this function. **/
    public fun requirePermissions(vararg perms: Permission) {
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
    public open fun allowRole(role: Snowflake) {
        allowByDefault = false

        allowedRoles.add(role)
    }

    /** Register an allowed role, and set [allowByDefault] to `false`. **/
    public open fun allowRole(role: UserBehavior): Unit =
        allowRole(role.id)

    /** Register a disallowed role, and set [allowByDefault] to `false`. **/
    public open fun disallowRole(role: Snowflake) {
        allowByDefault = false

        disallowedRoles.add(role)
    }

    /** Register a disallowed role, and set [allowByDefault] to `false`. **/
    public open fun disallowRole(role: UserBehavior): Unit =
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
        checks.forEach { checkList.add(it) }
    }

    /**
     * Overloaded check function to allow for DSL syntax.
     *
     * @param check Check to apply to this command.
     */
    public open fun check(check: Check<E>) {
        checkList.add(check)
    }

    /** Override this in your subclass if you need to change how the command name is validated. **/
    public open fun validateName() {
        if (::name.isInitialized.not() || name.isEmpty()) {
            error("Application command names are required.")
        }
    }

    /** General command validation function. Can be overridden. **/
    public open fun validate() {
        validateName()
    }

    /** Called in order to execute the command. **/
    public open suspend fun doCall(event: E) {
        call(event)
    }

    /** Runs standard checks that can be handled in a generic way, without worrying about subclass-specific checks. **/
    @Throws(CommandException::class)
    public open suspend fun runStandardChecks(event: E): Boolean {
        val locale = event.getLocale()

        // TODO: Global checks
        // TODO: Extension-level checks

        checkList.forEach { check ->
            val context = CheckContext(event, locale)

            check(context)

            if (!context.passed) {
                context.throwIfFailedWithMessage()

                return false
            }
        }

        // Handle discord-side perms checks, as they can't be relied on to enforce them

        val channel = event.interaction.channel.asChannelOrNull() as? GuildMessageChannel ?: return allowByDefault
        val member = event.interaction.user.asMember(channel.guildId)

        val isAllowed = member.id in allowedUsers || member.roles.any { it.id in allowedRoles }
        val isDenied = member.id in disallowedUsers || member.roles.any { it.id in disallowedRoles }

        return if (allowByDefault) {
            !isDenied
        } else {
            isAllowed && !isDenied
        }
    }

    /** Override this in order to implement any subclass-specific checks. **/
    @Throws(CommandException::class)
    public open suspend fun runChecks(event: E): Boolean =
        runStandardChecks(event)

    /** Override this to implement the calling logic for your subclass. **/
    public abstract suspend fun call(event: E)
}
