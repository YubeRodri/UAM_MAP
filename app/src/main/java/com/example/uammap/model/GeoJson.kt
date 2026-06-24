package com.example.uammap.model

data class GeoJson(
    val type: String = "FeatureCollection",
    val features: List<Feature>
)

data class Feature(
    val type: String = "Feature",
    val properties: Map<String, String>?,
    val geometry: Geometry,
    val id: Int? = null
)

data class Geometry(
    val type: String,
    val coordinates: List<Any>? = null
)