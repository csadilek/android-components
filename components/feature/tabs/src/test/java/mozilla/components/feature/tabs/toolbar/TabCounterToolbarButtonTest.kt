/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.tabs.toolbar

import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.TestCoroutineDispatcher
import mozilla.components.browser.state.action.TabListAction
import mozilla.components.browser.state.state.BrowserState
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
    private val showTabs: () -> Unit = mock()
    private val tabCounterMenu: TabCounterMenu = mock()
    private val menuController: MenuController = mock()

    private lateinit var lifecycleOwner: MockedLifecycleOwner

    private val testDispatcher = TestCoroutineDispatcher()

    @get:Rule
    val coroutinesTestRule = MainCoroutineRule(testDispatcher)

    @Before
    fun setUp() {
        whenever(tabCounterMenu.menuController).thenReturn(menuController)
        lifecycleOwner = MockedLifecycleOwner(Lifecycle.State.STARTED)
    }

    @Test
    fun `TabCounter has initial count set`() {
        val button = spy(
            TabCounterToolbarButton(
                lifecycleOwner,
                false,
                showTabs,
                BrowserStore(),
                tabCounterMenu
            )
        )
        val view = button.createView(LinearLayout(testContext) as ViewGroup) as TabCounter
        assertEquals("0", view.text.text)
    }

    /*@Test
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
    }*/

    @Test
    fun `WHEN tab is added THEN tab count is updated`() {
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

        store.dispatch(TabListAction.AddTabAction(createTab("https://www.mozilla.org"))).joinBlocking()
        verify(button).updateCount(eq(1))
    }

    @Test
    fun `WHEN tab is removed THEN tab count is updated`() {
        val tab = createTab("https://www.mozilla.org")
        val store = BrowserStore(BrowserState(tabs = listOf(tab)))
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

        store.dispatch(TabListAction.RemoveTabAction(tab.id)).joinBlocking()
        verify(button).updateCount(eq(0))
    }

    @Test
    fun `WHEN private tab is added THEN tab count is updated`() {
        val store = BrowserStore()
        val button = spy(
            TabCounterToolbarButton(
                lifecycleOwner,
                true,
                showTabs,
                store,
                tabCounterMenu
            )
        )

        whenever(button.updateCount(anyInt())).then { }
        button.createView(LinearLayout(testContext) as ViewGroup) as TabCounter

        store.dispatch(TabListAction.AddTabAction(createTab("https://www.mozilla.org", private = true))).joinBlocking()
        verify(button).updateCount(eq(1))
    }

    @Test
    fun `WHEN private tab is removed THEN tab count is updated`() {
        val tab = createTab("https://www.mozilla.org", private = true)
        val store = BrowserStore(BrowserState(tabs = listOf(tab)))
        val button = spy(
            TabCounterToolbarButton(
                lifecycleOwner,
                true,
                showTabs,
                store,
                tabCounterMenu
            )
        )

        whenever(button.updateCount(anyInt())).then { }
        button.createView(LinearLayout(testContext) as ViewGroup) as TabCounter

        store.dispatch(TabListAction.RemoveTabAction(tab.id)).joinBlocking()
        verify(button).updateCount(eq(0))
    }

    internal class MockedLifecycleOwner(initialState: Lifecycle.State) : LifecycleOwner {
        val lifecycleRegistry = LifecycleRegistry(this).apply {
            currentState = initialState
        }

        override fun getLifecycle(): Lifecycle = lifecycleRegistry
    }
}