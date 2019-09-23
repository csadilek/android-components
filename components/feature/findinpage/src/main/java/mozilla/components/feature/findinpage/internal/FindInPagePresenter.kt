/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.findinpage.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import mozilla.components.browser.state.selector.findTabOrCustomTab
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.feature.findinpage.view.FindInPageView
import mozilla.components.lib.state.ext.flowScoped
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifChanged

/**
 * Presenter implementation that will observe [SessionState] changed and update the view whenever
 * a find result was added.
 */
internal class FindInPagePresenter(
    private val store: BrowserStore,
    private val view: FindInPageView
) {
    private var started: Boolean = false
    private var session: SessionState? = null
    private var scope: CoroutineScope? = null

    fun start() {
        scope = session?.let { observeFindResults(it) }
        started = true
    }

    fun stop() {
        scope?.cancel()
        started = false
    }

    fun bind(session: SessionState) {
        this.session = session

        if (started) {
            scope?.cancel()
            scope = observeFindResults(session)
        }

        view.focus()
    }

    private fun observeFindResults(session: SessionState): CoroutineScope? {
        return store.flowScoped { flow ->
            flow.map { state -> state.findTabOrCustomTab(session.id) }
                .ifChanged { it?.content?.findResults }
                .collect {
                    it?.let {
                        val results = it.content.findResults
                        if (results.isNotEmpty()) {
                            view.displayResult(results.last())
                        }
                    }
                }
        }
    }

    fun unbind() {
        view.clear()
        session = null
    }
}
