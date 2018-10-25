/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.engine.gecko

import android.content.Context
import android.net.Uri
import mozilla.components.concept.engine.file.FilePrompt
import org.mozilla.geckoview.GeckoSession

class GeckoFilePrompt(private val context: Context,
                      private val title: String,
                      private val type: Int,
                      private val mimetype: Array<String>,
                      private val callback: GeckoSession.PromptDelegate.FileCallback
) : FilePrompt {

    override fun confirm(uris: Array<Uri>) {
        callback.confirm(context, uris)
    }
}