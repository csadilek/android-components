/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.toolbar

import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.PRIVATE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import mozilla.components.browser.state.action.WebExtensionAction
import mozilla.components.browser.state.selector.findCustomTabOrSelectedTab
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.state.WebExtensionState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.webextension.BrowserAction
import mozilla.components.concept.toolbar.Toolbar
import mozilla.components.concept.toolbar.Toolbar.SiteTrackingProtection
import mozilla.components.feature.toolbar.internal.URLRenderer
import mozilla.components.lib.state.ext.flowScoped
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifAnyChanged
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifChanged

/**
 * Presenter implementation for a toolbar implementation in order to update the toolbar whenever
 * the state of the selected session or web extensions changes.
 */
@Suppress("TooManyFunctions")
class ToolbarPresenter(
    private val toolbar: Toolbar,
    private val store: BrowserStore,
    private val customTabId: String? = null,
    urlRenderConfiguration: ToolbarFeature.UrlRenderConfiguration? = null
) {
    @VisibleForTesting
    internal var renderer = URLRenderer(toolbar, urlRenderConfiguration)

    private var scope: CoroutineScope? = null

    /**
     * Start presenter: Display data in toolbar.
     */
    fun start() {
        renderer.start()

        scope = store.flowScoped { flow ->
            flow.ifAnyChanged { arrayOf(it.findCustomTabOrSelectedTab(customTabId), it.extensions) }
                .collect { state ->
                    render(state)
                }
        }
    }

    fun stop() {
        scope?.cancel()
        renderer.stop()
    }

    @VisibleForTesting(otherwise = PRIVATE)
    internal fun render(state: BrowserState) {
        val tab = state.findCustomTabOrSelectedTab(customTabId)
        if (tab != null) {
            renderer.post(tab.content.url)

            toolbar.setSearchTerms(tab.content.searchTerms)
            toolbar.displayProgress(tab.content.progress)

            toolbar.siteSecure = if (tab.content.securityInfo.secure) {
                Toolbar.SiteSecurity.SECURE
            } else {
                Toolbar.SiteSecurity.INSECURE
            }

            toolbar.siteTrackingProtection = when {
                tab.trackingProtection.enabled && tab.trackingProtection.blockedTrackers.isNotEmpty() ->
                    SiteTrackingProtection.ON_TRACKERS_BLOCKED

                tab.trackingProtection.enabled -> SiteTrackingProtection.ON_NO_TRACKERS_BLOCKED

                else -> SiteTrackingProtection.OFF_GLOBALLY
            }
        }


    }

    @VisibleForTesting(otherwise = PRIVATE)
    internal fun render(extensions: List<WebExtensionState>) {
        // Todo: we are going to need a way to link between a webExtensionState?.browserAction and
        // toolbar browser action, to remove the previous one if exits.

        extensions.forEach { extension ->
            extension.browserAction?.let { extensionAction ->
                val toolbarAction = extensionAction.toToolbarAction()
                toolbar.addBrowserAction(toolbarAction)
                store.dispatch(WebExtensionAction.ConsumeBrowserAction(extension.id))
            }
        }
    }

    private fun BrowserAction.toToolbarAction() = Toolbar.ActionButton(
        contentDescription = title, // TODO: Should we show the title on the UI or just the image?
        imageDrawable = icon,
        background = badgeBackgroundColor
        // TODO: TBD  action.badgeText?
    ) {
        onClick()
    }

    private fun observeWebExtensions() {
        webExtensionScope = store.flowScoped { flow ->
            flow.map { state -> state.extensions }
                .ifChanged()
                .collect { extensionsState ->
                    if (extensionsState.isEmpty()) {
                        clearWebExtensionItems()
                    } else {
                        render(extensionsState)
                    }
                }
        }
    }

    @VisibleForTesting(otherwise = PRIVATE)
    internal fun clearWebExtensionItems() {
        // TODO: Add api for removing toolbar items
    }

    @VisibleForTesting(otherwise = PRIVATE)
    internal fun clear() {
        renderer.post("")

        toolbar.setSearchTerms("")
        toolbar.displayProgress(0)

        toolbar.siteSecure = Toolbar.SiteSecurity.INSECURE

        toolbar.siteTrackingProtection = SiteTrackingProtection.OFF_GLOBALLY
    }
}
