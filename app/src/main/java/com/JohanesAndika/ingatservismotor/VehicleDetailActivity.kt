package com.JohanesAndika.ingatservismotor

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.JohanesAndika.ingatservismotor.databinding.ActivityVehicleDetailBinding
import com.JohanesAndika.ingatservismotor.databinding.DialogAddTrackedComponentBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class VehicleDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVehicleDetailBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var currentOdometer: Long = 0
    private var vehicleId: String = ""
    private var currentBrandType: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVehicleDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vehicleId = intent.getStringExtra("vehicleId") ?: ""
        if (vehicleId.isEmpty()) {
            Toast.makeText(this, "ID Kendaraan tidak valid!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.btnInputLog.setOnClickListener {
            val intent = Intent(this, InputLogActivity::class.java)
            intent.putExtra("vehicleId", vehicleId)
            intent.putExtra("currentOdometer", currentOdometer)
            startActivity(intent)
        }

        binding.btnHistory.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            intent.putExtra("vehicleId", vehicleId)
            startActivity(intent)
        }

        binding.btnDeleteVehicle.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        binding.fabAddComponent.setOnClickListener {
            showAddComponentDialog(vehicleId)
        }
    }

    override fun onResume() {
        super.onResume()
        loadVehicleDetail()
    }

    private fun loadVehicleDetail() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("vehicles").document(vehicleId)
            .get()
            .addOnSuccessListener { doc ->
                val vehicle = doc.toObject(Vehicle::class.java)
                if (vehicle != null) {
                    currentOdometer = vehicle.current_odometer
                    currentBrandType = vehicle.brand_type
                    binding.tvVehicleHeader.text = vehicle.brand_type.uppercase()
                    binding.tvOdometerInfo.text = "Odometer: ${vehicle.current_odometer} km"
                    loadComponentHealth(uid, vehicleId, vehicle.average_daily_km)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat detail kendaraan: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadComponentHealth(uid: String, vehicleId: String, averageDailyKm: Int) {
        db.collection("users").document(uid).collection("vehicles").document(vehicleId)
            .collection("maintenance_logs")
            .get()
            .addOnSuccessListener { snapshot ->
                val logs = snapshot.documents
                val healthList = mutableListOf<ComponentHealth>()
                
                // Group by type and sort manually to avoid index requirement for now
                val latestLogs = logs.groupBy { it.getString("component_type") }

                latestLogs.forEach { (type, typeLogs) ->
                    // Sort descending by replaced_date
                    val lastLog = typeLogs.sortedByDescending { it.getTimestamp("replaced_date") }.firstOrNull()
                    val replacedAt = (lastLog?.getLong("replaced_at_odometer") ?: 0L).toInt()
                    
                    val lifespan = when (type) {
                        "Oli" -> 2000
                        "Oli Gardan" -> 8000
                        "Busi" -> 8000
                        "Filter Udara" -> 16000
                        "V-Belt" -> 24000
                        "Rantai" -> 24000
                        else -> 5000
                    }

                    val used = currentOdometer - replacedAt
                    var percentage = ((lifespan - used).toFloat() / lifespan * 100).toInt()
                    if (percentage < 0) percentage = 0
                    if (percentage > 100) percentage = 100

                    val remainingKm = (lifespan - used).coerceAtLeast(0)

                    healthList.add(ComponentHealth(type ?: "Unknown", percentage, averageDailyKm, remainingKm))
                }
                binding.rvComponentHealth.adapter = ComponentHealthAdapter(healthList)
            }
            .addOnFailureListener { e ->
                android.util.Log.e("VehicleDetail", "Error loading logs", e)
            }
    }

    private fun getMotorCategory(brandType: String): String {
        val matic = listOf(
            "Honda Beat", "Honda Genio", "Honda Scoopy", "Honda Vario",
            "Honda PCX", "Honda ADV", "Honda Freego"
        )
        val bebek = listOf(
            "Honda Revo", "Honda Blade", "Honda Supra X 125", "Honda Supra X 125 FI"
        )
        return when {
            matic.any { brandType.startsWith(it) } -> "matic"
            bebek.any { brandType.startsWith(it) } -> "bebek"
            else -> "kopling"
        }
    }

    private fun showAddComponentDialog(vehicleId: String) {
        val uid = auth.currentUser?.uid ?: return
        val dialogBinding = DialogAddTrackedComponentBinding.inflate(layoutInflater)

        val category = getMotorCategory(currentBrandType)
        val componentArrayId = when (category) {
            "matic" -> R.array.component_types_matic
            "bebek" -> R.array.component_types_bebek
            else -> R.array.component_types_kopling
        }
        val adapter = ArrayAdapter.createFromResource(this, componentArrayId, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerComponentType.adapter = adapter

        // Set default text for odometer
        dialogBinding.etReplacedAtOdometer.setText(currentOdometer.toString())

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancelComponent.setOnClickListener { dialog.dismiss() }

        dialogBinding.btnSaveComponent.setOnClickListener {
            val type = dialogBinding.spinnerComponentType.selectedItem.toString()
            val odo = dialogBinding.etReplacedAtOdometer.text.toString().toLongOrNull() ?: currentOdometer
            
            if (odo > currentOdometer) {
                Toast.makeText(this, "Odometer penggantian tidak boleh melebihi odometer saat ini!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val log = hashMapOf(
                "userId" to uid,
                "vehicleId" to vehicleId,
                "component_type" to type,
                "replaced_at_odometer" to odo,
                "notes" to "Registrasi komponen awal",
                "replaced_date" to com.google.firebase.Timestamp.now()
            )

            db.collection("users").document(uid).collection("vehicles").document(vehicleId)
                .collection("maintenance_logs").add(log)
                .addOnSuccessListener {
                    dialog.dismiss()
                    // Refetch vehicle to get the actual averageDailyKm or just reload activity
                    loadVehicleDetail()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal menyimpan komponen: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
        dialog.show()
    }

    private fun showDeleteConfirmationDialog() {
        val uid = auth.currentUser?.uid ?: return
        AlertDialog.Builder(this)
            .setTitle("Hapus Kendaraan")
            .setMessage("Apakah Anda yakin ingin menghapus kendaraan ini? Seluruh data riwayat servis juga akan dihapus permanen.")
            .setPositiveButton("Hapus") { _, _ ->
                // Delete maintenance logs sub-collection first
                val vehicleRef = db.collection("users").document(uid).collection("vehicles").document(vehicleId)
                
                vehicleRef.collection("maintenance_logs").get()
                    .addOnSuccessListener { snapshot ->
                        val batch = db.batch()
                        for (doc in snapshot.documents) {
                            batch.delete(doc.reference)
                        }
                        
                        // Commit delete sub-collection batch
                        batch.commit().addOnCompleteListener {
                            // Delete vehicle document
                            vehicleRef.delete()
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Kendaraan berhasil dihapus!", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Gagal menghapus kendaraan: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        // Fallback delete vehicle directly if subcollection fetch fails
                        vehicleRef.delete().addOnSuccessListener {
                            finish()
                        }
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}