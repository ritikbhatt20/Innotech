package com.example.innotech

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.material.button.MaterialButton

class PharmacyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pharmacy)

        val getBtn = findViewById<MaterialButton>(R.id.getBtn)
        val skipBtn = findViewById<MaterialButton>(R.id.skipBtn)

        getBtn.setOnClickListener{
            val intent = Intent(this, DoctorListActivity::class.java)
            startActivity(intent)
        }
        skipBtn.setOnClickListener{
            val intent = Intent(this, PharmacyActivity::class.java)
            startActivity(intent)
        }
    }
}