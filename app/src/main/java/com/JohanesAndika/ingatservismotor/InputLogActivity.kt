package com.JohanesAndika.ingatservismotor

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.JohanesAndika.ingatservismotor.databinding.ActivityInputLogBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class InputLogActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInputLogBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var vehicleId: String = ""
    private var currentOdometer: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInputLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vehicleId = intent.getStringExtra("vehicleId") ?: ""
        currentOdometer = intent.getLongExtra("currentOdometer", 0L)

        if (vehicleId.isEmpty()) {
            Toast.makeText(this, "ID Kendaraan tidak valid!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val components = arrayOf("Oli", "Kampas Rem", "Ban")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, components)
        binding.spComponentType.adapter = adapter

        // Set current odometer as default hint or value
        binding.etReplacedOdometer.setText(currentOdometer.toString())

        binding.btnSaveLog.setOnClickListener {
            val componentType = binding.spComponentType.selectedItem.toString()
            val odometer = binding.etReplacedOdometer.text.toString().toLongOrNull()
            val notes = binding.etNotes.text.toString().trim()
            val uid = auth.currentUser?.uid

            if (odometer == null || odometer <= 0) {
                Toast.makeText(this, "Odometer harus berupa angka positif!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (uid != null) {
                val log = hashMapOf(
                    "userId" to uid,
                    "vehicleId" to vehicleId,
                    "component_type" to componentType,
                    "replaced_at_odometer" to odometer,
                    "notes" to notes,
                    "replaced_date" to Timestamp.now()
                )

                // Save to sub-collection
                db.collection("users").document(uid).collection("vehicles").document(vehicleId)
                    .collection("maintenance_logs").add(log)
                    .addOnSuccessListener {
                        // Update parent vehicle fields if necessary
                        val updateMap = mutableMapOf<String, Any>()
                        if (odometer > currentOdometer) {
                            updateMap["current_odometer"] = odometer
                        }
                        
                        if (componentType == "Oli") {
                            val todayStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                            updateMap["last_service_km"] = odometer
                            updateMap["last_service_date"] = todayStr
                        }

                        if (updateMap.isNotEmpty()) {
                            db.collection("users").document(uid).collection("vehicles").document(vehicleId)
                                .update(updateMap)
                                .addOnCompleteListener {
                                    Toast.makeText(this, "Log servis berhasil disimpan!", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                        } else {
                            Toast.makeText(this, "Log servis berhasil disimpan!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Gagal menyimpan log: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
}