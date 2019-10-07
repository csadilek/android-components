/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.concept.engine.webextension

import mozilla.components.concept.engine.EngineSession
import mozilla.components.support.test.mock
import org.junit.Assert
import org.junit.Test

class WebExtensionsTabDelegateTest {
    private val webExtensionsTabsDelegate = object : WebExtensionsTabsDelegate {}

    @Test
    fun `onCloseTab invokes a UnsupportedOperationException`() {
        try {
            val engineSession: EngineSession = mock()
            webExtensionsTabsDelegate.onCloseTab(null, engineSession)
            // Private browsing not yet supported
            Assert.fail("Expected UnsupportedOperationException")
        } catch (e: UnsupportedOperationException) { }
    }
}
