package com.night.dmcscrapped.work

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.night.dmcscrapped.data.db.MyRoomDB
import com.night.dmcscrapped.gen.Repository
import kotlinx.coroutines.delay

class SyncWork(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams){

    val context = context.applicationContext

    val repository = Repository(context)

    override fun doWork(): Result {

//        Thread.sleep(10000) //測試用

        //先檢查有無未上傳資料
        val list = MyRoomDB.getDatabase(context).dao().getUnUploadLog()
        if (list.isEmpty()) {
            return Result.success()
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


        return Result.success()
    }
}