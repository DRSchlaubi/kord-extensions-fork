/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.kordex.modules.func.welcome.blocks

import dev.kord.common.Color
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import dev.kord.rest.builder.message.embed
import dev.kord.rest.builder.message.modify.MessageModifyBuilder
import dev.kordex.core.DISCORD_BLURPLE
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("DataClassContainsFunctions")
@Serializable
@SerialName("links")
data class LinksBlock(
	val title: String,
	val links: Map<String, String>,
	val text: String? = null,
	val color: Color = DISCORD_BLURPLE,
	val description: String? = null,
	val template: String = "**»** [{TEXT}]({URL})",
) : Block() {
	init {
		if (links.isEmpty()) {
			error("Must provide at least one link")
		}
	}

	private fun buildDescription() = buildString {
		if (description != null) {
			append(description)

			appendLine()
			appendLine()
		}

		links.forEach { (text, url) ->
			appendLine(
				template
					.replace("{TEXT}", text)
					.replace("{URL}", url)
			)
		}
	}

	override suspend fun create(builder: MessageCreateBuilder) {
		builder.content = text

		builder.embed {
			title = this@LinksBlock.title
			color = this@LinksBlock.color

			description = buildDescription()
		}
	}

	override suspend fun edit(builder: MessageModifyBuilder) {
		builder.content = text
		builder.components = mutableListOf()

		builder.embed {
			title = this@LinksBlock.title
			color = this@LinksBlock.color

			description = buildDescription()
		}
	}
}
