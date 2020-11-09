package com.night.dmcscrapped.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey


data class ROption(
    var option: List<ROptionItem>,
    var optDep: List<OptDep>,
    var snList: String
)


//報廢選項
@Entity(tableName = "retirement_option")
data class ROptionItem(
    @PrimaryKey var sn : String,
    var title : String,
    var optNo: String,
    var state: String,
    var isLock: String,
    var uSn: String,
    var setDate: String,
    var depSn : String?
)

//待續
@Entity(tableName = "optDep")
data class OptDep(
    @PrimaryKey val optSn : String,
    var depSn : String,
    var sortId: String?,
    var uSn : String,
    var setDate: String
)

@Entity(tableName = "snList")
data class SnList(
    @PrimaryKey var primaryKey : Int? = 0,
    var snList : String
)

