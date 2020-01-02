/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.support.migration

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.webextension.WebExtension

/**
 * Wraps [AddonMigrationResult] in an exception so that it can be returned via [Result.Failure].
 *
 * @property failure Wrapped [AddonMigrationResult] indicating exact failure reason.
 */
class AddonMigrationException(val failure: AddonMigrationResult.Failure) : Exception(failure.toString())

/**
 * Result of an Add-on migration.
 */
sealed class AddonMigrationResult {
    /**
     * Success variants of an Add-on migration.
     */
    sealed class Success : AddonMigrationResult() {
        /**
         * No Add-ons installed to migrate.
         */
        object NoAddons : Success()

        /**
         * Successfully migrated Add-ons.
         */
        data class AddonsMigrated(
            val supportedAddons: List<WebExtension>,
            val unsupportedAddons: List<WebExtension>
        ) : Success() {
            // TODO Should we record which addons are supported (kept enabled)
            // and which we disabled because they're unsupported?
            override fun toString(): String {
                return "Successfully migrated Add-ons..."
            }
        }
    }

    /**
     * Failure variants of an Add-on migration.
     */
    sealed class Failure : AddonMigrationResult() {
        internal data class FailedToMigrateAddons(val throwable: Throwable) : Failure() {
            override fun toString(): String {
                return "Failed to migrate Add-ons: ${throwable::class}"
            }
        }
    }
}

/**
 * Helper for migrating Add-ons.
 */
internal object AddonMigration {

    /**
     * Performs a migration of all installed Add-ons. The only step involved in migration
     * is to make sure we disable currently unsupported Add-ons.
     */
    @SuppressWarnings("TooGenericExceptionCaught")
    suspend fun migrate(engine: Engine): Result<AddonMigrationResult> {
        try {
            val installedAddons = getInstalledAddons(engine)
            if (installedAddons.isEmpty()) {
                return Result.Success(AddonMigrationResult.Success.NoAddons)
            }

            val (supportedAddons, unsupportedAddons) = installedAddons
                .map { maybeDisableAddon(engine, it) }
                .partition { it.isEnabled() }

            return Result.Success(AddonMigrationResult.Success.AddonsMigrated(supportedAddons, unsupportedAddons))
        } catch (e: Exception) {
            return Result.Failure(AddonMigrationException(AddonMigrationResult.Failure.FailedToMigrateAddons(e)))
        }
    }

    private suspend fun getInstalledAddons(engine: Engine): List<WebExtension> {
        return CompletableDeferred<List<WebExtension>>().also { result ->
            // Discuss: We need a thread with a looper but could create a separate one instead
            // of using main. Don't really see an issue with main though as we're also
            // using it in other migrations.
            withContext(Dispatchers.Main) {
                engine.listInstalledWebExtensions(
                    onSuccess = { result.complete(it) },
                    onError = { result.completeExceptionally(it) }
                )
            }
        }.await()
    }

    private suspend fun maybeDisableAddon(engine: Engine, addon: WebExtension): WebExtension {
        // TODO (For final migration): check if addon is supported (against AMO via
        // AddonCollectionProvider and/or offline whitelist). For now let's disable
        // unconditionally, as we don't support any addon yet.
        return CompletableDeferred<WebExtension>().also { result ->
            withContext(Dispatchers.Main) {
                engine.disableWebExtension(
                    addon,
                    onSuccess = { result.complete(it) },
                    onError = { result.completeExceptionally(it) }
                )
            }
        }.await()
    }
}
