package com.lostf1sh.pixelplayeross.data.offline

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.lostf1sh.pixelplayeross.data.database.MusicDao
import com.lostf1sh.pixelplayeross.data.database.OfflineTrackDao
import com.lostf1sh.pixelplayeross.data.database.OfflineTrackEntity
import com.lostf1sh.pixelplayeross.data.jellyfin.JellyfinRepository
import com.lostf1sh.pixelplayeross.data.navidrome.NavidromeRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class OfflineCopyCoversTest {

    @TempDir
    lateinit var filesDir: File

    private val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

    private val rows = CopyOnWriteArrayList<OfflineTrackEntity>()
    private val dao = mockk<OfflineTrackDao> {
        coEvery { getCompleted() } answers {
            rows.filter { it.state == OfflineDownloadStatus.COMPLETE.storageValue }
        }
        coEvery { setCoverUris(any(), any(), any()) } answers {
            val index = rows.indexOfFirst { it.downloadId == firstArg() }
            if (index >= 0) {
                rows[index] = rows[index].copy(songCoverUri = secondArg(), albumCoverUri = thirdArg())
            }
        }
        coEvery { getCoverUrisInUse() } answers {
            rows.flatMap { listOfNotNull(it.songCoverUri, it.albumCoverUri) }.distinct()
        }
    }

    private val songArt = mutableMapOf<String, String>()
    private val albumArt = mutableMapOf<String, String>()
    private val musicDao = mockk<MusicDao> {
        coEvery { getSongArtUriByContentUri(any()) } answers { songArt[firstArg()] }
        coEvery { getAlbumArtUriByContentUri(any()) } answers { albumArt[firstArg()] }
    }

    private val navidrome = mockk<NavidromeRepository> {
        every { isLoggedIn } returns true
        every { getCoverArtUrl(any(), any()) } answers {
            "https://navidrome.example/rest/getCoverArt.view?id=${firstArg<String>()}&size=${secondArg<Int>()}"
        }
    }
    private val jellyfin = mockk<JellyfinRepository> {
        every { isLoggedIn } returns true
        every { getImageUrl(any(), any()) } answers {
            "https://jellyfin.example/Items/${firstArg<String>()}/Images/Primary?maxWidth=${secondArg<Int>()}"
        }
        every { getAuthorizationHeader() } returns "MediaBrowser Token=\"token\""
    }

    private val requestedUrls = CopyOnWriteArrayList<String>()
    private var serverUp = true
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            requestedUrls += chain.request().url.toString()
            if (!serverUp) throw IOException("Connection refused")
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(png.toResponseBody("image/png".toMediaType()))
                .build()
        }
        .build()

    private val context by lazy { mockk<Context> { every { filesDir } returns this@OfflineCopyCoversTest.filesDir } }
    private val covers by lazy { OfflineCopyCovers(context, dao, musicDao, navidrome, jellyfin, client) }

    @Test
    fun `complete offline copy saves its cover at 1000 px`() = runTest {
        addOfflineCopy("navidrome://song-1", songCover = "navidrome_cover://al-7")

        assertThat(covers.saveMissing()).isTrue()

        assertThat(savedFile("navidrome_cover://al-7").readBytes()).isEqualTo(png)
        assertThat(requestedUrls).containsExactly(
            "https://navidrome.example/rest/getCoverArt.view?id=al-7&size=1000"
        )
    }

    @Test
    fun `jellyfin offline copy saves both its own cover and its album cover`() = runTest {
        addOfflineCopy(
            "jellyfin://item-2",
            songCover = "jellyfin_cover://item-2",
            albumCover = "jellyfin_cover://item-1"
        )

        covers.saveMissing()

        assertThat(savedFile("jellyfin_cover://item-2").exists()).isTrue()
        assertThat(savedFile("jellyfin_cover://item-1").exists()).isTrue()
    }

    @Test
    fun `offline copies that share a cover fetch it once`() = runTest {
        addOfflineCopy("navidrome://song-1", songCover = "navidrome_cover://al-7")
        addOfflineCopy("navidrome://song-2", songCover = "navidrome_cover://al-7")

        covers.saveMissing()
        covers.saveMissing()

        assertThat(requestedUrls).hasSize(1)
    }

    @Test
    fun `removing one of several offline copies keeps the shared cover, removing the last deletes it`() = runTest {
        addOfflineCopy("navidrome://song-1", songCover = "navidrome_cover://al-7")
        addOfflineCopy("navidrome://song-2", songCover = "navidrome_cover://al-7")
        covers.saveMissing()

        rows.removeIf { it.sourceUri == "navidrome://song-1" }
        covers.deleteUnused()
        assertThat(savedFile("navidrome_cover://al-7").exists()).isTrue()

        rows.removeIf { it.sourceUri == "navidrome://song-2" }
        covers.deleteUnused()
        assertThat(savedFile("navidrome_cover://al-7").exists()).isFalse()
    }

    @Test
    fun `failed cover leaves the offline copy complete and is fetched on the next run`() = runTest {
        addOfflineCopy("navidrome://song-1", songCover = "navidrome_cover://al-7")
        serverUp = false

        assertThat(covers.saveMissing()).isFalse()
        assertThat(savedFile("navidrome_cover://al-7").exists()).isFalse()
        assertThat(rows.single().state).isEqualTo(OfflineDownloadStatus.COMPLETE.storageValue)

        serverUp = true
        assertThat(covers.saveMissing()).isTrue()
        assertThat(savedFile("navidrome_cover://al-7").exists()).isTrue()
    }

    @Test
    fun `server error body is not saved as a cover`() = runTest {
        val errorClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("""{"error":"not found"}""".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()
        addOfflineCopy("navidrome://song-1", songCover = "navidrome_cover://al-7")

        val saved = OfflineCopyCovers(context, dao, musicDao, navidrome, jellyfin, errorClient).saveMissing()

        assertThat(saved).isFalse()
        assertThat(savedFile("navidrome_cover://al-7").exists()).isFalse()
    }

    @Test
    fun `offline copy still downloading gets no cover`() = runTest {
        addOfflineCopy(
            "navidrome://song-1",
            songCover = "navidrome_cover://al-7",
            state = OfflineDownloadStatus.DOWNLOADING
        )

        covers.saveMissing()

        assertThat(requestedUrls).isEmpty()
    }

    @Test
    fun `cover removed while it was being saved is deleted`() = runTest {
        addOfflineCopy("navidrome://song-1", songCover = "navidrome_cover://al-7")
        val removingClient = client.newBuilder()
            .apply { interceptors().add(0, Interceptor { chain -> rows.clear(); chain.proceed(chain.request()) }) }
            .build()

        OfflineCopyCovers(context, dao, musicDao, navidrome, jellyfin, removingClient).saveMissing()

        assertThat(savedFile("navidrome_cover://al-7").exists()).isFalse()
    }

    @Test
    fun `cleanup keeps files still being written`() {
        val directory = OfflineCopyCovers.directory(filesDir).apply { mkdirs() }
        val inFlight = File(directory, "navidrome_cover_al-7.jpg123.tmp").apply { writeBytes(png) }
        val unused = File(directory, "navidrome_cover_al-8.jpg").apply { writeBytes(png) }

        assertThat(OfflineCopyCovers.unusedFiles(filesDir, emptyList())).containsExactly(unused)
        assertThat(inFlight.exists()).isTrue()
    }

    @Test
    fun `saved cover file ignores the requested size`() {
        assertThat(OfflineCopyCovers.fileFor(filesDir, "navidrome_cover://al-7?size=300"))
            .isEqualTo(OfflineCopyCovers.fileFor(filesDir, "navidrome_cover://al-7"))
        assertThat(OfflineCopyCovers.fileFor(filesDir, "https://navidrome.example/cover.jpg")).isNull()
    }

    private fun savedFile(coverUri: String): File = OfflineCopyCovers.fileFor(filesDir, coverUri)!!

    private fun addOfflineCopy(
        sourceUri: String,
        songCover: String,
        albumCover: String = songCover,
        state: OfflineDownloadStatus = OfflineDownloadStatus.COMPLETE
    ) {
        songArt[sourceUri] = songCover
        albumArt[sourceUri] = albumCover
        rows += OfflineTrackEntity(
            downloadId = CloudOfflineRepository.downloadId(sourceUri),
            attemptId = "attempt-1",
            songId = sourceUri,
            sourceUri = sourceUri,
            provider = CloudOfflineRepository.providerFor(sourceUri)!!,
            title = "Made-up Song",
            mimeType = "audio/flac",
            localPath = null,
            state = state.storageValue,
            createdAt = 0L,
            updatedAt = 0L
        )
    }
}
