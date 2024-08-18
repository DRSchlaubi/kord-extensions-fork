/*
 * Copyrighted (Kord Extensions, 2024). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

package dev.kordex.modules.func.mappings.utils

import dev.kord.core.entity.interaction.AutoCompleteInteraction
import dev.kordex.core.DiscordRelayedException
import dev.kordex.core.commands.converters.builders.ConverterBuilder
import dev.kordex.core.utils.FilterStrategy
import dev.kordex.core.utils.suggestStringMap
import me.shedaniel.linkie.Namespace
import me.shedaniel.linkie.Namespaces
import me.shedaniel.linkie.namespaces.MojangHashedNamespace
import me.shedaniel.linkie.namespaces.QuiltMappingsNamespace

@PublishedApi
internal const val MAX_RESULTS = 25 // set by Discord's API

/**
 * Define autocomplete for a list of strings.
 * [versions] is a supplier of the list of strings to autocomplete from.
 */
inline fun <reified T> ConverterBuilder<T>.autocompleteVersions(
	crossinline versions: AutoCompleteInteraction.() -> List<String>,
) {
	autoComplete {
		val partiallyTyped = focusedOption.value as? String

		val map = versions()
			.filter { it.startsWith(partiallyTyped ?: "") }
			.take(MAX_RESULTS)
			.associateBy { it }

		suggestStringMap(map, FilterStrategy.Contains)
	}
}

/**
 * Turn this [String] into a [Namespace].
 */
@Suppress("TooGenericExceptionCaught") // sorry, but that's what Linkie throws
fun String.toNamespace(): Namespace = when (this) {
	"hashed", "hashed_mojang", "hashed-mojang" -> MojangHashedNamespace
	"quilt-mappings", "quilt" -> QuiltMappingsNamespace
	else -> try {
		Namespaces[this]
	} catch (e: NullPointerException) {
		throw DiscordRelayedException("Invalid namespace: $this")
	}
}
