package com.JohanesAndika.ingatservismotor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.JohanesAndika.ingatservismotor.databinding.ItemPartBinding

data class PartModel(val name: String, val partNumber: String, val price: String)

class PartAdapter : RecyclerView.Adapter<PartAdapter.PartViewHolder>() {

    private val parts = listOf(
        PartModel("Oli MPX-1", "08232-2MB-K1LZ0", "Rp 55.000"),
        PartModel("Kampas Rem Depan", "06455-KPP-901", "Rp 45.000"),
        PartModel("Busi", "31916-K03-H11", "Rp 15.000")
    )

    class PartViewHolder(val binding: ItemPartBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PartViewHolder {
        val binding = ItemPartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PartViewHolder, position: Int) {
        val part = parts[position]
        holder.binding.tvPartName.text = part.name
        holder.binding.tvPartNumber.text = part.partNumber
        holder.binding.tvPrice.text = part.price
    }

    override fun getItemCount(): Int = parts.size
}