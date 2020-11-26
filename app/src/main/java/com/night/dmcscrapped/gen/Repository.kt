package com.night.dmcscrapped.gen

import android.content.Context
import android.util.Log
import com.boardtek.appcenter.AppCenter
import com.boardtek.appcenter.NetworkInformation
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.night.dmcscrapped.data.db.MyRoomDB
import com.night.dmcscrapped.data.model.*
import com.night.dmcscrapped.data.model.net.*
import com.night.dmcscrapped.units.NightUnit
import com.night.dmcscrapped.units.OnStateCallback
import org.json.JSONObject


//讀取與下載資料
class Repository(
    private val context: Context
) {
    private val dao = MyRoomDB.getDatabase(context).dao()

    //-------------------------------------------------------
    /**上傳錯誤訊息*/


    //---------------------------------------------------
    /**上傳重要資料*/

    fun uploadClearDmcScrappedRecord(onStateCallback: OnStateCallback? = null) {
        val mac = NetworkInformation.macAddress
        val logList = dao.getUnUploadLog(1)
        val data = logList.map {
            JsonObject().apply {
                addProperty("sn", it.sn)
                addProperty("infoId", it.infoId)
                addProperty("ln", it.ln.replace("\n", ""))
                addProperty("position", it.position)
                addProperty("panel", it.panel)
                addProperty("optionId", it.optionId)
                addProperty("uSn", it.uSn)
                addProperty("uName", it.uName)
                addProperty("setDate", it.setDate)
            }.toString()
        }.joinToString()
        val params = listOf(
            "mac" to mac,
            "data" to "[$data]"
        )
        val re = NightUnit.nRequest(context, P.ACTION_syncDataDel, params, onStateCallback)
        re?.let {
            try {
                val jo = JSONObject(it)
                val state = jo.getString("state")
                when (state) {
                    "okDel" -> {
                        dao.insertLogList(logList.apply {
                            forEach {
                                it.isUpload = true
                                it.logState = "上傳成功(刪除報廢選項)"
                            }
                        })
                    }
                    "error" -> {
                        dao.insertLogList(logList.apply {
                            forEach { it.logState = "上傳失敗(${jo.getString("msg")})" }
                        })
                        throw Exception(jo.getString("msg"))
                    }
                    else -> null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onStateCallback?.onError(it, e)
            }
        }
    }

    //上傳報廢紀錄
    fun uploadAddDmcScrappedRecord(onStateCallback: OnStateCallback? = null) {
        val mac = NetworkInformation.macAddress
        //取得未上暫存傳資料
        val logList = dao.getUnUploadLog(0)
//        val dmcList = dao.getUnUploadDmcRecord()

        val data = logList.map {
            JSONObject(Gson().toJson(it)).apply {
                getString("ln")
                remove("isTestData")
                remove("isUpload")
            }.toString()
        }.joinToString().replace("\\n", "")

        val params = listOf(
            "mac" to mac,
            "data" to "[$data]"
        )
        val re = NightUnit.nRequest(context, P.ACTION_syncData, params, onStateCallback)
        re?.let {
            Log.d("@@@syncTest", "$it")
            //後端回傳
            try {
                val snRes = Gson().fromJson(it, SyncRes::class.java)
                if (snRes.state == "error") {
                    throw Exception(snRes.msg)
                }

                snRes.snList.let {
                    if (snRes.error.isNullOrEmpty()) {
                        dao.insertLogList(logList.apply {
                            forEach {
                                it.isUpload = true
                                it.logState = "上傳成功"
                            }
                        })
                    } else {
                        snRes.error.forEach { errorSn ->
                            logList.find { it.sn.toString() == errorSn.sn }?.let {
                                dao.insertLog(it.apply {
                                    isUpload = false
                                    logState = "上傳失敗 :: ${errorSn.remark}"
                                })
                            }
                        }
                        val t = snRes.error.map { it.sn }
                        snRes.snList.filterNot {
                            t.contains(t.toString())
                        }.forEach { i ->
                            logList.find { it.sn == i }?.let {
                                dao.insertLog(it.apply {
                                    isUpload = true
                                    logState = "上傳成功"
                                })
                            }
                        }

                    }

                }
            } catch (e: Exception) {
                e.printStackTrace()
                onStateCallback?.onError(it, e)
            }
        }
    }

    /**
     * 片報!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     *
     * */

    fun uploadWLScrappedRecord(onStateCallback: OnStateCallback? = null) {
        val mac = NetworkInformation.macAddress
        val logList = dao.getUnUploadLog(3)
        val data = logList.map {
            JsonObject().apply {
                addProperty("ln", it.ln.replace("\n", ""))
                addProperty("optionId", it.optionId)
                addProperty("uSn", it.uSn)
                addProperty("setDate", it.setDate)
                addProperty("gSn", it.gSn)
            }.toString()
        }.joinToString()
        val params = listOf(
            "mac" to mac,
            "data" to "[$data]"
        )
        val re = NightUnit.nRequest(context, P.ACTION_syncPnlScrap, params, onStateCallback)
        re?.let {
            try {
                val jo = JSONObject(it)
                val state = jo.getString("state")
                when (state) {
                    "ok" -> {
                        dao.insertLogList(logList.apply {
                            forEach {
                                it.isUpload = true
                                it.logState = "上傳成功(新增片報)"
                            }
                        })
                    }
                    "error" -> {
                        dao.insertLogList(logList.apply {
                            forEach {
                                it.logState = "上傳失敗(新增片報)"
                            }
                        })
                        throw Exception(jo.getString("msg"))
                    }
                    else -> null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onStateCallback?.onError(it, e)
            }
        }
    }

    fun uploadClearWLeScrappedRecord(onStateCallback: OnStateCallback? = null) {
        val mac = NetworkInformation.macAddress
        val logList = dao.getUnUploadLog(4)
        val data = logList.map {
            JsonObject().apply {
                addProperty("ln", it.ln.replace("\n", ""))
                addProperty("optionId", it.optionId)
                addProperty("uSn", it.uSn)
                addProperty("setDate", it.setDate)
                addProperty("gSn", it.gSn)
            }.toString()
        }.joinToString()
        val params = listOf(
            "mac" to mac,
            "data" to "[$data]"
        )
        val re = NightUnit.nRequest(context, P.ACTION_syncPnlScrapDel, params, onStateCallback)
        re?.let {
            try {
                val jo = JSONObject(it)
                val state = jo.getString("state")
                when (state) {
                    "ok" -> {
                        dao.insertLogList(logList.apply {
                            forEach {
                                it.isUpload = true
                                it.logState = "上傳成功(刪除片報)"
                            }
                        })
                    }
                    "error" -> {
                        dao.insertLogList(logList.apply {
                            forEach {
                                it.logState = "上傳失敗(刪除片報)"
                            }
                        })
                        throw Exception(jo.getString("msg"))
                    }
                    else -> null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onStateCallback?.onError(it, e)
            }
        }
    }


    //上傳檢測紀錄(刷入)
    fun uploadScanInRecord(onStateCallback: OnStateCallback? = null) {
        val list = dao.getUnUploadLog(2)
        val data = JsonArray().apply {
            list.forEach {
                add(JsonObject().apply {
                    addProperty("sn", it.sn)
                    addProperty("ln", it.ln)
                    addProperty("infoId", it.infoId)
                    addProperty("uSn", it.uSn)
                    addProperty("uName", it.uName)
                    addProperty("setDate", it.setDate)
                    addProperty("gSn", it.gSn)
                })
            }
        }.toString().replace("\\n", "")
        val params = listOf(
            "data" to data
        )
        val re = NightUnit.nRequest(context, P.ACTION_syncScanList, params, onStateCallback)
        re?.let {
            try {
                val scanInRes = Gson().fromJson(it, SyncScanInRes::class.java)
                if (scanInRes.state == "error") {
                    throw Exception("上傳檢測紀錄失敗: ${scanInRes.msg}")
                }

                scanInRes.snList?.let {
                    val listInt = it.split(",").map { it.toInt() }
                    if (listInt.size == list.size) {
                        list.forEach {
                            dao.insertLog(it.apply {
                                isUpload = true
                                logState = "上傳成功(檢測)"
                            })
                        }
                    }
                }

                scanInRes.state?.let {
                    if (it == "error") {
                        throw  Exception(scanInRes.msg)
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                onStateCallback?.onError(it, e)
            }
        }
    }

    //漏刷檢查
    fun missBrushCheck(
        dmc: String,
        gSn: String,
        onStateCallback: OnStateCallback? = null
    ): List<MissBrush>? {
        val params = listOf(
            "ln" to dmc.split(",")[0],
            "gSn" to gSn
        )
        val re = NightUnit.nRequest(context, P.ACTION_getMissList, params, onStateCallback)
        return re?.let {
            try {
                val jo = JSONObject(it)
                if (jo.has("state")) {
                    val state = jo.getString("state")
                    if (state == "error") {
                        throw Exception(jo.getString("msg"))
                    }
                }
                val missBrushList =
                    Gson().fromJson<List<MissBrush>>(jo.getJSONArray("data").toString(),
                        object : TypeToken<List<MissBrush>>() {}.type
                    )
                missBrushList
            } catch (e: Exception) {
                e.printStackTrace()
                onStateCallback?.onError(it, e)
                null
            }
        }
    }

    //批號查料號
    suspend fun loadDmcData(dmc: String, onStateCallback: OnStateCallback? = null) {
        val params = listOf(
            "dmc" to dmc
        )
        val result = NightUnit.nRequest(context, P.ACTION_searchPN, params, onStateCallback)
        result?.let {
            try {
                val re = Gson().fromJson(it, Res::class.java)
                //發生錯誤(後端回報錯誤)
                if (re.state == "error") {
                    throw Exception(re.msg)
                }
                //搜尋料號成功後載入單報資料
                loadDmcReportRecord(re.partNumber!!, dmc, re.sn!!, onStateCallback)
            } catch (e: Exception) {
                e.printStackTrace()
                onStateCallback?.onError(it, e)
            }
        }
    }

    //下載單報紀錄資料
    private suspend fun loadDmcReportRecord(
        pn: String,
        ln: String,
        sn: String,
        onStateCallback: OnStateCallback? = null
    ) {
        val params = listOf(
            "ln" to ln
        )
        val result = NightUnit.nRequest(context, P.ACTION_downloadRecord, params, onStateCallback)
        result?.let {
            try {
                val jo = JSONObject(it)
                val infoId = jo.getString("infoId")
                val data = jo.getJSONArray("data")
                //報廢記錄
                val lnSingleReportRecordList = Gson().fromJson<List<DmcScrappedRecord>>(
                    data.toString(),
                    object : TypeToken<List<DmcScrappedRecord>>() {}.type
                )
                //先儲存已存在的紀錄
                dao.setScrappedReport(lnSingleReportRecordList.apply {
                    forEach {
                        it.pn = pn
//                        it.isUpload = true
                        it.isTestData = P.isTest
                    }
                })
                //Insert刷入(檢測)紀錄
                dao.insertLog(
                    MyLog(
                        null,
                        pn,
//                        null,
                        2,
                        infoId,
                        ln,
                        null,
                        null,
                        null,
                        P.getAppCenterTime(),
                        AppCenter.uName,
                        AppCenter.uSn.toString(),
                        P.station.toString(),
                        false,
                        null,
                        P.isTest
                    )
                )
                //取得設定UI需要的資料
                val plate = dao.getPlate(sn)



                //呼叫Callback 正式準備單報作業
                onStateCallback?.onSetPlate(pn, plate, ln, infoId)
            } catch (e: Exception) {
                e.printStackTrace()
                onStateCallback?.onError(it, e)
            }
        }
    }

    //---------------------------------------------------------------
    //讀取版件資訊(初始化)
    suspend fun loadPlateInfo(setDate: String, onStateCallback: OnStateCallback? = null) {
        val params = listOf(
            "setDate" to setDate, //null載全部資料
//            "pn" to pn
        )
        val result = NightUnit.nRequest(context, P.ACTION_loadPnList, params, onStateCallback)
        result?.let {
            try {
                val jo = JSONObject(it)
                val data = jo.getJSONArray("data")
                val plateInfoList = Gson().fromJson<List<PlateInfo>>(
                    data.toString(),
                    object : TypeToken<List<PlateInfo>>() {}.type
                )
                //板件資料為空
//                if (plateInfoList.isNullOrEmpty()) throw Exception("板件資料為空")
                dao.insertPlateInfoList(plateInfoList)
            } catch (e: Exception) {
                e.printStackTrace()
                onStateCallback?.onError(it, e)
            }
        }
    }

    //讀取選項
    suspend fun loadOption(onStateCallback: OnStateCallback? = null) {
        val result = NightUnit.nRequest(context, P.ACTION_getOptions, null, onStateCallback)
        result?.also {
            try {
                val rOption = Gson().fromJson(it, ROption::class.java)
                rOption.also { ro ->
                    dao.insertOption(ro.option)
                    dao.insertOptDep(ro.optDep)
                    dao.insertSnList(SnList(0, ro.snList))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onStateCallback?.onError(it, e)
            }
        }
    }

    //讀取站別
    suspend fun loadStation(onStateCallback: OnStateCallback? = null) {
        val result = NightUnit.nRequest(context, P.ACTION_loadGroup, null, onStateCallback)
        result?.let {
            try {


                val jo = JSONObject(it)
                val data = jo.getJSONArray("data")
                val stationList = Gson().fromJson<List<Station>>(
                    data.toString(),
                    object : TypeToken<List<Station>>() {}.type
                )
                dao.insertStation(stationList)
            } catch (e: Exception) {
                e.printStackTrace()
                onStateCallback?.onError(it,e)
            }
        }
    }

    //讀取藍芽清單
    suspend fun loadBluetoothDeviceList(
        setDate: String,
        onStateCallback: OnStateCallback? = null
    ) {
        val params = listOf(
            "setDate" to setDate
        )
        val result = NightUnit.nRequest(context, P.ACTION_downloadDevice, params, onStateCallback)
        result?.also {
            try {
                val bdList =
                    Gson().fromJson<List<BDevice>>(it, object : TypeToken<List<BDevice>>() {}.type)
                dao.insertTrustBDevice(bdList)
            } catch (e: Exception) {
                e.printStackTrace()
                onStateCallback?.onError(it, e)
            }
        }
    }

    //讀取下載圖檔路件
    suspend fun loadCheckPhoto(
        setDate: String,
        mac: String,
        onStateCallback: OnStateCallback? = null
    ) {
        val params = listOf(
            "setDate" to setDate,
            "MAC" to mac
        )
        val result = NightUnit.nRequest(context, P.ACTION_checkPhoto, params, onStateCallback)
        result?.let {
            try {
                val res = Gson().fromJson(it, Res::class.java)
                when (res.state) {
                    "ok" -> {
                        val paramss = listOf(
                            "fName" to res.fileName!!,
                            "fPath" to res.path!!
                        )
                        loadDownloadPhotoZip(res.fileName, paramss, onStateCallback)
                    }
                    "error" -> onStateCallback?.onState("no new image setting", null)
                    else -> throw Exception(it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onStateCallback?.onError(it, e)
                null
            }
        }
    }

    /*
    * 下載圖片zip檔
    * 成功會直接載入zip二進位檔
    * 失敗才轉型成功
    * */
    private fun loadDownloadPhotoZip(
        fileName: String,
        params: List<Pair<String, String>>,
        onStateCallback: OnStateCallback? = null
    ) {
        val result = NightUnit.downloadZip(context, P.ACTION_downloadPhoto, params, onStateCallback)
        result?.let {
            try {
                storageFile(fileName, it, onStateCallback)
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    val errorRes = Gson().fromJson(it.toString(), Res::class.java)
                    errorRes.msg?.let { }
                } catch (e: Exception) {
                    e.printStackTrace()
                    onStateCallback?.onError(null, e)
                }
            }
        }
    }

    //-----------------------我是分隔線-----net up local down
    fun getTrustBDMac() = dao.getTrustBluetoothMacList()
    suspend fun getPlateInfo(sn: String) = dao.getPlate(sn)
    suspend fun getROption(optionId: String) = dao.getOption(optionId)
    fun getStation() = dao.getStationList()
    fun getDmcScrappedRecordLiveData() = dao.getDmcScrappedRecordListLiveData()
    fun deleteDmcScrappedRecord() = dao.deleteAllDmcScrappedRecord()
    fun getAllScrappedOptionItem() = dao.getAllScrappedOption()

    fun insertDmcRecordList(dmcRecordList: List<DmcScrappedRecord>) =
        dao.insertDmcScrappedRecordList(dmcRecordList)

    fun insertDmcRecord(dmcScrappedRecord: DmcScrappedRecord) =
        dao.insertDmcScrappedRecord(dmcScrappedRecord)

    fun insertLog(myLog: MyLog) = dao.insertLog(myLog)

    fun deleteLog(myLog: MyLog) = dao.deleteLog(myLog)

    fun deleteDmcRecord(dmcScrappedRecord: DmcScrappedRecord) =
        dao.deleteDmcRecord(dmcScrappedRecord)

    fun getDmcRecordByPosition(position: String) = dao.getDmcRecord(position)

    fun getLogLiveData() = dao.getLogLiveData()

    //儲存檔案 -> 解壓縮
    private fun storageFile(
        fileName: String,
        data: ByteArray,
        onStateCallback: OnStateCallback? = null
    ): Boolean {
        onStateCallback?.onState("storageFile", "檔案儲存中...")
        return try {
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
                it.write(data)
            }
            //開始解壓縮
            NightUnit.unZip(context, fileName, onStateCallback)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            onStateCallback?.onError(null, e)
            false
        }
    }

}