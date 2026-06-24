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
            val location = locationResult.lastLocation
            _currentLocation.value = location
            
            // Opcional: Enviar ubicación al servidor (API REST)
            location?.let {
                scope.launch {
                    try {
                        apiService.updateLocation(
                            UserLocationRequest(
                                userId = "user_123", // ID de usuario dinámico
                                latitude = it.latitude,
                                longitude = it.longitude
                            )
                        )
                    } catch (e: Exception) {
                        // Silently fail or log
                    }
                }
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
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
            .setMinUpdateIntervalMillis(2000)
            .setMinUpdateDistanceMeters(2f)
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
