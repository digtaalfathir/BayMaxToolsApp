package com.example.blescannerapp

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("ble_settings", Context.MODE_PRIVATE)

        val switchWhatsapp = findViewById<SwitchMaterial>(R.id.switchWhatsapp)
        val switchMaps = findViewById<SwitchMaterial>(R.id.switchMaps)

        // Load nilai dari SharedPreferences
        switchWhatsapp.isChecked = prefs.getBoolean("send_whatsapp", true)
        switchMaps.isChecked = prefs.getBoolean("send_maps", true)

        // Simpan nilai saat berubah
        switchWhatsapp.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("send_whatsapp", isChecked).apply()
        }
        switchMaps.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("send_maps", isChecked).apply()
        }
    }
}
