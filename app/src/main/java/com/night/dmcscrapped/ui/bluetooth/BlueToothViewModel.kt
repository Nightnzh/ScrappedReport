package com.night.dmcscrapped.ui.bluetooth

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.night.dmcscrapped.data.model.net.BDevice
import com.night.dmcscrapped.gen.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

class BlueToothViewModel(application: Application) : AndroidViewModel(application) {

    private val repository  = Repository(getApplication())

    var trustDbDevice : List<BDevice>? = null




    fun init() = GlobalScope.launch(Dispatchers.Default) {
        trustDbDevice = repository.getTrustBDMac()
        Log.d("@@@init TrustDevice", "$trustDbDevice")
    }


    //存放掃描到的裝置


    //傳入bluetoothDevice與信任清單做比對
    fun isTrustItem(bluetoothDevice: BluetoothDevice): BDevice? {
        if(trustDbDevice == null ) init()
        return trustDbDevice?.find { it.mac?.toUpperCase(Locale.ROOT) == bluetoothDevice.address.toUpperCase(Locale.ROOT) }
    }

}