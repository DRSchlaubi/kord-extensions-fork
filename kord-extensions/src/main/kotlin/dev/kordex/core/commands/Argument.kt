/*
 * Copyrighted (Kord Extensions, 2024). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

package dev.kordex.core.commands

import dev.kordex.core.commands.converters.Converter
import dev.kordex.core.i18n.TranslationsProvider

/**
 * Data class representing a single argument.
 *
 * @param displayName Name shown on Discord in help messages, and used for keyword arguments.
 * @param description Short description explaining what the argument does.
 * @param converter Argument converter to use for this argument.
 */
public data class Argument<T : Any?>(
	val displayName: String,
	val description: String,
	val converter: Converter<T, *, *, *>,
) {
	init {
		converter.argumentObj = this
	}
}

internal fun Argument<*>.getDefaultTranslatedDisplayName(provider: TranslationsProvider, command: Command): String =
	provider.translate(
		key = displayName,
		bundleName = command.resolvedBundle ?: converter.bundle,
		locale = provider.defaultLocale
	)
