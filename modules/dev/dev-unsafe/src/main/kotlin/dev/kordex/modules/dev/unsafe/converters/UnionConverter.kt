/*
 * Copyrighted (Kord Extensions, 2024). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

@file:OptIn(
	ConverterToOptional::class,
	UnexpectedFunctionBehaviour::class
)
@file:Suppress("StringLiteralDuplication")

package dev.kordex.modules.dev.unsafe.converters

import dev.kord.core.entity.interaction.OptionValue
import dev.kord.rest.builder.interaction.OptionsBuilder
import dev.kord.rest.builder.interaction.StringChoiceBuilder
import dev.kordex.core.DiscordRelayedException
import dev.kordex.core.annotations.UnexpectedFunctionBehaviour
import dev.kordex.core.commands.Argument
import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.CommandContext
import dev.kordex.core.commands.converters.*
import dev.kordex.core.i18n.DEFAULT_KORDEX_BUNDLE
import dev.kordex.core.i18n.TranslationsProvider
import dev.kordex.modules.dev.unsafe.annotations.UnsafeAPI
import dev.kordex.parser.StringParser
import org.koin.core.component.inject

@UnsafeAPI
private typealias GenericConverter = Converter<*, *, *, *>

/**
 * Experimental converter allowing for combining other converters together, with the caveat of type erasure.
 *
 * This converter does not support optional or defaulting converters.
 */
@UnsafeAPI
public class UnionConverter(
	private val converters: Collection<GenericConverter>,

	typeName: String? = null,
	shouldThrow: Boolean = false,

	override val bundle: String? = DEFAULT_KORDEX_BUNDLE,
	override var validator: Validator<Any> = null,
) : CoalescingConverter<Any>(shouldThrow) {
	private val translations: TranslationsProvider by inject()

	override val signatureType: String = typeName ?: converters.joinToString(" | ") {
		translations.translate(key = it.signatureType, bundleName = it.bundle)
	}

	/** @suppress Internal validation function. **/
	public fun validateUnion() {
		val allConverters: MutableList<GenericConverter> = converters.toMutableList()

		allConverters.removeLast()  // The last converter can be any type.

		for (converter in allConverters) {
			when (converter) {
				is DefaultingConverter<*>, is DefaultingCoalescingConverter<*> -> error(
					"Invalid converter: $converter - " +
						"Defaulting converters are only supported by union converters if they're the last " +
						"provided converter."
				)

				is OptionalConverter<*>, is OptionalCoalescingConverter<*> -> error(
					"Invalid converter: $converter - " +
						"Optional converters are only supported by union converters if they're the last " +
						"provided converter."
				)
			}
		}
	}

	override suspend fun parse(parser: StringParser?, context: CommandContext, named: List<String>?): Int {
		for (converter in converters) {
			@Suppress("TooGenericExceptionCaught")
			when (converter) {
				is SingleConverter<*> -> try {
					val result: Boolean = converter.parse(parser, context, named?.first())

					if (result) {
						converter.parseSuccess = true
						this.parsed = converter.parsed

						return 1
					}
				} catch (t: Throwable) {
					if (shouldThrow) throw t
				}

				is DefaultingConverter<*> -> try {
					val result: Boolean = converter.parse(parser, context, named?.first())

					if (result) {
						converter.parseSuccess = true
						this.parsed = converter.parsed

						return 1
					}
				} catch (t: Throwable) {
					if (shouldThrow) throw t
				}

				is OptionalConverter<*> -> try {
					val result: Boolean = converter.parse(parser, context, named?.first())

					if (result && converter.parsed != null) {
						converter.parseSuccess = true
						this.parsed = converter.parsed!!

						return 1
					}
				} catch (t: Throwable) {
					if (shouldThrow) throw t
				}

				is ListConverter<*> -> try {
					val result: Int = converter.parse(parser, context, named)

					if (result > 0) {
						converter.parseSuccess = true
						this.parsed = converter.parsed

						return result
					}
				} catch (t: Throwable) {
					if (shouldThrow) throw t
				}

				is CoalescingConverter<*> -> try {
					val result: Int = converter.parse(parser, context, named)

					if (result > 0) {
						converter.parseSuccess = true
						this.parsed = converter.parsed

						return result
					}
				} catch (t: Throwable) {
					if (shouldThrow) throw t
				}

				is DefaultingCoalescingConverter<*> -> try {
					val result: Int = converter.parse(parser, context, named)

					if (result > 0) {
						converter.parseSuccess = true
						this.parsed = converter.parsed

						return result
					}
				} catch (t: Throwable) {
					if (shouldThrow) throw t
				}

				is OptionalCoalescingConverter<*> -> try {
					val result: Int = converter.parse(parser, context, named)

					if (result > 0 && converter.parsed != null) {
						converter.parseSuccess = true
						this.parsed = converter.parsed!!

						return result
					}
				} catch (t: Throwable) {
					if (shouldThrow) throw t
				}

				else -> throw DiscordRelayedException(
					context.translate(
						"converters.union.error.unknownConverterType",
						replacements = arrayOf(converter)
					)
				)
			}
		}

		return 0
	}

	override suspend fun toSlashOption(arg: Argument<*>): OptionsBuilder =
		StringChoiceBuilder(arg.displayName, arg.description).apply { required = true }

	override suspend fun parseOption(context: CommandContext, option: OptionValue<*>): Boolean {
		for (converter in converters) {
			@Suppress("TooGenericExceptionCaught")
			when (converter) {
				is SingleConverter<*> -> try {
					val result: Boolean = converter.parseOption(context, option)

					if (result) {
						converter.parseSuccess = true
						this.parsed = converter.parsed

						return true
					}
				} catch (t: Throwable) {
					if (shouldThrow) throw t
				}

				is DefaultingConverter<*> -> try {
					val result: Boolean = converter.parseOption(context, option)

					if (result) {
						converter.parseSuccess = true
						this.parsed = converter.parsed

						return true
					}
				} catch (t: Throwable) {
					if (shouldThrow) throw t
				}

				is OptionalConverter<*> -> try {
					val result: Boolean = converter.parseOption(context, option)

					if (result && converter.parsed != null) {
						converter.parseSuccess = true
						this.parsed = converter.parsed!!

						return true
					}
				} catch (t: Throwable) {
					if (shouldThrow) throw t
				}

				is ListConverter<*> -> throw DiscordRelayedException(
					context.translate(
						"converters.union.error.unknownConverterType",
						replacements = arrayOf(converter)
					)
				)

				is CoalescingConverter<*> -> try {
					val result: Boolean = converter.parseOption(context, option)

					if (result) {
						converter.parseSuccess = true
						this.parsed = converter.parsed

						return true
					}
				} catch (t: Throwable) {
					if (shouldThrow) throw t
				}

				is DefaultingCoalescingConverter<*> -> try {
					val result: Boolean = converter.parseOption(context, option)

					if (result) {
						converter.parseSuccess = true
						this.parsed = converter.parsed

						return true
					}
				} catch (t: Throwable) {
					if (shouldThrow) throw t
				}

				is OptionalCoalescingConverter<*> -> try {
					val result: Boolean = converter.parseOption(context, option)

					if (result && converter.parsed != null) {
						converter.parseSuccess = true
						this.parsed = converter.parsed!!

						return result
					}
				} catch (t: Throwable) {
					if (shouldThrow) throw t
				}

				else -> throw DiscordRelayedException(
					context.translate(
						"converters.union.error.unknownConverterType",
						replacements = arrayOf(converter)
					)
				)
			}
		}

		return false
	}
}

/**
 * Create a union converter, for combining other converters into a single argument - with the caveat of type erasure.
 *
 * This function will automatically remove converters if they were previously registered, so you can use pass it the
 * results of the usual extension functions.
 *
 * @see UnionConverter
 */
@UnsafeAPI
public fun Arguments.union(
	displayName: String,
	description: String,
	typeName: String? = null,
	shouldThrow: Boolean = false,
	vararg converters: GenericConverter,
	bundle: String? = null,
	validator: Validator<Any> = null,
): UnionConverter {
	val converter: UnionConverter = UnionConverter(converters.toList(), typeName, shouldThrow, bundle, validator)

	converter.validateUnion()

	this.args.toList().forEach {
		if (it.converter in converters) {
			this.args.remove(it)
		}
	}

	arg(displayName, description, converter)

	return converter
}

/**
 * Create an optional union converter, for combining other converters into a single argument - with the caveat of
 * type erasure.
 *
 * This function will automatically remove converters if they were previously registered, so you can use pass it the
 * results of the usual extension functions.
 *
 * @see UnionConverter
 */
@UnsafeAPI
public fun Arguments.optionalUnion(
	displayName: String,
	description: String,
	typeName: String? = null,
	shouldThrow: Boolean = false,
	vararg converters: GenericConverter,
	bundle: String? = null,
	validator: Validator<Any?> = null,
): OptionalCoalescingConverter<Any> {
	val converter: UnionConverter = UnionConverter(converters.toList(), typeName, shouldThrow, bundle)

	converter.validateUnion()

	this.args.toList().forEach {
		if (it.converter in converters) {
			this.args.remove(it)
		}
	}

	val optionalConverter: OptionalCoalescingConverter<Any> = converter.toOptional(nestedValidator = validator)

	arg(displayName, description, optionalConverter)

	return optionalConverter
}
