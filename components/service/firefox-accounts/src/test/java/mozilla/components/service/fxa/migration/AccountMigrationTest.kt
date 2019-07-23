package mozilla.components.service.fxa.migration

import android.content.ContentProviderClient
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.content.pm.SigningInfo
import android.database.Cursor
import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.service.fxa.migration.AccountMigration.KEY_EMAIL
import mozilla.components.service.fxa.migration.AccountMigration.KEY_KSYNC
import mozilla.components.service.fxa.migration.AccountMigration.KEY_KXSCS
import mozilla.components.service.fxa.migration.AccountMigration.KEY_SESSION_TOKEN
import mozilla.components.support.ktx.kotlin.toHexString
import mozilla.components.support.test.any
import mozilla.components.support.test.mock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`

@RunWith(AndroidJUnit4::class)
class AccountMigrationTest {

    @Test
    fun `getSignaturePostAPI28 - return null if app does not exist`() {
        val packageManager: PackageManager = mock()
        val packageName = "org.mozilla.firefox"

        `when`(packageManager.getPackageInfo(anyString(), anyInt())).thenThrow(PackageManager.NameNotFoundException())
        assertNull(AccountMigration.getSignaturePostAPI28(packageManager, packageName))
    }

    @Test
    fun `getSignaturePostAPI28 - return null if it has multiple signers`() {
        val packageManager: PackageManager = mock()
        val packageName = "org.mozilla.firefox"
        val packageInfo: PackageInfo = mock()
        val signingInfo: SigningInfo = mock()
        packageInfo.signingInfo = signingInfo

        `when`(signingInfo.hasMultipleSigners()).thenReturn(true)
        `when`(packageManager.getPackageInfo(anyString(), anyInt())).thenReturn(packageInfo)
        assertNull(AccountMigration.getSignaturePostAPI28(packageManager, packageName))
    }

    @Test
    fun `getSignaturePostAPI28 - return null if certificate rotation was performed`() {
        val packageManager: PackageManager = mock()
        val packageName = "org.mozilla.firefox"
        val packageInfo: PackageInfo = mock()
        val signingInfo: SigningInfo = mock()
        packageInfo.signingInfo = signingInfo

        `when`(signingInfo.hasMultipleSigners()).thenReturn(false)
        `when`(signingInfo.hasPastSigningCertificates()).thenReturn(true)
        `when`(packageManager.getPackageInfo(anyString(), anyInt())).thenReturn(packageInfo)
        assertNull(AccountMigration.getSignaturePostAPI28(packageManager, packageName))
    }

    @Test
    fun `getSignaturePostAPI28 - return first available signature`() {
        val packageManager: PackageManager = mock()
        val packageName = "org.mozilla.firefox"
        val packageInfo: PackageInfo = mock()
        val signingInfo: SigningInfo = mock()
        val signatureValue = "308201".toByteArray()
        val signature = Signature(signatureValue)
        val signatures = arrayOf(signature)

        packageInfo.signingInfo = signingInfo

        `when`(signingInfo.signingCertificateHistory).thenReturn(signatures)
        `when`(signingInfo.hasMultipleSigners()).thenReturn(false)
        `when`(signingInfo.hasPastSigningCertificates()).thenReturn(false)
        `when`(packageManager.getPackageInfo(anyString(), anyInt())).thenReturn(packageInfo)
        val result = AccountMigration.getSignaturePostAPI28(packageManager, packageName)

        assertEquals(signatureValue.toHexString(), result)
    }

    @Test
    fun `getSignaturePreAPI28 - return null if app does not exist`() {
        val packageManager: PackageManager = mock()
        val packageName = "org.mozilla.firefox"

        `when`(packageManager.getPackageInfo(anyString(), anyInt())).thenThrow(PackageManager.NameNotFoundException())
        assertNull(AccountMigration.getSignaturePreAPI28(packageManager, packageName))
    }

    @Test
    @Suppress("DEPRECATION")
    fun `getSignaturePreAPI28 - return null if it has multiple signers`() {
        val packageManager: PackageManager = mock()
        val packageName = "org.mozilla.firefox"
        val packageInfo: PackageInfo = mock()
        packageInfo.signatures = arrayOf(Signature("00"), Signature("11"))

        `when`(packageManager.getPackageInfo(anyString(), anyInt())).thenReturn(packageInfo)
        assertNull(AccountMigration.getSignaturePreAPI28(packageManager, packageName))
    }

    @Test
    @Suppress("DEPRECATION")
    fun `getSignaturePreAPI28 - return first available signature`() {
        val packageManager: PackageManager = mock()
        val packageName = "org.mozilla.firefox"
        val packageInfo: PackageInfo = mock()
        val signatureValue = "308201".toByteArray()
        packageInfo.signatures = arrayOf(Signature(signatureValue))

        `when`(packageManager.getPackageInfo(anyString(), anyInt())).thenReturn(packageInfo)
        val result = AccountMigration.getSignaturePreAPI28(packageManager, packageName)
        assertEquals(signatureValue.toHexString(), result)
    }

    @Test
    fun queryMigratableAccounts() {
        val context: Context = mock()
        val packageNameRelease = "org.mozilla.firefox"
        val packageNameBeta = "org.mozilla.firefox_beta"
        val packageManager: PackageManager = mock()
        val packageInfo: PackageInfo = mock()
        val signingInfo: SigningInfo = mock()
        val signatureValue = AccountMigration.ecosystemProviderPackages[0].second
        val signature = Signature(signatureValue)
        val signatures = arrayOf(signature)
        val contentResolver: ContentResolver = mock()
        val contentProviderClient: ContentProviderClient = mock()
        packageInfo.signingInfo = signingInfo

        `when`(signingInfo.signingCertificateHistory).thenReturn(signatures)
        `when`(signingInfo.hasMultipleSigners()).thenReturn(false)
        `when`(signingInfo.hasPastSigningCertificates()).thenReturn(false)
        `when`(packageManager.getPackageInfo(anyString(), anyInt())).thenReturn(packageInfo)
        `when`(context.packageManager).thenReturn(packageManager)
        `when`(contentResolver.acquireContentProviderClient("$packageNameRelease.fxa.auth"))
                .thenReturn(contentProviderClient)
        `when`(contentResolver.acquireContentProviderClient("$packageNameBeta.fxa.auth"))
                .thenReturn(contentProviderClient)
        `when`(context.contentResolver).thenReturn(contentResolver)

        // Account without tokens should not be returned
        val expiredAccount = createAccount("user@mozilla.org")
        `when`(contentProviderClient.query(any(), any(), any(), any(), any())).thenReturn(expiredAccount)
        var result = AccountMigration.queryMigratableAccounts(context)
        assertTrue(result.isEmpty())

        // Accounts with valid tokens should be returned in order
        val validAccount = createAccount("user@mozilla.org", "sessionToken", "ksync", "kxscs")
        `when`(contentProviderClient.query(any(), any(), any(), any(), any())).thenReturn(validAccount)
        result = AccountMigration.queryMigratableAccounts(context)
        assertEquals(2, result.size)
        val expectedAccountRelease = MigratableAccount(
            "user@mozilla.org",
            packageNameRelease,
            MigratableAuthInfo(
                "sessionToken".toByteArray().toHexString(),
                "ksync".toByteArray().toHexString(),
                "kxscs"
            )
        )
        assertEquals(expectedAccountRelease, result[0])

        val expectedAccountBeta = MigratableAccount(
                "user@mozilla.org",
                packageNameBeta,
                MigratableAuthInfo(
                    "sessionToken".toByteArray().toHexString(),
                    "ksync".toByteArray().toHexString(),
                    "kxscs"
                )
        )
        assertEquals(expectedAccountBeta, result[1])
    }

    private fun createAccount(
        email: String? = null,
        sessionToken: String? = null,
        ksync: String? = null,
        kxscs: String? = null
    ): Cursor {
        val cursor: Cursor = mock()
        `when`(cursor.getColumnIndex(KEY_EMAIL)).thenReturn(0)
        `when`(cursor.getColumnIndex(KEY_SESSION_TOKEN)).thenReturn(1)
        `when`(cursor.getColumnIndex(KEY_KSYNC)).thenReturn(2)
        `when`(cursor.getColumnIndex(KEY_KXSCS)).thenReturn(3)
        `when`(cursor.getString(0)).thenReturn(email)
        `when`(cursor.getBlob(1)).thenReturn(sessionToken?.toByteArray())
        `when`(cursor.getBlob(2)).thenReturn(ksync?.toByteArray())
        `when`(cursor.getString(3)).thenReturn(kxscs)
        return cursor
    }
}