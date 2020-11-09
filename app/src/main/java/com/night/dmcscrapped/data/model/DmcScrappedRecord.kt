package com.night.dmcscrapped.data.model
import androidx.room.Entity
import androidx.room.PrimaryKey



//報廢
@Entity(tableName = "dmcScrappedRecord")
data class DmcScrappedRecord(
    @PrimaryKey(autoGenerate = true) var sn: Int? = null,
    val infoId: String,
    val ln: String,
    var optionId: String,
    val panel: String,
    val position: String,
    var setDate: String,
    var uName: String?,
    var uSn: String,
    var gSn: String?,
    var isUpload:Boolean?,
    var isTestData:Boolean?,
    var sSn : Int?,
)

/**
 *
 * state 0 -> 新增 1 -> 清除 2 -> 檢測紀錄 3-> 新增片報 4-> 刪除片報
 *
 * */
@Entity(tableName = "log")
data class MyLog(
    @PrimaryKey(autoGenerate = true) var sn:Int? = null,
    var state: Int,
    val infoId: String,
    val ln: String,
    val optionId: String?,
    var panel: String?,
    var position: String?,
    val setDate: String,
    val uName: String?,
    val uSn: String,
    val gSn: String?,
    var isUpload:Boolean?,
    var logState:String? = null,
    var isTestData:Boolean?,
)
