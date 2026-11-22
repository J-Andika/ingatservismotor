package com.JohanesAndika.ingatservismotor

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.JohanesAndika.ingatservismotor.databinding.ActivityAddVehicleBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AddVehicleActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddVehicleBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddVehicleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup dropdown tipe motor
        val motorTypes = resources.getStringArray(R.array.motor_types)
        val dropdownAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, motorTypes)
        binding.actvBrandType.setAdapter(dropdownAdapter)

        // Buka dropdown saat field disentuh
        binding.actvBrandType.setOnClickListener {
            binding.actvBrandType.showDropDown()
        }

        binding.btnSave.setOnClickListener {
            val brandType = binding.actvBrandType.text.toString().trim()
            val year = binding.etYear.text.toString().trim()
            val odometer = binding.etOdometer.text.toString().toLongOrNull()
            val uid = auth.currentUser?.uid

            if (brandType.isEmpty()) {
                binding.tilBrandType.error = "Silakan pilih merk & tipe motor!"
                return@setOnClickListener
            } else {
                binding.tilBrandType.error = null
            }

            if (year.isEmpty()) {
                Toast.makeText(this, "Tahun tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (odometer == null || odometer < 0) {
                Toast.makeText(this, "Odometer harus angka positif!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (uid != null) {
                Log.d("AddVehicle", "User UID: $uid")
                val todayStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                val vehicle = hashMapOf(
                    "userId" to uid,
                    "brand_type" to brandType,
                    "year" to year,
                    "current_odometer" to odometer,
                    "last_service_km" to odometer,
                    "last_service_date" to todayStr,
                    "average_daily_km" to 0,
                    "weekly_distances" to emptyList<Int>(),
                    "last_update_odometer" to Timestamp.now()
                )

                Log.d("AddVehicle", "Writing vehicle data to Firestore: $vehicle")
                db.collection("users").document(uid).collection("vehicles").add(vehicle)
                    .addOnSuccessListener { docRef ->
                        Log.d("AddVehicle", "Vehicle added successfully with ID: ${docRef.id}")
                        Toast.makeText(this, "Kendaraan berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Log.e("AddVehicle", "FAILED to add vehicle: ${e.message}", e)
                        Toast.makeText(this, "Gagal menambahkan: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Log.e("AddVehicle", "UID is null - user not authenticated!")
                Toast.makeText(this, "Anda belum login!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}