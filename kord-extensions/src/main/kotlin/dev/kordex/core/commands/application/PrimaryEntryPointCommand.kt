/*
 * Copyrighted (Kord Extensions, 2024). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

package dev.kordex.core.commands.application

import dev.kord.common.entity.ApplicationCommandType
import dev.kord.common.entity.PrimaryEntryPointCommandHandlerType
import dev.kordex.core.extensions.Extension
import dev.kordex.core.i18n.types.Key
import dev.kordex.core.utils.MutableStringKeyedMap

public open class PrimaryEntryPointCommand(extension: Extension) : ApplicationCommand<Nothing>(extension) {

	public lateinit var handler: PrimaryEntryPointCommandHandlerType

	/** Command description, as displayed on Discord. **/
	public open lateinit var description: Key

	/**
	 * A [Localized] version of [description].
	 */
	public val localizedDescription: Localized<String> by lazy { localize(description) }

	override val type: ApplicationCommandType = ApplicationCommandType.PrimaryEntryPoint

	override suspend fun call(
		event: Nothing,
		cache: MutableStringKeyedMap<Any>,
	): Nothing = error("Primary entry point commands can't be called")

	override fun validate() {
		require(this::handler.isInitialized) { "Handler needs to be set" }
		require(this::description.isInitialized) { "Handler description to be set" }
	}
}
