/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.engine.system

import android.os.Message
import mozilla.components.concept.engine.window.Window

class SystemWindow(private val isDialog: Boolean,
                   private val isUserGestures: Boolean,
                   private val resultMessage: Message
): Window {

    override fun onDisplayed() {
        resultMessage.sendToTarget()
    }
}