package com.night.dmcscrapped.units

import android.content.Context
import android.util.Log
import com.boardtek.appcenter.NetworkInformation
import com.github.kittinunf.fuel.httpPost
import com.night.dmcscrapped.data.model.ActionDebug
import com.night.dmcscrapped.gen.P
import java.io.File
import java.util.zip.ZipFile


//這是 網路請求的部分 zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz
/**Http Post 請求
 *  context => 檢查網路
 *  actionName => 請求Action更換
 *  params => post參數
 *  onStateCallBack =>
 */

object NightUnit {

    fun nRequest(
        context: Context,
        actionName: String,
        params: List<Pair<String, String>>?,
        onStateCallback: OnStateCallback? = null
    ): String? {
        //網路判斷
        //請求

        return try {
            if (!NetworkInformation.isConnected(context)) {
                throw Exception("No Network...")
            }

            if(actionName == P.ACTION_syncScanList) {
                onStateCallback?.onState("scanInLoading", null)
            } else{
                onStateCallback?.onState("loading", "請稍後...")
            }
            val (request, response, result) = P.getUrlByAndActionName(actionName)
                .httpPost(params)
                .timeout(20000)
//                .timeoutRead(20000)
                .responseString(Charsets.UTF_8)

            Log.d(
                "@@@request",
                "${P.getUrlByAndActionName(actionName)}\n\n params:$params \n\n ${result.component1()}"
            )
//            Log.d("@@@Request", request.toString())
//            Log.d("@@@Response", response.toString())
//            Log.d("@@@Result", result.toString())
            //發生錯誤
            result.component2()?.let {
                throw it
            }
            //回傳結果
            result.component1().also {
                if (P.isActionDebug && P.actionMap[actionName]!!) {
                    //action debug
                    it?.also {
                        val actionDebug = ActionDebug(
                            P.getUrlByAndActionName(actionName),
                            params.toString(),
                            it
                        )
                        onStateCallback?.onActionDebug(actionDebug)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onStateCallback?.onError(null, e)
            null
        }

    }

    //for download zip
    fun downloadZip(
        context: Context,
        actionName: String,
        params: List<Pair<String, String>>?,
        onStateCallback: OnStateCallback? = null
    ): ByteArray? {
        //網路判斷
        //請求
        return try {
            if (!NetworkInformation.isConnected(context)) {
                throw Exception("No Network")
            }
            onStateCallback?.onState("loading", "請稍後...")
            val (request, response, result) = P.getUrlByAndActionName(actionName)
                .httpPost(params)
                .timeout(10000)
//                .timeoutRead(20000)
                .response()

            Log.d(
                "@@@request",
                "${P.getUrlByAndActionName(actionName)}\n\n params:$params \n\n ${result.component1()}"
            )
//            Log.d("@@@Request", request.toString())
//            Log.d("@@@Response", response.toString())
//            Log.d("@@@Result", result.toString())

            result.component2()?.let {
                throw it
            }

            result.component1().also {
                if (P.isActionDebug && P.actionMap[actionName]!!) {
                    //action debug
                    it?.also {
                        val actionDebug = ActionDebug(
                            P.getUrlByAndActionName(actionName),
                            params.toString(),
                            it.toString()
                        )
                        onStateCallback?.onActionDebug(actionDebug)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onStateCallback?.onError(null, e)
            null
        }

    }


    //解壓縮
    fun unZip(
        context: Context,
        fileName: String,
        onStateCallback: OnStateCallback? = null
    ): Boolean {
        onStateCallback?.onState("unZip", "解壓縮檔案中...")

        try {
            val file = File(context.filesDir, fileName)
            val zipFile = ZipFile(file)
            //解壓縮!!
            zipFile.entries().asSequence().forEach { entry ->
                zipFile.getInputStream(entry).use { input ->
                    File(context.filesDir, entry.name).outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            file.deleteOnExit()
//            context.filesDir.list { file, s ->
//                if (s == fileName) {
//                    file.deleteOnExit()
//                }
//                Log.d("@@@UnZipFile", s)
//                true
//            }
        } catch (e: Exception) {
            onStateCallback?.onError("$fileName unZip Error", e)
            e.printStackTrace()
            return false
        } finally {

        }
        return true
    }


    //Plate 正面選轉ㄒㄩㄝ
 fun changePcs(model:String,pcs:String,display:Int,displayDegree:Int): String {

        var wh = model.split("*").map { it.toInt() }
//        println("@@@$wh")
        val pcsAry = pcs.split(",").map { it.toInt() }

        var map = ArrayList<ArrayList<Int>>()
//        map.add(intArrayOf(4,3))

        for( i in 0 until wh[1]){
            map.add(arrayListOf())
            for(j in 0 until wh[0]){
                map[i].add(pcsAry[i*wh[0]+j])
            }
        }

        when(displayDegree){
            90 ->{
                wh = wh.reversed()
                val newMap = ArrayList<ArrayList<Int>>()
                for(i in 0 until wh[1]){
                    newMap.add(arrayListOf())
                    var k = wh[0]-1
                    for(j in 0..k){
                        newMap[i].add(map[k][i])
                        k--
                    }
                }
                map = newMap
            }
            180 -> {
                map.apply {
                    forEach {
                        it.reverse()
                    }
                    reverse()
                }
            }
            270 -> {
                wh = wh.reversed()
                val newMap = ArrayList<ArrayList<Int>>()
                var k = wh[1]-1

                for(i in 0..k){
                    newMap.add(arrayListOf())
                    for (j in 0 until wh[0]){
                        newMap[i].add(map[j][k])
                    }
                    k--
                }
                map = newMap
            }
        }

        if(display == 2){
            map.apply {
                map{
                    it.reverse()
                }
            }
        }

        val out = map.map { it.joinToString { it.toString() } }.joinToString { "$it" }.replace(" ","")

        return  out
    }
}





