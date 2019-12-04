/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.addons

import mozilla.components.browser.state.store.BrowserStore

/**
 * Provided access to installed and recommended [Addon]s and manages their state.
 *
 * @property store The application's [BrowserStore].
 * @property addonsProvider A [AddonsProvider] to query available [Addon]s
 */
class AddonManager(
    private val store: BrowserStore,
    private val addonsProvider: AddonsProvider
) {

    /**
     * Returns the list of all installed and recommended add-ons. [Addon.installedState] is
     * up-to-date for all returned add-ons.
     *
     * @return list of all [Addon]s.
     */
    suspend fun getAddons(): List<Addon> {
        // wait until we heard back from Gecko....
        val addons = addonsProvider.getAvailableAddons()

        // WebExtensionSupport.loadInstalledAddons.await()

        return addons.map { addon ->
            store.state.extensions[addon.id]?.let {
                // TODO Add fields once we can implement management API:
                //  https://github.com/mozilla-mobile/android-components/issues/4500
                addon.copy(installedState = Addon.InstalledState(it.id, "", ""))
            } ?: addon
        }
    }
}
