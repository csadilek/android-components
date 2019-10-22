/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.downloads

import android.annotation.TargetApi
import android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE
import android.app.DownloadManager.EXTRA_DOWNLOAD_ID
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Environment
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mozilla.components.browser.state.state.content.DownloadState
import mozilla.components.concept.fetch.Client
import mozilla.components.concept.fetch.Header
import mozilla.components.concept.fetch.Headers.Names.CONTENT_LENGTH
import mozilla.components.concept.fetch.Headers.Names.CONTENT_TYPE
import mozilla.components.concept.fetch.Headers.Names.REFERRER
import mozilla.components.concept.fetch.MutableHeaders
import mozilla.components.concept.fetch.Request
import mozilla.components.concept.fetch.toMutableHeaders
import mozilla.components.feature.downloads.ext.addCompletedDownload
import mozilla.components.feature.downloads.ext.getDownloadExtra
import mozilla.components.feature.downloads.ext.withResponse
import mozilla.components.support.base.ids.cancel
import mozilla.components.support.base.ids.notify
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.random.Random

/**
 * Service that performs downloads through a fetch [Client] rather than through the native
 * Android download manager.
 *
 * To use this service, you must create a subclass in your application and it to the manifest.
 */
abstract class AbstractFetchDownloadService : CoroutineService() {

    protected abstract val httpClient: Client
    @VisibleForTesting
    internal val broadcastManager by lazy { LocalBroadcastManager.getInstance(this) }
    @VisibleForTesting
    internal val context: Context get() = this

    private var listOfDownloadJobs = mutableMapOf<Long, DownloadJobState>()

    data class DownloadJobState(
        var job: Job? = null,
        var state: DownloadState,
        var currentBytesCopied: Long = 0,
        var status: DownloadJobStatus,
        var foregroundServiceId: Int = 0
    )

    enum class DownloadJobStatus {
        ACTIVE,
        PAUSED,
        CANCELLED
    }

    private val broadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent?) {
                val downloadId = intent?.extras?.getLong(DownloadNotification.EXTRA_DOWNLOAD_ID) ?: return
                val currentDownloadJobState = listOfDownloadJobs[downloadId] ?: return

                when (intent.action) {
                    ACTION_PAUSE -> {
                        currentDownloadJobState.status = DownloadJobStatus.PAUSED
                        currentDownloadJobState.job?.cancel()
                    }

                    ACTION_RESUME -> {
                        displayOngoingDownloadNotification(currentDownloadJobState.state)
                        currentDownloadJobState.job = startDownloadJob(currentDownloadJobState.state, true)
                        currentDownloadJobState.status = DownloadJobStatus.ACTIVE
                    }

                    ACTION_CANCEL -> {
                        currentDownloadJobState.status = DownloadJobStatus.CANCELLED
                        currentDownloadJobState.job?.cancel()
                        NotificationManagerCompat.from(context).cancel(context, currentDownloadJobState.foregroundServiceId.toString())
                    }
                    ACTION_TRY_AGAIN -> {
                        currentDownloadJobState.job = startDownloadJob(currentDownloadJobState.state, false)
                        currentDownloadJobState.status = DownloadJobStatus.ACTIVE
                        displayOngoingDownloadNotification(currentDownloadJobState.state)
                    }
                }
            }
        }
    }

    // TODO: Do I really need to startForeground immediately? This causes double notifs to appear
    // TODO: cc @NotWoods

    override fun onBind(intent: Intent?): IBinder? = null

    override suspend fun onStartCommand(intent: Intent?, flags: Int) {
        val download = intent?.getDownloadExtra() ?: return
        registerForUpdates()

        // TODO: Is there a problem with using a random int for the foregroundServiceId? Could this cause collisions?
        val foregroundServiceId = Random.nextInt()

        // Create a new job and add it, with its downloadState to the map
        listOfDownloadJobs[download.id] = DownloadJobState(
                job = startDownloadJob(download, false),
                state = download,
                foregroundServiceId = foregroundServiceId,
                status = DownloadJobStatus.ACTIVE
        )
    }

    private fun startDownloadJob(download: DownloadState, isResuming: Boolean): Job {
        return CoroutineScope(IO).launch {
            var tag: String
            val notification = try {
                performDownload(download, isResuming)

                if (listOfDownloadJobs[download.id]?.status == DownloadJobStatus.CANCELLED) { return@launch }

                if (listOfDownloadJobs[download.id]?.status == DownloadJobStatus.PAUSED) {
                    tag = listOfDownloadJobs[download.id]?.foregroundServiceId?.toString()!!
                    DownloadNotification.createPausedDownloadNotification(context, download)
                } else {
                    tag = listOfDownloadJobs[download.id]?.foregroundServiceId?.toString()!!
                    DownloadNotification.createDownloadCompletedNotification(context, download.fileName)
                }
            } catch (e: IOException) {
                tag = listOfDownloadJobs[download.id]?.foregroundServiceId?.toString()!!
                DownloadNotification.createDownloadFailedNotification(context, download)
            }

            NotificationManagerCompat.from(context).notify(
                    context,
                    tag,
                    notification
            )

            sendDownloadCompleteBroadcast(download.id)
        }
    }

    private fun registerForUpdates() {
        val filter = IntentFilter().apply {
            addAction(ACTION_PAUSE)
            addAction(ACTION_RESUME)
            addAction(ACTION_CANCEL)
            addAction(ACTION_TRY_AGAIN)
        }

        context.registerReceiver(broadcastReceiver, filter)
    }

    // TODO: Do I need to unregister?
    private fun unregisterForUpdates() {
        context.unregisterReceiver(broadcastReceiver)
    }

    private fun displayOngoingDownloadNotification(download: DownloadState?) {
        val ongoingDownloadNotification = DownloadNotification.createOngoingDownloadNotification(
            context,
            download
        )

        NotificationManagerCompat.from(context).notify(
            context,
            listOfDownloadJobs[download?.id]?.foregroundServiceId?.toString() ?: "",
            ongoingDownloadNotification
        )
    }

    private fun performDownload(download: DownloadState, isResuming: Boolean) {
        val headers = getHeadersFromDownload(download)
        val request = Request(download.url, headers = headers)
        val response = httpClient.fetch(request)

        response.body.useStream { inStream ->
            val newDownloadState = download.withResponse(response.headers, inStream)
            listOfDownloadJobs[download.id]?.state = newDownloadState

            displayOngoingDownloadNotification(newDownloadState)

            useFileStream(newDownloadState, isResuming) { outStream ->
                if (isResuming) {
                    inStream.skip(listOfDownloadJobs[download.id]?.currentBytesCopied ?: 0)
                }
                copyInChunks(listOfDownloadJobs[download.id]!!, inStream, outStream)
            }
        }
    }

    private fun copyInChunks(downloadJobState: DownloadJobState, inStream: InputStream, outStream: OutputStream) {
        // To ensure that we copy all files (even ones that don't have fileSize, we must NOT check < fileSize
        while (downloadJobState.status == DownloadJobStatus.ACTIVE) {
            val data = ByteArray(chunkSize)
            val bytesRead = inStream.read(data)

            // If bytesRead is -1, there's no data left to read from the stream
            if (bytesRead == -1) { break }

            downloadJobState.currentBytesCopied += bytesRead

            outStream.write(data, 0, bytesRead)
        }
    }

    private fun getHeadersFromDownload(download: DownloadState): MutableHeaders {
        return listOf(
                CONTENT_TYPE to download.contentType,
                CONTENT_LENGTH to download.contentLength?.toString(),
                REFERRER to download.referrerUrl
        ).mapNotNull { (name, value) ->
            if (value.isNullOrBlank()) null else Header(name, value)
        }.toMutableHeaders()
    }

    /**
     * Informs [mozilla.components.feature.downloads.manager.FetchDownloadManager] that a download
     * has been completed.
     */
    private fun sendDownloadCompleteBroadcast(downloadID: Long) {
        val intent = Intent(ACTION_DOWNLOAD_COMPLETE)
        intent.putExtra(EXTRA_DOWNLOAD_ID, downloadID)
        broadcastManager.sendBroadcast(intent)
    }

    /**
     * Creates an output stream on the local filesystem, then informs the system that a download
     * is complete after [block] is run.
     *
     * Encapsulates different behaviour depending on the SDK version.
     */
    internal fun useFileStream(
        download: DownloadState,
        append: Boolean,
        block: (OutputStream) -> Unit
    ) {
        if (SDK_INT >= Build.VERSION_CODES.Q) {
            useFileStreamScopedStorage(download, append, block)
        } else {
            useFileStreamLegacy(download, append, block)
        }
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private fun useFileStreamScopedStorage(download: DownloadState, append: Boolean, block: (OutputStream) -> Unit) {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, download.fileName)
            put(MediaStore.Downloads.MIME_TYPE, download.contentType ?: "*/*")
            put(MediaStore.Downloads.SIZE, download.contentLength)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        // TODO: How do we handle the Q version...?
        Log.d("Sawyer", "in new file stream, append: " + append)

        val resolver = applicationContext.contentResolver
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val item = resolver.insert(collection, values)

        val pfd = resolver.openFileDescriptor(item!!, "w")
        ParcelFileDescriptor.AutoCloseOutputStream(pfd).use(block)

        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(item, values, null, null)
    }

    @TargetApi(Build.VERSION_CODES.P)
    @Suppress("Deprecation")
    private fun useFileStreamLegacy(download: DownloadState, append: Boolean, block: (OutputStream) -> Unit) {
        val dir = Environment.getExternalStoragePublicDirectory(download.destinationDirectory)
        val file = File(dir, download.fileName!!)
        FileOutputStream(file, append).use(block)

        addCompletedDownload(
            title = download.fileName!!,
            description = download.fileName!!,
            isMediaScannerScannable = true,
            mimeType = download.contentType ?: "*/*",
            path = file.absolutePath,
            length = download.contentLength ?: file.length(),
            // Only show notifications if our channel is blocked
            showNotification = !DownloadNotification.isChannelEnabled(context),
            uri = download.url.toUri(),
            referer = download.referrerUrl?.toUri()
        )
    }

    companion object {
        const val ACTION_PAUSE = "mozilla.components.feature.downloads.PAUSE"
        const val ACTION_RESUME = "mozilla.components.feature.downloads.RESUME"
        const val ACTION_CANCEL = "mozilla.components.feature.downloads.CANCEL"
        const val ACTION_TRY_AGAIN = "mozilla.components.feature.downloads.TRY_AGAIN"
        const val chunkSize = 4 * 1024
    }
}
