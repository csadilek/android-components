/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.ui.tabcounter

import android.content.Context
import androidx.core.content.ContextCompat.getColor
import mozilla.components.browser.menu2.BrowserMenuController
import mozilla.components.concept.menu.MenuController
import mozilla.components.concept.menu.candidate.DrawableMenuIcon
import mozilla.components.concept.menu.candidate.MenuCandidate
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

    companion object {
        fun create(context: Context): TabCounterMenu {
            return TabCounterMenu(
                context = context,
                onItemTapped = { item ->
                    when (item) {
                        is Item.CloseTab -> {
                            // replace alllll this:
                            sessionManager.selectedSession?.let {
                                // When closing the last tab we must show the undo snackbar in the home fragment
                                if (sessionManager.sessionsOfType(it.private).count() == 1) {
                                    // The tab tray always returns to normal mode so do that here too
                                    activity.browsingModeManager.mode = BrowsingMode.Normal
                                    homeViewModel.sessionToDelete = it.id
                                    navController.navigate(
                                        BrowserFragmentDirections.actionGlobalHome()
                                    )
                                } else {
                                    onCloseTab.invoke(it)
                                    // The removeTab use case does not currently select a parent session, so
                                    // we are using sessionManager.remove
                                    sessionManager.remove(it, selectParentIfExists = true)
                                }
                            }
                        }
                        is Item.NewTab -> {
//                            context.browsingModeManager.mode = item // normal mode
//                            navController.navigate(BrowserFragmentDirections.actionGlobalHome(focusOnAddressBar = true))
                            // what is the default way to open a new tab?
                        }
                        is Item.NewPrivateTab -> {
//                            activity.browsingModeManager.mode = item.mode // private mode
//                            navController.navigate(BrowserFragmentDirections.actionGlobalHome(focusOnAddressBar = true))
                            // what is the default way to open a new private tab?
                        }
                    }
                }
            )
        }
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

    internal fun menuItems(privateMode: Boolean): List<MenuCandidate> {
        return when (privateMode) {
            false -> listOf(newTabItem)
            true -> listOf(newPrivateTabItem)
        }
    }

    // move to fenix
//    @VisibleForTesting
//    internal fun menuItems(toolbarPosition: ToolbarPosition): List<MenuCandidate> {
//        val items = listOf(
//                newTabItem,
//                newPrivateTabItem,
//                DividerMenuCandidate(),
//                closeTabItem
//        )
//
//        return when (toolbarPosition) {
//            ToolbarPosition.BOTTOM -> items.reversed()
//            ToolbarPosition.TOP -> items
//        }
//    }

    /**
     * Update the displayed menu items.
     * @param isInPrivateMode Show only the new tab item corresponding to the given browsing mode.
     */
    fun updateMenu(isInPrivateMode: Boolean) {
        menuController.submitList(menuItems(isInPrivateMode))
    }

    // move to fenix
//    /**
//     * Update the displayed menu items.
//     * @param toolbarPosition Return a list that is ordered based on the given [ToolbarPosition].
//     */
//    fun updateMenu(toolbarPosition: ToolbarPosition) {
//        menuController.submitList(menuItems(toolbarPosition))
//    }
}