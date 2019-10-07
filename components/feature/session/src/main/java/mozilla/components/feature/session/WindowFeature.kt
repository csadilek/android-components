/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.session

import androidx.annotation.VisibleForTesting
import mozilla.components.browser.session.SelectionAwareSessionObserver
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.webextension.WebExtension
import mozilla.components.concept.engine.webextension.WebExtensionsTabsDelegate
import mozilla.components.concept.engine.window.WindowRequest
import mozilla.components.support.base.feature.LifecycleAwareFeature

/**
 * Feature implementation for handling window requests.
 *
 * Also subscribes for when a web extensions opens a new tab, adds a new session.
 * @property sessionManager ....
 *
 * @param engine (optional) when do I provide this?
 */
class WindowFeature(
    private val sessionManager: SessionManager,
    engine: Engine? = null
) : LifecycleAwareFeature {

    @VisibleForTesting
    internal val webExtensionsTabsDelegate = object : WebExtensionsTabsDelegate {
        override fun onNewTab(webExtension: WebExtension?, url: String, engineSession: EngineSession) {
            sessionManager.add(Session(url), true, engineSession)
        }
    }

    @VisibleForTesting
    internal val windowObserver = object : SelectionAwareSessionObserver(sessionManager) {
        override fun onOpenWindowRequested(session: Session, windowRequest: WindowRequest): Boolean {
            val newSession = Session(windowRequest.url, session.private)
            val newEngineSession = windowRequest.prepare()

            sessionManager.add(newSession, true, newEngineSession, parent = session)
            windowRequest.start()
            return true
        }

        override fun onCloseWindowRequested(session: Session, windowRequest: WindowRequest): Boolean {
            sessionManager.remove(session)
            return true
        }
    }

    init {
        engine?.registerWebExtensionsTabDelegate(webExtensionsTabsDelegate)
    }

    /**
     * Starts the feature and a observer to listen for window requests.
     */
    override fun start() {
        windowObserver.observeSelected()
    }

    /**
     * Stops the feature and the window request observer.
     */
    override fun stop() {
        windowObserver.stop()
    }
}
