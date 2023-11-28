package com.example.innotech

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class LocationActivity : AppCompatActivity() {

    private lateinit var longitudeTxt: TextView
    private lateinit var latitudeTxt: TextView

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        latitudeTxt = findViewById(R.id.lat)
        longitudeTxt = findViewById(R.id.lon)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            requestLocationUpdates()
        }
    }

    private fun requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request location permissions
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            // Permissions are already granted, proceed with getting location
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    // Got last known location. Use it if available.
                    if (location != null) {
                        val latitude = location.latitude
                        val longitude = location.longitude

                        latitudeTxt.text = latitude.toString()
                        longitudeTxt.text = longitude.toString()

                        // Get nearest ambulance data from intent
                        val nearestAmbulanceLatitude = intent.getDoubleExtra("latitude", 0.0)
                        val nearestAmbulanceLongitude = intent.getDoubleExtra("longitude", 0.0)

                        // Display nearest ambulance data
                        // You might want to perform additional actions with this data
                        // (e.g., show it on the map, calculate distance, etc.)
                        Toast.makeText(this,"Nearest Ambulance: Lat $nearestAmbulanceLatitude, Lon $nearestAmbulanceLongitude", Toast.LENGTH_LONG).show()
                    } else {
                        // Handle the case where location is null
                        Toast.makeText(this, "Location is null", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocationUpdates()
            } else {
                // Handle the case where the user denied the location permission
                Toast.makeText(this, "User denied Permission", Toast.LENGTH_SHORT).show()
            }
        }
    }
}


