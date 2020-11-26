package com.night.dmcscrapped.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.Dao
import com.night.dmcscrapped.data.model.*
import com.night.dmcscrapped.data.model.net.BDevice

@Dao
interface Dao {
    //批號紀錄相關
    @Transaction
    fun setScrappedReport(list:List<DmcScrappedRecord>){
        deleteAllDmcScrappedRecord()
        insertDmcScrappedRecordList(list)
    }
    //檢測紀錄(只要有刷入成功)

    //單報相關
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDmcScrappedRecordList(list:List<DmcScrappedRecord>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDmcScrappedRecord(dmcScrappedRecord: DmcScrappedRecord) : Long
    @Query("SELECT * FROM dmcScrappedRecord")
    fun getDmcScrappedRecordList() : List<DmcScrappedRecord>
    @Query("DELETE FROM dmcScrappedRecord")
    fun deleteAllDmcScrappedRecord()
    @Query("SELECT * FROM dmcScrappedRecord")
    fun getDmcScrappedRecordListLiveData() : LiveData<List<DmcScrappedRecord>>
//    @Query("SELECT * FROM dmcScrappedRecord WHERE isUpload = 0")
//    fun getUnUploadDmcRecord() : List<DmcScrappedRecord>
    @Query("SELECT * FROM dmcScrappedRecord WHERE :position = position")
    fun getDmcRecord(position:String) : DmcScrappedRecord
    @Query("DELETE FROM dmcScrappedRecord WHERE optionId = :optionId")
    fun deleteScrappedReocrdByOptionId(optionId: String)
    @Delete
    fun deleteDmcRecord(dmcScrappedRecord: DmcScrappedRecord)
//    @Query("Update dmcScrappedRecord SET isUpload = 1 WHERE optionId = :optionId")
//    fun updateDmcScrappedWrnlSrnl(optionId: String)

    //TODO:
    @Query("SELECT * FROM dmcScrappedRecord ")
    fun getRemoveDmcRecord() : List<DmcScrappedRecord>

    //判斷超允用的
    @Query("SELECT COUNT(*) FROM dmcScrappedRecord WHERE optionId != '224'")
    fun getUpSize() : Int

    //Log
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLog(myLog:MyLog) : Long
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLogList(list:List<MyLog>)

    @Query("SELECT * FROM log WHERE isUpload = 0")
    fun getUnUploadLog() : List<MyLog>

    @Delete
    fun deleteLog(myLog: MyLog)

    //state  0 , 1 , 2
    @Query("SELECT * FROM log WHERE isUpload = 0 AND :state = state")
    fun getUnUploadLog(state:Int) : List<MyLog>
    @Query("SELECT * FROM log ORDER BY setDate DESC,isUpload ASC")
    fun getLogLiveData() : LiveData<List<MyLog>>

    //板件相關
    @Query("SELECT * FROM plateInfo WHERE :sn = sn")
    suspend fun getPlate(sn:String) : PlateInfo
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlateInfoList(list:List<PlateInfo>)

    //站別相關 Station
    @Query("SELECT * FROM station ORDER by sn ASC")
    fun getStationList() : LiveData<List<Station>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertStation(list:List<Station>)

    //選項相關
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOption(list:List<ROptionItem>)
    @Query("SELECT * FROM retirement_option WHERE :optionId = sn")
    suspend fun getOption(optionId:String) : ROptionItem
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOptDep(list:List<OptDep>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnList(snList:SnList)

    @Query("SELECT * FROM retirement_option as A LEFT JOIN optDep as B WHERE B.optSn = A.sn AND A.sn != '224'")
    fun getAllScrappedOption(): LiveData<List<ROptionItem>>

    //藍芽相關
    @Query("SELECT * FROM bluetoothDevice")
    fun getTrustBluetoothMacList(): List<BDevice>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrustBDevice(listD:List<BDevice>)


    @Query("SELECT COUNT(*) FROM log WHERE isUpload = 0")
    fun getSyncCount() : LiveData<Int>

    //判斷是否有未上傳資料
    @Query("SELECT EXISTS(SELECT * FROM log WHERE isUpload = 0) ")
    fun haveUploadRecord() : Boolean


    //清除7天前資料
    @Query("DELETE FROM log WHERE setDate < :setDate and isUpload = 1")
    fun clearLog7(setDate : String)


    //清除資料用
//    @Transaction
//    fun clearData(setDate: String){
//
//    }

}