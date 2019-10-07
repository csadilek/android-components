/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.session

import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.webextension.WebExtension
import mozilla.components.concept.engine.window.WindowRequest
import mozilla.components.support.test.any
import mozilla.components.support.test.argumentCaptor
import mozilla.components.support.test.mock
import mozilla.components.support.test.whenever
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.notNull
import org.mockito.Mockito.verify

class WindowFeatureTest {

    private lateinit var engine: Engine
    private lateinit var sessionManager: SessionManager

    @Before
    fun setup() {
        engine = mock()
        sessionManager = mock()
    }

    @Test
    fun `start registers window observer`() {
        val feature = WindowFeature(sessionManager)
        feature.start()
        verify(sessionManager).register(feature.windowObserver)
    }

    @Test
    fun `observer handles open window request`() {
        val session = Session("https://www.mozilla.org")
        val request = mock<WindowRequest>()
        whenever(request.url).thenReturn("about:blank")

        val feature = WindowFeature(sessionManager)
        feature.windowObserver.onOpenWindowRequested(session, request)

        verify(request).prepare()
        verify(sessionManager).add(any(), eq(true), any(), eq(session))
        verify(request).start()
    }

    @Test
    fun `session is removed when window should be closed`() {
        val session = Session("https://www.mozilla.org")

        val feature = WindowFeature(sessionManager)
        feature.windowObserver.onCloseWindowRequested(session, mock())
        verify(sessionManager).remove(session)
    }

    @Test
    fun `stop unregisters window observer`() {
        val feature = WindowFeature(sessionManager)
        feature.stop()
        verify(sessionManager).unregister(feature.windowObserver)
    }

    @Test
    fun `session is added when a web extension opens a new tab`(){
        val feature = WindowFeature(sessionManager, engine)

        val webExtension: WebExtension = mock()
        val engineSession: EngineSession = mock()
        feature.webExtensionsTabsDelegate.onNewTab(webExtension, "https://www.mozilla.org", engineSession)

        val sessionCaptor = argumentCaptor<Session>()
        verify(sessionManager).add(sessionCaptor.capture(), eq(true), notNull(), any())

        assertEquals(sessionCaptor.value.url, "https://www.mozilla.org")
    }
}
