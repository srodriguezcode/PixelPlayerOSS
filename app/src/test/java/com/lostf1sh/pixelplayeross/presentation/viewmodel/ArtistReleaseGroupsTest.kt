package com.lostf1sh.pixelplayeross.presentation.viewmodel

import com.lostf1sh.pixelplayeross.data.model.ReleaseType
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
            )
        )

        assertEquals(
            listOf(ReleaseType.ALBUM, ReleaseType.EP, ReleaseType.SINGLE, ReleaseType.NONE),
            groups.map { it.type }
        )
    }

    @Test
    fun `a group without releases is hidden`() {
        val groups = groupReleaseSections(sections, mapOf(1L to ReleaseType.EP))

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
            )
        )

        assertEquals(listOf("Neon Ritual", "Glad EP", "Paper Boats"), groups[0].sections.map { it.title })
        assertEquals(listOf("Low Tide", "Morning Static"), groups[1].sections.map { it.title })
    }

    @Test
    fun `two albums and three untyped releases give albums then other releases`() {
        val groups = groupReleaseSections(sections, mapOf(1L to ReleaseType.ALBUM, 2L to ReleaseType.ALBUM))

        assertEquals(listOf(ReleaseType.ALBUM, ReleaseType.NONE), groups.map { it.type })
        assertEquals(2, groups[0].sections.size)
        assertEquals(3, groups[1].sections.size)
        assertTrue(groups.showsGroupTitles())
    }

    @Test
    fun `no group titles when every release has no type`() {
        val groups = groupReleaseSections(sections, mapOf(1L to ReleaseType.NONE))

        assertEquals(1, groups.size)
        assertEquals(sections, groups.single().sections)
        assertFalse(groups.showsGroupTitles())
    }

    @Test
    fun `no sections give no groups`() {
        assertTrue(groupReleaseSections(emptyList(), emptyMap()).isEmpty())
    }

    private fun section(albumId: Long, title: String, year: Int) = ArtistAlbumSection(
        albumId = albumId,
        title = title,
        year = year,
        albumArtUriString = null,
        songs = emptyList()
    )
}
