package com.night.dmcscrapped.units


import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.*
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.night.dmcscrapped.R
import com.night.dmcscrapped.databinding.BluetoothBinding
import com.night.dmcscrapped.databinding.BluetoothItemBinding


class NBluetooth(context: Context) {

    var canReceive = true

    private val binding = BluetoothBinding.inflate(LayoutInflater.from(context)).apply {

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)

        //開始掃描
        bBScan.setOnClickListener {
            if (bluetoothAdapter!!.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }
            bluetoothAdapter.startDiscovery()
        }
        //清空
        bBClear.setOnClickListener {

            bluetoothAdapter!!.cancelDiscovery()
            blueCallback.onClear()
            //取消所有藍芽連線設備
            bluetoothProfile?.let {
                it.connectedDevices.forEach { bd ->
                    val removeBond = bd.javaClass.getMethod("removeBond")
                    removeBond.invoke(bd)
                }
            }
        }
    }

    val alert by lazy {
        AlertDialog.Builder(context)
            .setCancelable(false)
            .setView(binding.root)
            .setPositiveButton(context.getString(R.string.close), null)
            .create().apply {
                setOnShowListener {
                    canReceive = true
                }
                setOnDismissListener {
                    Log.d("@@@BlueAlert", "Dismiss")
                    canReceive = false
                    blueCallback.onClear()
                }
            }
    }

    val intentFilter = IntentFilter().apply {
        addAction(BluetoothDevice.ACTION_FOUND)
        addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        addAction(BluetoothDevice.EXTRA_BOND_STATE)
        addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
        addAction(BluetoothProfile.EXTRA_STATE)
        addAction(BluetoothHidDevice.EXTRA_STATE)
        addAction("android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED")
    }

    //藍芽裝置
    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    var bluetoothProfile: BluetoothProfile? = null

    val profileListener = object : BluetoothProfile.ServiceListener {
        @RequiresApi(Build.VERSION_CODES.P)
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            Log.d("@@@Profile", "$profile $proxy")
            when (profile) {
                4 -> {
                    bluetoothProfile = proxy
                }
                BluetoothProfile.HID_DEVICE -> bluetoothProfile = proxy as BluetoothHidDevice
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            Log.d("@@@Bluetooth disConn", profile.toString())
        }
    }


    val bReceiver = object : BroadcastReceiver() {
        @SuppressLint("LongLogTag")
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!canReceive) return
            intent?.action?.let {
//                it.also { Log.d("@@@Receive Action:", it) }
                when (it) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device =
                            intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                                .also {
                                    it?.let {
                                        blueCallback.onDeviceFound(it)
                                    }
                                }
//                        Log.d("@@@Bluetooth device", device.toString())
                    }

                    //監聽配對結果
                    BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
//                        Log.d("@@@BluetoothDevice", "EXTRA_BOND_STATE")
                        val device =
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) as BluetoothDevice?
                        when (device!!.bondState) {
                            BluetoothDevice.BOND_NONE -> Log.i("@@@Bond", "--- 配對失敗 ---")
                            BluetoothDevice.BOND_BONDING -> Log.i("@@@Bond", "--- 配對中... ---")
                            BluetoothDevice.BOND_BONDED -> Log.i("@@@Bond", "--- 配對成功 ---")
                            else -> null
                        }
                    }
                    "android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED" -> {
                        val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, 0)
                        when (state) {
                            BluetoothProfile.STATE_CONNECTING -> {
                                Log.d("@@@ACTION_BOND_STATE_CHANGED", "---連線中---")
                                blueCallback.onDeviceConnecting()
                            }
                            BluetoothProfile.STATE_CONNECTED -> {
                                alert.dismiss()
                                Log.d("@@@ACTION_BOND_STATE_CHANGED", "---連線成功---")
                                blueCallback.onDeviceDisconnect()
                            }
                            BluetoothProfile.STATE_DISCONNECTED -> {
                                Log.d(
                                    "@@@ACTION_BOND_STATE_CHANGED", "---斷線---"
                                )
                            }
                            BluetoothProfile.STATE_DISCONNECTING -> {
                                Log.d("@@@ACTION_BOND_STATE_CHANGED", "---斷線中---")
                                blueCallback.onDeviceDisconnect()
                            }
                            else -> null
                        }
                    }
                    else -> null
                }
            }
        }
    }

    //更新Adapter
    fun setFoundDeviceList(list: List<BluetoothDevice>) {
        binding.recyclerView.adapter = BAdapter(list)

    }

    //Adapter 用於顯示掃掃描到的藍芽設備
    inner class BAdapter(private val bDList: List<BluetoothDevice>) :
        RecyclerView.Adapter<BViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BViewHolder {
            val bItemBinding = BluetoothItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return BViewHolder(bItemBinding.root)
        }

        override fun getItemCount(): Int = bDList.size

        override fun onBindViewHolder(holder: BViewHolder, position: Int) {
            holder.bindTo(holder.adapterPosition, bDList[holder.adapterPosition])
        }
    }

    inner class BViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val bItemBinding = BluetoothItemBinding.bind(itemView)
        fun bindTo(adapterPosition: Int, bluetoothDevice: BluetoothDevice) {
            bItemBinding.tBTiem.text = "${adapterPosition + 1}. ${itemView.context.getString(R.string.device)}(${bluetoothDevice.name})"
            bItemBinding.tBIsConn.isVisible = false
            //連結設備
            bItemBinding.bBConn.setOnClickListener {
                bluetoothAdapter?.cancelDiscovery()
                try {
                    bluetoothDevice.createBond()
                }catch (e: Exception){
                    e.printStackTrace()
                    Toast.makeText(itemView.context, e.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback(){}



    //定義CallBack
    interface DeviceCallBack {
        fun onDeviceFound(bluetoothDevice: BluetoothDevice)
        fun onDeviceConnecting()
        fun onDeviceConnect()
        fun onDeviceDisconnect()
        fun onClear()
    }

    //給予預設callback 類似init
    private val defaultCallback = object : DeviceCallBack {
        override fun onDeviceFound(bluetoothDevice: BluetoothDevice) {}
        override fun onDeviceConnecting() {}
        override fun onDeviceConnect() {}
        override fun onDeviceDisconnect() {}
        override fun onClear() {}
    }

    //預設callback
    var blueCallback: DeviceCallBack = defaultCallback

}
