package mozilla.components.browser.session

import mozilla.components.browser.session.engine.EngineObserver
import mozilla.components.concept.engine.EngineSession

class EngineSessionHolder {
    var engineSession: EngineSession? = null
    var engineObserver: EngineObserver? = null
}
