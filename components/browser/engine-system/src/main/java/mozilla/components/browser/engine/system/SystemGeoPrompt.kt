/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.engine.system

import android.webkit.GeolocationPermissions
import mozilla.components.concept.engine.geo.GeoPrompt

class SystemGeoPrompt(private val origin: String,
                      private val callback:  GeolocationPermissions.Callback
) : GeoPrompt {
    override fun setPermission(allow: Boolean, retain: Boolean) {
        callback.invoke(origin, allow, retain)
    }
}