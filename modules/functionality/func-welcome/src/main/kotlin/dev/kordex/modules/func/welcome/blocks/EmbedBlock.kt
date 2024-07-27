/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.kordex.modules.func.welcome.blocks

import dev.kord.core.Kord
import dev.kord.core.cache.data.EmbedData
import dev.kord.core.entity.Embed
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import dev.kord.rest.builder.message.embed
import dev.kord.rest.builder.message.modify.MessageModifyBuilder
import dev.kordex.core.koin.KordExKoinComponent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.core.component.inject

@Suppress("MagicNumber")
@Serializable
@SerialName("embed")
data class EmbedBlock(
	val embeds: List<EmbedData>,
	val text: String? = null,
) : Block(), KordExKoinComponent {
	val kord: Kord by inject()

	init {
		if (embeds.isEmpty() || embeds.size > 10) {
			error("Must provide up to 10 embeds")
		}
	}

	override suspend fun create(builder: MessageCreateBuilder) {
		builder.content = text

		embeds.forEach { embed ->
			builder.embed {
				Embed(embed, kord).apply(this)
			}
		}
	}

	override suspend fun edit(builder: MessageModifyBuilder) {
		builder.content = text
		builder.components = mutableListOf()

		embeds.forEach { embed ->
			builder.embed {
				Embed(embed, kord).apply(this)
			}
		}
	}
}
