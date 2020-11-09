package com.night.dmcscrapped.ui.scrapedoption

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.room.Query
import com.boardtek.appcenter.AppCenter
import com.boardtek.appcenter.NetworkInformation
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.night.dmcscrapped.data.db.MyRoomDB
import com.night.dmcscrapped.data.model.DmcScrappedRecord
import com.night.dmcscrapped.data.model.MyLog
import com.night.dmcscrapped.data.model.ROptionItem
import com.night.dmcscrapped.data.model.net.Res
import com.night.dmcscrapped.data.model.net.SyncRes
import com.night.dmcscrapped.gen.P
import com.night.dmcscrapped.gen.Repository
import com.night.dmcscrapped.units.NightUnit
import com.night.dmcscrapped.units.OnStateCallback
import kotlinx.coroutines.*
import org.json.JSONObject


class OptionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = Repository(application)
    var dmcScrappedRecord: DmcScrappedRecord? = null
    var nowPosition: String? = null

    //所有報廢選項
    val allScrappedOption = repository.getAllScrappedOptionItem()

    //當下報廢選項
    var optionItem: ROptionItem? = null

    //Insert單報資料(pcs)
    fun setScrappedData(
        dmcScrappedRecord: DmcScrappedRecord? = null,
        onStateCallback: OnStateCallback
    ) =
        GlobalScope.launch(Dispatchers.Default) {
            var panel = when (P.display) {
                0, 1 -> 0
                2 -> 1
                else -> 2
            }
            val setDate = P.getAppCenterTime()

            //檢查 484 片報
            val log = MyLog(
                null,
                0,
                P.infoId.toString(),
                P.dmcCode!!,
                optionItem?.sn,
                panel.toString(),
                nowPosition,
                setDate,
                AppCenter.uName,
                AppCenter.uSn.toString(),
                P.station.toString(),
                false,
                null,
                P.isTest
            )

            var key: Long? = null
            var dmcKey :Long? =null
            var dmcRecord : DmcScrappedRecord? = null
            val dmcList : MutableList<DmcScrappedRecord> = mutableListOf()

            when (optionItem!!.sn) {
                "225", "227" -> {
                    log.apply {
                        state = 3 //整片報廢
                        panel = 2
                        position = null
                    }
                    val size = P.pSize.split("*").map { it.toInt() }.reduce { acc, i -> acc*i }
                    for (i in 1..size) {
                        dmcList.add(
                            DmcScrappedRecord(
                                null,
                                log.infoId,
                                log.ln,
                                log.optionId.toString(),
                                log.panel.toString(),
                                i.toString(),
                                log.setDate,
                                log.uName,
                                log.uSn,
                                log.gSn,
                                log.isUpload,
                                log.isTestData,
                                null
                            )
                        )
                    }
                    val kkey = repository.insertLog(log)
                    log.sn = kkey.toInt()
                    repository.insertDmcRecordList(dmcList.toList())
                }
                else -> {
                    key = repository.insertLog(log)
                    log.apply {
                        sn = key.toInt()
                    }
                    dmcRecord = dmcScrappedRecord?.apply {
                        sSn = key.toInt()
                        isUpload = false
                        uName = log.uName
                        uSn = log.uSn
                    } ?: DmcScrappedRecord(
                        null,
                        log.infoId,
                        log.ln,
                        log.optionId!!,
                        log.panel!!,
                        log.position!!,
                        log.setDate,
                        log.uName,
                        log.uSn,
                        log.gSn,
                        log.isUpload,
                        log.isTestData,
                        key.toInt()
                    )

                    dmcKey = repository.insertDmcRecord(dmcRecord)
                    dmcRecord.apply {
                        sn = dmcKey.toInt()
                    }
                }
            }


            //線上單筆同步
            if (!P.isOffline) {
                when (log.state) {
                    0 -> {
                        val data = JsonObject().apply {
                            addProperty("sn", key)
                            addProperty("infoId", log.infoId)
                            addProperty("ln", log.ln.replace("\n", ""))
                            addProperty("position", log.position)
                            addProperty("panel", log.panel)
                            addProperty("optionId", log.optionId)
                            addProperty("uSn", log.uSn)
                            addProperty("uName", log.uName)
                            addProperty("setDate", log.setDate)
                            addProperty("gSn", log.gSn)
                        }.toString()
                        val params = listOf(
                            "mac" to NetworkInformation.macAddress,
                            "data" to "[$data]"
                        )
                        val re = NightUnit.nRequest(
                            getApplication(),
                            P.ACTION_syncData,
                            params,
                            onStateCallback
                        )
                        re?.let {
                            //處理回傳
                            try {
                                val res = Gson().fromJson(it, SyncRes::class.java)

                                res.state?.let {
                                    if (it == "error") throw Exception(res.msg)
                                }

                                if(res.error.isNullOrEmpty()){
                                    repository.insertLog(log.apply {
                                        isUpload = true
                                        logState = "上傳成功(新增、修改)"
                                    })
                                    repository.insertDmcRecord(dmcRecord!!.apply {
                                        isUpload = true
                                    })

                                } else {
                                    repository.insertLog(log.apply {
                                        isUpload = false
                                        logState = "上傳失敗(新增、修改): ${res.error?.get(0)?.remark}"
                                    })
                                }

                            } catch (e: Exception) {
                                e.printStackTrace()
                                onStateCallback.onError(it, e)
                            }
                        }
                    }
                    //state = 3 -> 片報
                    3 -> {
                        val data = JsonObject().apply {
                            addProperty("ln", log.ln)
                            addProperty("optionId", log.optionId)
                            addProperty("uSn", log.uSn)
                            addProperty("setDate", log.setDate)
                            addProperty("gSn", log.gSn)
                        }
                        val params = listOf(
                            "mac" to NetworkInformation.macAddress,
                            "data" to "[$data]"
                        )
                        val re = NightUnit.nRequest(
                            getApplication(),
                            P.ACTION_syncPnlScrap,
                            params,
                            onStateCallback
                        )
                        re?.let {
                            try {
                                val jo = JSONObject(it)
                                val state = jo.getString("state")
                                val m = when (log.optionId) {
                                    "225" -> "Spnl"
                                    "227" -> "wpnl"
                                    else -> null
                                }
                                when (state) {
                                    "ok" -> {

                                        repository.insertLog(log.apply {
                                            isUpload = true
                                            logState = "上傳成功(片報: $m)"
                                        })

                                        MyRoomDB.getDatabase(getApplication()).dao().updateDmcScrappedWrnlSrnl(log.optionId!!)

                                    }
                                    "error" -> {
                                        repository.insertLog(log.apply {
                                            logState = "上傳失敗(片報: $m)"
                                        })
                                        throw Exception(jo.getString("msg"))
                                    }
                                    else -> null
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                onStateCallback.onError(it, e)
                            }
                        }
                    }
                }
                onStateCallback.onFinished()
            }
        }


    //清除該位置的報廢原因
    fun setClearScrappedData(position: String, onStateCallback: OnStateCallback) =
        GlobalScope.launch(Dispatchers.Default) {

            val dmcRecord = repository.getDmcRecordByPosition(position)
            if (dmcRecord.isUpload == false){
                MyRoomDB.getDatabase(getApplication()).dao().deleteDmcRecord(dmcRecord)
                return@launch
            }
            var setDate = P.getAppCenterTime()
            val log = MyLog(
                null,
                1,
                dmcRecord.infoId,
                dmcRecord.ln,
                dmcRecord.optionId,
                dmcRecord.panel,
                dmcRecord.position,
                setDate,
                AppCenter.uName,
                AppCenter.uSn.toString(),
                P.station.toString(),
                false,
                null,
                P.isTest
            )

            when(log.optionId){
                "225","227"->{
                    log.apply {
                        state = 4
                        panel = 2.toString()
                    }
                    val key = repository.insertLog(log)
                    log.apply {
                        sn = key.toInt()
                    }
                    repository.insertDmcRecord(dmcRecord.apply {
                        sSn = key.toInt()
                        setDate = log.setDate
                        isUpload = false
                        uSn = log.uSn
                        uName = log.uName
                    })

                }
                else -> {
                    val key = repository.insertLog(log)
                    log.apply {
                        sn = key.toInt()
                    }
                    repository.insertDmcRecord(dmcRecord.apply {
                        sSn = key.toInt()
                        setDate = log.setDate
                        isUpload = false
                        uSn = log.uSn
                        uName = log.uName
                    })
                }
            }


            //上傳(單筆)
            if (!P.isOffline) {
                when(log.state){
                    4 ->{
                        val data = JsonObject().apply {
                            addProperty("ln",log.ln)
                            addProperty("optionId",log.optionId)
                            addProperty("uSn",log.uSn)
                            addProperty("setDate",log.setDate)
                            addProperty("gSn",log.uSn)
                        }
                        val params = listOf(
                            "mac" to NetworkInformation.macAddress,
                            "data" to "[$data]"
                        )
                        val re = NightUnit.nRequest(getApplication(),P.ACTION_syncPnlScrapDel,params, onStateCallback)
                        re?.let {
                            try {
                                val jo = JSONObject(it)
                                val state = jo.getString("state")
                                when(state){
                                    "ok" -> {
                                        repository.insertLog(log.apply {
                                            isUpload = true
                                            logState = "上傳成功(刪除片報)"
                                        })
                                        MyRoomDB.getDatabase(getApplication()).dao().deleteScrappedWrnlSrnl()
                                    }
                                    "error"->{
                                        repository.insertLog(
                                            log.apply {
                                                logState = "上傳失敗(刪除片報)"
                                            }
                                        )
                                        throw Exception(jo.getString("msg"))
                                    }
                                    else -> null
                                }
                            }catch (e:Exception){
                                e.printStackTrace()
                                onStateCallback.onError(it,e)
                            }
                        }
                    }
                    else -> {
                        val data = JsonObject().apply {
                            addProperty("sn", log.sn)
                            addProperty("infoId", log.infoId)
                            addProperty("ln", log.ln)
                            addProperty("position", log.position)
                            addProperty("panel", log.panel)
                            addProperty("optionId", log.optionId)
                            addProperty("uSn", log.uSn)
                            addProperty("uName", log.uName)
                            addProperty("setDate", log.setDate)
                        }
                        val params = listOf(
                            "mac" to NetworkInformation.macAddress,
                            "data" to "[$data]"
                        )
                        val re = NightUnit.nRequest(
                            getApplication(),
                            P.ACTION_syncDataDel,
                            params,
                            onStateCallback
                        )
                        re?.let {
                            try {
                                val res = Gson().fromJson(it, Res::class.java)
                                res.state?.let {
                                    if (it == "error") {
                                        throw Exception(res.msg)
                                    } else if (it == "okDel") {
                                        repository.insertLog(log.apply {
                                            isUpload = true
                                            logState = "上傳成功(刪除報廢選項)"
                                        })
                                        repository.deleteDmcRecord(dmcRecord)
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                onStateCallback.onError(it, e)
                            }
                        }
                    }
                }
            }
            onStateCallback.onFinished()
        }
}