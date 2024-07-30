/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.kordex.test.bot.extensions

import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.application.slash.publicSubCommand
import dev.kordex.core.commands.converters.impl.string
import dev.kordex.core.components.components
import dev.kordex.core.components.forms.ModalForm
import dev.kordex.core.components.forms.widgets.LineTextWidget
import dev.kordex.core.components.forms.widgets.ParagraphTextWidget
import dev.kordex.core.components.publicButton
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.publicMessageCommand
import dev.kordex.core.extensions.publicSlashCommand
import dev.kordex.core.extensions.publicUserCommand

public class ModalTestExtension : Extension() {
	override val name: String = "modals"
	override val bundle: String = "test.strings"

	@Suppress("StringLiteralDuplication")
	override suspend fun setup() {
		publicUserCommand(::Modal) {
			name = "Modal"

			action { modal ->
				respond {
					content = buildString {
						if (modal == null) {
							append("**No modal found!**")

							return@buildString
						}

						append("**Line:** `")
						appendLine(modal.line.value)
						append("`")
						appendLine()

						appendLine("**Paragraph:** ```")
						appendLine(modal.paragraph.value)
						append("```")
						appendLine()
					}
				}
			}
		}

		publicMessageCommand(::Modal) {
			name = "Modal"

			action { modal ->
				respond {
					content = buildString {
						if (modal == null) {
							append("**No modal found!**")

							return@buildString
						}

						append("**Line:** `")
						appendLine(modal.line.value)
						append("`")
						appendLine()

						appendLine("**Paragraph:** ```")
						appendLine(modal.paragraph.value)
						append("```")
						appendLine()
					}
				}
			}
		}

		publicSlashCommand {
			name = "modals"
			description = "Modal testing commands"

			publicSubCommand {
				name = "button"
				description = "Test a modal response to a button"

				action {
					respond {
						components {
							publicButton(::Modal) {
								bundle = "test.strings"
								label = "Modal!"

								action { modal ->
									respond {
										content = buildString {
											if (modal == null) {
												append("**No modal found!**")

												return@buildString
											}

											append("**Line:** `")
											appendLine(modal.line.value)
											append("`")
											appendLine()

											appendLine("**Paragraph:** ```")
											appendLine(modal.paragraph.value)
											append("```")
											appendLine()
										}
									}
								}
							}
						}
					}
				}
			}

			publicSubCommand(::Args, ::Modal) {
				name = "command"
				description = "Test a modal response to a command"

				action { modal ->
					respond {
						content = buildString {
							append("**Argument:** `")
							appendLine(arguments.str)
							append("`")
							appendLine()

							if (modal == null) {
								append("**No modal found!**")

								return@buildString
							}

							append("**Line:** `")
							appendLine(modal.line.value)
							append("`")
							appendLine()

							appendLine("**Paragraph:** ```")
							appendLine(modal.paragraph.value)
							append("```")
							appendLine()
						}
					}
				}
			}
		}
	}

	public inner class Args : Arguments() {
		public val str: String by string {
			name = "string"
			description = "A string argument"
		}
	}

	public inner class Modal : ModalForm() {
		override var title: String = "modal.title"

		public val line: LineTextWidget = lineText {
			label = "modal.line"
			placeholder = "modal.line.placeholder"
		}

		public val paragraph: ParagraphTextWidget = paragraphText {
			label = "modal.paragraph"
			placeholder = "modal.paragraph.placeholder"
		}
	}
}