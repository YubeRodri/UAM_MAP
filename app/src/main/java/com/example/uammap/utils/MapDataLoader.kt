package com.example.uammap.utils

import android.content.Context
import android.util.Log
import androidx.compose.ui.geometry.Offset
import com.example.uammap.model.*
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

object MapDataLoader {
    private const val TAG = "MapDataLoader"
    private const val MAP_SIZE = 10000f

    var edificios: List<Edificio> = emptyList()
    var nodos: List<Nodo> = emptyList()
    var aristas: List<Arista> = emptyList()
    var puntosInteres: List<PuntoDeInteres> = emptyList()
    var lastError: String? = null

    private var loaded = false

    suspend fun load(context: Context) {
        if (loaded) return
        try {
            withContext(Dispatchers.IO) {
                Log.d(TAG, "Abriendo archivo campus.geojson...")
                val inputStream = context.assets.open("campus.geojson")
                val reader = InputStreamReader(inputStream, Charsets.UTF_8)
                val gson = GsonBuilder()
                    .registerTypeAdapter(Geometry::class.java, GeometryDeserializer())
                    .create()
                Log.d(TAG, "Parseando JSON...")
                val geoJson = gson.fromJson(reader, GeoJson::class.java)
                Log.d(TAG, "Total features: ${geoJson.features.size}")

                var minLon = Double.MAX_VALUE
                var maxLon = Double.MIN_VALUE
                var minLat = Double.MAX_VALUE
                var maxLat = Double.MIN_VALUE

                val tempEdificios = mutableListOf<Pair<String, List<List<Double>>>>()
                val tempPoints = mutableListOf<Pair<String, List<Double>>>()

                for (feature in geoJson.features) {
                    val props = feature.properties
                    val name = props?.keys?.firstOrNull() ?: continue
                    val geom = feature.geometry
                    if (geom.coordinates == null || geom.coordinates.isEmpty()) continue

                    when (geom.type) {
                        "Polygon" -> {
                            val rings = geom.coordinates[0] as? List<List<List<Double>>> ?: continue
                            val exterior = rings.firstOrNull() ?: continue
                            for (point in exterior) {
                                val lon = point[0]
                                val lat = point[1]
                                if (lon < minLon) minLon = lon
                                if (lon > maxLon) maxLon = lon
                                if (lat < minLat) minLat = lat
                                if (lat > maxLat) maxLat = lat
                            }
                            tempEdificios.add(name to exterior)
                        }
                        "Point" -> {
                            val coords = geom.coordinates[0] as? List<Double> ?: continue
                            val lon = coords[0]
                            val lat = coords[1]
                            if (lon < minLon) minLon = lon
                            if (lon > maxLon) maxLon = lon
                            if (lat < minLat) minLat = lat
                            if (lat > maxLat) maxLat = lat
                            tempPoints.add(name to coords)
                        }
                    }
                }

                Log.d(TAG, "Encontrados ${tempEdificios.size} polígonos y ${tempPoints.size} puntos")
                Log.d(TAG, "Límites: lon[$minLon, $maxLon] lat[$minLat, $maxLat]")

                val lonRange = maxLon - minLon
                val latRange = maxLat - minLat
                val aspect = lonRange / latRange
                val worldWidth: Float
                val worldHeight: Float
                if (aspect > 1) {
                    worldWidth = MAP_SIZE
                    worldHeight = MAP_SIZE / aspect.toFloat()
                } else {
                    worldHeight = MAP_SIZE
                    worldWidth = MAP_SIZE * aspect.toFloat()
                }

                fun proj(lon: Double, lat: Double): Offset {
                    val x = ((lon - minLon) / lonRange * worldWidth).toFloat()
                    val y = ((maxLat - lat) / latRange * worldHeight).toFloat()
                    return Offset(x, y)
                }

                edificios = tempEdificios.map { (name, exterior) ->
                    val projectedPoints = exterior.map { proj(it[0], it[1]) }
                    val centroid = Offset(
                        projectedPoints.map { it.x }.average().toFloat(),
                        projectedPoints.map { it.y }.average().toFloat()
                    )
                    val color = when {
                        name.contains("Biblioteca", ignoreCase = true) -> 0xFF1E88E5.toInt()
                        name.contains("Caja", ignoreCase = true) || name.contains("Cartera", ignoreCase = true) -> 0xFFFFA000.toInt()
                        name.contains("Cafetería", ignoreCase = true) || name.contains("Food", ignoreCase = true) -> 0xFFE53935.toInt()
                        name.contains("Jaguar", ignoreCase = true) -> 0xFF43A047.toInt()
                        else -> 0xFF9E9E9E.toInt()
                    }
                    Edificio(name, color, projectedPoints, centroid)
                }

                Log.d(TAG, "Edificios creados: ${edificios.size}")
                edificios.take(3).forEach { ed ->
                    Log.d(TAG, "${ed.name}: ${ed.points.size} puntos, centroide=(${ed.centroid.x},${ed.centroid.y})")
                }

                nodos = edificios.mapIndexed { index, ed ->
                    Nodo(
                        id = index.toString(),
                        nombre = ed.name,
                        x = ed.centroid.x,
                        y = ed.centroid.y,
                        tipo = TipoNodo.EDIFICIO
                    )
                }

                val umbral = 500f
                val tempAristas = mutableListOf<Arista>()
                for (i in nodos.indices) {
                    for (j in i + 1 until nodos.size) {
                        val ni = nodos[i]
                        val nj = nodos[j]
                        val dx = ni.x - nj.x
                        val dy = ni.y - nj.y
                        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                        if (dist < umbral) {
                            tempAristas.add(Arista(ni.id, nj.id, dist.toDouble()))
                        }
                    }
                }
                aristas = tempAristas
                Log.d(TAG, "Aristas generadas: ${aristas.size}")

                puntosInteres = tempPoints.map { (name, point) ->
                    val offset = proj(point[0], point[1])
                    PuntoDeInteres(
                        id = name,
                        nombre = name,
                        descripcion = "",
                        nodoAsociado = "",
                        categoria = CategoriaPOI.OTRO
                    )
                }
            }
            loaded = true
            Log.d(TAG, "Carga completada exitosamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading GeoJSON", e)
            lastError = e.message
            loaded = true
        }
    }
}