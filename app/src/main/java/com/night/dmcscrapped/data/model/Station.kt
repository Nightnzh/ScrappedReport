package com.night.dmcscrapped.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "station")
data class Station(
    @PrimaryKey val sn: String,
    val depSn: String,
    val sort: String,
    val title: String
)