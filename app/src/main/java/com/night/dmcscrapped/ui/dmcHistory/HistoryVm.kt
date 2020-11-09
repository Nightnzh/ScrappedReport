package com.night.dmcscrapped.ui.dmcHistory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.kittinunf.fuel.util.encodeBase64UrlToString
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.night.dmcscrapped.data.model.net.DmcHistory
import com.night.dmcscrapped.gen.P
import com.night.dmcscrapped.units.NightUnit
import com.night.dmcscrapped.units.OnStateCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.Exception

class HistoryVm(application: Application) : AndroidViewModel(application) {

    var dmc : String? = null
    var listDmcHistory : List<DmcHistory>? = null
    val history = MutableLiveData<List<DmcHistory>?>(null)

    fun getDmcHistory(dmc:String , onStateCallback: OnStateCallback) = GlobalScope.launch(Dispatchers.Default){
        val params = listOf(
            "ln" to dmc.replace("\n","")
        )
        val re = NightUnit.nRequest(getApplication(),P.ACTION_getLog,params,onStateCallback)

        re?.let {
            val jo = JSONObject(it)
            try {
                listDmcHistory = Gson().fromJson<List<DmcHistory>>(jo.getJSONArray("data").toString(),object :TypeToken<List<DmcHistory>>(){}.type)
                history.postValue(listDmcHistory)
            }catch (e:Exception){
                e.printStackTrace()
                onStateCallback.onError(it,e)
            } finally {
                onStateCallback.onFinished()
            }
        }
    }

}