package com.example.uammap.utils

import android.content.Context
import android.util.Log
import androidx.compose.ui.geometry.Offset
import com.example.uammap.model.*
import com.example.uammap.network.ApiService
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import kotlin.math.sqrt

object MapDataLoader {
    private const val TAG      = "MapDataLoader"
    private const val MAP_SIZE = 10000f

    // Keys that are GeoJSON style properties, not feature names
    private val STYLE_KEYS = setOf(
        "stroke", "stroke-width", "stroke-opacity",
        "fill", "fill-opacity",
        "marker-color", "marker-size", "marker-symbol"
    )

    var edificios     : List<Edificio>       = emptyList()
    var calles        : List<Calle>          = emptyList()
    var nodos         : List<Nodo>           = emptyList()
    var aristas       : List<Arista>         = emptyList()
    var puntosInteres : List<PuntoDeInteres> = emptyList()
    var lastError     : String?              = null

    var worldWidth    : Float                = 0f
    var worldHeight   : Float                = 0f
    var isConnectedToApi: Boolean            = false

    // Projection bounds
    var minLon: Double = 0.0
    var maxLon: Double = 0.0
    var minLat: Double = 0.0
    var maxLat: Double = 0.0

    private var loaded = false

    fun project(lon: Double, lat: Double): Offset {
        val lonRange = maxLon - minLon
        val latRange = maxLat - minLat
        if (lonRange == 0.0 || latRange == 0.0) return Offset.Zero
        return Offset(
            ((lon - minLon) / lonRange * worldWidth).toFloat(),
            ((maxLat - lat) / latRange * worldHeight).toFloat()
        )
    }

    suspend fun load(context: Context) {
        if (loaded) return
        
        // Intentar cargar desde la API (con simulador activado)
        isConnectedToApi = loadFromApi(context)
        if (isConnectedToApi) {
            loaded = true
            return
        }

        // Si falla la API, cargar desde assets localmente
        try {
            withContext(Dispatchers.IO) {
                Log.d(TAG, "Abriendo campus.geojson desde assets…")
                val inputStream = context.assets.open("campus.geojson")
                val reader      = InputStreamReader(inputStream, Charsets.UTF_8)
                val root        = JsonParser.parseReader(reader).asJsonObject
                parseGeoJson(root)
            }
            loaded = true
            Log.d(TAG, "Carga completa desde Assets ✓")
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando GeoJSON local", e)
            lastError = "${e.javaClass.simpleName}: ${e.message}"
            loaded    = true
        }
    }

    private suspend fun loadFromApi(context: Context): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                Log.d(TAG, "Intentando cargar desde API REST (Simulada)…")
                val apiService = ApiService.create(context)
                val geoJson = apiService.getCampusData()
                // Convertir el modelo GeoJson a JsonObject para reusar el parser existente
                val jsonString = Gson().toJson(geoJson)
                val root = JsonParser.parseString(jsonString).asJsonObject
                parseGeoJson(root)
                Log.d(TAG, "Carga completa desde API Simulada ✓")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando desde API", e)
            false
        }
    }

    fun parseGeoJson(root: JsonObject) {
        val features = root.getAsJsonArray("features")
        Log.d(TAG, "Features totales: ${features.size()}")

        // ── Bounding box ───────────────────────────────────────────────
        var tempMinLon = Double.MAX_VALUE;  var tempMaxLon = -Double.MAX_VALUE
        var tempMinLat = Double.MAX_VALUE;  var tempMaxLat = -Double.MAX_VALUE

        data class RawPoly(val name: String, val exterior: List<Pair<Double, Double>>, val fillColor: String)
        data class RawPoint(val name: String, val lon: Double, val lat: Double)
        data class RawLine(val coords: List<Pair<Double, Double>>)

        val tempPolys  = mutableListOf<RawPoly>()
        val tempPoints = mutableListOf<RawPoint>()
        val tempLines  = mutableListOf<RawLine>()

        fun updateBounds(lon: Double, lat: Double) {
            if (lon < tempMinLon) tempMinLon = lon; if (lon > tempMaxLon) tempMaxLon = lon
            if (lat < tempMinLat) tempMinLat = lat; if (lat > tempMaxLat) tempMaxLat = lat
        }

        for (featureEl in features) {
            val feature  = featureEl.asJsonObject
            val geom     = feature.getAsJsonObject("geometry") ?: continue
            val geomType = geom.get("type")?.asString ?: continue
            val coordsEl = geom.get("coordinates") ?: continue
            val props    = feature.getAsJsonObject("properties")

            when (geomType) {
                "Polygon" -> {
                    val outerRingEl = try {
                        coordsEl.asJsonArray[0].asJsonArray
                    } catch (e: Exception) { continue }

                    val pts = mutableListOf<Pair<Double, Double>>()
                    for (ptEl in outerRingEl) {
                        try {
                            val pair = ptEl.asJsonArray
                            if (pair.size() >= 2) {
                                val lon = pair[0].asDouble
                                val lat = pair[1].asDouble
                                updateBounds(lon, lat)
                                pts.add(lon to lat)
                            }
                        } catch (e: Exception) { /* skip malformed point */ }
                    }

                    if (pts.size >= 3) {
                        val name      = extractName(props)
                        val fillColor = props?.get("fill")?.asString ?: ""
                        tempPolys.add(RawPoly(name, pts, fillColor))
                    }
                }

                "Point" -> {
                    try {
                        val pair = coordsEl.asJsonArray
                        if (pair.size() >= 2) {
                            val lon  = pair[0].asDouble
                            val lat  = pair[1].asDouble
                            val name = extractName(props)
                            updateBounds(lon, lat)
                            tempPoints.add(RawPoint(name, lon, lat))
                        }
                    } catch (e: Exception) { /* skip */ }
                }

                "LineString" -> {
                    val pts = mutableListOf<Pair<Double, Double>>()
                    try {
                        for (ptEl in coordsEl.asJsonArray) {
                            val pair = ptEl.asJsonArray
                            if (pair.size() >= 2) {
                                val lon = pair[0].asDouble
                                val lat = pair[1].asDouble
                                updateBounds(lon, lat)
                                pts.add(lon to lat)
                            }
                        }
                    } catch (e: Exception) { /* skip malformed line */ }
                    if (pts.size >= 2) tempLines.add(RawLine(pts))
                }
            }
        }

        Log.d(TAG, "Polígonos: ${tempPolys.size}  Puntos: ${tempPoints.size}  Líneas: ${tempLines.size}")
        Log.d(TAG, "BBox: lon[$tempMinLon,$tempMaxLon]  lat[$tempMinLat,$tempMaxLat]")

        if (tempPolys.isEmpty()) {
            lastError = "No se encontraron edificios en el GeoJSON."
            return
        }

        // Guardar límites globales para la proyección externa
        minLon = tempMinLon; maxLon = tempMaxLon
        minLat = tempMinLat; maxLat = tempMaxLat

        // ── Proyección lon/lat → coordenadas de pantalla ───────────────
        val lonRange = maxLon - minLon
        val latRange = maxLat - minLat
        val aspect   = lonRange / latRange
        val worldW   = if (aspect > 1) MAP_SIZE else (MAP_SIZE * aspect).toFloat()
        val worldH   = if (aspect > 1) (MAP_SIZE / aspect).toFloat() else MAP_SIZE

        worldWidth  = worldW
        worldHeight = worldH

        fun proj(lon: Double, lat: Double) = project(lon, lat)

        // ── Edificios ──────────────────────────────────────────────────
        edificios = tempPolys.map { (name, exterior, fillColor) ->
            val pts = exterior.map { (lon, lat) -> proj(lon, lat) }
            val centroid = Offset(
                pts.map { it.x }.average().toFloat(),
                pts.map { it.y }.average().toFloat()
            )
            Edificio(name, buildingColor(name, fillColor), pts, centroid)
        }
        Log.d(TAG, "Edificios creados: ${edificios.size}")

        // ── Calles ─────────────────────────────────────────────────────
        calles = tempLines.map { (coords) ->
            Calle(coords.map { (lon, lat) -> proj(lon, lat) })
        }
        Log.d(TAG, "Calles creadas: ${calles.size}")

        // ── Nodos (uno por edificio) ────────────────────────────────────
        nodos = edificios.mapIndexed { index, ed ->
            Nodo(
                id     = index.toString(),
                nombre = ed.name,
                x      = ed.centroid.x,
                y      = ed.centroid.y,
                tipo   = nodeTipo(ed.name)
            )
        }

        // ── Aristas (grafo de navegación) ──────────────────────────────
        val umbral      = 2000f
        val tempAristas = mutableListOf<Arista>()
        val adjCount    = IntArray(nodos.size)

        for (i in nodos.indices) {
            val ni = nodos[i]
            for (j in i + 1 until nodos.size) {
                val nj   = nodos[j]
                val dx   = ni.x - nj.x; val dy = ni.y - nj.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < umbral) {
                    tempAristas.add(Arista(ni.id, nj.id, dist.toDouble()))
                    adjCount[i]++; adjCount[j]++
                }
            }
        }

        // Garantía mínima: cada nodo conectado a sus 3 vecinos más cercanos
        for (i in nodos.indices) {
            if (adjCount[i] == 0) {
                val ni      = nodos[i]
                val closest = nodos.indices
                    .filter { it != i }
                    .sortedBy { j ->
                        val nj = nodos[j]
                        val dx = ni.x - nj.x; val dy = ni.y - nj.y
                        sqrt(dx * dx + dy * dy)
                    }
                    .take(3)
                for (j in closest) {
                    val nj   = nodos[j]
                    val dx   = ni.x - nj.x; val dy = ni.y - nj.y
                    val dist = sqrt(dx * dx + dy * dy)
                    tempAristas.add(Arista(ni.id, nj.id, dist.toDouble()))
                }
            }
        }

        aristas = tempAristas
        Log.d(TAG, "Aristas generadas: ${aristas.size}")

        // ── Puntos de interés ──────────────────────────────────────────
        puntosInteres = tempPoints.map { (name, lon, lat) ->
            val p        = proj(lon, lat)
            val nearestId = nodos.minByOrNull { n ->
                val dx = n.x - p.x; val dy = n.y - p.y
                dx * dx + dy * dy
            }?.id ?: ""
            PuntoDeInteres(
                id           = name,
                nombre       = name,
                descripcion  = categoryDescription(name),
                nodoAsociado = nearestId,
                categoria    = poiCategoria(name)
            )
        }
        Log.d(TAG, "POIs: ${puntosInteres.size}")
    }

    // ── Extrae el nombre del feature desde sus propiedades ─────────────────────
    private fun extractName(props: JsonObject?): String {
        if (props == null || props.size() == 0) return "Elemento"
        for (entry in props.entrySet()) {
            val key = entry.key ?: continue
            if (key in STYLE_KEYS) continue
            val cleanKey = key.trim()
            if (cleanKey.isBlank()) continue

            val rawValue = try { entry.value?.asString?.trim() ?: "" } catch (e: Exception) { "" }

            // Preferir valor corto si es más compacto que la clave
            val name = if (rawValue.isNotBlank() && rawValue.length < cleanKey.length) rawValue
            else cleanKey

            // Si es bilingüe ("Nombre/Name"), tomar la parte en español
            var finalName = name.split("/")[0].trim().ifBlank { name }
            
            // ── Correcciones Ortográficas y de Formato ───────────────────────
            // 1. Primero normalizar el prefijo de forma atómica para evitar recursión
            if (finalName.startsWith("Ed.", ignoreCase = true) || 
                finalName.startsWith("Edif", ignoreCase = true) || 
                finalName.startsWith("Edificio", ignoreCase = true)) {
                
                // Extraer el identificador (la letra o nombre después del prefijo)
                val identifier = finalName
                    .replace(Regex("^Edificio\\.?\\s*", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("^Edif\\.?\\s*", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("^Ed\\.?\\s*", RegexOption.IGNORE_CASE), "")
                    .trim()
                
                finalName = if (identifier.isNotEmpty()) "Edificio $identifier" else "Edificio"
            }

            // 2. Otras correcciones específicas y traducciones
            finalName = finalName
                .replace("Contenedoresd", "Contenedores", ignoreCase = true)
                .replace("Park", "Parqueo", ignoreCase = true)
                .replace("Besiball", "Béisbol", ignoreCase = true)
                .replace("Baseball", "Béisbol", ignoreCase = true)
                .replace("Odontologïa", "Odontología", ignoreCase = true)
                .replace("Sálon", "Salón", ignoreCase = true)
                .replace("Gym", "Gimnasio", ignoreCase = true)
                .replace("Food Court", "Área de Comidas", ignoreCase = true)
                .replace("Parking", "Estacionamiento", ignoreCase = true)
                .replace("Bathroom", "Baño", ignoreCase = true)
                .replace("Security", "Seguridad", ignoreCase = true)
                .replace(Regex("\\s+"), " ")
                .trim()

            return finalName
        }
        return "Elemento"
    }

    // ── Color del edificio: primero desde GeoJSON fill, luego por nombre ───────
    private fun buildingColor(name: String, fillHex: String): Int {
        // Colores más suaves y profesionales (Material Design Palette)
        return when {
            name.contains("Biblioteca", ignoreCase = true)                      -> 0xFFBBDEFB.toInt() // Blue 100
            name.contains("Caja", ignoreCase = true)
                    || name.contains("Cartera", ignoreCase = true)
                    || name.contains("Cobro", ignoreCase = true)
                    || name.contains("Administracion", ignoreCase = true)
                    || name.contains("Recepcion", ignoreCase = true)
                    || name.contains("Rectoria", ignoreCase = true)
                    || name.contains("Presidencia", ignoreCase = true)          -> 0xFFFFECB3.toInt() // Amber 100
            name.contains("Food", ignoreCase = true)
                    || name.contains("Cafetería", ignoreCase = true)
                    || name.contains("Chilamate", ignoreCase = true)
                    || name.contains("Ranchito", ignoreCase = true)
                    || name.contains("Jaguarcito", ignoreCase = true)
                    || name.contains("Kiosko", ignoreCase = true)               -> 0xFFFFCDD2.toInt() // Red 100
            name.contains("Jaguar", ignoreCase = true)
                    || name.contains("Cancha", ignoreCase = true)
                    || name.contains("Campo", ignoreCase = true)
                    || name.contains("Gym", ignoreCase = true)
                    || name.contains("Complejo", ignoreCase = true)
                    || name.contains("Deport", ignoreCase = true)               -> 0xFFC8E6C9.toInt() // Green 100
            name.contains("Baño", ignoreCase = true)
                    || name.contains("Sanitario", ignoreCase = true)
                    || name.contains("Vestidor", ignoreCase = true)             -> 0xFFF5F5F5.toInt() // Grey 100
            name.contains("Parqueo", ignoreCase = true)
                    || name.contains("Parking", ignoreCase = true)
                    || name.contains("Bahia", ignoreCase = true)                -> 0xFFECEFF1.toInt() // BlueGrey 50
            name.contains("Banco", ignoreCase = true)
                    || name.contains("Bampro", ignoreCase = true)
                    || name.contains("Banpro", ignoreCase = true)
                    || name.contains("ATM", ignoreCase = true)                  -> 0xFFC5CAE9.toInt() // Indigo 100
            name.contains("Auditorio", ignoreCase = true)                       -> 0xFFD7CCC8.toInt() // Brown 100
            name.contains("Salon", ignoreCase = true)
                    || name.contains("Lenguaje", ignoreCase = true)
                    || name.contains("Aula", ignoreCase = true)                 -> 0xFFB2DFDB.toInt() // Teal 100
            name.contains("Edificio", ignoreCase = true)                        -> 0xFFE0F2F1.toInt() // Teal 50
            else                                                                 -> 0xFFEEEEEE.toInt() // Grey 200
        }
    }

    private fun nodeTipo(name: String): TipoNodo = when {
        name.contains("Biblioteca", ignoreCase = true)                   -> TipoNodo.BIBLIOTECA
        name.contains("Caja", ignoreCase = true)
                || name.contains("Cartera", ignoreCase = true)
                || name.contains("Cobro", ignoreCase = true)             -> TipoNodo.CAJA
        name.contains("Food", ignoreCase = true)
                || name.contains("Cafetería", ignoreCase = true)
                || name.contains("Jaguarcito", ignoreCase = true)        -> TipoNodo.CAFETERIA
        else                                                              -> TipoNodo.EDIFICIO
    }

    private fun poiCategoria(name: String): CategoriaPOI = when {
        name.contains("Biblioteca", ignoreCase = true)                   -> CategoriaPOI.BIBLIOTECA
        name.contains("Caja", ignoreCase = true)
                || name.contains("Cartera", ignoreCase = true)
                || name.contains("Cobro", ignoreCase = true)             -> CategoriaPOI.CAJA
        name.contains("Food", ignoreCase = true)
                || name.contains("Cafetería", ignoreCase = true)
                || name.contains("Jaguarcito", ignoreCase = true)        -> CategoriaPOI.CAFETERIA
        name.contains("Auditorio", ignoreCase = true)                    -> CategoriaPOI.AUDITORIO
        else                                                              -> CategoriaPOI.OTRO
    }

    private fun categoryDescription(name: String): String = when {
        name.contains("Biblioteca", ignoreCase = true) -> "Recursos académicos y digitales"
        name.contains("Caja", ignoreCase = true)       -> "Servicios financieros estudiantiles"
        name.contains("Jaguar", ignoreCase = true)     -> "Instalaciones deportivas UAM"
        name.contains("Food", ignoreCase = true)       -> "Zona de alimentación"
        else                                           -> "Campus UAM"
    }
}
