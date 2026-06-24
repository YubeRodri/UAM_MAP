package com.example.uammap.network

data class UserLocationRequest(
    val userId: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis()
)
