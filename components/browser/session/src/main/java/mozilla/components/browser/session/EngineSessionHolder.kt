/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.session

import mozilla.components.browser.session.engine.EngineObserver
import mozilla.components.concept.engine.EngineSession

class EngineSessionHolder {
    var engineSession: EngineSession? = null
    var engineObserver: EngineObserver? = null
}
