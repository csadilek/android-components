/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.tabs.toolbar

import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.components.browser.state.selector.findCustomTab
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.CustomTabSessionState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.toolbar.Toolbar
import mozilla.components.support.test.any
import mozilla.components.support.test.mock
import mozilla.components.support.test.whenever
import mozilla.components.ui.tabcounter.TabCounterMenu
import org.junit.Test
import org.mockito.Mockito.*

@ExperimentalCoroutinesApi
class TabsToolbarFeatureTest {

    private val toolbar: Toolbar = mock()
    private val browserStore: BrowserStore = mock()
    private val lifecycleOwner: LifecycleOwner = mock()
    private val showTabs: () -> Unit = mock()
    private val tabCounterMenu: TabCounterMenu = mock()
    private var sessionId: String? = "12345"
    private lateinit var tabsToolbarFeature: TabsToolbarFeature

    private fun createToolbarFeature() {
        tabsToolbarFeature = spy(
            TabsToolbarFeature(
                toolbar,
                browserStore,
                sessionId,
                lifecycleOwner,
                showTabs,
                tabCounterMenu
            )
        )
    }

    @Test
    fun `feature adds "tabs" button to toolbar`() {
        createToolbarFeature()
        whenever(browserStore.state.findCustomTab(anyString())).thenReturn(null)

        verify(toolbar).addBrowserAction(any())
    }

    @Test
    fun `feature does not add tabs button when session is a customtab`() {
        val mockCustomTabSession: CustomTabSessionState = mock()
        whenever(browserStore.state.findCustomTab(sessionId!!)).thenReturn(mockCustomTabSession)

        createToolbarFeature()

        verify(toolbar, never()).addBrowserAction(any())
    }

    @Test
    fun `feature adds tab button when session found but not a customtab`() {
        whenever(browserStore.state.findCustomTab(anyString())).thenReturn(null)
        createToolbarFeature()
        verify(toolbar).addBrowserAction(any())
    }
}