package com.JohanesAndika.ingatservismotor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object LocationUtils {
    fun openNearestAHASS(context: Context) {
        val uri = Uri.parse("geo:0,0?q=Bengkel+AHASS+terdekat")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback: Open in web browser
            val webUri = Uri.parse("https://www.google.com/maps/search/Bengkel+AHASS+terdekat")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri)
            try {
                context.startActivity(webIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "Aplikasi peta tidak ditemukan!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}