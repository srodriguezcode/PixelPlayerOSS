package com.lostf1sh.pixelplayeross.presentation.viewmodel

import com.lostf1sh.pixelplayeross.data.model.ArtistRef
import com.lostf1sh.pixelplayeross.data.model.ReleaseType
import com.lostf1sh.pixelplayeross.data.model.Song
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ArtistReleaseGroupsTest {

    private val sections = listOf(
        section(1L, "Neon Ritual", 2023),
        section(2L, "Low Tide", 2021),
        section(3L, "Glad EP", 2020),
        section(4L, "Morning Static", 2019),
        section(5L, "Paper Boats", 2018)
    )

    @Test
    fun `groups come as albums, EPs, singles, other releases`() {
        val groups = groupReleaseSections(
            sections,
            mapOf(
                1L to ReleaseType.SINGLE,
                2L to ReleaseType.EP,
                3L to ReleaseType.ALBUM,
                4L to ReleaseType.NONE,
                5L to ReleaseType.ALBUM
            ),
            ARTIST_ID
        )

        assertEquals(
            listOf(ReleaseType.ALBUM, ReleaseType.EP, ReleaseType.SINGLE, ReleaseType.NONE),
            groups.map { it.type }
        )
    }

    @Test
    fun `a group without releases is hidden`() {
        val groups = groupReleaseSections(sections, mapOf(1L to ReleaseType.EP), ARTIST_ID)

        assertEquals(listOf(ReleaseType.EP, ReleaseType.NONE), groups.map { it.type })
    }

    @Test
    fun `newest release comes first in each group`() {
        val groups = groupReleaseSections(
            sections,
            mapOf(
                1L to ReleaseType.ALBUM,
                2L to ReleaseType.EP,
                3L to ReleaseType.ALBUM,
                4L to ReleaseType.EP,
                5L to ReleaseType.ALBUM
            ),
            ARTIST_ID
        )

        assertEquals(listOf("Neon Ritual", "Glad EP", "Paper Boats"), groups[0].sections.map { it.title })
        assertEquals(listOf("Low Tide", "Morning Static"), groups[1].sections.map { it.title })
    }

    @Test
    fun `two albums and three untyped releases give albums then other releases`() {
        val groups = groupReleaseSections(sections, mapOf(1L to ReleaseType.ALBUM, 2L to ReleaseType.ALBUM), ARTIST_ID)

        assertEquals(listOf(ReleaseType.ALBUM, ReleaseType.NONE), groups.map { it.type })
        assertEquals(2, groups[0].sections.size)
        assertEquals(3, groups[1].sections.size)
        assertTrue(groups.showsGroupTitles())
    }

    @Test
    fun `no group titles when every release has no type`() {
        val groups = groupReleaseSections(sections, mapOf(1L to ReleaseType.NONE), ARTIST_ID)

        assertEquals(1, groups.size)
        assertEquals(sections, groups.single().sections)
        assertFalse(groups.showsGroupTitles())
    }

    @Test
    fun `songs in any order give newest release first in each group`() {
        val songs = listOf(
            song("a", "Paper Boats", 5L, 2018),
            song("b", "Neon Ritual", 1L, 2023),
            song("c", "Undated Demos", 6L, 0),
            song("d", "Low Tide", 2L, 2021),
            song("e", "Glad EP", 3L, 2020)
        )

        val groups = groupReleaseSections(
            buildAlbumSections(songs),
            mapOf(5L to ReleaseType.ALBUM, 1L to ReleaseType.ALBUM, 6L to ReleaseType.ALBUM, 3L to ReleaseType.EP),
            ARTIST_ID
        )

        assertEquals(listOf("Neon Ritual", "Paper Boats", "Undated Demos"), groups[0].sections.map { it.title })
        assertEquals(listOf("Glad EP"), groups[1].sections.map { it.title })
        assertEquals(listOf("Low Tide"), groups[2].sections.map { it.title })
    }

    @Test
    fun `album where the artist is primary on one of three songs stays under albums`() {
        val album = section(
            1L, "Neon Ritual", 2023,
            listOf(featuring("a"), primary("b"), featuring("c"))
        )

        val groups = groupReleaseSections(listOf(album), mapOf(1L to ReleaseType.ALBUM), ARTIST_ID)

        assertEquals(listOf(ReleaseType.ALBUM), groups.map { it.type })
    }

    @Test
    fun `album where the artist only features goes under singles`() {
        val album = section(1L, "Neon Ritual", 2023, listOf(featuring("a"), featuring("b")))

        val groups = groupReleaseSections(listOf(album), mapOf(1L to ReleaseType.ALBUM), ARTIST_ID)

        assertEquals(listOf(ReleaseType.SINGLE), groups.map { it.type })
    }

    @Test
    fun `featured-only album and own single share singles, newest first`() {
        val ownSingle = section(1L, "Paper Boats", 2018, listOf(primary("a")))
        val featuredAlbum = section(2L, "Low Tide", 2021, listOf(featuring("b")))

        val groups = groupReleaseSections(
            buildAlbumSections(ownSingle.songs + featuredAlbum.songs),
            mapOf(1L to ReleaseType.SINGLE, 2L to ReleaseType.ALBUM),
            ARTIST_ID
        )

        assertEquals(listOf(ReleaseType.SINGLE), groups.map { it.type })
        assertEquals(listOf("Low Tide", "Paper Boats"), groups.single().sections.map { it.title })
    }

    @Test
    fun `featured-only EP stays under EPs`() {
        val ep = section(1L, "Glad EP", 2020, listOf(featuring("a")))

        val groups = groupReleaseSections(listOf(ep), mapOf(1L to ReleaseType.EP), ARTIST_ID)

        assertEquals(listOf(ReleaseType.EP), groups.map { it.type })
    }

    @Test
    fun `album with songs that carry no artist list counts as the artist's own when the song artist matches`() {
        val album = section(1L, "Neon Ritual", 2023, listOf(song("a", "Neon Ritual", 1L, 2023)))

        val groups = groupReleaseSections(listOf(album), mapOf(1L to ReleaseType.ALBUM), ARTIST_ID)

        assertEquals(listOf(ReleaseType.ALBUM), groups.map { it.type })
    }

    @Test
    fun `album where no artist is marked primary counts as the first artist's own`() {
        val song = Song.emptySong().copy(
            id = "a",
            artistId = OTHER_ID,
            artists = listOf(ArtistRef(ARTIST_ID, "Mira Vale"), ArtistRef(OTHER_ID, "Juno Park"))
        )
        val album = section(1L, "Neon Ritual", 2023, listOf(song))

        val groups = groupReleaseSections(listOf(album), mapOf(1L to ReleaseType.ALBUM), ARTIST_ID)

        assertEquals(listOf(ReleaseType.ALBUM), groups.map { it.type })
    }

    @Test
    fun `without an artist id a compilation stays under albums`() {
        val compilation = section(1L, "Summer Mixtape", 2022, listOf(featuring("a"), featuring("b")))

        val groups = groupReleaseSections(listOf(compilation), mapOf(1L to ReleaseType.ALBUM), artistId = null)

        assertEquals(listOf(ReleaseType.ALBUM), groups.map { it.type })
    }

    @Test
    fun `no sections give no groups`() {
        assertTrue(groupReleaseSections(emptyList(), emptyMap(), ARTIST_ID).isEmpty())
    }

    private fun song(id: String, album: String, albumId: Long, year: Int) = Song.emptySong().copy(
        id = id,
        title = "Track $id",
        artistId = ARTIST_ID,
        album = album,
        albumId = albumId,
        year = year
    )

    private fun primary(id: String) = Song.emptySong().copy(
        id = id,
        artistId = ARTIST_ID,
        artists = listOf(ArtistRef(ARTIST_ID, "Mira Vale", isPrimary = true), ArtistRef(OTHER_ID, "Juno Park"))
    )

    private fun featuring(id: String) = Song.emptySong().copy(
        id = id,
        artistId = OTHER_ID,
        artists = listOf(ArtistRef(OTHER_ID, "Juno Park", isPrimary = true), ArtistRef(ARTIST_ID, "Mira Vale"))
    )

    private fun section(
        albumId: Long,
        title: String,
        year: Int,
        songs: List<Song> = listOf(primary("p$albumId"))
    ) = ArtistAlbumSection(
        albumId = albumId,
        title = title,
        year = year,
        albumArtUriString = null,
        songs = songs.map { it.copy(album = title, albumId = albumId, year = year) }
    )

    private companion object {
        const val ARTIST_ID = 7L
        const val OTHER_ID = 8L
    }
}
