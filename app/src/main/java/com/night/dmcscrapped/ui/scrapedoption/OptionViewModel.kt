package com.night.dmcscrapped.ui.scrapedoption

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.boardtek.appcenter.AppCenter
import com.night.dmcscrapped.data.db.MyRoomDB
import com.night.dmcscrapped.data.model.DmcScrappedRecord
import com.night.dmcscrapped.data.model.MyLog
import com.night.dmcscrapped.data.model.ROptionItem
import com.night.dmcscrapped.gen.P
import com.night.dmcscrapped.gen.Repository
import com.night.dmcscrapped.units.OnStateCallback
import kotlinx.coroutines.*


class OptionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = Repository(application)
    var dmcScrappedRecord: DmcScrappedRecord? = null
    var nowPosition: String? = null

    //所有報廢選項
    val allScrappedOptionLive = repository.getAllScrappedOptionItem()


    //用於成行站
    val station = MutableLiveData("-1")

    //當下報廢選項
    var optionItem: ROptionItem? = null


    //Insert單報資料(pcs) test
    fun setScrappedData(
        dmcScrappedRecord: DmcScrappedRecord? = null,
        onStateCallback: OnStateCallback
    ) =
        GlobalScope.launch(Dispatchers.Default) {

            val setDate = P.getAppCenterTime()
            var panel = when (P.display) {
                0, 1 -> 0
                2 -> 1
                else -> 2
            }
            val state = when (optionItem!!.sn) {
                "225", "227" -> {
                    panel = 2
                    3
                }
                else -> 0
            }
            val gSn = P.station
            val log = MyLog(
                null,
                P.pn,
                state,
                P.infoId.toString(),
                P.dmcCode.toString(),
                optionItem!!.sn,
                panel.toString(),
                nowPosition,
                setDate,
                AppCenter.uName,
                AppCenter.uSn.toString(),
                gSn.toString(),
                false,
                null,
                P.isTest
            )
            val key = repository.insertLog(log)
            when (log.optionId) {
                "225", "227" -> {
                    val size = P.plateInfo!!.model.split("*").map { it.toInt() }
                        .reduce { acc, i -> acc * i }
                    val mutableList = mutableListOf<DmcScrappedRecord>()

                    val list = MyRoomDB.getDatabase(getApplication()).dao().getDmcScrappedRecordList().filter { it.panel != "2" }.map { it.position.toInt() }

                    for (i in 1..size) {
                        if(list.contains(i))
                            continue
                        mutableList.add(
                            DmcScrappedRecord(
                                null,
                                log.pn,
                                log.infoId,
                                log.ln,
                                log.optionId,
                                log.panel!!,
                                i.toString(),
                                log.setDate,
                                log.uName,
                                log.uSn,
                                log.gSn,
//                                log.isUpload,
                                log.isTestData,
                                key.toInt()
                            )
                        )
                    }
                    repository.insertDmcRecordList(mutableList)
                }
                else -> {

                    val dmcRecord = dmcScrappedRecord?.apply {
                        sSn = key.toInt()
                        optionId = log.optionId!!
                        uName = log.uName
                        uSn = log.uSn
//                        isUpload = log.isUpload
                    } ?: DmcScrappedRecord(
                        null,
                        log.pn,
                        log.infoId,
                        log.ln,
                        log.optionId!!,
                        log.panel!!,
                        log.position!!,
                        log.setDate,
                        log.uName,
                        log.uSn,
                        log.gSn,
//                        log.isUpload,
                        log.isTestData,
                        key.toInt()
                    )
                    repository.insertDmcRecord(dmcRecord)

                    //判斷超允
                    val isUpThenAcceptQty = MyRoomDB.getDatabase(getApplication()).dao()
                        .getUpSize() > P.plateInfo!!.allowAcceptQty.toInt()

                    if (isUpThenAcceptQty) {
                        val size = P.plateInfo!!.model.split("*").map { it.toInt() }
                            .reduce { acc, i -> acc * i }
                        val mutableList = mutableListOf<DmcScrappedRecord>()
                        for (i in 1..size) {
                            mutableList.add(
                                DmcScrappedRecord(
                                    null,
                                    log.pn,
                                    log.infoId,
                                    log.ln,
                                    "224",
                                    "2",
                                    i.toString(),
                                    log.setDate,
                                    log.uName,
                                    log.uSn,
                                    log.gSn,
//                                    log.isUpload,
                                    log.isTestData,
                                    key.toInt()
                                )
                            )
                        }
                        repository.insertDmcRecordList(mutableList)
                    } else {
                        MyRoomDB.getDatabase(getApplication()).dao()
                            .deleteScrappedReocrdByOptionId("224")
                    }
                }
            }
            onStateCallback.onSync()

        }


    //清除該位置的報廢原因
    fun setClearScrappedData(dmcScrappedRecord: DmcScrappedRecord,onStateCallback: OnStateCallback) =
        GlobalScope.launch(Dispatchers.Default) {

            val setDate = P.getAppCenterTime()

            val state = when (dmcScrappedRecord.optionId) {
                "225", "227" -> 4
                else -> 1
            }

            val log = MyLog(
                null,
                dmcScrappedRecord.pn,
                state,
                dmcScrappedRecord.infoId,
                dmcScrappedRecord.ln,
                dmcScrappedRecord.optionId,
                dmcScrappedRecord.panel,
                dmcScrappedRecord.position,
                setDate,
                AppCenter.uName,
                AppCenter.uId,
                P.station.toString(),
                false,
                null,
                P.isTest
            )

            val key = repository.insertLog(log)

            when (state) {
                4 -> {
                    MyRoomDB.getDatabase(getApplication()).dao()
                        .deleteScrappedReocrdByOptionId(dmcScrappedRecord.optionId)
                    val isUpThenAcceptQty = MyRoomDB.getDatabase(getApplication()).dao()
                        .getUpSize() > P.plateInfo!!.allowAcceptQty.toInt()
                    if (!isUpThenAcceptQty) {
                        MyRoomDB.getDatabase(getApplication()).dao()
                            .deleteScrappedReocrdByOptionId("224")
                    }
                }
                else -> {
                    repository.deleteDmcRecord(dmcScrappedRecord)
                    //判斷超允
                    val isUpThenAcceptQty = MyRoomDB.getDatabase(getApplication()).dao()
                        .getUpSize() > P.plateInfo!!.allowAcceptQty.toInt()
                    if (!isUpThenAcceptQty) {
                        MyRoomDB.getDatabase(getApplication()).dao()
                            .deleteScrappedReocrdByOptionId("224")
                    } else {
                        val mutableList = mutableListOf<DmcScrappedRecord>()
                        val list =
                            MyRoomDB.getDatabase(getApplication()).dao().getDmcScrappedRecordList()
                        val size = P.plateInfo!!.model.split("*").map { it.toInt() }
                            .reduce { acc, i -> acc * i }
                        val t = list.filter { it.panel == "2" }.map { it.position.toInt() }

                        for (i in 1..size) {
                            if (t.contains(i))
                                continue
                            mutableList.add(
                                DmcScrappedRecord(
                                    null,
                                    log.pn,
                                    log.infoId,
                                    log.ln,
                                    "224",
                                    "2",
                                    i.toString(),
                                    log.setDate,
                                    log.uName,
                                    log.uSn,
                                    log.gSn,
//                                    log.isUpload,
                                    log.isTestData,
                                    key.toInt()
                                )
                            )
                        }
                        repository.insertDmcRecordList(mutableList)
                    }
                }
            }
            onStateCallback.onSync()
        }
}