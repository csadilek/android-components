/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.engine.system

import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import mozilla.components.concept.engine.file.FilePrompt

class SystemFilePrompt(private val view: WebView,
                       private val callback: ValueCallback<Array<Uri>>,
                       private val fileChooserParams: WebChromeClient.FileChooserParams
) : FilePrompt {

    override fun confirm(uris: Array<Uri>) {
        callback.onReceiveValue(uris)
    }
}