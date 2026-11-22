package com.lostf1sh.pixelplayeross.data.navidrome

import com.google.common.truth.Truth.assertThat
import com.lostf1sh.pixelplayeross.data.model.ReleaseType
import org.junit.jupiter.api.Test

class NavidromeReleaseTypesTest {

    @Test
    fun `album maps to album`() {
        assertThat(releaseTypeFromNavidrome(listOf("album"))).isEqualTo(ReleaseType.ALBUM)
    }

    @Test
    fun `ep maps to ep`() {
        assertThat(releaseTypeFromNavidrome(listOf("ep"))).isEqualTo(ReleaseType.EP)
    }

    @Test
    fun `single maps to single`() {
        assertThat(releaseTypeFromNavidrome(listOf("single"))).isEqualTo(ReleaseType.SINGLE)
    }

    @Test
    fun `values are matched without regard to case`() {
        assertThat(releaseTypeFromNavidrome(listOf("Album"))).isEqualTo(ReleaseType.ALBUM)
        assertThat(releaseTypeFromNavidrome(listOf("EP"))).isEqualTo(ReleaseType.EP)
        assertThat(releaseTypeFromNavidrome(listOf("SiNgLe"))).isEqualTo(ReleaseType.SINGLE)
    }

    @Test
    fun `primary value with a secondary value keeps the primary type`() {
        assertThat(releaseTypeFromNavidrome(listOf("album", "live"))).isEqualTo(ReleaseType.ALBUM)
        assertThat(releaseTypeFromNavidrome(listOf("live", "single"))).isEqualTo(ReleaseType.SINGLE)
    }

    @Test
    fun `first primary value in the list wins`() {
        assertThat(releaseTypeFromNavidrome(listOf("live", "ep", "album"))).isEqualTo(ReleaseType.EP)
        assertThat(releaseTypeFromNavidrome(listOf("single", "album"))).isEqualTo(ReleaseType.SINGLE)
    }

    @Test
    fun `other values only map to album`() {
        assertThat(releaseTypeFromNavidrome(listOf("broadcast"))).isEqualTo(ReleaseType.ALBUM)
        assertThat(releaseTypeFromNavidrome(listOf("other"))).isEqualTo(ReleaseType.ALBUM)
        assertThat(releaseTypeFromNavidrome(listOf("compilation", "live"))).isEqualTo(ReleaseType.ALBUM)
        assertThat(releaseTypeFromNavidrome(listOf("mixtape"))).isEqualTo(ReleaseType.ALBUM)
    }

    @Test
    fun `empty or absent maps to none`() {
        assertThat(releaseTypeFromNavidrome(emptyList())).isEqualTo(ReleaseType.NONE)
        assertThat(releaseTypeFromNavidrome(null)).isEqualTo(ReleaseType.NONE)
    }
}
