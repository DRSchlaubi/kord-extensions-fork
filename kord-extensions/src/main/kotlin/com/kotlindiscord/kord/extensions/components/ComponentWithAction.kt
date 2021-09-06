package com.kotlindiscord.kord.extensions.components

import com.kotlindiscord.kord.extensions.CommandException
import com.kotlindiscord.kord.extensions.checks.types.Check
import com.kotlindiscord.kord.extensions.checks.types.CheckContext
import com.kotlindiscord.kord.extensions.utils.getLocale
import com.kotlindiscord.kord.extensions.utils.permissionsForMember
import com.kotlindiscord.kord.extensions.utils.translate
import dev.kord.common.entity.Permission
import dev.kord.core.entity.channel.GuildChannel
import dev.kord.core.event.interaction.ComponentInteractionCreateEvent
import mu.KLogger
import mu.KotlinLogging

public abstract class ComponentWithAction<E : ComponentInteractionCreateEvent, C : ComponentContext<*>>
    : ComponentWithID() {
    private val logger: KLogger = KotlinLogging.logger {}

    public open var deferredAck: Boolean = true

    /** @suppress **/
    public open val checkList: MutableList<Check<E>> = mutableListOf()

    /** Permissions required to be able to run execute this component's action. **/
    public open val requiredPerms: MutableSet<Permission> = mutableSetOf()

    /** Component body, to be called when the component is interacted with. **/
    public lateinit var body: suspend C.() -> Unit

    /** Call this to supply a component [body], to be called when the component is interacted with. **/
    public fun action(action: suspend C.() -> Unit) {
        body = action
    }

    /**
     * Define a check which must pass for the component's body to be executed.
     *
     * A component may have multiple checks - all checks must pass for the component's body to be executed.
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

    override fun validate() {
        super.validate()

        if (!::body.isInitialized) {
            error("No component body given.")
        }
    }

    /** If your bot requires permissions to be able to execute this component's body, add them using this function. **/
    public fun requirePermissions(vararg perms: Permission) {
        perms.forEach { requiredPerms.add(it) }
    }

    /** Runs standard checks that can be handled in a generic way, without worrying about subclass-specific checks. **/
    @Throws(CommandException::class)
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

        return true
    }

    /** Override this in order to implement any subclass-specific checks. **/
    @Throws(CommandException::class)
    public open suspend fun runChecks(event: E): Boolean =
        runStandardChecks(event)



    /** Checks whether the bot has the specified required permissions, throwing if it doesn't. **/
    @Throws(CommandException::class)
    public open suspend fun checkBotPerms(context: C) {
        if (context.guild != null) {
            val perms = (context.channel.asChannel() as GuildChannel)
                .permissionsForMember(kord.selfId)

            val missingPerms = requiredPerms.filter { !perms.contains(it) }

            if (missingPerms.isNotEmpty()) {
                throw CommandException(
                    context.translate(
                        "commands.error.missingBotPermissions",
                        null,

                        replacements = arrayOf(
                            missingPerms.map { it.translate(context.getLocale()) }.joinToString(", ")
                        )
                    )
                )
            }
        }
    }

    /** Override this to implement your component's calling logic. Check subtypes for examples! **/
    public abstract suspend fun call(event: E)
}