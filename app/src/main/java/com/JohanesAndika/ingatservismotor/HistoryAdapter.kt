package com.JohanesAndika.ingatservismotor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.JohanesAndika.ingatservismotor.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(private val logs: List<MaintenanceLog>) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
    class ViewHolder(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = 
        ViewHolder(ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val log = logs[position]
        holder.binding.tvComponent.text = log.component_type.uppercase()
        holder.binding.tvDate.text = "Tanggal: " + SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(log.replaced_date?.toDate() ?: Date())
        holder.binding.tvOdometer.text = "Odometer: ${log.replaced_at_odometer} km"
        holder.binding.tvNotes.text = if (log.notes.isNotEmpty()) "Catatan: ${log.notes}" else "Tidak ada catatan"
    }

    override fun getItemCount() = logs.size
}