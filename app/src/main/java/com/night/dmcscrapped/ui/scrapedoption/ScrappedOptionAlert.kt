package com.night.dmcscrapped.ui.scrapedoption

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import androidx.recyclerview.widget.SortedListAdapterCallback
import com.google.android.material.snackbar.Snackbar
import com.night.dmcscrapped.R
import com.night.dmcscrapped.data.model.DmcScrappedRecord
import com.night.dmcscrapped.data.model.ROptionItem
import com.night.dmcscrapped.databinding.ScrappedOptionBinding
import com.night.dmcscrapped.gen.P
import com.night.dmcscrapped.units.OnStateCallback
import kotlinx.coroutines.MainScope


//報廢選項 Dialog 用fragment實踐模組化
class ScrappedOptionAlert(private val onStateCallback: OnStateCallback) : DialogFragment() {

    private lateinit var binding: ScrappedOptionBinding
    private lateinit var adapter: ScrappedOptionAdapter
    private val vm by viewModels<OptionViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.ThemeOverlay_MaterialComponents_Dark)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = ScrappedOptionBinding.inflate(layoutInflater)
        binding.scrappedReycler.setHasFixedSize(true)
        binding.scrappedReycler.layoutManager = GridLayoutManager(requireContext(), 3)
        adapter = ScrappedOptionAdapter()
        binding.scrappedReycler.adapter = ScrappedOptionAdapter()

        //關閉Dialog
        binding.bScrappedCancel.setOnClickListener {
            dismiss()
        }

        //新增報廢選項
        binding.bScrappedOk.setOnClickListener {
            if (vm.optionItem == null) {
                Toast.makeText(requireContext(), "未選擇任何報廢選項", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            vm.dmcScrappedRecord?.let {
                if (it.optionId == vm.optionItem!!.sn) {
                    Snackbar.make(binding.root, "此報廢原因已存在!請檢查刷新資料。", Snackbar.LENGTH_LONG).show()
                    return@setOnClickListener
                }
            }
            vm.setScrappedData(vm.dmcScrappedRecord, onStateCallback)
            dismiss()

        }

        //清除報廢選項
        binding.bScrappedClear.setOnClickListener {
            val msg = when (P.display) {
                0, 1 -> "正面"
                2 -> "反面"
                else -> null
            }
            val msgg = when (vm.dmcScrappedRecord!!.optionId) {
                "225" -> "確定要刪除此片( Spnl )的片報紀錄?"
                "227" -> "確定要刪除此片( Wpnl )的片報紀錄?"
                else -> "確定要清除位置為 ${vm.nowPosition} 的( $msg )報廢紀錄?"
            }
            AlertDialog.Builder(requireContext()).setCancelable(false).setTitle("⚠清除報廢選項")
                .setMessage(msgg)
                .setPositiveButton(getString(R.string.ok)) { _, _ ->
                    dismiss()
                    vm.setClearScrappedData(vm.dmcScrappedRecord!!, onStateCallback)
                }.setNegativeButton(getString(R.string.cancel), null).show()
        }

        vm.allScrappedOptionLive.observe(this) {optionList ->
            val adapter =
                (binding.scrappedReycler.adapter as ScrappedOptionAlert.ScrappedOptionAdapter)
            binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    query?.let{
                        adapter.sortedList.replaceAll(optionList)
                    }
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    when (newText) {
                        "" -> {
                            //不篩選
                            adapter.sortedList.replaceAll(optionList)
                        }
                        else -> {
                            val filterList =
                                optionList.filter {
                                    it.optNo.contains(newText!!) or it.title.contains(
                                        newText
                                    )
                                }
                            adapter.sortedList.replaceAll(filterList)
                        }
                    }
                    return true
                }
            })
            vm.station.observe(this){
                when(it){
                    "4" -> {
                        if(adapter.sortedList != optionList.filter { it.sn == "225" || it.sn == "227"})
                            adapter.sortedList.replaceAll(optionList.filter { it.sn == "225" || it.sn == "227"})
                    }
                    else -> {
                        if(adapter.sortedList!= optionList)
                            adapter.sortedList.replaceAll(optionList)
                        //其他站暫時不篩選
                    }
                }
            }
        }


        return binding.root
    }


    override fun setCancelable(cancelable: Boolean) {
        super.setCancelable(false)
    }


    inner class ScrappedOptionAdapter :
        RecyclerView.Adapter<ScrappedOptionAdapter.ScrappedOptionViewHolder>() {
        val sortedList: SortedList<ROptionItem>

        init {
            sortedList = SortedList(ROptionItem::class.java,
                object : SortedListAdapterCallback<ROptionItem>(this) {
                    override fun onInserted(position: Int, count: Int) {
                        super.onInserted(position, count)
                    }

                    override fun onRemoved(position: Int, count: Int) {
                        super.onRemoved(position, count)
                    }

                    override fun compare(o1: ROptionItem?, o2: ROptionItem?): Int {
                        return o1?.sn?.toInt() ?: 0
                    }

                    override fun areContentsTheSame(
                        oldItem: ROptionItem?,
                        newItem: ROptionItem?
                    ): Boolean {
                        return (oldItem == newItem).also {
                            Log.d("@@@areContentsTheSame", "$it")
                        }
                    }


                    override fun areItemsTheSame(
                        item1: ROptionItem?,
                        item2: ROptionItem?
                    ): Boolean {
                        return (item1 == item2).also {
                            Log.d("@@@areContentsTheSame", "$it")
                        }
                    }
                })
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ScrappedOptionViewHolder {

            val view = RadioButton(parent.context).apply {
                textSize = 25f
                background = ContextCompat.getDrawable(parent.context, R.drawable.green)
            }
            return ScrappedOptionViewHolder(view)
        }

        override fun getItemCount(): Int {
            return sortedList.size()
        }

        override fun onBindViewHolder(holder: ScrappedOptionViewHolder, position: Int) {
            holder.binTo(sortedList[holder.adapterPosition])
//            holder.setIsRecyclable(false)
        }

        inner class ScrappedOptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val radioButton = itemView as RadioButton
            fun binTo(rOptionItem: ROptionItem) {
                val query = binding.searchView.query
                val text = "${rOptionItem.optNo} ${rOptionItem.title}"
                radioButton.text = "${rOptionItem.optNo} ${rOptionItem.title}"
                //當下報廢選項
                val isMatch = rOptionItem == vm.optionItem
                radioButton.isChecked = isMatch
                if (isMatch) {
                }
                radioButton.setOnClickListener {
                    vm.optionItem = rOptionItem
                    notifyDataSetChanged()
                    Log.d("@@@optionItemOnSelected", "$rOptionItem")
                }
            }
        }
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState)
    }


    fun myShow(
        fragmentManager: FragmentManager,
        tag: String?,
        nowPosition: String,
        rOptionItem: ROptionItem?,
        dmcScrappedRecord: DmcScrappedRecord?,
        station: String
    ) {
        //判斷不同按別時課長協助
        super.showNow(fragmentManager, tag)
        vm.optionItem = rOptionItem
        vm.nowPosition = nowPosition
        binding.bScrappedClear.isVisible = false
        vm.dmcScrappedRecord = dmcScrappedRecord
        vm.optionItem?.let { binding.bScrappedClear.isVisible = true }
        vm.station.postValue(station)
    }


    override fun onStart() {
        super.onStart()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        binding.searchView.setQuery("", false)
        vm.optionItem = null
    }
}