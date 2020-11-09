package com.night.dmcscrapped.ui.syncstate

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.night.dmcscrapped.data.db.MyRoomDB
import com.night.dmcscrapped.data.model.MyLog
import com.night.dmcscrapped.gen.Repository
import com.night.dmcscrapped.units.OnStateCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SyncVM(application: Application) : AndroidViewModel(application) {

    private val repository = Repository(application)
    val logLiveData = repository.getLogLiveData()
    val stateLiveData = repository.getStation()
    val rOptionLiveData = repository.getAllScrappedOptionItem()

    val type = arrayOf("––全部––", "新增(修改)報廢選項", "清除報廢選項", "檢測紀錄", "新增片報", "刪除片報")

    var dateList: List<String>? = null
    var selectedDate: String? = null
    var selectedState = 0

    fun filterItem(list: List<MyLog>, setDate: String?, state: Int): List<MyLog> {
        Log.d("@@@TestListBefore", "$list")

        return when (state) {
            0 -> {
                list.filter { it.setDate.contains(setDate ?: " ") }.also {
                    Log.d("@@@TestListAfter", "$it")
                }
            }
            else -> {
                list.filter { it.setDate.contains(setDate ?: " ") && it.state == state - 1 }.also {
                    Log.d("@@@TestListAfter", "$it")
                }
            }
        }

    }

    //同步資料
    fun sync(onStateCallback: OnStateCallback) = GlobalScope.launch(Dispatchers.Default) {
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
            repository.uploadScanInRecord(onStateCallback)
        }

        if (list0.isNotEmpty()) {
            repository.uploadAddDmcScrappedRecord(onStateCallback)
        }

        if (list1.isNotEmpty()) {
            repository.uploadClearDmcScrappedRecord(onStateCallback)
        }

        if (list3.isNotEmpty()) {
            repository.uploadWLScrappedRecord(onStateCallback)
        }

        if (list4.isNotEmpty()) {
            repository.uploadClearWLeScrappedRecord(onStateCallback)
        }


        onStateCallback.onFinished()
    }

    //刪除未上傳紀錄
    fun delete(myLog: MyLog) = GlobalScope.launch(Dispatchers.Default) {
        repository.deleteLog(myLog)
    }

}