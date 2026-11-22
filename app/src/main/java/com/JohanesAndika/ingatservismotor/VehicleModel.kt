package com.JohanesAndika.ingatservismotor

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

data class Vehicle(
    @get:Exclude var id: String = "",
    var userId: String = "",
    @get:PropertyName("brand_type")
    @set:PropertyName("brand_type")
    var brand_type: String = "",
    var year: String = "",
    @get:PropertyName("current_odometer")
    @set:PropertyName("current_odometer")
    var current_odometer: Long = 0,
    @get:PropertyName("last_service_km")
    @set:PropertyName("last_service_km")
    var last_service_km: Long = 0,
    @get:PropertyName("last_service_date")
    @set:PropertyName("last_service_date")
    var last_service_date: String = "",
    @get:PropertyName("average_daily_km")
    @set:PropertyName("average_daily_km")
    var average_daily_km: Int = 0,
    @get:PropertyName("weekly_distances")
    @set:PropertyName("weekly_distances")
    var weekly_distances: List<Int> = emptyList(),
    @get:PropertyName("last_update_odometer")
    @set:PropertyName("last_update_odometer")
    var last_update_odometer: Timestamp? = null
)

data class MaintenanceLog(
    var userId: String = "",
    var vehicleId: String = "",
    @get:PropertyName("component_type")
    @set:PropertyName("component_type")
    var component_type: String = "",
    @get:PropertyName("replaced_at_odometer")
    @set:PropertyName("replaced_at_odometer")
    var replaced_at_odometer: Long = 0,
    var notes: String = "",
    @get:PropertyName("replaced_date")
    @set:PropertyName("replaced_date")
    var replaced_date: Timestamp? = null
)