/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.fxa.migration

import android.content.ContentProviderClient
import android.content.Context
import android.net.Uri
import mozilla.components.support.ktx.kotlin.toHexString

/**
 * TODO
 */
object AccountMigration {
    private const val KEY_EMAIL = "email"
    private const val KEY_SESSION_TOKEN = "sessionToken"
    private const val KEY_KSYNC = "kSync"
    private const val KEY_KXSCS = "kXSCS"

    private val ecosystemProviders: Map<String, String> = mapOf(
            "org.mozilla.fennec_csadilek.fxa.auth" to "signature"
    )

    fun queryMigratableAccounts(context: Context): List<MigratableAccount> {
        return ecosystemProviders.filter {
            isTrustedProvider(it.key)
        }.mapNotNull { authMap ->
            // get client
            context.contentResolver.acquireContentProviderClient(authMap.key)?.let {
                authMap.key to it
            }
        }.mapNotNull {
            // query client, get account obj
            it.second.use { client ->
                queryForAccount(it.first, client)
            }
        }
    }

    private fun queryForAccount(authAuthority: String, client: ContentProviderClient): MigratableAccount? {
        val authAuthorityUri = Uri.parse("content://$authAuthority")
        val authStateUri = Uri.withAppendedPath(authAuthorityUri, "state")

        client.query(
                authStateUri,
                arrayOf(KEY_EMAIL, KEY_SESSION_TOKEN, KEY_KSYNC, KEY_KXSCS),
                null, null, null
        ).use { cursor ->
            // Could not read account from the provider. Either it's logged out, or in a bad state.
            if (cursor == null) {
                return null
            }

            cursor.moveToFirst()

            val email = cursor.getString(cursor.getColumnIndex(KEY_EMAIL))
            val sessionToken = cursor.getBlob(cursor.getColumnIndex(KEY_SESSION_TOKEN))?.toHexString()
            val kSync = cursor.getBlob(cursor.getColumnIndex(KEY_KSYNC))?.toHexString()
            val kXSCS = cursor.getString(cursor.getColumnIndex(KEY_KXSCS))

            if (email != null && sessionToken != null && kSync != null && kXSCS != null) {
                return MigratableAccount(email, authAuthority, MigratableAuthInfo(
                        sessionToken, kSync, kXSCS
                ))
            }
        }
        return null
    }

    @Suppress("UNUSED_PARAMETER")
    private fun isTrustedProvider(provider: String): Boolean {
        // TODO see https://bugzilla.mozilla.org/show_bug.cgi?id=1545232 for how to implement this
        return true
    }
}

data class MigratableAuthInfo(val sessionToken: String, val kSync: String, val kXSCS: String)

data class MigratableAccount(
        val email: String,
        val source: String,
        internal val authInfo: MigratableAuthInfo
)