package com.night.dmcscrapped.ui.main

import android.Manifest
import android.app.Application
import android.bluetooth.*
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.graphics.ColorFilter
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
import androidx.core.content.edit
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.boardtek.appcenter.AppCenter
import com.boardtek.appcenter.NetworkInformation
import com.bumptech.glide.Glide
import com.bumptech.glide.load.Transformation
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.night.dmcscrapped.ui.scrapedoption.ScrappedOptionAlert
import com.night.dmcscrapped.R
import com.night.dmcscrapped.data.db.MyRoomDB
import com.night.dmcscrapped.data.model.ActionDebug
import com.night.dmcscrapped.data.model.DmcScrappedRecord
import com.night.dmcscrapped.data.model.PlateInfo
import com.night.dmcscrapped.data.model.ROptionItem
import com.night.dmcscrapped.databinding.*
import com.night.dmcscrapped.gen.P
import com.night.dmcscrapped.ui.Gui
import com.night.dmcscrapped.ui.bluetooth.BluetoothDialogAlert
import com.night.dmcscrapped.ui.dmcHistory.HistoryDialogFragment
import com.night.dmcscrapped.ui.syncstate.SyncStateDialogFragment
import com.night.dmcscrapped.units.*
import com.night.dmcscrapped.work.SyncWork
import kotlinx.coroutines.*
import java.util.*


class MainActivity : AppCompatActivity(), OnStateCallback, MenuItem.OnMenuItemClickListener,
    CompoundButton.OnCheckedChangeListener {


    private val tempOpen = "tempOpen"

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
        AppCenter.init(this)
        vm = ViewModelProvider(this).get(MainViewModel::class.java)
        requestPermission()
        init()
        setContentView(mainBinding.root)
        plateBinding.recycler.setHasFixedSize(true)


        //圖片
        vm.plateMutableLiveData.observe(this) { plateInfo ->

            if (plateInfo == null) {
                mainBinding.includeMain.includeOption.tWaringMsg.visibility = View.GONE
                mainBinding.includeMain.includePanel.imageView.visibility = View.INVISIBLE
                optionItemBinding.tTb.visibility = View.GONE
                optionItemBinding.bTurnDisplay.visibility = View.GONE
                optionItemBinding.bTurnLeft.visibility = View.GONE
                optionItemBinding.bTurnRight.visibility = View.GONE
            }
            plateInfo?.let {

                mainBinding.includeMain.includePanel.imageView.visibility = View.VISIBLE
                optionItemBinding.tTb.visibility = View.VISIBLE
                optionItemBinding.bTurnDisplay.visibility = View.VISIBLE
                optionItemBinding.bTurnLeft.visibility = View.VISIBLE
                optionItemBinding.bTurnRight.visibility = View.VISIBLE

                //750007E 不可再FQC-2貼上黑色貼紙
                if (plateInfo.pn.contains("750007E")) {
                    gui.showWaringDialog(
                        HtmlCompat.fromHtml(
                            "此類料號(<font color='#020887'>750007E</font>)在FQC-2站不可貼上黑色貼紙。<br><br> This PN item like <font color='#020887'>750007E</font> in 'FQC-2' station can't be labeled black sticker.",
                            HtmlCompat.FROM_HTML_MODE_COMPACT
                        )
                    )
                    mainBinding.includeMain.includeOption.tWaringMsg.visibility = View.VISIBLE
                } else {
                    mainBinding.includeMain.includeOption.tWaringMsg.visibility = View.GONE
                }

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
                        P.display,
                        P.displayDegree,
                        plateInfo
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

        //同步為上傳log數量
        vm.synCount.observe(this) {
            if (it == 0) {
                optionItemBinding.tSyncCount.isVisible = false
            } else {
                optionItemBinding.tSyncCount.isVisible = true
                optionItemBinding.tSyncCount.text = "$it"
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
                    if (P.getAppCenterTime() != null) {
                        drawerHeaderBinding.tHeaderSystemTime.text =
                            "${getString(R.string.system_time)}:\n${P.getAppCenterTime()}"
                    }
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
        mainBinding.navView.menu.findItem(R.id.data_refresh).apply {
            setOnMenuItemClickListener(this@MainActivity)
        }

        //開發人員選項
        if (AppCenter.depSn == 60 || AppCenter.depSn == 156) {
            mainBinding.navView.menu.findItem(R.id.menu_test_mode).isVisible = true
            mainBinding.navView.menu.findItem(R.id.menu_action).isVisible = true
        } else {
            mainBinding.navView.menu.findItem(R.id.menu_test_mode).isVisible = false
            mainBinding.navView.menu.findItem(R.id.menu_action).isVisible = false
        }

        optionItemBinding.tSyncCount.bringToFront()
        optionItemBinding.tUser.text = "${AppCenter.uName} : ${AppCenter.uId}"

//        optionItemBinding.bDisplay.text = vm.displaySurface[P.display]
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
                //單報(pcs 優先)
                dmcScrappedRecordList.find { it.panel == panel.toString() }?.let {
                    val rOptionItem =
                        withContext(Dispatchers.Default) { vm.getROption(it.optionId) }
                    setScrappedUI(rOptionItem)
                    scrappedReportItemBinding.root.setOnClickListener { _ ->
                        if (it.gSn != P.station.toString()) {
                            AlertDialog.Builder(this@MainActivity).setTitle("不同站別更改報廢請找課長或主管協助!!")
                                .setPositiveButton(getString(R.string.ok), null).show()
                            return@setOnClickListener
                        }
                        scrappedAlert.myShow(
                            supportFragmentManager,
                            null,
                            sPosition,
                            rOptionItem,
                            it,
                            P.station.toString()
                        )
                    }
                    return@launch
                }
                //單報(pcs 優先) 沒此面找另一面
                val tPanel = if (panel == 0) 1 else 0
                dmcScrappedRecordList.find { it.panel == tPanel.toString() }?.let {
                    val rOptionItem =
                        withContext(Dispatchers.Default) { vm.getROption(it.optionId) }
                    setScrappedUI(rOptionItem)
                    scrappedReportItemBinding.root.setOnClickListener {
                        gui.showWaringDialog("此報廢紀錄在另一面!!")
                    }
                    return@launch
                }

                //片報
                dmcScrappedRecordList.find {
                    it.optionId == "225" || it.optionId == "227"
                }?.let {
                    val rOptionItem =
                        withContext(Dispatchers.Default) { vm.getROption(it.optionId) }
                    setScrappedUI(rOptionItem)
                    scrappedReportItemBinding.root.setOnClickListener { _ ->
                        scrappedAlert.myShow(
                            supportFragmentManager,
                            null,
                            sPosition,
                            rOptionItem,
                            it,
                            P.station.toString()
                        )
                    }
                    return@launch
                }

                //超允(後端配置判斷)
                dmcScrappedRecordList.find { it.optionId == "224" }?.let {
                    val rOptionItem =
                        withContext(Dispatchers.Default) { vm.getROption(it.optionId) }
                    setScrappedUI(rOptionItem)
                    scrappedReportItemBinding.root.setOnClickListener {
                        gui.showWaringDialog("系統設置不可修改")
                    }
                    return@launch
                }


                scrappedReportItemBinding.root.setOnClickListener {
                    scrappedAlert.myShow(
                        supportFragmentManager,
                        null,
                        sPosition,
                        null,
                        null,
                        P.station.toString()
                    )
                }


            }
        }

        private fun setScrappedUI(
//            dmcScrappedRecord: DmcScrappedRecord,
            rOptionItem: ROptionItem
        ) {
            scrappedReportItemBinding.tSC.isVisible = true
            scrappedReportItemBinding.tSTitle.isVisible = true
            kotlin.runCatching {
                scrappedReportItemBinding.tSTitle.text = rOptionItem.title
                scrappedReportItemBinding.tSC.text = rOptionItem.optNo
            }
            scrappedReportItemBinding.root.background = ContextCompat.getDrawable(
                itemView.context,
                R.drawable.red
            )

//            scrappedReportItemBinding.root.background = ContextCompat.getDrawable(
//                itemView.context,
//                if (dmcScrappedRecord.isUpload!!) R.drawable.red else R.drawable.green
//            )
        }


    }

    private val blueAlert by lazy {
        BluetoothDialogAlert()
    }

    private fun setUpOptionClick() {

        //批號搜搜尋
        optionItemBinding.bSearchPn.setOnClickListener { gui.showLnSearchAlert(this) }
        //測試用
        optionItemBinding.bTestTest.setOnClickListener {
            val setDate = P.getAppCenterTime()
            vm.loadInitData(setDate, this)
        }
        //藍芽配對搜尋
        optionItemBinding.bBlue.setOnClickListener {
            if (BluetoothAdapter.getDefaultAdapter() == null) {
                gui.showWaringDialog("此裝置沒有藍芽!!!")
                return@setOnClickListener
            }
            if (!BluetoothAdapter.getDefaultAdapter().isEnabled) {
                gui.showWaringDialog("手機藍芽未開啟...請通知前言處請求協助(#72677)。")
                return@setOnClickListener
            }
            //Android 版本 >= 9(P API 28) 藍芽配對時會觸發APP Lock ，顯示提示訊息!
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                AlertDialog.Builder(this)
                    .setTitle("此手機因Android版本與資安政策要求下會造成藍芽配對的問題，如需協助請通知前言處(#72677)。")
                    .setCancelable(false)
                    .setPositiveButton("繼續") { dialogInterface: DialogInterface, i: Int ->
                        blueAlert.show(supportFragmentManager, null)
                    }.setNegativeButton("取消", null).show()
            } else {
                blueAlert.show(supportFragmentManager, null)
            }
        }

        //歷史紀錄
        optionItemBinding.bHistory.setOnClickListener {
            if (P.dmcCode == null) {
                Toast.makeText(this, "請刷入Code...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            historyDialogFragment.myShow(supportFragmentManager, null, P.dmcCode!!)
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
                onFinished()
                list.let {
                    if (it.isNullOrEmpty()) {
                        Snackbar.make(mainBinding.root, "無漏刷", Snackbar.LENGTH_SHORT).show()
                    } else {
                        val s = it.map { "${it.lotId},${it.wpnl},${it.spnl}" }
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("漏刷清單")
                            .setItems(s.toTypedArray(), null)
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
        Log.d("@@@test dep", "${AppCenter.depSn}")

        //Station 站別
        vm.stationLiveData.observe(this) {
            val sList = when (AppCenter.depSn) {
                60, 156 -> it.map { it.title }
                AppCenter.depSn -> it.filter { AppCenter.depSn == it.depSn.toInt() }
                    .map { it.title }
                else -> null
            }

//            if(sList.isNullOrEmpty()){
//                AlertDialog.Builder(this).setCancelable(false).setTitle("請檢查認證中心使用者登入資訊，系統檢測您無權限使用...")
//                    .setPositiveButton("關閉"){ _: DialogInterface, _: Int ->
//                        finish()
//                    }
//                    .show()
//                return@observe
//            }

            var index: Int = 0
            optionItemBinding.bStation.setOnClickListener { v ->

                AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setSingleChoiceItems(
                        sList!!.toTypedArray(),
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


    }

    //權限 => 藍芽 儲存讀寫
    private fun requestPermission() {
        if (getSharedPreferences("setting", Context.MODE_PRIVATE).getBoolean(tempOpen, true))
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


    //權限
    private val registerPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            Log.d("@@@Permission", "$it")
            val allTrue = it.values.all { it == true }
            if (allTrue) {
                //取的權限後 開始初始化讀資料
                val isFirstOpen = getSharedPreferences(
                    "setting",
                    Context.MODE_PRIVATE
                ).getBoolean("isFirstOpen", true)
                val setDate = if (isFirstOpen) "" else P.getAppCenterTime()
                vm.loadInitData(setDate, this).invokeOnCompletion {
                    it?.let {
                        it.printStackTrace()
                        onError(null, Exception("初始化錯誤..."))
                        //已上成功才算完成初始化( isFirstOpen = true )
                        getSharedPreferences(
                            "setting",
                            Context.MODE_PRIVATE
                        ).edit().putBoolean("isFirstOpen", false).apply()
                    }
                    runOnUiThread {
                        optionItemBinding.bStation.performClick()
                    }
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
//                    blue.alert.show()
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


//    //登入
//    private val registerAppCenter =
//        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
//            if (it.resultCode == RESULT_OK) {
//                Toast.makeText(this, "登入成功", Toast.LENGTH_SHORT).show()
//            }
//        }


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
                R.id.data_refresh -> {
                    val setDate = ""
                    vm.loadInitData(setDate, this)
                }
                else -> null
            }
        }
        return true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        /*if (newConfig.orientation === Configuration.ORIENTATION_LANDSCAPE) {
            Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show()
        } else if (newConfig.orientation === Configuration.ORIENTATION_PORTRAIT) {
            Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show()
        }*/
        super.onConfigurationChanged(newConfig)
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
        P.plateInfo?.let {
            vm.plateMutableLiveData.postValue(it)
        }
        vm.clearData7()


    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        vm.deleteDmcRecord()
        vm.plateMutableLiveData.postValue(null)
        vm.clearData7()
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

    override fun onSetPlate(pn: String, plateInfo: PlateInfo, dmc: String, infoId: String) {
        Log.d("@@@onSetPlate", "\nDMC: $dmc\nPlateInfo: $plateInfo")
        P.pn = pn
        P.plateInfo = plateInfo
        P.dmcCode = dmc
        P.infoId = infoId
        P.pSize = plateInfo.model
        runOnUiThread {
            optionItemBinding.tPnDmc.text = "料號: ${plateInfo.pn}\n條碼: ${dmc.replace("\n", "")}"
            optionItemBinding.tAcceptQty.text = "允收數: ${plateInfo.allowAcceptQty}"
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
                "star scan" -> {
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


    override fun onSync() {
        if (!P.isOffline) {
            val uniqueWorkRequest = OneTimeWorkRequestBuilder<SyncWork>()
                .build()
            WorkManager.getInstance(this)
                .beginUniqueWork("uniWorkForSync", ExistingWorkPolicy.KEEP, uniqueWorkRequest)
                .enqueue()
        }
    }

}
