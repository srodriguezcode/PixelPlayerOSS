package com.lostf1sh.pixelplayeross.data.navidrome

import com.lostf1sh.pixelplayeross.data.model.ReleaseType

private val PRIMARY_RELEASE_TYPES = mapOf(
    "album" to ReleaseType.ALBUM,
    "ep" to ReleaseType.EP,
    "single" to ReleaseType.SINGLE
)

fun releaseTypeFromNavidrome(values: List<String>?): ReleaseType {
    if (values.isNullOrEmpty()) return ReleaseType.NONE
    return values.firstNotNullOfOrNull { PRIMARY_RELEASE_TYPES[it.trim().lowercase()] }
        ?: ReleaseType.ALBUM
}
