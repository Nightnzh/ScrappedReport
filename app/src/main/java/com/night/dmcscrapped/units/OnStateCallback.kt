package com.night.dmcscrapped.units

import com.night.dmcscrapped.data.model.ActionDebug
import com.night.dmcscrapped.data.model.DmcScrappedRecord
import com.night.dmcscrapped.data.model.PlateInfo

interface OnStateCallback {

     //用於dmc找料號(重要) 3種觸發方式 1.手動輸入 2.相機掃描 3.HID Device輸入(藍芽 或 USB)
     fun onSingReportSearch(dmc:String)
     //用於刷入正確單報資料時(重要)
     fun onSetPlate(plateInfo: PlateInfo, dmc:String,infoId:String)
     //用於各種狀態 較常用於網路請求時
     fun onState(state:String,msg:String?)
     //各種Exception
     fun onError( result : String? =null, e:Exception )
     //公司Action api Debug
     fun onActionDebug(actionDebug: ActionDebug)
     //用於任務結束
     fun onFinished()
}