package com.night.dmcscrapped.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey


//板件資訊
@Entity(tableName = "plateInfo")
data class PlateInfo(
    @PrimaryKey val pn: String,
    val allowAcceptQty: String,
    val codePanel: String,
    val model: String,
    val pcs: String,
    val pic_btm_name: String,
    val pic_top_name: String,
    val scale_bottom: String,
    val scale_left: String,
    val scale_right: String,
    val scale_top: String,
    val setDate: String,
    val sn: String
)