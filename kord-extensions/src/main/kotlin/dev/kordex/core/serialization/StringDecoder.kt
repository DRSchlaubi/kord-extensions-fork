/*
 * Copyrighted (Kord Extensions, 2024). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

package dev.kordex.core.serialization

import dev.kordex.core.utils.parseBoolean
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import java.util.Locale

@OptIn(ExperimentalSerializationApi::class)
public class StringDecoder(
	private val string: String,
	private val locale: Locale?,
) : AbstractDecoder() {
	override val serializersModule: SerializersModule = EmptySerializersModule()

	override fun decodeElementIndex(descriptor: SerialDescriptor): Int = 0
	override fun decodeInline(descriptor: SerialDescriptor): Decoder = this

	override fun decodeBoolean(): Boolean = if (locale != null) {
		string.parseBoolean(locale) == true
	} else {
		string.toBoolean()
	}

	override fun decodeByte(): Byte = string.toByte()
	override fun decodeChar(): Char = string.first()
	override fun decodeDouble(): Double = string.toDouble()
	override fun decodeEnum(enumDescriptor: SerialDescriptor): Int = string.toInt()
	override fun decodeFloat(): Float = string.toFloat()
	override fun decodeInt(): Int = string.toInt()
	override fun decodeLong(): Long = string.toLong()
	override fun decodeNotNullMark(): Boolean = string == "null"
	override fun decodeNull(): Nothing? = null
	override fun decodeShort(): Short = string.toShort()
	override fun decodeString(): String = string
}
