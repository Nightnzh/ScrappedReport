package com.night.dmcscrapped.ui.main

import android.Manifest
import android.bluetooth.*
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.KeyEvent.KEYCODE_ENTER
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.boardtek.appcenter.AppCenter
import com.boardtek.appcenter.NetworkInformation
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.night.dmcscrapped.ui.scrapedoption.ScrappedOptionAlert
import com.night.dmcscrapped.R
import com.night.dmcscrapped.data.model.ActionDebug
import com.night.dmcscrapped.data.model.DmcScrappedRecord
import com.night.dmcscrapped.data.model.PlateInfo
import com.night.dmcscrapped.data.model.ROptionItem
import com.night.dmcscrapped.databinding.*
import com.night.dmcscrapped.gen.P
import com.night.dmcscrapped.ui.Gui
import com.night.dmcscrapped.ui.dmcHistory.HistoryDialogFragment
import com.night.dmcscrapped.ui.syncstate.SyncStateDialogFragment
import com.night.dmcscrapped.units.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*


class MainActivity : AppCompatActivity(), OnStateCallback, MenuItem.OnMenuItemClickListener,
    CompoundButton.OnCheckedChangeListener {

    private lateinit var mainBinding: ActivityMainBinding
    private lateinit var drawerHeaderBinding: NavHeaderMainBinding
    private lateinit var optionItemBinding: OptionItemBinding
    private lateinit var plateBinding: PlateBinding
    private lateinit var vm: MainViewModel
    private lateinit var gui: Gui
    private lateinit var scrappedAlert: ScrappedOptionAlert

    private val historyDialogFragment by lazy {
        HistoryDialogFragment(this)
    }
    private val syncStateDialogFragment by lazy {
        SyncStateDialogFragment(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        vm = ViewModelProvider(this).get(MainViewModel::class.java)
        init()
        setContentView(mainBinding.root)
        requestPermission()
        plateBinding.recycler.setHasFixedSize(true)

        //圖片
        vm.plateMutableLiveData.observe(this) { plateInfo ->
            plateInfo?.let {
                //初始化，正反面與角度
                val imgName = when (P.display) {
                    0, 1 -> plateInfo.pic_top_name
                    2 -> plateInfo.pic_btm_name
                    else -> plateInfo.pic_top_name
                }
                try {
                    val bitmap = ImageUnit.getBitmap(
                        this,
                        imgName,
                        plateBinding.root.width,
                        plateBinding.root.height,
                        P.displayDegree
                    )
                    if (bitmap == null) {
                        Toast.makeText(this, "圖片讀取失敗", Toast.LENGTH_LONG).show()
                        return@let
                    }
                    Glide.with(this)
                        .load(bitmap)
                        .skipMemoryCache(true)
                        .into(plateBinding.imageView)
                } catch (e: Exception) {
                    e.printStackTrace()
                    onError(null, e)
                }

                //先刪除監聽
                vm.dmcScrappedRecordListLiveData
                    .hasActiveObservers().also {
                        if (it)
                            vm.dmcScrappedRecordListLiveData
                                .removeObservers { this.lifecycle }
                    }

                //再開始監聽一次
                vm.dmcScrappedRecordListLiveData
                    .observe(this) { dmcScrappedRecordList ->

                        val model = plateInfo.model
                        val wh =
                            if (P.displayDegree == 90 || P.displayDegree == 270) model.split("*")
                                .map { it.toInt() }.reversed() else model.split("*")
                                .map { it.toInt() }
                        plateBinding.recycler.layoutManager = GridLayoutManager(this, wh[0])
                        val sAdapter = SingleReportAdapter(it, dmcScrappedRecordList)
                        plateBinding.recycler.adapter = sAdapter
                    }


            }
        }

    }

    private fun init() {

        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        optionItemBinding =
            OptionItemBinding.bind(mainBinding.includeMain.includeOption.optionItem.root)
        plateBinding = PlateBinding.bind(mainBinding.includeMain.includePanel.root)
        scrappedAlert = ScrappedOptionAlert(this)
        gui = Gui(this)
        //初始化抽屜選單紹定
        val toolbar = mainBinding.includeMain.includeOption.toolbar
        setSupportActionBar(toolbar)
        val toggle = ActionBarDrawerToggle(
            this,
            mainBinding.drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        mainBinding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        //設定資選單資料
        drawerHeaderBinding = NavHeaderMainBinding.bind(mainBinding.navView.getHeaderView(0))
        drawerHeaderBinding.tHeaderUserInfo.text = "${AppCenter.uName}: ${AppCenter.uId}"
        drawerHeaderBinding.tHeaderMobileNumber.text =
            "${getString(R.string.moblie)}: ${AppCenter.mobileSn}"
        val netState =
            if (NetworkInformation.isConnected(this)) "${getString(R.string.network_state)}: ${
                getString(R.string.online)
            }" else "${getString(R.string.network_state)}: ${getString(R.string.offline)}"
        drawerHeaderBinding.tHeaderNetState.text = netState
        Timer().schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    getString(R.string.system_time)
                    drawerHeaderBinding.tHeaderSystemTime.text =
                        "${getString(R.string.system_time)}:\n${P.getAppCenterTime()}"
                }
            }
        }, 0, 1000)
        //初始化選單基本功能
        mainBinding.navView.menu.findItem(R.id.menu_language).apply {
            setOnMenuItemClickListener(this@MainActivity)
        }
        mainBinding.navView.menu.findItem(R.id.menu_test_mode).apply {
            (actionView as SwitchMaterial).apply {
                isChecked = P.isTest
                setOnCheckedChangeListener(this@MainActivity)
            }
            setOnMenuItemClickListener(this@MainActivity)
        }
        mainBinding.navView.menu.findItem(R.id.menu_action).apply {
            (actionView as SwitchMaterial).apply {
                isChecked = P.isActionDebug
                setOnCheckedChangeListener(this@MainActivity)
            }
            setOnMenuItemClickListener(this@MainActivity)
        }
        mainBinding.navView.menu.findItem(R.id.app_info).apply {
            setOnMenuItemClickListener(this@MainActivity)
        }
        mainBinding.navView.menu.findItem(R.id.menu_offline).apply {
            (actionView as SwitchMaterial).apply {
                isChecked = P.isOffline
                setOnCheckedChangeListener(this@MainActivity)
            }
        }

        optionItemBinding.bDisplay.text = vm.displaySurface[P.display]
        //設定右邊選項按鈕事件
        setUpOptionClick()
    }

    inner class SingleReportAdapter(
        private val plateInfo: PlateInfo,
        private val dmcScrappedRecordList: List<DmcScrappedRecord>,
    ) : RecyclerView.Adapter<SingleReportViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SingleReportViewHolder {
            val singleReportItemBinding =
                SingleReportItemBinding.inflate(layoutInflater, parent, false)
            return SingleReportViewHolder(singleReportItemBinding.root)
        }

        override fun getItemCount(): Int =
            plateInfo.model.split("*").map { it.toInt() }.reduce { acc, s -> acc * s }

        override fun onBindViewHolder(holder: SingleReportViewHolder, position: Int) {
            val sPosition =
                NightUnit.changePcs(plateInfo.model, plateInfo.pcs, P.display, P.displayDegree)
                    .split(",")[holder.adapterPosition]
            holder.bindTo(
                sPosition,
                plateInfo,
                dmcScrappedRecordList.filter { it.position == sPosition })
        }

    }

    inner class SingleReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val scrappedReportItemBinding = SingleReportItemBinding.bind(itemView)
        fun bindTo(
            sPosition: String,
            plateInfo: PlateInfo,
            dmcScrappedRecordList: List<DmcScrappedRecord>
        ) {
            scrappedReportItemBinding.tSC.isVisible = false
            scrappedReportItemBinding.tSTitle.isVisible = false

            //設定整體高度
            scrappedReportItemBinding.apply {
                val layoutParams = ConstraintLayout.LayoutParams(root.layoutParams)
                val divisor =
                    if (P.displayDegree == 90 || P.displayDegree == 270) plateInfo.model.split("*")[0] else plateInfo.model.split(
                        "*"
                    )[1]
                layoutParams.height = plateBinding.imageView.height / divisor.toInt()
                root.layoutParams = layoutParams
            }
            scrappedReportItemBinding.tSIndex.text = sPosition
            //有報廢紀錄
            val panel = when (P.display) {
                0, 1 -> 0
                2 -> 1
                else -> 2
            }
            MainScope().launch {
                dmcScrappedRecordList.find { it.panel == panel.toString() }?.let {
                    val rOptionItem =
                        withContext(Dispatchers.Default) { vm.getROption(it.optionId) }
                    setScrappedUI(it, rOptionItem)
                    scrappedReportItemBinding.root.setOnClickListener { _ ->
                        scrappedAlert.myShow(
                            supportFragmentManager,
                            null,
                            sPosition,
                            rOptionItem,
                            it
                        )
                    }
                    return@launch
                }
                val tPanel = if (panel == 0) 1 else 0
                dmcScrappedRecordList.find { it.panel == tPanel.toString() }?.let {
                    val rOptionItem =
                        withContext(Dispatchers.Default) { vm.getROption(it.optionId) }
                    setScrappedUI(it, rOptionItem)
                    scrappedReportItemBinding.root.setOnClickListener {
                        gui.showWaringDialog("此報廢紀錄在背面!!")
                    }
                    return@launch
                }

                dmcScrappedRecordList.find { it.panel == "2" }?.let {
                    val rOptionItem =
                        withContext(Dispatchers.Default) { vm.getROption(it.optionId) }
                    setScrappedUI(it, rOptionItem)
                    scrappedReportItemBinding.root.setOnClickListener {
                        gui.showWaringDialog("單報超允")
                    }
                    return@launch
                }

                scrappedReportItemBinding.root.setOnClickListener {
                    scrappedAlert.myShow(supportFragmentManager, null, sPosition, null, null)
                }

            }
        }

        private suspend fun setScrappedUI(
            dmcScrappedRecord: DmcScrappedRecord,
            rOptionItem: ROptionItem
        ) {
            scrappedReportItemBinding.tSC.isVisible = true
            scrappedReportItemBinding.tSTitle.isVisible = true
            scrappedReportItemBinding.tSTitle.text = rOptionItem.title
            scrappedReportItemBinding.tSC.text = rOptionItem.optNo
            scrappedReportItemBinding.root.background = ContextCompat.getDrawable(
                itemView.context,
                if (dmcScrappedRecord.isUpload!!) R.drawable.red else R.drawable.green
            )
        }


    }

    private fun setUpOptionClick() {

        //批號搜搜尋
        optionItemBinding.bSearchPn.setOnClickListener { gui.showLnSearchAlert(this) }
        //測試用
        optionItemBinding.bTestTest.setOnClickListener { vm.loadInitData(this) }
        //藍芽配對搜尋
        optionItemBinding.bBlue.setOnClickListener { blue.alert.show() }

        //歷史紀錄
        optionItemBinding.bHistory.setOnClickListener {
            if (P.dmcCode == null) {
                Toast.makeText(this, "請刷入Code...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            historyDialogFragment.myShow(supportFragmentManager, null, P.dmcCode!!)
//            registerScan.launch(Intent(P.scanActionName))sa
        }

        //上傳狀況
        optionItemBinding.bUploadState.setOnClickListener {
            syncStateDialogFragment.showNow(supportFragmentManager, null)
        }

        //漏刷清單
        optionItemBinding.bMissionCheck.setOnClickListener {
            if (P.dmcCode.isNullOrEmpty()) {
                Toast.makeText(this, "請刷入Code...", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            MainScope().launch {
                val list = vm.loadMissBrush(P.dmcCode!!, P.station.toString(), this@MainActivity)
                list?.let {
                    if (it.isNullOrEmpty()) {
                        Snackbar.make(mainBinding.root, "無漏刷", Snackbar.LENGTH_SHORT).show()
                    } else {
                        val s = it.map { "${it.lotId},${it.wpnl},${it.spnl}" }
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("漏刷清單")
                            .setItems(s.toTypedArray(),null)
                            .setCancelable(false)
                            .setPositiveButton(getString(R.string.ok), null)
                            .show()
                        onFinished()
                    }
                }
            }
        }

        //項目旋轉 目前先當測試掃描用

        //翻面 TOP BACK
        optionItemBinding.bTurnDisplay.setOnClickListener {
            P.display = when (P.display) {
                1 -> 2
                2 -> 1
                else -> 2
            }
            (it as Button).text = vm.displaySurface[P.display]
            vm.plateMutableLiveData.value?.let {
                vm.plateMutableLiveData.postValue(it)
            }
        }

        //右璇
        optionItemBinding.bTurnRight.setOnClickListener {
            if (P.display == 2)
                P.displayDegree = when (P.displayDegree) {
                    0 -> 270
                    90 -> 0
                    180 -> 90
                    270 -> 180
                    else -> 0
                }
            else {
                P.displayDegree = when (P.displayDegree) {
                    0 -> 90
                    90 -> 180
                    180 -> 270
                    270 -> 0
                    else -> 0
                }
            }
            vm.plateMutableLiveData.value?.let {
                vm.plateMutableLiveData.postValue(it)
            }
        }

        //左旋
        optionItemBinding.bTurnLeft.setOnClickListener {
            if (P.display == 2)
                P.displayDegree = when (P.displayDegree) {
                    0 -> 90
                    90 -> 180
                    180 -> 270
                    270 -> 0
                    else -> 0
                }
            else {
                P.displayDegree = when (P.displayDegree) {
                    0 -> 270
                    90 -> 0
                    180 -> 90
                    270 -> 180
                    else -> 0
                }
            }
            vm.plateMutableLiveData.value?.let {
                vm.plateMutableLiveData.postValue(it)
            }
        }

        //ttttttttestetet
//        optionItemBinding.tPnDmc.setOnClickListener {
//            vm.testUpload(this)
//        }

        //Station
        vm.stationLiveData.observe(this) {
            val sList = it.map { it.title }
            var index: Int = 0
            optionItemBinding.bStation.setOnClickListener { v ->
                AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setSingleChoiceItems(
                        sList.toTypedArray(),
                        index
                    ) { dialogInterface: DialogInterface, i: Int ->
                        index = i
                    }
                    .setPositiveButton(getString(R.string.ok)) { _: DialogInterface, i: Int ->
                        P.station = it[index].sn.toInt()
                        P.dep = it[index].depSn.toInt()
                        optionItemBinding.bStation.text = it[index].title
                        Log.d("@@@station", "${it[index].title} sn: ${P.station} dep: ${P.dep} ")
                    }
                    .show()
            }
        }
        //顯示面 預設 正面 背面
        optionItemBinding.bDisplay.setOnClickListener {
            AlertDialog.Builder(this)
                .setCancelable(false)
                .setPositiveButton(getString(R.string.ok), null)
                .setSingleChoiceItems(
                    vm.displaySurface.toTypedArray(),
                    P.display
                ) { dialogInterface: DialogInterface, i: Int ->
                    Log.d("@@@plateDisplay", vm.displaySurface[i])
                    optionItemBinding.bDisplay.text = vm.displaySurface[i]
                    P.display = i
                }.show()
        }

    }

    //權限 => 藍芽 儲存讀寫
    private fun requestPermission() {
        registerPermission.launch(
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_ADMIN,
//                Manifest.permission.BLUETOOTH_PRIVILEGED, //只有系統APP才可用
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        )
    }

    //BlueToothAlert 藍芽裝置搜尋視窗
    //權限
    private val registerPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            Log.d("@@@Permission", "$it")
            val allTrue = it.values.all { it == true }
            if (allTrue) {
                //取的權限 開始初始化讀資料
                vm.loadInitData(this).invokeOnCompletion {
                    it?.let {
                        it.printStackTrace()
                        onError(null, Exception("初始化錯誤..."))
                    }
                    optionItemBinding.bStation.performClick()
                }
            } else {
                Toast.makeText(this, getString(R.string.permission_dined_msg), Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }

    //要求開啟藍芽
    private val registerOpenBluetooth =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            //開啟藍芽成功
            if (it.resultCode == RESULT_OK) {
                if (BluetoothAdapter.getDefaultAdapter() != null) {
                    //顯示掃描視窗
                    blue.alert.show()
                } else
                    Toast.makeText(this, getString(R.string.no_bluetooth_msg), Toast.LENGTH_SHORT)
                        .show()
            } else {
                //使用者拒絕開啟藍芽
                Snackbar.make(
                    mainBinding.root,
                    getString(R.string.open_b_msg),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }


    //註冊掃描
    private val registerScan =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val scanResult = it.data?.extras?.get("scan_result") as String?
                scanResult?.let {
                    onSingReportSearch(scanResult)
                }
            }
        }


    //藍芽
    private val blue by lazy {
        NBluetooth(this).apply {
            val tempInt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) 4 else 4
            bluetoothAdapter?.getProfileProxy(this@MainActivity, profileListener, tempInt)
            blueCallback = myDeviceCallback
            vm.scanedMutableLiveData.observe(this@MainActivity) {
                setFoundDeviceList(it.toList())
            }
        }
    }

    //藍芽裝置Callback
    private val myDeviceCallback = object : NBluetooth.DeviceCallBack {
        override fun onDeviceFound(bluetoothDevice: BluetoothDevice) {
            vm.filterAndUpdateBDevice(bluetoothDevice)
        }

        override fun onDeviceConnecting() {

        }

        override fun onDeviceConnect() {

        }

        override fun onDeviceDisconnect() {

        }

        //清空資料
        override fun onClear() {
            if (vm.mutableSetBDevice.isNullOrEmpty())
                return
            vm.mutableSetBDevice.clear()
            vm.scanedMutableLiveData.postValue(vm.mutableSetBDevice)
        }
    }

    override fun onMenuItemClick(mi: MenuItem?): Boolean {
        Log.d("@@@onMenuItemClick", "${mi?.itemId?.let { resources.getResourceName(it) }}")
        mi?.let {
            when (it.itemId) {
                R.id.menu_language -> {
                    // TODO:語言切換
                }
                //測試 與 action debug 觸發
                R.id.menu_test_mode -> (it.actionView as SwitchMaterial).performClick()
                R.id.menu_offline -> (it.actionView as SwitchMaterial).performClick()
                R.id.menu_action -> gui.showActionDebugChoiceAlert()
                R.id.app_info -> gui.showAppInfoAlert() //手機資訊
                else -> null
            }
        }
        return true
    }

    override fun onCheckedChanged(c: CompoundButton?, check: Boolean) {
        Log.d("@@@onCheckedChanged", "${c?.id?.let { resources.getResourceName(it) }}")
        c?.let {
            when (it.id) {
                R.id.menu_action -> P.isActionDebug = check
                R.id.menu_test_mode -> P.isTest = check
                R.id.menu_offline -> P.isOffline = check
            }
        }
    }

    //輸入暫存 -> Enter 時重製
    private val inputStringBuffer = StringBuilder()

    //額外鍵盤裝置接收
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        event?.device?.let {
            val intChar = event.unicodeChar
            if (intChar == 0) return false
            inputStringBuffer.append(event.displayLabel)
            if (keyCode == KEYCODE_ENTER) {
                Log.d("@@@test", inputStringBuffer.toString())
                onSingReportSearch(inputStringBuffer.toString())
                inputStringBuffer.clear()
            }
        }
        return false
    }


    override fun onStart() {
        super.onStart()
        registerReceiver(blue.bReceiver, blue.intentFilter)

    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(blue.bReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        vm.deleteDmcRecord()
        vm.plateMutableLiveData.postValue(null)
        blue.bluetoothAdapter?.closeProfileProxy(4,blue.bluetoothProfile)
    }


    override fun onSingReportSearch(dmc: String) {
        Log.d("@@@onSingleReportSearch", "$dmc")
        val isTypeMatch =
            Regex("M+O[PS][a-zA-Z\\d]{7}-\\d{2}-\\d{3}-\\d{2}-\\d,\\d{3},\\d{2}").containsMatchIn(
                dmc
            )
        if (isTypeMatch) {
            vm.singleReportSearch(dmc, this)
        } else {
            Toast.makeText(this, "Code Type Error!!!!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onSetPlate(plateInfo: PlateInfo, dmc: String, infoId: String) {
        Log.d("@@@onSetPlate", "\nDMC: $dmc\nPlateInfo: $plateInfo")
        P.dmcCode = dmc
        P.infoId = infoId
        P.pSize = plateInfo.model
        runOnUiThread {
            optionItemBinding.tAcceptQty.text = "允收數: ${plateInfo.allowAcceptQty}"
            optionItemBinding.tPnDmc.text = "料號: ${plateInfo.pn}\n條碼: $dmc"
        }
        vm.plateMutableLiveData.postValue(plateInfo)

        //觸發上傳檢測紀錄
        vm.uploadScanInRecord(this)
    }


    //My Interface
    override fun onState(state: String, msg: String?) {
        Log.d("@@@OnStateCallback", "onState: $state $msg")
        runOnUiThread {
            when (state) {
                "no new image setting" -> {

                }
                "scanInLoading" -> {
                    //test 正式不必跳出
//                    Snackbar.make(mainBinding.root, "上傳檢測紀錄中...", Snackbar.LENGTH_LONG).show()
                }
                "upload scrapped error" -> {
                    msg?.let {
                        gui.showWaringDialog("💬上傳報廢紀錄錯誤:\n\n$it")
                    }
                }
                "star scan" ->{
                    registerScan.launch(Intent(P.NIGHT_SCAN))
                }
                //到這通常都是耗時處理，所以show loading alert
                else -> gui.showLoading(msg)
            }
        }
    }

    override fun onError(result: String?, e: Exception) {
        Log.d("@@@OnStateCallback", "onError")
        runOnUiThread {
//            gui.closeLoading()
            gui.showErrorAlert(result, e)
        }
    }

    override fun onActionDebug(actionDebug: ActionDebug) {
        Log.d("@@@OnStateCallback", "onActionDebug")
        runOnUiThread {
            gui.showActionDebugAlert(actionDebug)
        }
    }

    override fun onFinished() {
        Log.d("@@@OnStateCallback", "onFinished")
        runOnUiThread {
            gui.closeLoading()
        }
    }



}
