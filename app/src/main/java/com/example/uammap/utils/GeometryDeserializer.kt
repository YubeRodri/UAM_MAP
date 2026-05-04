package com.example.uammap.utils

import com.example.uammap.model.Geometry
import com.google.gson.*
import java.lang.reflect.Type

class GeometryDeserializer : JsonDeserializer<Geometry> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Geometry {
        val obj = json.asJsonObject
        val type = obj.get("type").asString
        val coordsElement = obj.get("coordinates")
        val coords: List<Any>? = when (type) {
            "Point" -> {
                val point = context.deserialize<List<Double>>(coordsElement, List::class.java)
                listOf(point)
            }
            "Polygon" -> {
                val rings = mutableListOf<List<List<Double>>>()
                for (ring in coordsElement!!.asJsonArray) {
                    val ringList = context.deserialize<List<List<Double>>>(ring, List::class.java)
                    rings.add(ringList)
                }
                listOf(rings)
            }
            else -> {
                // LineString, MultiPolygon, etc. -> devolvemos lista vacía para evitar null
                emptyList<Any>()
            }
        }
        return Geometry(type, coords)
    }
}