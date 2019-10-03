/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.concept.engine.webextension

import android.graphics.drawable.Drawable

data class BrowserAction(
    val title: String,
    val enabled: Boolean,
    val icon: Drawable,
    val uri: String,
    val badgeText: String,
    val badgeBackgroundColor: Int,
    val onClick: () -> Unit
)
