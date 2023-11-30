package com.example.innotech

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.math.*

class WelcomePageActivity : AppCompatActivity() {
    private val userLatitude = 28.7531864 // Replace with user's latitude
    private val userLongitude = 77.4944861 // Replace with user's longitude

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome_page)

        val btnAmbulance = findViewById<AppCompatButton>(R.id.btn_ambulance)
        btnAmbulance.setOnClickListener {

            val retrofitBuilder = Retrofit.Builder()
                .baseUrl("https://ambulance-drivers-api-using-springboot.onrender.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiInterface::class.java)

            val retrofitData = retrofitBuilder.getAmbulancesData()

            retrofitData.enqueue(object : Callback<List<MyDataItem>> {
                override fun onResponse(call: Call<List<MyDataItem>>, response: Response<List<MyDataItem>>) {
                    if (response.isSuccessful) {
                        val ambulanceList = response.body()
                        var nearestAmbulance: MyDataItem? = null
                        var minDistance = Double.MAX_VALUE

                        ambulanceList?.forEach { ambulance ->
                            val ambulanceLatitude = ambulance.latitude.toDouble()
                            val ambulanceLongitude = ambulance.longitude.toDouble()

                            val distance = calculateDistance(
                                userLatitude,
                                userLongitude,
                                ambulanceLatitude,
                                ambulanceLongitude
                            )

                            if (distance < minDistance) {
                                // Update the nearest ambulance
                                minDistance = distance
                                nearestAmbulance = ambulance
                            }
                        }

                        if (nearestAmbulance != null) {
                            // Add a marker to the map for the nearest ambulance
                            val intent = Intent(this@WelcomePageActivity, MapsActivity::class.java)
                            intent.putExtra("latitude", nearestAmbulance!!.latitude.toDouble())
                            intent.putExtra("longitude", nearestAmbulance!!.longitude.toDouble())
                            startActivity(intent)
                        } else {
                            Toast.makeText(this@WelcomePageActivity, "No ambulances found", Toast.LENGTH_SHORT).show()
                        }

                    } else {
                        Toast.makeText(this@WelcomePageActivity, "Failed to fetch data", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<List<MyDataItem>>, t: Throwable) {
                    Toast.makeText(this@WelcomePageActivity, "Error: " + t.message, Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371 // Radius of the Earth in kilometers
        val latDistance = Math.toRadians(lat2 - lat1)
        val lonDistance = Math.toRadians(lon2 - lon1)
        val a = sin(latDistance / 2) * sin(latDistance / 2) +
                (cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                        sin(lonDistance / 2) * sin(lonDistance / 2))
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c // Distance in kilometers
    }
}

