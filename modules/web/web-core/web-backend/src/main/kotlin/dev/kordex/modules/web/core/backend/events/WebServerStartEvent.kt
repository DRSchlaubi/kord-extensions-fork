/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.kordex.modules.web.core.backend.events

import dev.kordex.core.events.KordExEvent
import dev.kordex.modules.web.core.backend.server.WebServer

public class WebServerStartEvent(public val server: WebServer) : KordExEvent