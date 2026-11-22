package com.lostf1sh.pixelplayeross.data.offline

import android.content.Context
import coil.fetch.SourceResult
import com.lostf1sh.pixelplayeross.data.database.MusicDao
import com.lostf1sh.pixelplayeross.data.database.OfflineTrackDao
import com.lostf1sh.pixelplayeross.data.database.OfflineTrackEntity
import com.lostf1sh.pixelplayeross.data.image.RemoteArtworkCache
import com.lostf1sh.pixelplayeross.data.jellyfin.JellyfinRepository
import com.lostf1sh.pixelplayeross.data.navidrome.NavidromeRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

/**
 * Covers of offline copies, kept in `filesDir` (Android can clear `cacheDir`) so they show with
 * the music server down. One file per cover, deleted with the last offline copy that uses it.
 */
@Singleton
class OfflineCopyCovers @Inject constructor(
    @ApplicationContext context: Context,
    private val dao: OfflineTrackDao,
    private val musicDao: MusicDao,
    private val navidromeRepository: NavidromeRepository,
    private val jellyfinRepository: JellyfinRepository,
    private val okHttpClient: OkHttpClient
) {
    private val filesDir: File = context.filesDir

    /**
     * Fetches every missing cover of a COMPLETE offline copy. Returns false when one could not
     * be fetched; the offline copy stays COMPLETE either way.
     */
    suspend fun saveMissing(): Boolean = withContext(Dispatchers.IO) {
        var allSaved = true
        for (offlineCopy in dao.getCompleted()) {
            for (coverUri in coverUris(offlineCopy)) {
                val file = fileFor(filesDir, coverUri) ?: continue
                if (RemoteArtworkCache.isCachedImage(file)) continue
                try {
                    val request = coverRequest(coverUri) ?: continue
                    file.parentFile?.mkdirs()
                    RemoteArtworkCache.download(okHttpClient, request, file)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    allSaved = false
                    Timber.tag(TAG).w(e, "Could not save cover %s", coverUri)
                }
            }
        }
        deleteUnused()
        allSaved
    }

    /** Deletes saved covers no offline copy uses, also ones saved for a copy removed mid-fetch. */
    suspend fun deleteUnused() = withContext(Dispatchers.IO) {
        unusedFiles(filesDir, dao.getCoverUrisInUse()).forEach(File::delete)
    }

    /**
     * The row's cover URIs, read from the library and stored on the row the first time. They
     * are stored before the cover is fetched, so a concurrent [deleteUnused] keeps the file.
     */
    private suspend fun coverUris(offlineCopy: OfflineTrackEntity): List<String> {
        var songCoverUri = offlineCopy.songCoverUri
        var albumCoverUri = offlineCopy.albumCoverUri
        if (songCoverUri == null && albumCoverUri == null) {
            songCoverUri = musicDao.getSongArtUriByContentUri(offlineCopy.sourceUri)
                ?.takeIf { RemoteArtworkCache.parseCoverUri(it) != null }
            albumCoverUri = musicDao.getAlbumArtUriByContentUri(offlineCopy.sourceUri)
                ?.takeIf { RemoteArtworkCache.parseCoverUri(it) != null }
            if (songCoverUri == null && albumCoverUri == null) return emptyList()
            dao.setCoverUris(offlineCopy.downloadId, songCoverUri, albumCoverUri)
        }
        return listOfNotNull(songCoverUri, albumCoverUri).distinct()
    }

    /** Null with no login to that music server; the cover is skipped, since a retry would not help. */
    private fun coverRequest(coverUri: String): Request? {
        val cover = RemoteArtworkCache.parseCoverUri(coverUri) ?: return null
        return when (cover.scheme) {
            RemoteArtworkCache.NAVIDROME_COVER_SCHEME -> {
                if (!navidromeRepository.isLoggedIn) return null
                val url = navidromeRepository.getCoverArtUrl(cover.coverId, COVER_SIZE_PX) ?: return null
                Request.Builder().url(url).get().build()
            }
            RemoteArtworkCache.JELLYFIN_COVER_SCHEME -> {
                if (!jellyfinRepository.isLoggedIn) return null
                val url = jellyfinRepository.getImageUrl(cover.coverId, COVER_SIZE_PX) ?: return null
                val authHeader = jellyfinRepository.getAuthorizationHeader()
                Request.Builder()
                    .url(url)
                    .apply { if (authHeader != null) header(AUTHORIZATION_HEADER, authHeader) }
                    .get()
                    .build()
            }
            else -> null
        }
    }

    companion object {
        private const val TAG = "OfflineCopyCovers"
        private const val DIRECTORY = "offline_copy_covers"
        private const val AUTHORIZATION_HEADER = "Authorization"

        /** Suffix of the files [RemoteArtworkCache.download] writes before renaming them into place. */
        private const val IN_FLIGHT_SUFFIX = ".tmp"

        /** One size for every screen; Coil scales it down. */
        const val COVER_SIZE_PX = 1000

        fun directory(filesDir: File): File = File(filesDir, DIRECTORY)

        fun fileFor(filesDir: File, scheme: String, coverId: String): File =
            File(directory(filesDir), "${scheme}_$coverId.jpg")

        /** Saved cover file for [rawCoverUri] at any `?size=`, or null when it is not a cover URI. */
        fun fileFor(filesDir: File, rawCoverUri: String): File? =
            RemoteArtworkCache.parseCoverUri(rawCoverUri)
                ?.let { fileFor(filesDir, it.scheme, it.coverId) }

        /** The saved cover of [coverId], or null when no offline copy has saved it. */
        suspend fun savedResult(filesDir: File, scheme: String, coverId: String): SourceResult? =
            RemoteArtworkCache.cachedResult(fileFor(filesDir, scheme, coverId))

        /** Saved covers whose URI is not in [coverUrisInUse]. Files still being written are kept. */
        fun unusedFiles(filesDir: File, coverUrisInUse: Collection<String>): List<File> {
            val inUse = coverUrisInUse.mapNotNull { fileFor(filesDir, it)?.name }.toSet()
            return directory(filesDir).listFiles().orEmpty()
                .filter { it.isFile && !it.name.endsWith(IN_FLIGHT_SUFFIX) && it.name !in inUse }
        }
    }
}
