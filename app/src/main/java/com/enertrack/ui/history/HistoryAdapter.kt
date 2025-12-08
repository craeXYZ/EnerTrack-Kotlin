package com.enertrack.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.enertrack.data.model.HistoryItem
import com.enertrack.databinding.ItemHistoryCardBinding // Pastikan nama file ini benar

class HistoryAdapter(
    private val onDetailClick: (HistoryItem) -> Unit // Callback baru
) : ListAdapter<HistoryItem, HistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position), onDetailClick)
    }

    class HistoryViewHolder(private val binding: ItemHistoryCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HistoryItem, onDetailClick: (HistoryItem) -> Unit) {
            // Tampilan simpel sesuai desainmu yang sekarang
            binding.tvApplianceName.text = item.appliance
            binding.tvDate.text = item.date

            // Atur listener untuk tombol "Detail"
            binding.btnDetail.setOnClickListener { onDetailClick(item) }
        }
    }
}