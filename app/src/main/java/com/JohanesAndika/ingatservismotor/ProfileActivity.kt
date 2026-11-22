package com.JohanesAndika.ingatservismotor

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.JohanesAndika.ingatservismotor.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = auth.currentUser
        if (user != null) {
            binding.tvEmail.text = user.email
            binding.tvName.text = user.displayName ?: "Pengguna"

            // Fetch latest from Firestore for reliability
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val dbName = doc.getString("name")
                        if (!dbName.isNullOrEmpty()) {
                            binding.tvName.text = dbName
                        }
                    }
                }
                .addOnFailureListener {
                    // Fail silently, use Auth displayName
                }
        }

        binding.btnCatalog.setOnClickListener {
            startActivity(Intent(this, CatalogActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // ── Tombol Uji Notifikasi ──

        // 1. Uji notif perbarui odometer
        binding.btnTestNotifOdometer.setOnClickListener {
            showNotification(
                id = 9001,
                title = "Perbarui Odometer!",
                message = "Sudah lebih dari 7 hari odometer belum diperbarui. Perbarui sekarang agar estimasi servis tetap akurat."
            )
            Toast.makeText(this, "Notifikasi odometer dikirim", Toast.LENGTH_SHORT).show()
        }

        // 2. Uji notif waktu servis
        binding.btnTestNotifServis.setOnClickListener {
            val uid = auth.currentUser?.uid ?: return@setOnClickListener
            db.collection("users").document(uid).collection("vehicles")
                .get()
                .addOnSuccessListener { snapshot ->
                    val vehicle = snapshot.documents.firstOrNull()?.toObject(Vehicle::class.java)
                    val name = vehicle?.brand_type ?: "Kendaraan Anda"
                    val odometer = vehicle?.current_odometer ?: 0L
                    showNotification(
                        id = 9002,
                        title = "Waktunya Servis Berkala!",
                        message = "Motor $name mendekati batas servis berkala (odometer: $odometer km). Segera lakukan servis!"
                    )
                    Toast.makeText(this, "Notifikasi servis dikirim", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    // Fallback jika tidak ada kendaraan
                    showNotification(
                        id = 9002,
                        title = "Waktunya Servis Berkala!",
                        message = "Kendaraan Anda mendekati batas servis berkala. Segera lakukan servis!"
                    )
                    Toast.makeText(this, "Notifikasi servis dikirim", Toast.LENGTH_SHORT).show()
                }
        }

        // 3. Uji notif umur komponen < 10%
        binding.btnTestNotifKomponen.setOnClickListener {
            val uid = auth.currentUser?.uid ?: return@setOnClickListener
            db.collection("users").document(uid).collection("vehicles")
                .get()
                .addOnSuccessListener { vehicleSnapshot ->
                    val vehicleDoc = vehicleSnapshot.documents.firstOrNull()
                    if (vehicleDoc == null) {
                        showNotification(
                            id = 9003,
                            title = "Peringatan Komponen!",
                            message = "Terdapat komponen kendaraan yang umurnya di bawah 10%. Segera periksa dan ganti!"
                        )
                        Toast.makeText(this, "Notifikasi komponen dikirim", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }
                    val vehicle = vehicleDoc.toObject(Vehicle::class.java)?.apply { id = vehicleDoc.id }
                        ?: return@addOnSuccessListener

                    db.collection("users").document(uid)
                        .collection("vehicles").document(vehicle.id)
                        .collection("maintenance_logs").get()
                        .addOnSuccessListener { logsSnapshot ->
                            val lifespanMap = mapOf(
                                "Oli" to 2000L,
                                "Oli Gardan" to 8000L,
                                "Busi" to 8000L,
                                "Filter Udara" to 16000L,
                                "V-Belt" to 24000L,
                                "Rantai" to 24000L
                            )
                            val latestLogs = logsSnapshot.documents.groupBy { it.getString("component_type") }
                            var found = false
                            latestLogs.forEach { (type, typeLogs) ->
                                val logType = type ?: return@forEach
                                val lifespan = lifespanMap[logType] ?: 5000L
                                val lastLog = typeLogs.sortedByDescending { it.getTimestamp("replaced_date") }.firstOrNull()
                                val replacedAt = lastLog?.getLong("replaced_at_odometer") ?: 0L
                                val used = vehicle.current_odometer - replacedAt
                                val remaining = lifespan - used
                                val percentage = ((remaining.toFloat() / lifespan) * 100).toInt().coerceIn(0, 100)
                                if (percentage < 10) {
                                    showNotification(
                                        id = 9003 + logType.hashCode(),
                                        title = "Peringatan Komponen!",
                                        message = "Komponen $logType pada ${vehicle.brand_type} umurnya tinggal $percentage%. Segera ganti!"
                                    )
                                    found = true
                                }
                            }
                            if (!found) {
                                // Kirim notif demo meskipun tidak ada yang < 10%
                                showNotification(
                                    id = 9003,
                                    title = "Peringatan Komponen!",
                                    message = "Contoh: Komponen Oli pada ${vehicle.brand_type} umurnya tinggal 8%. Segera ganti!"
                                )
                            }
                            Toast.makeText(this, "Notifikasi komponen dikirim", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            showNotification(
                                id = 9003,
                                title = "Peringatan Komponen!",
                                message = "Terdapat komponen kendaraan yang umurnya di bawah 10%. Segera periksa dan ganti!"
                            )
                            Toast.makeText(this, "Notifikasi komponen dikirim", Toast.LENGTH_SHORT).show()
                        }
                }
        }
    }

    private fun showNotification(id: Int, title: String, message: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "service_reminder"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Pengingat Servis", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .build()

        manager.notify(id, notification)
    }
}