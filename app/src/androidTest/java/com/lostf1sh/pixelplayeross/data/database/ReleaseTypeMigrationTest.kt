package com.lostf1sh.pixelplayeross.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReleaseTypeMigrationTest {

    @get:Rule
    val migrationHelper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        databaseClass = PixelPlayerDatabase::class.java,
    )

    @Test
    @Throws(IOException::class)
    fun migrationFromSevenToEightGivesExistingRowsReleaseTypeNone() {
        migrationHelper.createDatabase(DATABASE_NAME, 7).apply {
            execSQL(
                "INSERT INTO albums (id, title, artist_name, artist_id, album_art_uri_string, " +
                    "song_count, date_added, year, album_artist) VALUES " +
                    "(-42, 'Harbor Lights', 'The Example Band', -7, NULL, 9, 0, 2021, NULL)"
            )
            execSQL(
                "INSERT INTO navidrome_songs (id, navidrome_id, playlist_id, title, artist, " +
                    "artist_id, album_artist, album, album_id, cover_art_id, duration, " +
                    "track_number, disc_number, year, genre, bitRate, mime_type, suffix, path, " +
                    "date_added) VALUES " +
                    "('library_s1', 's1', 'library', 'Tide Song', 'The Example Band', NULL, NULL, " +
                    "'Harbor Lights', 'al1', NULL, 180000, 1, 1, 2021, NULL, NULL, NULL, NULL, " +
                    "'Example/Harbor Lights/01.flac', 0)"
            )
            close()
        }

        migrationHelper.runMigrationsAndValidate(
            name = DATABASE_NAME,
            version = 8,
            validateDroppedTables = true,
            MIGRATION_7_8,
        ).use { database ->
            assertThat(database.releaseTypes("albums")).containsExactly("NONE")
            assertThat(database.releaseTypes("navidrome_songs")).containsExactly("NONE")
        }
    }

    private fun SupportSQLiteDatabase.releaseTypes(table: String): List<String> {
        return query("SELECT release_type FROM $table").use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.getString(0))
            }
        }
    }

    private companion object {
        const val DATABASE_NAME = "release-type-migration-test"
    }
}
