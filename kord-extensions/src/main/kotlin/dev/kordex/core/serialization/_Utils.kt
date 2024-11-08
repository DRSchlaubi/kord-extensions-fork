/*
 * Copyrighted (Kord Extensions, 2024). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

package dev.kordex.core.serialization

import kotlinx.serialization.KSerializer
import java.util.Locale

public fun <T : Any> KSerializer<T>.deserializeRaw(string: String, locale: Locale? = null): T {
	val decoder = StringDecoder(string, locale)

	return deserialize(decoder)
}
