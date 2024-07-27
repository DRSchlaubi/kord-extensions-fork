/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.kordex.parser.tokens

/**
 * Data class representing a single positional argument token.
 *
 * @param data Argument data
 */
public data class PositionalArgumentToken(
	override val data: String,
) : Token<String>
