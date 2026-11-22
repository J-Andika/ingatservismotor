package com.JohanesAndika.ingatservismotor

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.JohanesAndika.ingatservismotor.databinding.ActivityHistoryBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val vehicleId = intent.getStringExtra("vehicleId") ?: ""
        if (vehicleId.isEmpty()) {
            Toast.makeText(this, "ID Kendaraan tidak valid!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.rvHistory.layoutManager = LinearLayoutManager(this)

        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).collection("vehicles").document(vehicleId)
                .collection("maintenance_logs")
                .orderBy("replaced_date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { snapshot ->
                    val logs = snapshot.toObjects(MaintenanceLog::class.java)
                    binding.rvHistory.adapter = HistoryAdapter(logs)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Gagal memuat riwayat: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}