/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.utils

import com.kotlindiscord.kord.extensions.annotations.DoNotChain
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.RoleBehavior
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Member
import dev.kord.core.entity.Role
import dev.kord.rest.builder.member.MemberModifyBuilder
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.*
import kotlin.time.Duration

/** A more sensible name than `communicationDisabledUntil`. **/
public val Member.timeoutUntil: Instant?
    inline get() = this.communicationDisabledUntil

/** A more sensible name than `communicationDisabledUntil`. **/
public var MemberModifyBuilder.timeoutUntil: Instant?
    inline get() = this.communicationDisabledUntil
    inline set(value) {
        this.communicationDisabledUntil = value
    }

/**
 * Check if the user has the given [Role].
 *
 * @param role Role to check for
 * @return true if the user has the given role, false otherwise
 */
public fun Member.hasRole(role: RoleBehavior): Boolean = roleIds.contains(role.id)

/**
 * Check if the user has all of the given roles.
 *
 * @param roles Roles to check for.
 * @return `true` if the user has all of the given roles, `false` otherwise.
 */
public fun Member.hasRoles(vararg roles: RoleBehavior): Boolean = hasRoles(roles.toList())

/**
 * Check if the user has all of the given roles.
 *
 * @param roles Roles to check for.
 * @return `true` if the user has all of the given roles, `false` otherwise.
 */
public fun Member.hasRoles(roles: Collection<RoleBehavior>): Boolean =
    if (roles.isEmpty()) {
        true
    } else {
        this.roleIds.containsAll(roles.map { it.id })
    }

/**
 * Convenience function to retrieve a user's top [Role].
 *
 * @receiver The [Member] to get the top role for
 * @return The user's top role, or `null` if they have no roles
 */
public suspend fun Member.getTopRole(): Role? = roles.toList().maxOrNull()

/**
 * Convenience function to check whether a guild member has a permission.
 *
 * This function only checks for permissions based on roles, and does not deal with channel overrides. It will
 * always return `true` if the member has the `Administrator` permission in one of their roles.
 *
 * @receiver The [Member] check permissions for for
 * @return Whether the [Member] has the given permission, or the Administrator permission
 */
public suspend fun Member.hasPermission(perm: Permission): Boolean = perm in getPermissions()

/**
 * Convenience function to check whether a guild member has all of the given permissions.
 *
 * This function only checks for permissions based on roles, and does not deal with channel overrides. It will
 * always return `true` if the member has the `Administrator` permission.
 *
 * @receiver The [Member] check permissions for
 * @param perms The permissions to check for
 *
 * @return `true` if the collection is empty, or the [Member] has all of the given permissions, `false` otherwise
 */
public suspend inline fun Member.hasPermissions(vararg perms: Permission): Boolean = hasPermissions(perms.toList())

/**
 * Convenience function to check whether a guild member has all of the given permissions.
 *
 * This function only checks for permissions based on roles, and does not deal with channel overrides. It will
 * always return `true` if the member has the `Administrator` permission.
 *
 * @receiver The [Member] check permissions for
 * @param perms The permissions to check for
 *
 * @return `true` if the collection is empty, or the [Member] has all of the given permissions, `false` otherwise
 */
public suspend fun Member.hasPermissions(perms: Collection<Permission>): Boolean =
    if (perms.isEmpty()) {
        true
    } else {
        val permissions = getPermissions()

        perms.all { it in permissions }
    }

/**
 * Checks if this [Member] can interact (delete/edit/assign/..) with the specified [Role].
 *
 * This checks if the [Member] has any role which is higher in hierarchy than [Role].
 * The logic also accounts for [Guild] ownership.
 *
 * Throws an [IllegalArgumentException] if the role is from a different guild.
 */
public suspend fun Member.canInteract(role: Role): Boolean {
    val guild = getGuild()

    if (guild.ownerId == this.id) return true

    val highestRole = getTopRole() ?: guild.getEveryoneRole()
    return highestRole.canInteract(role)
}

/**
 * Checks if this [Member] can interact (kick/ban/..) with another [Member]
 *
 * This checks if the [Member] has any role which is higher in hierarchy than all [Role]s of the
 * specified [Member]
 * The logic also accounts for [Guild] ownership
 *
 * Throws an [IllegalArgumentException] if the member is from a different guild.
 */
public suspend fun Member.canInteract(member: Member): Boolean {
    val guild = getGuild()

    if (isOwner()) return true
    if (member.isOwner()) return false

    val highestRole = getTopRole() ?: guild.getEveryoneRole()
    val otherHighestRole = member.getTopRole() ?: guild.getEveryoneRole()

    return highestRole.canInteract(otherHighestRole)
}

/**
 * Convenience function to remove the timeout from a member, skipping the [edit] DSL.
 */
@DoNotChain
public suspend fun Member.removeTimeout(reason: String? = null): Member =
    edit {
        timeoutUntil = null

        this.reason = reason
    }

/**
 * Convenience function to time out a member using a [Duration], skipping the [edit] DSL.
 */
@DoNotChain
public suspend fun Member.timeout(until: Duration, reason: String? = null): Member =
    edit {
        timeoutUntil = Clock.System.now() + until

        this.reason = reason
    }

/**
 * Convenience function to time out a member using a [DateTimePeriod] and timezone, skipping the [edit] DSL.
 *
 * This will use [TimeZone.UTC] by default. You can provide another if you really need to.
 */
@DoNotChain
public suspend fun Member.timeout(
    until: DateTimePeriod,
    timezone: TimeZone = TimeZone.UTC,
    reason: String? = null
): Member =
    edit {
        timeoutUntil = Clock.System.now().plus(until, timezone)

        this.reason = reason
    }

/**
 * Convenience function to server mute a member, skipping the [edit] DSL.
 */
@DoNotChain
public suspend fun Member.mute(reason: String? = null): Member = edit {
    muted = true

    this.reason = reason
}

/**
 * Convenience function to undo a server mute for a member, skipping the [edit] DSL.
 */
@DoNotChain
public suspend fun Member.unMute(reason: String? = null): Member = edit {
    muted = false

    this.reason = reason
}

/**
 * Convenience function to server deafen a member, skipping the [edit] DSL.
 */
@DoNotChain
public suspend fun Member.deafen(reason: String? = null): Member = edit {
    deafened = true

    this.reason = reason
}

/**
 * Convenience function to undo a server deafen for a member, skipping the [edit] DSL.
 */
@DoNotChain
public suspend fun Member.unDeafen(reason: String? = null): Member = edit {
    deafened = false

    this.reason = reason
}

/**
 * Convenience function to set a member's nickname, skipping the [edit] DSL.
 *
 * You can also provide `null` to remove a nickname - [removeNickname] is a wrapper for this function that does
 * exactly that.
 */
@DoNotChain
public suspend fun Member.setNickname(nickname: String?, reason: String? = null): Member = edit {
    this.nickname = nickname

    this.reason = reason
}

/**
 * Convenience function to remove a member's nickname, skipping the [edit] DSL.
 *
 * This will simply call [setNickname] with a `nickname` of `null`.
 */
@DoNotChain
public suspend fun Member.removeNickname(reason: String? = null): Member =
    setNickname(null, reason)
