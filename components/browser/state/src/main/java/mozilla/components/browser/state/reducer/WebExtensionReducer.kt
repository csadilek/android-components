/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.state.reducer

import mozilla.components.browser.state.action.WebExtensionAction
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.WebExtensionState

internal object WebExtensionReducer {
    fun reduce(state: BrowserState, action: WebExtensionAction): BrowserState {
        return when (action) {
            is WebExtensionAction.InstallWebExtension -> {
                // Verify that the new extension doesn't already exist
                requireUniqueExtension(state, action.extension)

                state.copy(
                    extensions = state.extensions + action.extension
                )
            }
            is WebExtensionAction.UpdateBrowserAction -> {
                state.copyWithWebExtensionState(action.extensionId) {
                    it.copy(browserAction = action.browserAction)
                }
            }
            is WebExtensionAction.ConsumeBrowserAction -> {
                state.copyWithWebExtensionState(action.extensionId) {
                    it.copy(browserAction = null)
                }
            }
        }
    }
}

private fun requireUniqueExtension(state: BrowserState, extension: WebExtensionState) {
    require(state.extensions.find { it.id == extension.id } == null) {
        "Extension with same ID already exists"
    }
}

private fun BrowserState.copyWithWebExtensionState(
    extensionId: String,
    update: (WebExtensionState) -> WebExtensionState
): BrowserState {
    return copy(
        extensions = extensions.map { current ->
            if (current.id == extensionId) {
                update.invoke(current)
            } else {
                current
            }
        }
    )
}