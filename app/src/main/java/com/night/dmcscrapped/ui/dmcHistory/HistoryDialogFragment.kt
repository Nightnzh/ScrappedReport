package com.night.dmcscrapped.ui.dmcHistory

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import androidx.recyclerview.widget.SortedListAdapterCallback
import com.night.dmcscrapped.R
import com.night.dmcscrapped.data.model.ActionDebug
import com.night.dmcscrapped.data.model.PlateInfo
import com.night.dmcscrapped.data.model.net.DmcHistory
import com.night.dmcscrapped.databinding.DmcHistoryBinding
import com.night.dmcscrapped.databinding.DmcHistoryItemBinding
import com.night.dmcscrapped.units.OnStateCallback


//歷史紀錄
class HistoryDialogFragment(private val onStateCallback: OnStateCallback) : DialogFragment(){

    private val vm  by viewModels<HistoryVm>()
    private lateinit var binding : DmcHistoryBinding

    private lateinit var adapter : DmcHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.ThemeOverlay_MaterialComponents)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DmcHistoryBinding.inflate(layoutInflater)
        binding.recycler.setHasFixedSize(true)
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = DmcHistoryAdapter()
        binding.recycler.adapter = adapter

        vm.history.observe(viewLifecycleOwner){
            it?.let {
                (binding.recycler.adapter as DmcHistoryAdapter).sortList.replaceAll(it)
            }
        }

        binding.bRefresh.setOnClickListener {
            vm.listDmcHistory = null
            vm.history.postValue(vm.listDmcHistory)
            vm.getDmcHistory(vm.dmc!!,onStateCallback).invokeOnCompletion {
                onStateCallback.onFinished()
            }
        }

        return binding.root
    }

    inner class DmcHistoryAdapter : RecyclerView.Adapter<DmcHistoryViewHolder>(){

        val sortList = SortedList<DmcHistory>(DmcHistory::class.java,object :SortedListAdapterCallback<DmcHistory>(this){
            override fun compare(o1: DmcHistory?, o2: DmcHistory?): Int = 0

            override fun areContentsTheSame(oldItem: DmcHistory?, newItem: DmcHistory?): Boolean = oldItem == newItem

            override fun areItemsTheSame(item1: DmcHistory?, item2: DmcHistory?): Boolean = item1 == item2
        })

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DmcHistoryViewHolder {
            val itemBinding = DmcHistoryItemBinding.inflate(layoutInflater,parent,false)
            return DmcHistoryViewHolder(itemBinding.root)
        }

        override fun onBindViewHolder(holder: DmcHistoryViewHolder, position: Int) {
            holder.bindTo(sortList[holder.adapterPosition])
        }

        override fun getItemCount(): Int = sortList.size()
    }
    inner class DmcHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        private val itemBinding = DmcHistoryItemBinding.bind(itemView)
        fun bindTo(dmcHistory: DmcHistory){
            itemBinding.tTitle.text = dmcHistory.title
            val displayTB = when(dmcHistory.panel){
                "0" -> "TOP"
                "1" -> "BTM"
                "2" -> "TOP/BTM"
                else -> null
            }
            itemBinding.tPcsAndDisplay.text = "TOP/BTM: $displayTB 位置(pcs): ${dmcHistory.pcs}"
            itemBinding.tValue.text = "舊值: ${dmcHistory.oldValue} 新值: ${dmcHistory.newValue}"
            itemBinding.tUName.text = "設置人員: ${dmcHistory.setName}"
            itemBinding.tSetDate.text = "設置時間: ${dmcHistory.setDate}"
        }
    }

    fun myShow(fragmentManager: FragmentManager, tag:String? ,dmc:String){
        super.showNow(fragmentManager,tag)
        binding.tDmcCode.text = dmc.replace("\n","")
        vm.dmc = dmc
        vm.getDmcHistory(vm.dmc!!,onStateCallback).invokeOnCompletion {
            onStateCallback.onFinished()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        vm.dmc = null
        vm.listDmcHistory = null
        vm.history.postValue(vm.listDmcHistory)
    }
}