package com.night.dmcscrapped.ui.main

import android.app.Application
import android.bluetooth.*
import android.content.Context
import android.util.Log
import androidx.lifecycle.*
import com.boardtek.appcenter.AppCenter
import com.boardtek.appcenter.NetworkInformation
import com.night.dmcscrapped.data.model.BDevice
import com.night.dmcscrapped.data.model.PlateInfo
import com.night.dmcscrapped.gen.P
import com.night.dmcscrapped.gen.Repository
import com.night.dmcscrapped.units.OnStateCallback
import kotlinx.coroutines.*

class MainViewModel(application: Application) : AndroidViewModel(application){

    private val repository = Repository(application)


    init {
        AppCenter.init(application)
        NetworkInformation.init(application)
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

    //藍芽信任裝置列表
    private var trustDBDeviceList : List<BDevice>? = null
    //藍芽裝置
    val mutableSetBDevice = mutableSetOf<BluetoothDevice>()
    val scanedMutableLiveData = MutableLiveData(mutableSetBDevice)


    //初始化資料(後端)
    fun loadInitData(onStateCallback: OnStateCallback) = viewModelScope.launch {
        //判斷484第一次開啟App
        val isFirstOpen = getApplication<Application>().applicationContext.getSharedPreferences("setting", Context.MODE_PRIVATE).getBoolean("isFirstOpen",true)
        Log.d("@@@tetststg", isFirstOpen.toString())

        withContext(Dispatchers.IO) {
            val setDate = if(isFirstOpen) "" else P.getAppCenterTime()
            val mac = NetworkInformation.macAddress
            repository.loadPlateInfo(setDate,onStateCallback)
            repository.loadOption(onStateCallback)
            repository.loadStation(onStateCallback)
            repository.loadBluetoothDeviceList(setDate,onStateCallback)
            repository.loadCheckPhoto(setDate,mac,onStateCallback)
            //已上成功才算完成初始化( isFirstOpen = true )
            getApplication<Application>().applicationContext.getSharedPreferences("setting", Context.MODE_PRIVATE).edit().putBoolean("isFirstOpen",false).apply()
        }
        //設定藍芽信任清單
        setTrustBDevices()
        onStateCallback.onFinished()
    }

    //讀取報報廢選項
    suspend fun getROption(optionId:String) = withContext(Dispatchers.IO){
        repository.getROption(optionId)
    }

    fun deleteDmcRecord() = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteDmcScrappedRecord()
    }

    fun singleReportSearch(dmc:String,onStateCallback: OnStateCallback) = viewModelScope.launch(Dispatchers.IO) {
        repository.loadSingleReportData(dmc, onStateCallback)
        onStateCallback.onFinished()
    }

    private fun setTrustBDevices()  {
        viewModelScope.launch(Dispatchers.IO) {
            trustDBDeviceList = repository.getTrustBDMac()
        }
    }

    //過濾掃描到的藍芽設備
    fun filterAndUpdateBDevice(bluetoothDevice: BluetoothDevice){
        trustDBDeviceList?.let {
            val bDevice = it.find { bd ->
                bd.mac == bluetoothDevice.address.toUpperCase()
            }
            //找到信任裝置
            bDevice?.let {
                mutableSetBDevice.add(bluetoothDevice)
                scanedMutableLiveData.postValue(mutableSetBDevice)
            }
        }
    }


    //loadMission
    suspend fun loadMissBrush(dmc: String,gSn:String,onStateCallback: OnStateCallback) = withContext(Dispatchers.Default) {
        repository.missBrushCheck(dmc,gSn,onStateCallback)
    }

    //test
    fun testUpload(onStateCallback: OnStateCallback) = viewModelScope.launch(Dispatchers.Default) {
        repository.uploadAddDmcScrappedRecord(onStateCallback)
    }

    fun uploadScanInRecord(onStateCallback: OnStateCallback) = viewModelScope.launch(Dispatchers.Default) {
        if(!P.isOffline)
            repository.uploadScanInRecord(onStateCallback)
    }

}