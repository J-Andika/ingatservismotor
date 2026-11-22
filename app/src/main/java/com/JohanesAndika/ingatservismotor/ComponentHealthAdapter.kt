package com.JohanesAndika.ingatservismotor

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.JohanesAndika.ingatservismotor.databinding.ItemComponentHealthBinding

data class ComponentHealth(
    val name: String,
    val percentage: Int,
    val averageDailyKm: Int? = 0,
    val remainingKm: Long = 0
)

class ComponentHealthAdapter(private val components: List<ComponentHealth>) :
    RecyclerView.Adapter<ComponentHealthAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemComponentHealthBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemComponentHealthBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val component = components[position]
        holder.binding.tvComponentName.text = component.name.uppercase()

        // Always show health details, but toggle calibration note
        if (component.averageDailyKm == 0 || component.averageDailyKm == null) {
            holder.binding.tvCalibratingStatus.visibility = View.VISIBLE
        } else {
            holder.binding.tvCalibratingStatus.visibility = View.GONE
        }

        holder.binding.tvHealthPercentage.visibility = View.VISIBLE
        holder.binding.tvHealthStatus.visibility = View.VISIBLE
        holder.binding.pbHealth.progress = component.percentage
        holder.binding.tvHealthPercentage.text = "${component.percentage}%"

        val context = holder.itemView.context
        val (status, color) = when {
            component.percentage <= 0 -> "SERVIS SEKARANG JUGA!" to ContextCompat.getColor(context, R.color.danger)
            component.percentage <= 20 -> "BERSIAP UNTUK SERVIS" to ContextCompat.getColor(context, R.color.warning)
            component.percentage <= 50 -> "KONDISI CUKUP" to ContextCompat.getColor(context, R.color.warning)
            else -> "KONDISI BAIK" to ContextCompat.getColor(context, R.color.success)
        }

        holder.binding.tvHealthStatus.text = status
        holder.binding.tvHealthStatus.setTextColor(color)
        holder.binding.pbHealth.progressTintList = ColorStateList.valueOf(color)

        // Estimation days until replacement
        val avgDaily = component.averageDailyKm ?: 0
        if (component.remainingKm <= 0) {
            holder.binding.tvEstimationDays.text = "⚠️ Segera ganti komponen!"
            holder.binding.tvEstimationDays.setTextColor(ContextCompat.getColor(context, R.color.danger))
            holder.binding.tvEstimationDays.visibility = View.VISIBLE
        } else if (avgDaily > 0) {
            val daysLeft = (component.remainingKm / avgDaily).toInt()
            holder.binding.tvEstimationDays.text = "⏱️ Perkiraan penggantian: $daysLeft hari lagi"
            val daysColor = when {
                daysLeft <= 7 -> ContextCompat.getColor(context, R.color.danger)
                daysLeft <= 30 -> ContextCompat.getColor(context, R.color.warning)
                else -> Color.parseColor("#757575")
            }
            holder.binding.tvEstimationDays.setTextColor(daysColor)
            holder.binding.tvEstimationDays.visibility = View.VISIBLE
        } else {
            holder.binding.tvEstimationDays.text = "⏱️ Perkiraan penggantian: Belum dapat dihitung"
            holder.binding.tvEstimationDays.setTextColor(Color.parseColor("#757575"))
            holder.binding.tvEstimationDays.visibility = View.VISIBLE
        }
    }

    override fun getItemCount() = components.size
}