/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.kordex.core.commands.converters.impl

import dev.kord.core.entity.Attachment
import dev.kord.core.entity.interaction.AttachmentOptionValue
import dev.kord.core.entity.interaction.OptionValue
import dev.kord.rest.builder.interaction.AttachmentBuilder
import dev.kord.rest.builder.interaction.OptionsBuilder
import dev.kordex.core.DiscordRelayedException
import dev.kordex.core.annotations.converters.Converter
import dev.kordex.core.annotations.converters.ConverterType
import dev.kordex.core.commands.Argument
import dev.kordex.core.commands.CommandContext
import dev.kordex.core.commands.converters.SingleConverter
import dev.kordex.core.commands.converters.Validator
import dev.kordex.core.i18n.DEFAULT_KORDEX_BUNDLE
import dev.kordex.parser.StringParser

/**
 * Argument converter for Discord attachments.
 *
 * This converter can only be used in slash commands.
 */
@Converter(
	"attachment",

	types = [ConverterType.OPTIONAL, ConverterType.SINGLE],
)
public class AttachmentConverter(
	override var validator: Validator<Attachment> = null,
) : SingleConverter<Attachment>() {
	override val signatureTypeString: String = "converters.attachment.signatureType"
	override val bundle: String = DEFAULT_KORDEX_BUNDLE

	override suspend fun parse(parser: StringParser?, context: CommandContext, named: String?): Boolean =
		throw DiscordRelayedException(context.translate("converters.attachment.error.slashCommandsOnly"))

	override suspend fun toSlashOption(arg: Argument<*>): OptionsBuilder =
		AttachmentBuilder(arg.displayName, arg.description).apply { required = true }

	override suspend fun parseOption(context: CommandContext, option: OptionValue<*>): Boolean {
		val optionValue = (option as? AttachmentOptionValue)?.resolvedObject ?: return false
		this.parsed = optionValue

		return true
	}
}