package com.night.dmcscrapped.ui.syncstate

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import androidx.recyclerview.widget.SortedListAdapterCallback
import androidx.work.*
import com.google.android.material.snackbar.Snackbar
import com.night.dmcscrapped.R
import com.night.dmcscrapped.data.model.MyLog
import com.night.dmcscrapped.databinding.SyncStateBinding
import com.night.dmcscrapped.databinding.SyncStateItemBinding
import com.night.dmcscrapped.units.OnStateCallback
import com.night.dmcscrapped.work.SyncWork

//上傳狀況
class SyncStateDialogFragment(private val onStateCallback: OnStateCallback) : DialogFragment() {

    private val vm by viewModels<SyncVM>()
    private lateinit var binding: SyncStateBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.ThemeOverlay_MaterialComponents)
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = SyncStateBinding.inflate(layoutInflater)
        binding.sipnnerTtype.adapter = ArrayAdapter(requireContext(), R.layout.spinneritem, vm.type)
        binding.sipnnerTtype.onItemSelectedListener = typeSelectedListener

        binding.recyclerView2.setHasFixedSize(true)
        binding.recyclerView2.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView2.adapter = SyncStateAdapter()

        vm.logLiveData.observe(viewLifecycleOwner) {
            if (it.isNullOrEmpty()) return@observe
            vm.dateList = it.map { it.setDate.substring(0, 10) }.toSet().toList()
            binding.spinnerDate.adapter =
                ArrayAdapter(requireContext(), R.layout.spinneritem, vm.dateList!!.toTypedArray())
            binding.spinnerDate.onItemSelectedListener = dateSelectListener
//            (binding.recyclerView2.adapter as SyncStateAdapter).sortedList.replaceAll(it)
        }

        binding.bUpload.setOnClickListener {
            Log.d("@@@sync", "onClick")

            if(vm.canManulySync){
                vm.sync(onStateCallback)
            } else {
                Snackbar.make(requireView(),"資料上傳中，請稍後再試...",Snackbar.LENGTH_LONG).show()
            }

        }
        vm.workInfo.observe(viewLifecycleOwner){
            if(it.isNullOrEmpty()) {
                vm.canManulySync = true
                return@observe
            }
            vm.canManulySync = it[0].state.isFinished
        }

        vm.rOptionLiveData.observe(viewLifecycleOwner){

        }
        vm.stateLiveData.observe(viewLifecycleOwner) {

        }
        return binding.root
    }

    val dateSelectListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
            vm.selectedDate = vm.dateList!![p2]
            vm.logLiveData.value?.let {
                vm.filterItem(it, vm.selectedDate, vm.selectedState).also {
                    (binding.recyclerView2.adapter as SyncStateAdapter).sortedList.replaceAll(it)
                }
            }
        }

        override fun onNothingSelected(p0: AdapterView<*>?) {
        }
    }

    val typeSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
            vm.selectedState = p2
            vm.logLiveData.value?.let {
                vm.filterItem(it, vm.selectedDate, vm.selectedState).also {
                    (binding.recyclerView2.adapter as SyncStateAdapter).sortedList.replaceAll(it)
                }
            }
        }

        override fun onNothingSelected(p0: AdapterView<*>?) {
        }

    }

    inner class SyncStateAdapter : RecyclerView.Adapter<SyncStateHolder>() {

        val sortedList =
            SortedList<MyLog>(MyLog::class.java, object : SortedListAdapterCallback<MyLog>(this) {
                override fun compare(o1: MyLog?, o2: MyLog?): Int = 0

                override fun areContentsTheSame(oldItem: MyLog?, newItem: MyLog?): Boolean =
                    oldItem == newItem

                override fun areItemsTheSame(item1: MyLog?, item2: MyLog?): Boolean = item1 == item2
            })

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SyncStateHolder {
            val syncStateItemBinding = SyncStateItemBinding.inflate(layoutInflater, parent, false)
            return SyncStateHolder(syncStateItemBinding.root)
        }

        override fun getItemCount(): Int = sortedList.size()

        override fun onBindViewHolder(holder: SyncStateHolder, position: Int) {
            holder.bindTo(sortedList[holder.adapterPosition])
        }
    }

    inner class SyncStateHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemBinding = SyncStateItemBinding.bind(itemView)
        fun bindTo(myLog: MyLog) {
            itemBinding.tLogTest.isVisible = myLog.isTestData!!
            itemBinding.tLogTitle.text = vm.type[myLog.state + 1]
            itemBinding.tPn.text ="PN: ${myLog.pn}"
            itemBinding.tLogDmc.text = "LN: ${myLog.ln.replace("\n", "")}"
            itemBinding.tLogUserInfo.text = "設置人員: ${myLog.uName} | 設置時間: ${myLog.setDate}"
            val station = vm.stateLiveData.value?.let { it.find { it.sn == myLog.gSn } }?.title

            when (myLog.state) {
                0, 1 , 3 , 4-> {
                    val tb = when (myLog.panel) {
                        "0" -> "TOP"
                        "1" -> "BTM"
                        "2" -> "TOP/BTM"
                        else -> null
                    }
                    val optionItem =
                        vm.rOptionLiveData.value?.let { it.find { it.sn == myLog.optionId } }
                    itemBinding.tLogDetail.text =
                        "報廢選項: ${optionItem?.optNo}-${optionItem?.title}"
                    itemBinding.tStateStation.text = "站別: $station"
                    itemBinding.tTopBtm.isVisible = true
                    itemBinding.tTopBtm.text = "$tb | 位置(pcs): ${myLog.position} "
                }
                2 -> {
                    itemBinding.tLogDetail.visibility = View.GONE
                    itemBinding.tStateStation.text = "站別: $station"
                    itemBinding.tTopBtm.isVisible = false
                }
            }
            itemBinding.tLogState.text =
                if (myLog.isUpload!!) "狀態: ${myLog.logState}" else "狀態: 尚未上傳"
            itemBinding.imageView2.setImageDrawable(
                ContextCompat.getDrawable(
                    itemView.context,
                    if (myLog.isUpload!!) R.drawable.ic_check else R.drawable.ic_warning
                )
            )
            itemBinding.bLogDelete.isVisible = !myLog.isUpload!!
            if (!myLog.isUpload!!) {
                itemBinding.bLogDelete.setOnClickListener {
                    AlertDialog.Builder(itemView.context).setCancelable(false)
                        .setTitle("Sure to delete this Record?")
                        .setPositiveButton(getString(R.string.ok)){ _,_ ->
                            vm.delete(myLog)
                        }.show()
                }
            }
        }
    }

}