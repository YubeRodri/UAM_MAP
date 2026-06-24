package com.example.uammap.network

import com.example.uammap.model.GeoJson
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface ApiService {
    @GET("campus.geojson") // Reemplazar con el endpoint real
    suspend fun getCampusData(): GeoJson

    @retrofit2.http.POST("location")
    suspend fun updateLocation(@retrofit2.http.Body location: UserLocationRequest): retrofit2.Response<Unit>

    companion object {
        private const val BASE_URL = "http://10.0.27.190:8080/api/" // Tu IP configurada

        fun create(context: android.content.Context? = null): ApiService {
            val clientBuilder = OkHttpClient.Builder()
            
            // Comentado para usar el servidor REAL de Spring Boot
            /*
            if (context != null) {
                clientBuilder.addInterceptor(MockInterceptor(context))
            }
            */

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(clientBuilder.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}
