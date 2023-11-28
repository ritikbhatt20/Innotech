package com.example.innotech

import retrofit2.Call
import retrofit2.http.GET

interface ApiInterface {

    @GET("ambulances")
    fun getAmbulancesData() : Call<List<MyDataItem>>
}