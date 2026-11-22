package com.JohanesAndika.ingatservismotor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.JohanesAndika.ingatservismotor.databinding.DialogUpdateOdometerBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UpdateOdometerDialog(
    private val vehicleId: String,
    private val currentOdometer: Long,
    private val lastUpdate: Timestamp,
    private val weeklyDistances: List<Int>,
    private val onUpdated: () -> Unit
) : DialogFragment() {

    private lateinit var binding: DialogUpdateOdometerBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DialogUpdateOdometerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Show current value as placeholder/text
        binding.etNewOdometer.setText(currentOdometer.toString())

        binding.btnCancel.setOnClickListener { dismiss() }

        binding.btnUpdate.setOnClickListener {
            val newOdometer = binding.etNewOdometer.text.toString().toLongOrNull()
            
            if (newOdometer == null || newOdometer <= 0) {
                Toast.makeText(context, "Odometer harus berupa angka positif!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newOdometer < currentOdometer) {
                Toast.makeText(context, "Odometer baru tidak boleh lebih kecil dari odometer saat ini (${currentOdometer} km)!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val uid = auth.currentUser?.uid
            if (uid != null) {
                val diffOdo = (newOdometer - currentOdometer).toInt().coerceAtLeast(0)
                val newDistances = weeklyDistances + diffOdo
                
                // Calculate SMA over the last 4 periods (roughly a month)
                val avgDaily = CalculationUtils.calculateDailyAverageSMA(newDistances, 4)
                
                db.collection("users").document(uid).collection("vehicles").document(vehicleId)
                    .update(
                        mapOf(
                            "current_odometer" to newOdometer,
                            "average_daily_km" to avgDaily,
                            "weekly_distances" to newDistances,
                            "last_update_odometer" to Timestamp.now()
                        )
                    )
                    .addOnSuccessListener {
                        Toast.makeText(context, "Odometer berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                        onUpdated()
                        dismiss()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Gagal memperbarui: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
}