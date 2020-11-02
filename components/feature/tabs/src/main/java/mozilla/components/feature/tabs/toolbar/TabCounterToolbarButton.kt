/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.tabs.toolbar

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import mozilla.components.browser.state.selector.getNormalOrPrivateTabs
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.toolbar.Toolbar
import mozilla.components.lib.state.ext.flowScoped
import mozilla.components.support.ktx.android.content.res.resolveAttribute
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifChanged
import mozilla.components.ui.tabcounter.TabCounter
import mozilla.components.ui.tabcounter.TabCounterMenu
import java.lang.ref.WeakReference

/**
 * A [Toolbar.Action] implementation that shows a [TabCounter].
 */
@ExperimentalCoroutinesApi
class TabCounterToolbarButton(
    private val lifecycleOwner: LifecycleOwner,
    private val isPrivate: Boolean,
    private val showTabs: () -> Unit,
    private val store: BrowserStore,
    private val menu: TabCounterMenu
) : Toolbar.Action {

    private var reference: WeakReference<TabCounter> = WeakReference<TabCounter>(null)

    override fun createView(parent: ViewGroup): View {
        store.flowScoped(lifecycleOwner) { flow ->
            flow.map { state -> state.getNormalOrPrivateTabs(isPrivate).size }
                .ifChanged()
                .collect { tabs -> updateCount(tabs) }
        }

        val view = TabCounter(parent.context).apply {
            reference = WeakReference(this)
            setOnClickListener {
                showTabs.invoke()
            }

            setOnLongClickListener {
                menu.menuController.show(anchor = it)
                true
            }

            addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View?) {
                    setCount(store.state.getNormalOrPrivateTabs(isPrivate).size)
                }

                override fun onViewDetachedFromWindow(v: View?) { /* no-op */ }
            })
        }

        // Set selectableItemBackgroundBorderless
        view.setBackgroundResource(parent.context.theme.resolveAttribute(
            android.R.attr.selectableItemBackgroundBorderless
        ))
        return view
    }

    override fun bind(view: View) = Unit

    private fun updateCount(count: Int) {
        reference.get()?.setCountWithAnimation(count)
    }
}