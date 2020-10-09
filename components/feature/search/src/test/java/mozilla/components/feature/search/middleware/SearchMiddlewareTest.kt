/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.search.middleware

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.TestCoroutineDispatcher
import mozilla.components.browser.state.action.SearchAction
import mozilla.components.browser.state.search.RegionState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.support.test.ext.joinBlocking
import mozilla.components.support.test.libstate.ext.waitUntilIdle
import mozilla.components.support.test.robolectric.testContext
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class SearchMiddlewareTest {
    private lateinit var dispatcher: TestCoroutineDispatcher
    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        dispatcher = TestCoroutineDispatcher()
        originalLocale = Locale.getDefault()
    }

    @After
    fun tearDown() {
        dispatcher.cleanupTestCoroutines()

        if (Locale.getDefault() != originalLocale) {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun `Loads search engines for region`() {
        val store = BrowserStore(
            middleware = listOf(SearchMiddleware(testContext, ioDispatcher = dispatcher))
        )

        assertTrue(store.state.search.regionSearchEngines.isEmpty())

        store.dispatch(SearchAction.SetRegionAction(
            RegionState("US", "US")
        )).joinBlocking()

        dispatcher.advanceUntilIdle()
        // Why?
        Thread.sleep(1000)

        store.waitUntilIdle()

        assertTrue(store.state.search.regionSearchEngines.isNotEmpty())
    }
}
