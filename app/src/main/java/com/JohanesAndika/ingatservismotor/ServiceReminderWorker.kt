package com.JohanesAndika.ingatservismotor

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ServiceReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser ?: run {
            android.util.Log.w("ServiceReminderWorker", "No user logged in, skipping work")
            return Result.success()
        }
        val uid = currentUser.uid

        // Ensure auth state and token are fresh before Firestore calls
        try {
            currentUser.reload().await()
            currentUser.getIdToken(true).await()
            android.util.Log.d("ServiceReminderWorker", "Auth token refreshed for UID: $uid")
        } catch (e: Exception) {
            android.util.Log.e("ServiceReminderWorker", "Auth refresh failed", e)
            return Result.retry()
        }

        val db = FirebaseFirestore.getInstance()

        try {
            android.util.Log.d("ServiceReminderWorker", "Fetching vehicles for UID: $uid")
            val vehiclesSnapshot = db.collection("users").document(uid).collection("vehicles").get().await()
            val vehicles = vehiclesSnapshot.documents.mapNotNull { doc ->
                doc.toObject(Vehicle::class.java)?.apply { id = doc.id }
            }
            
            android.util.Log.d("ServiceReminderWorker", "Found ${vehicles.size} vehicles")

            for (vehicle in vehicles) {
                // 1. Check general service interval (3000 km)
                val serviceInterval = 3000L
                val diffService = vehicle.current_odometer - vehicle.last_service_km
                
                val daysUntilService = if (vehicle.average_daily_km > 0) {
                    ((serviceInterval - diffService) / vehicle.average_daily_km).toInt()
                } else {
                    Int.MAX_VALUE
                }
                
                if (daysUntilService <= 7 || diffService >= (serviceInterval - 300)) {
                    showNotification(
                        vehicle.id.hashCode() + 1,
                        "Waktunya Servis Berkala!",
                        "Motor ${vehicle.brand_type} mendekati batas servis berkala (odometer: ${vehicle.current_odometer} km)."
                    )
                }

                // 2. Check individual components health in maintenance_logs sub-collection
                android.util.Log.d("ServiceReminderWorker", "Fetching logs for vehicle: ${vehicle.id}")
                val logsSnapshot = db.collection("users").document(uid)
                    .collection("vehicles").document(vehicle.id)
                    .collection("maintenance_logs").get().await()
                
                val logs = logsSnapshot.documents
                val latestLogs = logs.groupBy { it.getString("component_type") }

                latestLogs.forEach { (type, typeLogs) ->
                    val logType = type ?: "Lainnya"
                    val lastLog = typeLogs.sortedByDescending { it.getTimestamp("replaced_date") }.firstOrNull()
                    val replacedAt = lastLog?.getLong("replaced_at_odometer") ?: 0L
                    
                    val lifespan = when (logType) {
                        "Oli" -> 2000L
                        "Oli Gardan" -> 8000L
                        "Busi" -> 8000L
                        "Filter Udara" -> 16000L
                        "V-Belt" -> 24000L
                        "Rantai" -> 24000L
                        else -> 5000L
                    }

                    val used = vehicle.current_odometer - replacedAt
                    
                    val daysUntilReplace = if (vehicle.average_daily_km > 0) {
                        ((lifespan - used) / vehicle.average_daily_km).toInt()
                    } else {
                        Int.MAX_VALUE
                    }
                    
                    if (daysUntilReplace <= 7 || (lifespan - used <= 300)) {
                        showNotification(
                            vehicle.id.hashCode() + logType.hashCode(),
                            "Peringatan Komponen!",
                            "Komponen ${logType} pada ${vehicle.brand_type} perlu segera diganti."
                        )
                    }
                }
            }
            return Result.success()
        } catch (e: Exception) {
            android.util.Log.e("ServiceReminderWorker", "Error running worker", e)
            return Result.retry()
        }
    }

    private fun showNotification(id: Int, title: String, message: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "service_reminder"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Pengingat Servis", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }
        
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .build()
        manager.notify(id, notification)
    }
}