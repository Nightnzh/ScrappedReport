package com.night.dmcscrapped.data.model.net

/**
 * 統一資料格式
 * 用於解析後端系統回傳
 *
 * 利用Gson 轉換成物件
 *
 */
data class Res(
    val state : String?,
    val msg : String?,
    val errorCode: Int?,
    val fileName : String?,
    val path : String?,
    val sn : String?,
    val partNumber : String?,
    val setDate: String?,
)