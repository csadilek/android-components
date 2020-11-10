/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.tabs.toolbar

import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.lifecycle.LifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.TestCoroutineDispatcher
import mozilla.components.browser.state.action.TabListAction
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.menu.MenuController
import mozilla.components.support.test.ext.joinBlocking
import mozilla.components.support.test.mock
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.test.rule.MainCoroutineRule
import mozilla.components.support.test.whenever
import mozilla.components.ui.tabcounter.TabCounter
import mozilla.components.ui.tabcounter.TabCounterMenu
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import java.lang.ref.WeakReference

@RunWith(AndroidJUnit4::class)
class TabCounterToolbarButtonTest {

    private val browserStore: BrowserStore = mock()
    private val lifecycleOwner: LifecycleOwner = mock()
    private val showTabs: () -> Unit = mock()
    private val tabCounterMenu: TabCounterMenu = mock()
    private val menuController: MenuController = mock()
    private val weakReference: WeakReference<TabCounter> = mock()
    private val tabCounter: TabCounter = mock()

    private lateinit var button: TabCounterToolbarButton

    private val testDispatcher = TestCoroutineDispatcher()

    @get:Rule
    val coroutinesTestRule = MainCoroutineRule(testDispatcher)

    @Before
    fun setUp() {
        whenever(tabCounterMenu.menuController).thenReturn(menuController)
        button =
            TabCounterToolbarButton(
                lifecycleOwner,
                false,
                showTabs,
                browserStore,
                tabCounterMenu
            )
    }

    @Test
    fun `TabCounter has initial count set`() {
        val view = button.createView(LinearLayout(testContext) as ViewGroup) as TabCounter
        assertEquals("0", view.text.text)
    }

    @Test
    fun `Clicking TabCounter invokes showTabs function`() {
        val view = button.createView(LinearLayout(testContext) as ViewGroup) as TabCounter
        view.performClick()
        verify(showTabs).invoke()
    }

    @Test
    fun `Long clicking TabCounter shows the controller menu`() {
        val view = button.createView(LinearLayout(testContext) as ViewGroup) as TabCounter
        view.performLongClick()
        verify(menuController).show(view)
    }

    @Test
    fun `Updating count sets the count with animation`() {
        val store = BrowserStore()
        val button = spy(
            TabCounterToolbarButton(
                lifecycleOwner,
                false,
                showTabs,
                store,
                tabCounterMenu
            )
        )

        whenever(button.updateCount(anyInt())).then { }
        button.createView(LinearLayout(testContext) as ViewGroup) as TabCounter

        store.dispatch(
            TabListAction.AddTabAction(createTab("https://www.mozilla.org"))
        ).joinBlocking()

        verify(button).updateCount(eq(1))
    }
}