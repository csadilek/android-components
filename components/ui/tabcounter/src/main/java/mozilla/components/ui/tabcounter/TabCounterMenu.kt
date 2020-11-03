/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.ui.tabcounter

import android.content.Context
import androidx.core.content.ContextCompat.getColor
import mozilla.components.browser.menu2.BrowserMenuController
import mozilla.components.concept.menu.MenuController
import mozilla.components.concept.menu.candidate.DrawableMenuIcon
import mozilla.components.concept.menu.candidate.TextMenuCandidate
import mozilla.components.concept.menu.candidate.TextStyle

open class TabCounterMenu(
    context: Context,
    private val onItemTapped: (Item) -> Unit
) {
    open class Item {
        object CloseTab : Item()
        object NewTab : Item()
        object NewPrivateTab : Item()
    }

    private val newTabItem: TextMenuCandidate
    private val newPrivateTabItem: TextMenuCandidate
    private val closeTabItem: TextMenuCandidate
    val menuController: MenuController by lazy { BrowserMenuController() }

    init {
        newTabItem = TextMenuCandidate(
            text = "New tab",
            start = DrawableMenuIcon(
                context,
                R.drawable.mozac_ui_tabcounter_ic_new,
                tint = getColor(context, R.color.mozac_ui_tabcounter_default_text)
            ),
            textStyle = TextStyle()
        ) {
            onItemTapped(Item.NewTab)
        }

        newPrivateTabItem = TextMenuCandidate(
            text = "New private tab",
            start = DrawableMenuIcon(
                context,
                R.drawable.mozac_ui_tabcounter_ic_private_browsing,
                tint = getColor(context, R.color.mozac_ui_tabcounter_default_text)
            ),
            textStyle = TextStyle()
        ) {
            onItemTapped(Item.NewPrivateTab)
        }


        closeTabItem = TextMenuCandidate(
            text = "Close tab",
            start = DrawableMenuIcon(
                    context,
                    R.drawable.mozac_ui_tabcounter_ic_close,
                    tint = getColor(context, R.color.mozac_ui_tabcounter_default_text)
            ),
            textStyle = TextStyle()
        ) {
            onItemTapped(Item.CloseTab)
        }
    }
}