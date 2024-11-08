/*
 * Copyrighted (Kord Extensions, 2024). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

package dev.kordex.core.commands.converters.impl

import dev.kord.core.entity.interaction.OptionValue
import dev.kord.core.entity.interaction.StringOptionValue
import dev.kord.rest.builder.interaction.StringChoiceBuilder
import dev.kordex.core.DiscordRelayedException
import dev.kordex.core.annotations.converters.Converter
import dev.kordex.core.annotations.converters.ConverterType
import dev.kordex.core.commands.Argument
import dev.kordex.core.commands.CommandContext
import dev.kordex.core.commands.OptionWrapper
import dev.kordex.core.commands.converters.SingleConverter
import dev.kordex.core.commands.converters.Validator
import dev.kordex.core.commands.wrapOption
import dev.kordex.core.i18n.generated.CoreTranslations
import dev.kordex.core.i18n.types.Key
import dev.kordex.core.i18n.withContext
import dev.kordex.core.serialization.deserializeRaw
import dev.kordex.parser.StringParser
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.KSerializer

/**
 * Converter that attempts to deserialize the argument to the given type.
 */
@Converter(
	"serialized",

	imports = [
		"kotlinx.serialization.KSerializer",
		"kotlinx.serialization.serializer",
	],

	types = [ConverterType.DEFAULTING, ConverterType.LIST, ConverterType.OPTIONAL, ConverterType.SINGLE],

	builderGeneric = "T: Any",
	builderConstructorArguments = ["public val serializer: KSerializer<T>"],
	builderFields = ["public lateinit var typeName: Key"],

	functionGeneric = "T: Any",
	functionBuilderArguments = ["serializer<T>()"],
)
public class SerializedConverter<T : Any>(
	typeName: Key,
	public val serializer: KSerializer<T>,

	override var validator: Validator<T> = null,
) : SingleConverter<T>() {
	private val logger = KotlinLogging.logger { }

	override val signatureType: Key = typeName

	override suspend fun parse(parser: StringParser?, context: CommandContext, named: String?): Boolean {
		val arg: String = named ?: parser?.parseNext()?.data ?: return false

		@Suppress("TooGenericExceptionCaught")
		try {
			this.parsed = serializer.deserializeRaw(arg, context.getLocale())
		} catch (e: Exception) {
			logger.error(e) { "Failed to deserialize value: $arg" }

			throw DiscordRelayedException(
				CoreTranslations.Converters.Serialized.error
					.withContext(context)
					.withNamedPlaceholders(
						"type" to signatureType
					)
			)
		}

		return true
	}

	override suspend fun toSlashOption(arg: Argument<*>): OptionWrapper<StringChoiceBuilder> =
		wrapOption(arg.displayName, arg.description) {
			required = true
		}

	override suspend fun parseOption(context: CommandContext, option: OptionValue<*>): Boolean {
		val optionValue = (option as? StringOptionValue)?.value ?: return false

		@Suppress("TooGenericExceptionCaught")
		try {
			this.parsed = serializer.deserializeRaw(optionValue, context.getLocale())
		} catch (e: Exception) {
			logger.error(e) { "Failed to deserialize value: $optionValue" }

			throw DiscordRelayedException(
				CoreTranslations.Converters.Serialized.error
					.withContext(context)
					.withNamedPlaceholders(
						"type" to signatureType
					)
			)
		}

		return true
	}
}
