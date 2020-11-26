package com.night.dmcscrapped

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.boardtek.appcenter.AppCenter
import com.boardtek.appcenter.NetworkInformation
import com.github.kittinunf.fuel.httpPost
import com.night.dmcscrapped.data.db.MyRoomDB
import com.night.dmcscrapped.gen.P
import com.night.dmcscrapped.gen.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.night.singlereport", appContext.packageName)
        NetworkInformation.init(appContext)
        val (request,response,result)= P.getUrlByAndActionName(P.ACTION_downloadDevice).httpPost().responseString()
        Log.d("@@@request","$request" )
        Log.d("@@@response","$response" )
        Log.d("@@@result","$result" )

    }

    @Test
    fun bluetoothTest(){
        val t = BluetoothAdapter.getDefaultAdapter()
        Log.d("@@@bluetooth", "${t.isEnabled}")
        t.bluetoothLeScanner.startScan(object : ScanCallback(){
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)
//                Log.d("@@@scanResult", "$result")
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                super.onBatchScanResults(results)
                Log.d("@@@scanResults", "$results")
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                Log.d("@@@scanFailed", "zz")
            }
        })
    }

    @Test
    fun writeTest(){

        MainScope().launch(Dispatchers.Main) {
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
            AppCenter.init(appContext)
            NetworkInformation.init(appContext)
            val repository = Repository(appContext)
            withContext(Dispatchers.IO) {
                val params = listOf(
                    "MAC" to NetworkInformation.macAddress.also { Log.d("@@@MAC", it ) },
                    "setDate" to P.getAppCenterTime()
                )
//                repository.loadCheckPhoto(params)
            }
        }
    }

    @Test
    fun TESTtt(){
        MainScope().launch(Dispatchers.Default) {
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
            val t = MyRoomDB.getDatabase(appContext).dao().haveUploadRecord()
            Log.d("@@@", "${t}")
        }
    }

}