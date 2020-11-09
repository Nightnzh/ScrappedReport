package com.night.dmcscrapped.ui

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import com.night.dmcscrapped.R
import com.night.dmcscrapped.data.model.ActionDebug
import com.night.dmcscrapped.databinding.LnSearchBinding
import com.night.dmcscrapped.databinding.LoadingBinding
import com.night.dmcscrapped.gen.P
import com.night.dmcscrapped.units.OnStateCallback

class Gui(private val context: Context) {



    private val waringDialog by lazy {
        AlertDialog.Builder(context)
            .setCancelable(false)
            .setTitle(context.getString(R.string.waring))
            .setPositiveButton(context.getString(R.string.ok),null)
            .create()
    }

    fun showWaringDialog(msg:String){
        waringDialog.setMessage(msg)
        waringDialog.show()
    }


    //*********----------------------------------------------------------------------------------------------------------
    //pn Search
    private val pnTitleArray =  arrayOf("MOP","MOS")
    private val lnSearchBinding by lazy {
        LnSearchBinding.inflate(LayoutInflater.from(context)).apply {
            spinnerType.adapter = ArrayAdapter(context,R.layout.support_simple_spinner_dropdown_item, pnTitleArray)
        }
    }

    private fun clearInput() {
        listOf(lnSearchBinding.e1,lnSearchBinding.e2,lnSearchBinding.e3,lnSearchBinding.e4,lnSearchBinding.e5,lnSearchBinding.eWpsn,lnSearchBinding.eSpnl).forEach {
            it.editableText.clear()
        }
    }

    private val lnSearchAlert by lazy {
        AlertDialog.Builder(context)
            .setTitle("PN Search")
            .setCancelable(false)
            .setView(lnSearchBinding.root)
            .setPositiveButton(context.getString(R.string.search),null)
            .setNegativeButton(context.getString(R.string.cancel),null)
            .setNeutralButton(context.getString(R.string.clear),null)
            .create()
    }

    fun showLnSearchAlert(onStateCallback:OnStateCallback){
        lnSearchAlert.apply {
            setOnShowListener {dialogInterface ->
                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    //TODO: 檢查輸入格式
                    val s = "${pnTitleArray[lnSearchBinding.spinnerType.selectedItemPosition]}${lnSearchBinding.e1.editableText}-${lnSearchBinding.e2.editableText}-${lnSearchBinding.e3.editableText}-${lnSearchBinding.e4.editableText}-${lnSearchBinding.e5.editableText},${lnSearchBinding.eWpsn.editableText},${lnSearchBinding.eSpnl.editableText}"
                    onStateCallback.onSingReportSearch(s)
                    dialogInterface.dismiss()
                }
                getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                    clearInput()
                }
            }
        }
        lnSearchBinding.bCScan.setOnClickListener {
            lnSearchAlert.dismiss()
            onStateCallback.onState("star scan",null)
        }
        lnSearchAlert.show()
    }

    //----------------------------------------------------------------------------------------------

    //loading binding
    private val loadingBinding by lazy {
        LoadingBinding.inflate(LayoutInflater.from(context))
    }
    //loading alert
    private val loadingAlert by lazy {
        AlertDialog.Builder(context)
            .setCancelable(false)
            .setView(loadingBinding.root)
            .setOnDismissListener {
                loadingBinding.tLoadingState.text = context.getString(R.string.please_wait)
            }
            .create()
    }

    //loading alert 免重覆顯示
    fun showLoading(loadingState : String? = null){
        if(!loadingAlert.isShowing)
            loadingAlert.show()
        loadingState?.let { loadingBinding.tLoadingState.text = it }
    }
    //關閉loading alert
    fun closeLoading(){
        loadingAlert.dismiss()
    }

    //APP info 免重覆顯示
    private val appInfoAlert by lazy {
        AlertDialog.Builder(context)
            .setTitle("App Info")
            .setMessage("如有異常請通知開人員:\n\t手機異常: 許証皓(#2677) \n\t資料異常: 簡德瑋(#2678)")
            .create()
    }

    fun showAppInfoAlert(){
        appInfoAlert.show()
    }

    //action debug alert
    private val actionDebugAlert by lazy {
        AlertDialog.Builder(context)
            .setMultiChoiceItems(P.actionMap.keys.toTypedArray(),P.actionMap.values.toBooleanArray()){ _: DialogInterface, i: Int, b: Boolean ->
                P.actionMap[P.actionMap.keys.toTypedArray()[i]] = b
            }
            .create()
    }

    fun showActionDebugChoiceAlert(){
        actionDebugAlert.show()
    }

    //需重複顯示
    fun showActionDebugAlert(actionDebug: ActionDebug) {
        val result = if(actionDebug.result.length>200) "${actionDebug.result.substring(0,200)}..." else actionDebug.result
        val s =
            "🔹Url:<br><font color='#020887'>${actionDebug.url}</font><br><br>💧Params:<br><font color='00241B'>${actionDebug.params}</font><br><br>💬Result:<br>${result}"
        AlertDialog.Builder(context)
            .setTitle("ActionDebug")
            .setMessage(HtmlCompat.fromHtml(s, HtmlCompat.FROM_HTML_MODE_COMPACT))
            .show()
    }

    //需重複顯示
    fun showErrorAlert(result:String?,e: Exception) {
        val temp = "⚠Exception:\n\t${e.message}\n\n${result?.let { if(it.length> 200) result.substring(0,200) + "..." else it  } }"
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.error))
            .setMessage(temp)
            .setCancelable(false)
            .setPositiveButton(context.getString(R.string.ok), null)
            .show()
    }
}