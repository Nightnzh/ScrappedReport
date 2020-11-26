package com.night.dmcscrapped.ui.main

import android.app.Application
import android.bluetooth.*
import android.content.Context
import android.util.Log
import androidx.lifecycle.*
import com.boardtek.appcenter.AppCenter
import com.boardtek.appcenter.NetworkInformation
import com.night.dmcscrapped.data.db.MyRoomDB
import com.night.dmcscrapped.data.model.PlateInfo
import com.night.dmcscrapped.gen.P
import com.night.dmcscrapped.gen.Repository
import com.night.dmcscrapped.units.OnStateCallback
import kotlinx.coroutines.*
import net.danlew.android.joda.JodaTimeAndroid
import org.joda.time.format.DateTimeFormat

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = Repository(application)


    init {
        AppCenter.init(application)
        NetworkInformation.init(application)
        JodaTimeAndroid.init(getApplication());
    }


    val dmcScrappedRecordListLiveData = repository.getDmcScrappedRecordLiveData()

    //Plate
    val plateMutableLiveData = MutableLiveData<PlateInfo>(null)

    //站別(Station)
    val stationLiveData = repository.getStation()

    //Plate 顯示面
    val displaySurface = listOf(
        "預設",
        "TOP",
        "BTM"
    )

    //藍芽裝置
    val mutableSetBDevice = mutableSetOf<BluetoothDevice>()
    val scanedMutableLiveData = MutableLiveData(mutableSetBDevice)


    //初始化資料(後端)
    fun loadInitData(setDate: String, onStateCallback: OnStateCallback) =
        viewModelScope.launch(Dispatchers.IO) {
            //判斷484第一次開啟App

//        Log.d("@@@isFirstOpen", isFirstOpen.toString())


//        val setDate = if (isFirstOpen) "" else P.getAppCenterTime()
            val mac = NetworkInformation.macAddress

            launch {
                repository.loadPlateInfo(
                    setDate,
                    onStateCallback
                )
            }.join()
            launch {
                repository.loadOption(
                    onStateCallback
                )
            }.join()
            launch {
                repository.loadStation(
                    onStateCallback
                )
            }.join()
            launch {
                repository.loadBluetoothDeviceList(
                    setDate,
                    onStateCallback
                )
            }.join()
            launch {
                repository.loadCheckPhoto(
                    setDate,
                    mac,
                    onStateCallback
                )
            }.join()

            onStateCallback.onFinished()
        }

    //讀取報報廢選項
    suspend fun getROption(optionId: String) = withContext(Dispatchers.IO) {
        repository.getROption(optionId)
    }

    fun deleteDmcRecord() = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteDmcScrappedRecord()
    }

    fun singleReportSearch(dmc: String, onStateCallback: OnStateCallback) =
        viewModelScope.launch(Dispatchers.IO) {
            repository.loadDmcData(dmc, onStateCallback)
            onStateCallback.onFinished()
        }


    //loadMission
    suspend fun loadMissBrush(dmc: String, gSn: String, onStateCallback: OnStateCallback) =
        withContext(Dispatchers.Default) {
            repository.missBrushCheck(dmc, gSn, onStateCallback)
        }

    //test
    fun testUpload(onStateCallback: OnStateCallback) = viewModelScope.launch(Dispatchers.Default) {
        repository.uploadAddDmcScrappedRecord(onStateCallback)
    }

    fun uploadScanInRecord(onStateCallback: OnStateCallback) =
        viewModelScope.launch(Dispatchers.Default) {
            if (!P.isOffline)
                repository.uploadScanInRecord(onStateCallback)
        }

    val synCount = MyRoomDB.getDatabase(getApplication()).dao().getSyncCount()

    fun syncByNotOnState() = GlobalScope.launch(Dispatchers.Default) {
        //先檢查有無未上傳資料
        val list = MyRoomDB.getDatabase(getApplication()).dao().getUnUploadLog()
        if (list.isEmpty()) {
            return@launch
        }
        //檢測
        val list2 = list.filter { it.state == 2 }
        val list0 = list.filter { it.state == 0 }
        val list1 = list.filter { it.state == 1 }
        val list3 = list.filter { it.state == 3 }
        val list4 = list.filter { it.state == 4 }

        if (list2.isNotEmpty()) {
            repository.uploadScanInRecord()
        }

        if (list0.isNotEmpty()) {
            repository.uploadAddDmcScrappedRecord()
        }

        if (list1.isNotEmpty()) {
            repository.uploadClearDmcScrappedRecord()
        }

        if (list3.isNotEmpty()) {
            repository.uploadWLScrappedRecord()
        }

        if (list4.isNotEmpty()) {
            repository.uploadClearWLeScrappedRecord()
        }

    }


    //機算7天前的日期做資料刪除
    fun clearData7() = GlobalScope.launch(Dispatchers.Default) {
        try {
            val minus7DaysDate = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
                .parseLocalDateTime(AppCenter.getSystemTime()).minusDays(7).toString("yyyy-MM-dd")
                .also {
                    Log.d("@@@minus7DaysDate", "$it")
                }
            MyRoomDB.getDatabase(getApplication()).dao().clearLog7(minus7DaysDate)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


}