package com.lostf1sh.pixelplayeross.data.database

import androidx.room.ColumnInfo
import com.lostf1sh.pixelplayeross.data.model.ReleaseType

data class AlbumReleaseTypeRow(
    @ColumnInfo(name = "album_id") val albumId: Long,
    @ColumnInfo(name = "release_type") val releaseType: ReleaseType
)
