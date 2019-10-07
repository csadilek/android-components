/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.concept.engine.webextension

import mozilla.components.concept.engine.EngineSession
import java.lang.UnsupportedOperationException

/**
 * Provides delegation for when a extension opens or closes a tab.
 */
interface WebExtensionsTabsDelegate {
    fun onNewTab(webExtension: WebExtension?, url: String, engineSession: EngineSession) = Unit
    fun onCloseTab(webExtension: WebExtension?, engineSession: EngineSession): Unit =
        throw UnsupportedOperationException("WebExtensionsTabsDelegate.onCloseTab is not available in this engine")
}