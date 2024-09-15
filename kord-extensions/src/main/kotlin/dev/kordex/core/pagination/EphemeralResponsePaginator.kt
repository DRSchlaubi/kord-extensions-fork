/*
 * Copyrighted (Kord Extensions, 2024). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

package dev.kordex.core.pagination

import dev.kord.core.behavior.UserBehavior
import dev.kord.core.behavior.interaction.response.EphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.edit
import dev.kord.core.entity.ReactionEmoji
import dev.kordex.core.i18n.types.Bundle
import dev.kordex.core.pagination.builders.PageTransitionCallback
import dev.kordex.core.pagination.builders.PaginatorBuilder
import dev.kordex.core.pagination.pages.Pages
import java.util.*

/**
 * Class representing a button-based paginator that operates by editing the given ephemeral interaction response.
 *
 * @param interaction Interaction response behaviour to work with.
 */
public class EphemeralResponsePaginator(
	pages: Pages,
	owner: UserBehavior? = null,
	chunkedPages: Int = 1,
	timeoutSeconds: Long? = null,
	switchEmoji: ReactionEmoji = if (pages.groups.size == 2) EXPAND_EMOJI else SWITCH_EMOJI,
	mutator: PageTransitionCallback? = null,
	bundle: Bundle? = null,
	locale: Locale? = null,

	public val interaction: EphemeralMessageInteractionResponseBehavior,
) : BaseButtonPaginator(pages, chunkedPages, owner, timeoutSeconds, true, switchEmoji, mutator, bundle, locale) {
	/** Whether this paginator has been set up for the first time. **/
	public var isSetup: Boolean = false

	override suspend fun send() {
		if (!isSetup) {
			isSetup = true

			setup()
		} else {
			updateButtons()
		}

		interaction.edit {
			applyPage()

			with(this@EphemeralResponsePaginator.components) {
				this@edit.applyToMessage()
			}
		}
	}

	override suspend fun destroy() {
		if (!active) {
			return
		}

		active = false

		interaction.edit {
			applyPage()

			this.components = mutableListOf()
		}

		super.destroy()
	}
}

/** Convenience function for creating an interaction button paginator from a paginator builder. **/
@Suppress("FunctionNaming")  // Factory function
public fun EphemeralResponsePaginator(
	builder: PaginatorBuilder,
	interaction: EphemeralMessageInteractionResponseBehavior,
): EphemeralResponsePaginator = EphemeralResponsePaginator(
	pages = builder.pages,
	chunkedPages = builder.chunkedPages,
	owner = builder.owner,
	timeoutSeconds = builder.timeoutSeconds,
	mutator = builder.mutator,
	bundle = builder.bundle,
	locale = builder.locale,
	interaction = interaction,

	switchEmoji = builder.switchEmoji ?: if (builder.pages.groups.size == 2) EXPAND_EMOJI else SWITCH_EMOJI,
)
