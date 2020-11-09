package com.night.dmcscrapped.gen

import com.boardtek.appcenter.AppCenter
import com.boardtek.appcenter.NetworkInformation
import com.night.dmcscrapped.data.model.ROptionItem

class P {
    companion object {

        var isTest = true //測試模式

        var isOffline = false  //離線

        var isActionDebug = true //ActionDebug

        var dmcCode : String?= null

        var infoId:String? = null  //InfoId

        var display = 0   //單報正反面 0=>預設 1=>正面 2=>反面

        var displayDegree = 0  // 0 90 180 270

        var station = 0 //station

        var dep  : Int = 0

        var pSize = ""

        //藍芽裝置連接

        //PnSearch
        const val Night_PN_SEARCH = "PN_SEARCH"
        const val Night_SET_PLATE = "SET_PLATE"

        //http://retek-06/system_mvc/controller.php?s=dev,011608,500,FQCReport,mobile,mobile&action=
        const val URL_OFFICIAL = "modules_mvc,500,FQCReport,mobile,mobile&action="
        const val URL_TEST = "dev,011608,500,FQCReport,mobile,mobile&action="
        const val ACTION_syncPnlScrap = "syncPnlScrap"
        const val ACTION_getOptions = "getOptions"
        const val ACTION_downloadPhoto = "downloadPhoto"
        const val ACTION_errorLog = "errorLog"
        const val ACTION_syncPnlScrapDel = "syncPnlScrapDel"
        const val ACTION_syncData = "syncData"
        const val ACTION_syncDataDel = "syncDataDel"
        const val ACTION_syncScanList = "syncScanList"
        const val ACTION_loadGroup = "loadGroup"
        const val ACTION_downloadRecord = "downloadRecord"
        const val ACTION_loadPnList = "loadPnList"
        const val ACTION_checkPhoto = "checkPhoto"
        const val ACTION_searchPN = "searchPN"
        const val ACTION_getMissList = "getMissList"
        const val ACTION_checkDMC = "checkDMC"
        const val ACTION_getLog = "getLog"

        //http://retek-06/system_mvc/controller.php?s=dev,011608,d30,mobileDeviceBluetooth,main&action=downloadDevice
        //http://retek-06/system_mvc/controller.php?s=dev,011608,500,FQCReport,mobile,mobile&action=getLog

        //讀取藍芽裝置 只有正式區有效
        const val ACTION_downloadDevice = "downloadDevice"

        fun getUrlByAndActionName(actionName: String): String {
            val temp =
                StringBuilder("http://${NetworkInformation.actionIP}/system_mvc/controller.php?s=")

            if(actionName == ACTION_downloadDevice){
                if(isTest){
                    temp.append("dev,011608,d30,mobileDeviceBluetooth,main&action=")
                } else {
                    temp.append("modules_mvc,d30,mobileDeviceBluetooth,main&action=")
                }
            }
            else {
                if (isTest) {
                    temp.append(URL_TEST)
                } else {
                    temp.append(URL_OFFICIAL)
                }
            }
            temp.append(actionName)

            return temp.toString()
        }

        val actionMap = mutableMapOf(
            ACTION_syncPnlScrap to false,
            ACTION_getOptions to false,
            ACTION_downloadPhoto to false,
            ACTION_errorLog to false,
            ACTION_syncPnlScrapDel to false,
            ACTION_syncData to false,
            ACTION_syncDataDel to false,
            ACTION_syncScanList to false,
            ACTION_loadGroup to false,
            ACTION_downloadRecord to false,
            ACTION_loadPnList to false,
            ACTION_checkPhoto to false,
            ACTION_searchPN to false,
            ACTION_getMissList to false,
            ACTION_checkDMC to false,
            ACTION_downloadDevice to false,
            ACTION_getLog to false
        )

        fun getAppCenterTime(): String = AppCenter.getSystemTime()

        val NIGHT_SCAN = "com.boardtek.scan"



    }
}