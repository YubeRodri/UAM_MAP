package com.example.uammap.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.example.uammap.network.ApiService
import com.example.uammap.network.UserLocationRequest
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object LocationManager {
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private val apiService by lazy { ApiService.create() }

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location = locationResult.lastLocation ?: return
            
            // Reducimos filtros al mínimo para máxima velocidad de respuesta
            if (location.accuracy > 30f) return 

            _currentLocation.value = location
            
            // Enviar ubicación al servidor (API REST)
            scope.launch {
                try {
                    apiService.updateLocation(
                        UserLocationRequest(
                            userId = "user_123",
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                    )
                } catch (e: Exception) {}
            }
        }
    }

    fun init(context: Context) {
        if (fusedLocationClient == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 100) // 100ms para respuesta instantánea total
            .setMinUpdateIntervalMillis(50) 
            .setMinUpdateDistanceMeters(0f)  // Sin umbral para que sea ultra sensible
            .setWaitForAccurateLocation(false)
            .build()

        fusedLocationClient?.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun stopLocationUpdates() {
        fusedLocationClient?.removeLocationUpdates(locationCallback)
    }
}
