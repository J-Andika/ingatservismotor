package com.JohanesAndika.ingatservismotor

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.JohanesAndika.ingatservismotor.databinding.ItemVehicleBinding
import java.text.SimpleDateFormat
import java.util.*

class VehicleAdapter(
    private val vehicles: List<Vehicle>,
    private val onUpdateClick: (Vehicle) -> Unit,
    private val onDetailClick: (String) -> Unit
) : RecyclerView.Adapter<VehicleAdapter.VehicleViewHolder>() {

    class VehicleViewHolder(val binding: ItemVehicleBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleViewHolder {
        val binding = ItemVehicleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VehicleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VehicleViewHolder, position: Int) {
        val vehicle = vehicles[position]
        holder.binding.tvBrandType.text = vehicle.brand_type.uppercase()
        holder.binding.tvOdometer.text = "Odometer: ${vehicle.current_odometer} km"
        
        // SMA Calculation
        val averageDaily = if (vehicle.weekly_distances.isEmpty()) {
            vehicle.average_daily_km // Fallback
        } else {
            CalculationUtils.calculateDailyAverageSMA(vehicle.weekly_distances, 4)
        }

        if (vehicle.weekly_distances.isEmpty() && averageDaily == 0) {
            holder.binding.tvSmaResult.text = "📊 Rata-rata: Belum ada data"
            holder.binding.tvEstimationDays.text = "⏱️ Estimasi servis: Tidak dapat dihitung"
            holder.binding.tvEstimationDate.text = "📅 Perkiraan: -"
            holder.binding.tvEstimationDays.setTextColor(Color.parseColor("#757575"))
        } else {
            holder.binding.tvSmaResult.text = "📊 Rata-rata: $averageDaily km/hari (SMA)"
            
            val serviceInterval = 3000L
            val diffService = vehicle.current_odometer - vehicle.last_service_km
            val remainingKm = serviceInterval - diffService

            if (remainingKm <= 0) {
                holder.binding.tvEstimationDays.text = "⚠️ Sudah waktunya servis!"
                holder.binding.tvEstimationDays.setTextColor(Color.RED)
                holder.binding.tvEstimationDate.text = "📅 Segera bawa ke bengkel!"
            } else if (averageDaily > 0) {
                val daysUntilService = (remainingKm / averageDaily).toInt()
                holder.binding.tvEstimationDays.text = "⏱️ Estimasi servis: $daysUntilService hari lagi"
                holder.binding.tvEstimationDays.setTextColor(Color.parseColor("#757575"))
                
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, daysUntilService)
                val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
                holder.binding.tvEstimationDate.text = "📅 Perkiraan: ${dateFormat.format(calendar.time)}"
            } else {
                holder.binding.tvEstimationDays.text = "⏱️ Estimasi servis: Tidak dapat dihitung"
                holder.binding.tvEstimationDate.text = "📅 Perkiraan: -"
                holder.binding.tvEstimationDays.setTextColor(Color.parseColor("#757575"))
            }
        }

        holder.binding.btnUpdateOdometer.setOnClickListener { onUpdateClick(vehicle) }

        // Make card clickable
        holder.binding.root.setOnClickListener {
            onDetailClick(vehicle.id)
        }

        holder.binding.btnMoreOptions.setOnClickListener { view ->
            val popup = PopupMenu(view.context, view)
            popup.menu.add("Lihat Detail")
            popup.setOnMenuItemClickListener { item ->
                if (item.title == "Lihat Detail") {
                    onDetailClick(vehicle.id)
                    true
                } else {
                    false
                }
            }
            popup.show()
        }
    }

    override fun getItemCount(): Int = vehicles.size
}