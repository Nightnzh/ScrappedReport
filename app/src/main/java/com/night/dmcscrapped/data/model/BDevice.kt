package com.night.dmcscrapped.data.model

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "bluetoothDevice")
data class BDevice(
    @PrimaryKey val sn: String,
    val mac: String?,
    val device: String?,
    val deviceName: String?,
    val setDate: String?
) {
    fun removeBond(){
        val bluetoothDevice = BluetoothAdapter.getDefaultAdapter().bondedDevices.find {
            it.address.toUpperCase() == mac
        }
        bluetoothDevice?.let {
            val removeBond = it.javaClass.getMethod("removeBond")
            removeBond.invoke(it)
        }
    }

    fun connectWithOutPair(bluetoothDevice: BluetoothDevice){
        bluetoothDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString("930d2d11-19d5-4d8a-9f7e-bb1160976467")).connect()
    }
}
