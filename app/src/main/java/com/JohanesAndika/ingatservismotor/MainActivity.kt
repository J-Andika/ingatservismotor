package com.JohanesAndika.ingatservismotor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.JohanesAndika.ingatservismotor.databinding.ActivityMainBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firestore settings are now configured in App.kt

        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        requestNotificationPermission()

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_add_vehicle -> {
                    startActivity(Intent(this, AddVehicleActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }

        binding.btnFindAhass.setOnClickListener {
            LocationUtils.openNearestAHASS(this)
        }

        binding.btnHistory.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            // Just open history activity, let history handle showing list
            // Pass the first vehicle ID if available or show default
            db.collection("users").document(currentUser.uid).collection("vehicles")
                .limit(1)
                .get()
                .addOnSuccessListener { snapshot ->
                    val firstVehicleId = snapshot.documents.firstOrNull()?.id
                    if (firstVehicleId != null) {
                        intent.putExtra("vehicleId", firstVehicleId)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "Tambahkan kendaraan terlebih dahulu untuk melihat riwayat!", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        scheduleSundayReminder()
        scheduleServiceReminder()
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNav.selectedItemId = R.id.nav_home
        loadVehicles()
    }

    private fun loadVehicles() {
        val currentUser = auth.currentUser ?: return
        val uid = currentUser.uid
        android.util.Log.d("MainActivity", "Loading vehicles for UID: $uid")
        db.collection("users").document(uid).collection("vehicles")
            .get()
            .addOnSuccessListener { snapshot ->
                android.util.Log.d("MainActivity", "Loaded ${snapshot.documents.size} vehicle documents")
                val vehicles = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Vehicle::class.java)?.apply { id = doc.id }
                }
                
                if (vehicles.isEmpty()) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.rvVehicles.visibility = View.GONE
                } else {
                    binding.tvEmptyState.visibility = View.GONE
                    binding.rvVehicles.visibility = View.VISIBLE
                }

                binding.rvVehicles.adapter = VehicleAdapter(vehicles, { vehicle ->
                    val dialog = UpdateOdometerDialog(
                        vehicle.id,
                        vehicle.current_odometer,
                        vehicle.last_update_odometer ?: Timestamp.now(),
                        vehicle.weekly_distances
                    ) {
                        loadVehicles()
                    }
                    dialog.show(supportFragmentManager, "update_odometer")
                }, { vehicleId ->
                    val intent = Intent(this, VehicleDetailActivity::class.java)
                    intent.putExtra("vehicleId", vehicleId)
                    startActivity(intent)
                })
            }
            .addOnFailureListener { e ->
                android.util.Log.e("MainActivity", "FAILED to load vehicles: ${e.message}", e)
                Toast.makeText(this, "Gagal memuat kendaraan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }

    private fun scheduleSundayReminder() {
        val now = Calendar.getInstance()
        val daysUntilSunday = (Calendar.SUNDAY - now.get(Calendar.DAY_OF_WEEK) + 7) % 7
        val initialDelay = if (daysUntilSunday == 0) 7L else daysUntilSunday.toLong()

        val workRequest = PeriodicWorkRequestBuilder<SundayWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.DAYS)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "SundayNotification",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun scheduleServiceReminder() {
        val workRequest = PeriodicWorkRequestBuilder<ServiceReminderWorker>(1, TimeUnit.DAYS)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "ServiceReminderNotification",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}