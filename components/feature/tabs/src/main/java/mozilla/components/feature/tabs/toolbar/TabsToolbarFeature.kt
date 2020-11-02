/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.tabs.toolbar

import androidx.lifecycle.LifecycleOwner
import mozilla.components.browser.session.SessionManager
import mozilla.components.browser.session.runWithSession
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.toolbar.Toolbar
import mozilla.components.ui.tabcounter.TabCounterMenu

/**
 * Feature implementation for connecting a tabs tray implementation with a toolbar implementation.
 */

class TabsToolbarFeature(
    toolbar: Toolbar,
    store: BrowserStore,
    lifecycleOwner: LifecycleOwner,
    showTabs: () -> Unit,
    tabCounterMenu: TabCounterMenu
) {
    init {
        run {
//            sessionManager.runWithSession(sessionId) {
//                it.isCustomTabSession()
//            }.also { isCustomTab ->
//                if (isCustomTab) return@run
//            }
            val tabsAction = TabCounterToolbarButton(
                lifecycleOwner,
                false,
                showTabs,
                store,
                tabCounterMenu
            )
            toolbar.addBrowserAction(tabsAction)
        }
    }
}
