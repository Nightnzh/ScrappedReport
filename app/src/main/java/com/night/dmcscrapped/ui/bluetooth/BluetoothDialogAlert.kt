package com.night.dmcscrapped.ui.bluetooth

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothProfile
import android.content.*
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import androidx.recyclerview.widget.SortedListAdapterCallback
import com.night.dmcscrapped.R
import com.night.dmcscrapped.data.model.net.BDevice
import com.night.dmcscrapped.databinding.BluetoothBinding
import com.night.dmcscrapped.databinding.BluetoothItemBinding
import com.night.dmcscrapped.databinding.LoadingBinding
import com.shizhefei.view.largeimage.LargeImageView
import java.lang.reflect.Method


//藍芽搜搜尋!!!
class BluetoothDialogAlert : DialogFragment() {

    private val vm by lazy {
        ViewModelProvider(requireActivity()).get(BlueToothViewModel::class.java)
    }

    private lateinit var binding: BluetoothBinding
    private lateinit var sortedList: SortedList<BDevice>
    private lateinit var lBinding : LoadingBinding

    companion object {
        var bluetoothProfile: BluetoothProfile? = null
    }

    private val bluetoothAdapter by lazy {
        BluetoothAdapter.getDefaultAdapter().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                getProfileProxy(requireContext(), object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(p0: Int, p1: BluetoothProfile?) {
                        if (p0 == BluetoothProfile.HID_DEVICE) {
                            bluetoothProfile = p1 as BluetoothHidDevice
                        }
                    }

                    override fun onServiceDisconnected(p0: Int) {
                        if (p0 == BluetoothProfile.HID_DEVICE)
                            bluetoothProfile = null
                    }
                }, BluetoothProfile.HID_DEVICE)
            } else {
                getProfileProxy(requireContext(), object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(p0: Int, p1: BluetoothProfile?) {
                        if (p0 == 4) {
                            bluetoothProfile = p1
                        }
                    }

                    override fun onServiceDisconnected(p0: Int) {
                        if (p0 == 4)
                            bluetoothProfile = null
                    }
                }, 4)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm.init()
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Material_Light_DialogWhenLarge)
    }

    override fun onStop() {
        super.onStop()
        requireActivity().unregisterReceiver(receiver)
    }

    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.EXTRA_BOND_STATE)
            addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
            addAction(BluetoothProfile.EXTRA_STATE)
            addAction(BluetoothHidDevice.EXTRA_STATE)
            addAction("android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED")
        }

        requireActivity().registerReceiver(receiver, intentFilter)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        lBinding = LoadingBinding.inflate(layoutInflater)
        binding = BluetoothBinding.inflate(layoutInflater)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = BAdapter()
        sortedList = (binding.recyclerView.adapter as BAdapter).sortedList


        binding.bBluePairPng.setOnClickListener {
            pairPngAlert.show()
        }

        //開始搜尋裝置
        binding.bBScan.setOnClickListener {
            Log.d("@@@bScan", "STAR")
//            sortedList.add(BDevice("848", null, null, "TEST", "2020-10-15"))
            if (bluetoothAdapter.isDiscovering) {
                Toast.makeText(requireContext(), "正在搜尋裝置中", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sortedList.clear()
            bluetoothAdapter.startDiscovery()
        }

        //取消配對
        binding.bBClear.setOnClickListener {
            bluetoothAdapter.cancelDiscovery()

            bluetoothAdapter.bondedDevices.forEach {
                val removeBond = it.javaClass.getMethod("removeBond")
                removeBond.invoke(it)
            }

            sortedList.clear()
        }



        return binding.root
    }


//    private val connStateAlert by lazy {
//
//        AlertDialog.Builder(requireContext(),android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar)
//            .setView(lBinding.root)
//            .setCancelable(false)
//            .create()
//    }
//
//    fun showConnStateAlert(msg:String){
//        lBinding.tLoadingState.text = msg
//        connStateAlert.show()
//    }

    //顯示藍芽機配對
    private val pairPngAlert by lazy {
        val imageView = LargeImageView(context).apply {

            setImageDrawable(ContextCompat.getDrawable(context, R.drawable.blue_pair))
        }
        AlertDialog.Builder(requireContext(), R.style.Theme_AppCompat)
            .setView(imageView)
            .setPositiveButton(requireContext().getString(R.string.ok), null)
            .create()
    }

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("LongLogTag")
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.action?.let {
//                it.also { Log.d("@@@Receive Action:", it) }
                when (it) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device =
                            intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                                .also {
                                    Log.d("@@@BluetoothDevice", "$it")
                                    it?.let {
                                        vm.isTrustItem(it).also {   //確認是否為任張裝置並加入View 中
                                            it?.let {
                                                sortedList.add(it)
                                            }
                                        }
                                    }
                                }
                    }

                    //監聽配對結果
                    BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                        val device =
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) as BluetoothDevice?
                        when (device!!.bondState) {
                            BluetoothDevice.BOND_NONE -> {
                                Log.i("@@@Bond", "--- 配對失敗 ---")
                                binding.recyclerView.adapter?.notifyDataSetChanged()
                            }
                            BluetoothDevice.BOND_BONDING -> {
                                Log.i("@@@Bond", "--- 配對中... ---")
                                binding.recyclerView.adapter?.notifyDataSetChanged()
                            }
                            BluetoothDevice.BOND_BONDED -> {
                                Log.i("@@@Bond", "--- 配對成功 ---")
                                binding.recyclerView.adapter?.notifyDataSetChanged()
                            }
                            else -> null
                        }
                    }
                    "android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED" -> {
                        val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, 0)
                        when (state) {
                            BluetoothProfile.STATE_CONNECTING -> {
                                Log.d("@@@ACTION_BOND_STATE_CHANGED", "---連線中---")
//                                showConnStateAlert("---連線中---")
                            }
                            BluetoothProfile.STATE_CONNECTED -> {
                                Log.d("@@@ACTION_BOND_STATE_CHANGED", "---連線成功---")
                                Toast.makeText(requireContext(), "---連線成功---", Toast.LENGTH_LONG).apply{
                                }.show()
//                                connStateAlert.dismiss()
                                dismiss()
                            }
                            BluetoothProfile.STATE_DISCONNECTED -> {
                                Log.d(
                                    "@@@ACTION_BOND_STATE_CHANGED", "---斷線---"
                                )
//                                connStateAlert.dismiss()
                            }
                            BluetoothProfile.STATE_DISCONNECTING -> {
                                Log.d("@@@ACTION_BOND_STATE_CHANGED", "---斷線中---")
//                                showConnStateAlert("---斷線中---")
                            }
                            else -> null
                        }
                    }
                    else -> null
                }
            }
        }

    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)

    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        sortedList.clear()
        vm.init()
    }
}

class BAdapter : RecyclerView.Adapter<BluetoothViewHolder>() {
    val sortedList =
        SortedList(BDevice::class.java, object : SortedListAdapterCallback<BDevice>(this) {
            override fun compare(o1: BDevice?, o2: BDevice?): Int = o2?.sn?.toInt() ?: 0
            override fun areContentsTheSame(
                oldItem: BDevice?,
                newItem: BDevice?
            ): Boolean = oldItem == newItem

            override fun areItemsTheSame(item1: BDevice?, item2: BDevice?): Boolean = item1 == item2
        })

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BluetoothViewHolder {
        val binding =
            BluetoothItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BluetoothViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: BluetoothViewHolder, position: Int) {
        holder.bindTO(sortedList[holder.adapterPosition])
    }

    override fun getItemCount(): Int = sortedList.size()
}

class BluetoothViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val itemBinding = BluetoothItemBinding.bind(itemView)
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    fun bindTO(bDevice: BDevice) {
        Log.d("@@@blue bind", "$adapterPosition $bDevice")
        val bluetoothDevice = bluetoothAdapter.getRemoteDevice(bDevice.mac)
        when (bluetoothDevice.bondState){
            BluetoothDevice.BOND_BONDING -> {
                itemBinding.tBTiem.text = "${bDevice.deviceName}(${bDevice.sn})...配對中"
            }
            BluetoothDevice.BOND_BONDED -> {
                itemBinding.tBTiem.text = "${bDevice.deviceName}(${bDevice.sn})...已配對"
            }
            else -> {
                itemBinding.tBTiem.text = "${bDevice.deviceName}(${bDevice.sn})"
            }
        }

        itemBinding.bConn.setOnClickListener {

            bluetoothAdapter.cancelDiscovery()

            val d = bluetoothAdapter.getRemoteDevice(bDevice.mac)



            d.createBond().also {
                Log.d("@@@createdBond", "$it")
                if (!it) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        (BluetoothDialogAlert.bluetoothProfile as BluetoothHidDevice).also {
                            it.connect(d).also { t ->
                                Log.d("@@@connet", "$t")
                            }
                        }
                    } else {
                        val method: Method =
                            BluetoothDialogAlert.bluetoothProfile!!.javaClass.getMethod(
                                "connect",
                                BluetoothDevice::class.java
                            )
                        method.invoke(BluetoothDialogAlert.bluetoothProfile, d)

//                        try {
//
//                            val socket = d.createRfcommSocketToServiceRecord(d.uuids[0].uuid)
//                            socket.connect().also {
//                                Log.d("@@@blue conn", "$it")
//                            }
//                            socket.outputStream
//                        } catch (e:Exception){
//                            e.printStackTrace()
//                        }
                    }
                }
            }
        }
    }
}
