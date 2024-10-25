/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.commands.application

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.MutableStringKeyedMap
import dev.kord.common.entity.ApplicationCommandType
import dev.kord.common.entity.EntryPointCommandHandlerType

public open class PrimaryEntryPointCommand(extension: Extension) : ApplicationCommand<Nothing>(extension) {

	public lateinit var handler: EntryPointCommandHandlerType

	/** Command description, as displayed on Discord. **/
	public open lateinit var description: String

	/**
	 * A [Localized] version of [description].
	 */
	public val localizedDescription: Localized<String> by lazy { localize(description) }

	override val type: ApplicationCommandType = ApplicationCommandType.PrimaryEntryPoint

	override suspend fun call(
		event: Nothing,
		cache: MutableStringKeyedMap<Any>,
	): Nothing = error("Primary entry point commands can't be called")
}
