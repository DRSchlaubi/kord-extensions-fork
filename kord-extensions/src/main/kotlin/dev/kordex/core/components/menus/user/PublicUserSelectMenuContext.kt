/*
 * Copyrighted (Kord Extensions, 2024). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

package dev.kordex.core.components.menus.user

import dev.kord.core.behavior.interaction.response.PublicMessageInteractionResponseBehavior
import dev.kord.core.event.interaction.SelectMenuInteractionCreateEvent
import dev.kordex.core.components.forms.ModalForm
import dev.kordex.core.types.PublicInteractionContext
import dev.kordex.core.utils.MutableStringKeyedMap

/** Class representing the execution context for a public-only user select (dropdown) menu. **/
public class PublicUserSelectMenuContext<M : ModalForm>(
	override val component: PublicUserSelectMenu<M>,
	override val event: SelectMenuInteractionCreateEvent,
	override val interactionResponse: PublicMessageInteractionResponseBehavior,
	cache: MutableStringKeyedMap<Any>,
) : UserSelectMenuContext(component, event, cache), PublicInteractionContext
