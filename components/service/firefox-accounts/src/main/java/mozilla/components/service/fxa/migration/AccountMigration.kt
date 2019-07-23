/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.fxa.migration

import android.annotation.TargetApi
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import mozilla.components.support.ktx.kotlin.toHexString

data class MigratableAuthInfo(val sessionToken: String, val kSync: String, val kXSCS: String)

/**
 * Data structure describing an FxA account within another package that may be used to sign-in.
 */
data class MigratableAccount(
    val email: String,
    val sourcePackage: String,
    internal val authInfo: MigratableAuthInfo
)

/**
 * TODO
 */
object AccountMigration {
    internal const val KEY_EMAIL = "email"
    internal const val KEY_SESSION_TOKEN = "sessionToken"
    internal const val KEY_KSYNC = "kSync"
    internal const val KEY_KXSCS = "kXSCS"

    // NB: This defines order in which MigratableAccounts are returned in [queryMigratableAccounts].
    internal val ecosystemProviderPackages: List<Pair<String, String>> = listOf(
        "org.mozilla.firefox" to "308203a63082028ea00302010202044c72fd88300d06092a864886f70d0101050500308194310b3009060355040613025553311330110603550408130a43616c69666f726e6961311630140603550407130d4d6f756e7461696e2056696577311c301a060355040a13134d6f7a696c6c6120436f72706f726174696f6e311c301a060355040b131352656c6561736520456e67696e656572696e67311c301a0603550403131352656c6561736520456e67696e656572696e67301e170d3130303832333233303032345a170d3338303130383233303032345a308194310b3009060355040613025553311330110603550408130a43616c69666f726e6961311630140603550407130d4d6f756e7461696e2056696577311c301a060355040a13134d6f7a696c6c6120436f72706f726174696f6e311c301a060355040b131352656c6561736520456e67696e656572696e67311c301a0603550403131352656c6561736520456e67696e656572696e6730820122300d06092a864886f70d01010105000382010f003082010a0282010100b4160fb324eac03bd9f7ca21a094769d6811d5e9de2a2314b86f279b7de05b086465ec2dda1db2023bc1b33f73e92c52ed185bb95fcb5d2d81667f6e76266e76de836b3e928d94dd9675bb6ec051fc378affae85158e4ffad4ed27c9f3efc8fa7641ff08e43b4c56ded176d981cb83cf87002d0fe55ab00753f8f255b52f04b9d30173fc2c9b980b6ea24d1ae62e0fe0e73e692591e4f4d5701739929d91c6874ccb932bd533ba3f45586a2306bd39e7aa02fa90c0271a50fa3bde8fe4dd820fe8143a18795717349cfc32e9ceecbd01323c7c86f3132b140341bfc6dc26c0559127169510b0181cfa81b5491dd7c9dc0de3f2ab06b8dcdd7331692839f865650203010001300d06092a864886f70d010105050003820101007a06b17b9f5e49cfe86fc7bd9155b9e755d2f770802c3c9d70bde327bb54f5d7e41e8bb1a466dac30e9933f249ba9f06240794d56af9b36afb01e2272f57d14e9ca162733b0dd8ba373fb465428c5cfe14376f08e58d65c82f18f6c26255519f5244c3c34c9f552e1fcb52f71bcc62180f53e8027221af716be5adc55b939478725c12cb688bad617168d0f803513a6c10be147250ed7b5d2d37569135e81ceca38bba7bdcb5f9a802bae6740d85ae0a4c3fb27da78cc5b8c2fae4d8f361894ac70236bdcb3eadf9f36f46ee48662f9be4e22eda49e1b4db1e911ab972d8925298f16e831344da881059a9c0fbce229efeae719740e975d7f0dc691ccca0a9ce",
        "org.mozilla.firefox_beta" to "308203a63082028ea00302010202044c72fd88300d06092a864886f70d0101050500308194310b3009060355040613025553311330110603550408130a43616c69666f726e6961311630140603550407130d4d6f756e7461696e2056696577311c301a060355040a13134d6f7a696c6c6120436f72706f726174696f6e311c301a060355040b131352656c6561736520456e67696e656572696e67311c301a0603550403131352656c6561736520456e67696e656572696e67301e170d3130303832333233303032345a170d3338303130383233303032345a308194310b3009060355040613025553311330110603550408130a43616c69666f726e6961311630140603550407130d4d6f756e7461696e2056696577311c301a060355040a13134d6f7a696c6c6120436f72706f726174696f6e311c301a060355040b131352656c6561736520456e67696e656572696e67311c301a0603550403131352656c6561736520456e67696e656572696e6730820122300d06092a864886f70d01010105000382010f003082010a0282010100b4160fb324eac03bd9f7ca21a094769d6811d5e9de2a2314b86f279b7de05b086465ec2dda1db2023bc1b33f73e92c52ed185bb95fcb5d2d81667f6e76266e76de836b3e928d94dd9675bb6ec051fc378affae85158e4ffad4ed27c9f3efc8fa7641ff08e43b4c56ded176d981cb83cf87002d0fe55ab00753f8f255b52f04b9d30173fc2c9b980b6ea24d1ae62e0fe0e73e692591e4f4d5701739929d91c6874ccb932bd533ba3f45586a2306bd39e7aa02fa90c0271a50fa3bde8fe4dd820fe8143a18795717349cfc32e9ceecbd01323c7c86f3132b140341bfc6dc26c0559127169510b0181cfa81b5491dd7c9dc0de3f2ab06b8dcdd7331692839f865650203010001300d06092a864886f70d010105050003820101007a06b17b9f5e49cfe86fc7bd9155b9e755d2f770802c3c9d70bde327bb54f5d7e41e8bb1a466dac30e9933f249ba9f06240794d56af9b36afb01e2272f57d14e9ca162733b0dd8ba373fb465428c5cfe14376f08e58d65c82f18f6c26255519f5244c3c34c9f552e1fcb52f71bcc62180f53e8027221af716be5adc55b939478725c12cb688bad617168d0f803513a6c10be147250ed7b5d2d37569135e81ceca38bba7bdcb5f9a802bae6740d85ae0a4c3fb27da78cc5b8c2fae4d8f361894ac70236bdcb3eadf9f36f46ee48662f9be4e22eda49e1b4db1e911ab972d8925298f16e831344da881059a9c0fbce229efeae719740e975d7f0dc691ccca0a9ce",
        "org.mozilla.fennec_aurora" to "3082032f308202eda00302010202044bf2cf11300b06072a8648ce3804030500307b310b3009060355040613025553311330110603550408130a43616c69666f726e6961311630140603550407130d4d6f756e7461696e20566965773110300e060355040a13074d6f7a696c6c6131143012060355040b130b52656c456e67205465616d311730150603550403130e4d6f7a696c6c61204275696c6473301e170d3130303531383137333230315a170d3335303531323137333230315a307b310b3009060355040613025553311330110603550408130a43616c69666f726e6961311630140603550407130d4d6f756e7461696e20566965773110300e060355040a13074d6f7a696c6c6131143012060355040b130b52656c456e67205465616d311730150603550403130e4d6f7a696c6c61204275696c6473308201b73082012c06072a8648ce3804013082011f02818100fd7f53811d75122952df4a9c2eece4e7f611b7523cef4400c31e3f80b6512669455d402251fb593d8d58fabfc5f5ba30f6cb9b556cd7813b801d346ff26660b76b9950a5a49f9fe8047b1022c24fbba9d7feb7c61bf83b57e7c6a8a6150f04fb83f6d3c51ec3023554135a169132f675f3ae2b61d72aeff22203199dd14801c70215009760508f15230bccb292b982a2eb840bf0581cf502818100f7e1a085d69b3ddecbbcab5c36b857b97994afbbfa3aea82f9574c0b3d0782675159578ebad4594fe67107108180b449167123e84c281613b7cf09328cc8a6e13c167a8b547c8d28e0a3ae1e2bb3a675916ea37f0bfa213562f1fb627a01243bcca4f1bea8519089a883dfe15ae59f06928b665e807b552564014c3bfecf492a038184000281806d139acd20c3474d2f424859b33c66ee4f8e65ba30fe69e29fe45140dc3b0cb67a5c3f010b0d890e5e0da39ccaec31e247d94f0bf9468c075cca6485ccef06704db7e9c19a153b12b04ac6644616c195593ce9a34036f5a0f93be34cede8da0618189aab5ce6a46ce94e2b04a8c6339a5492592c1d5c814d4744db1cc04752e7300b06072a8648ce3804030500032f00302c021459726401585baf7c94296f07fb8bebd1a830f8f802147ccde4d9b4a6d378c94ebf5e9d7b5359100c7c6c"
    )

    /**
     * Queries the available migratable accounts. The accounts are returned in the following order,
     * as defined by [ecosystemProviderPackages]:
     * - Release -> "org.mozilla.firefox"
     * - Beta -> "org.mozilla.firefox_beta"
     * - Nightly -> "org.mozilla.fennec_aurora"
     *
     * @param context the application context.
     * @return list of accounts suitable for migration.
     */
    fun queryMigratableAccounts(context: Context): List<MigratableAccount> {
        val packageManager = context.packageManager
        return ecosystemProviderPackages.filter {
            // Leave only packages that are installed and match our expected signature.
            packageExistsWithSignature(packageManager, it.first, it.second)
        }.mapNotNull {
            queryForAccount(context, it.first)
        }
    }

    private fun queryForAccount(context: Context, packageName: String): MigratableAccount? {
        // assuming a certain formatting for all authorities from all sources
        val authority = "$packageName.fxa.auth"
        val client = context.contentResolver.acquireContentProviderClient(authority) ?: return null
        val authAuthorityUri = Uri.parse("content://$authority")
        val authStateUri = Uri.withAppendedPath(authAuthorityUri, "state")

        return client.use {
            it.query(
                authStateUri,
                arrayOf(KEY_EMAIL, KEY_SESSION_TOKEN, KEY_KSYNC, KEY_KXSCS),
                null, null, null
            )?.use { cursor ->
                cursor.moveToFirst()

                val email = cursor.getString(cursor.getColumnIndex(KEY_EMAIL))
                val sessionToken = cursor.getBlob(cursor.getColumnIndex(KEY_SESSION_TOKEN))?.toHexString()
                val kSync = cursor.getBlob(cursor.getColumnIndex(KEY_KSYNC))?.toHexString()
                val kXSCS = cursor.getString(cursor.getColumnIndex(KEY_KXSCS))

                if (email != null && sessionToken != null && kSync != null && kXSCS != null) {
                    MigratableAccount(
                        email = email,
                        sourcePackage = packageName,
                        authInfo = MigratableAuthInfo(sessionToken, kSync, kXSCS)
                    )
                } else {
                    null
                }
            }
        }
    }

    /**
     * Checks if package exists, and that its signature matches provided value.
     *
     * @param packageManager [PackageManager] used for running checks against [suspectPackage].
     * @param suspectPackage Package name to check.
     * @param expectedSignature Expected signature of the [suspectPackage].
     */
    internal fun packageExistsWithSignature(
        packageManager: PackageManager,
        suspectPackage: String,
        expectedSignature: String
    ): Boolean {
        val suspectSignature = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getSignaturePostAPI28(packageManager, suspectPackage)
        } else {
            getSignaturePreAPI28(packageManager, suspectPackage)
        }

        return expectedSignature == suspectSignature
    }

    /**
     *
     */
    @TargetApi(Build.VERSION_CODES.P)
    internal fun getSignaturePostAPI28(packageManager: PackageManager, packageName: String): String? {
        // For API28+, we can perform some extra checks.
        val packageInfo = try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        } catch (e: PackageManager.NameNotFoundException) {
            return null
        }
        // We don't expect our callers to have multiple signers, so we don't service such requests.
        if (packageInfo.signingInfo.hasMultipleSigners()) {
            return null
        }
        // We currently don't support servicing requests from callers that performed certificate rotation.
        if (packageInfo.signingInfo.hasPastSigningCertificates()) {
            return null
        }

        val signature = packageInfo.signingInfo.signingCertificateHistory[0]
        return signature?.toByteArray()?.toHexString()
    }

    internal fun getSignaturePreAPI28(packageManager: PackageManager, packageName: String): String? {
        // For older APIs, we use the deprecated `signatures` field, which isn't aware of certificate rotation.
        val packageInfo = try {
            @Suppress( "deprecation" )
            packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
        } catch (e: PackageManager.NameNotFoundException) {
            return null
        }

        // TODO VERIFY THIS is correct - is this conflating multiple signers with signature rotation?

        // We don't expect our callers to have multiple signers, so we don't service such requests.
        @Suppress( "deprecation" )
        val signature = if (packageInfo.signatures.size != 1) {
            null
        } else {
            // In case of signature rotation, this will report the oldest used certificate,
            // pretending that the signature rotation never took place.
            // We can only rely on our whitelist being up-to-date in this case.
            packageInfo.signatures[0]
        }

        return signature?.toByteArray()?.toHexString()
    }
}
