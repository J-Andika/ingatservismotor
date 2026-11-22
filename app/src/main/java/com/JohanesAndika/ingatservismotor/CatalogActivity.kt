package com.JohanesAndika.ingatservismotor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.JohanesAndika.ingatservismotor.databinding.ActivityCatalogBinding

class CatalogActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCatalogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCatalogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvCatalog.adapter = PartAdapter()
    }
}