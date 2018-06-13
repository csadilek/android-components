package mozilla.components.browser.session.engine

import mozilla.components.browser.session.Session
import mozilla.components.concept.engine.EngineSession

class EngineObserver(val session: Session) : EngineSession.Observer {
    override fun onLocationChange(url: String) {
        session.url = url
        session.searchTerms = ""
    }

    override fun onProgress(progress: Int) {
        session.progress = progress
    }

    override fun onLoadingStateChange(loading: Boolean) {
        session.loading = loading
    }

    override fun onNavigationStateChange(canGoBack: Boolean?, canGoForward: Boolean?) {
        canGoBack?.let { session.canGoBack = canGoBack }
        canGoForward?.let { session.canGoForward = canGoForward }
    }

    override fun onSecurityChange(secure: Boolean, host: String?, issuer: String?) {
        session.securityInfo = Session.SecurityInfo(secure, host
                ?: "", issuer ?: "")
    }
}
