package com.night.dmcscrapped.data.model.net

data class SyncRes(
    val snList :List<Int>,
    val error : List<ErrorSn>?,
    val state : String?,
    val msg : String?,
)

data class ErrorSn(
    val sn: String,
    val remark : String
)


data class SyncScanInRes(
    val snList : String?,
    val state : String?,
    val msg : String?,
)