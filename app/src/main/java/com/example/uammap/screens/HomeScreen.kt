package com.example.uammap.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.uammap.utils.MapDataLoader
import com.example.uammap.utils.IconUtils
import com.example.uammap.utils.LocationManager
import com.example.uammap.utils.CalculadorRutas
import kotlinx.coroutines.delay

private val UamBlue       = Color(0xFF019AA8)
private val UamGold       = Color(0xFFF5A623)
private val MapBackground = Color(0xFFF8F7F2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val textMeasurer = rememberTextMeasurer()
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        MapDataLoader.load(context)
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(MapBackground), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = UamBlue)
                Spacer(Modifier.height(12.dp))
                Text("Cargando Campus UAM...", color = UamBlue, fontWeight = FontWeight.Medium)
            }
        }
        return
    }

    // ── DATA PREPARATION (Optimized) ─────────────────────────────────────────
    val edificios = remember { MapDataLoader.edificios.filter { !it.name.contains("Parqueo", true) && !it.name.contains("Estacionamiento", true) && !it.name.contains("Park", true) } }
    val pois = remember { MapDataLoader.puntosInteres.filter { !it.nombre.contains("Parqueo", true) && !it.nombre.contains("Estacionamiento", true) && !it.nombre.contains("Park", true) } }
    val worldWidth = MapDataLoader.worldWidth
    val worldHeight = MapDataLoader.worldHeight
    val userLocation by LocationManager.currentLocation.collectAsState()

    // ── CACHED PATHS ─────────────────────────────────────────────────────────
    val cachedStreetPaths = remember { MapDataLoader.calles.map { c -> Path().apply { if (c.points.isNotEmpty()) { moveTo(c.points[0].x, c.points[0].y); for (i in 1 until c.points.size) lineTo(c.points[i].x, c.points[i].y) } } } }
    val cachedBuildingPaths = remember { edificios.map { ed -> Path().apply { if (ed.points.isNotEmpty()) { moveTo(ed.points[0].x, ed.points[0].y); for (i in 1 until ed.points.size) lineTo(ed.points[i].x, ed.points[i].y); close() } } } }

    // ── STATE MANAGEMENT ─────────────────────────────────────────────────────
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var userZoom by remember { mutableStateOf(1.5f) }
    var isFollowingUser by remember { mutableStateOf(false) }

    var isGpsEnabled by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while(true) {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
            isGpsEnabled = lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
            delay(2000)
        }
    }

    val isUserInCampus = remember(userLocation) {
        userLocation?.let { loc ->
            val p = MapDataLoader.project(loc.longitude, loc.latitude)
            p.x in -500f..worldWidth + 500f && p.y in -500f..worldHeight + 500f
        } ?: false
    }

    // --- OPTIMIZACIÓN DE MOVIMIENTO ULTRA INSTANTÁNEO ---
    val animatedUserLon by animateFloatAsState(
        targetValue = userLocation?.longitude?.toFloat() ?: 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh // Máxima rigidez para sincronización inmediata
        ),
        label = "lon"
    )
    val animatedUserLat by animateFloatAsState(
        targetValue = userLocation?.latitude?.toFloat() ?: 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "lat"
    )
    
    val smoothedUserPos = remember(animatedUserLon, animatedUserLat) {
        MapDataLoader.project(animatedUserLon.toDouble(), animatedUserLat.toDouble())
    }

    var destinationPoint by remember { mutableStateOf<Offset?>(null) }
    var destinationName by remember { mutableStateOf<String?>(null) }
    var currentRoute by remember { mutableStateOf<List<Offset>?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val searchResults = remember(searchQuery) { if (searchQuery.isBlank()) emptyList() else edificios.filter { it.name.contains(searchQuery, ignoreCase = true) }.distinctBy { it.name } }

    LaunchedEffect(userLocation, destinationPoint, isUserInCampus) {
        if (userLocation != null && destinationPoint != null && isUserInCampus) {
            // LÍNEA RECTA DIRECTA: Desde la posición visual hasta el destino
            // Eliminamos la lógica de navegación por nodos para evitar curvas
            currentRoute = listOf(smoothedUserPos, destinationPoint!!)
        } else {
            currentRoute = null
        }
    }

    // ── ANIMATIONS ───────────────────────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pScale by infiniteTransition.animateFloat(1f, 1.6f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "s")
    val pAlpha by infiniteTransition.animateFloat(0.4f, 0f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "a")

    // ── PAINTERS FOR CANVAS RENDERING (Very Fast) ────────────────────────────
    val poiPainters = pois.associate { it.id to rememberVectorPainter(IconUtils.getIconForCategory(it.categoria)) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("UAM MAP", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color.White); Text("Universidad Americana", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f)) } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = UamBlue)
            )
        }
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().background(MapBackground).padding(padding)) {
            val cw = this.constraints.maxWidth.toFloat()
            val ch = this.constraints.maxHeight.toFloat()
            val baseScale = remember(worldWidth, worldHeight, cw, ch) { minOf(cw / worldWidth, ch / worldHeight) * 0.9f }
            val initOffX = (cw - worldWidth * baseScale) / 2f
            val initOffY = (ch - worldHeight * baseScale) / 2f
            
            val totalScale = baseScale * userZoom

            LaunchedEffect(smoothedUserPos, isFollowingUser, isUserInCampus, totalScale) {
                if (isFollowingUser && isUserInCampus) {
                    offsetX = -smoothedUserPos.x * totalScale + cw / 2f - initOffX
                    offsetY = -smoothedUserPos.y * totalScale + ch / 2f - initOffY
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer() // GPU ACCELERATION
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            if (pan != Offset.Zero || zoom != 1f) isFollowingUser = false
                            val newZ = (userZoom * zoom).coerceIn(0.4f, 8f)
                            val dZ = newZ - userZoom
                            offsetX = (offsetX - centroid.x * dZ / userZoom + pan.x).coerceIn((cw / 2f - initOffX) - worldWidth * baseScale * newZ, cw / 2f - initOffX)
                            offsetY = (offsetY - centroid.y * dZ / userZoom + pan.y).coerceIn((ch / 2f - initOffY) - worldHeight * baseScale * newZ, ch / 2f - initOffY)
                            userZoom = newZ
                        }
                    }
                    .pointerInput(baseScale) {
                        detectTapGestures { tap ->
                            // CORRECCIÓN DEFINITIVA: Usar el mismo 'totalScale' que el Canvas
                            val currentTotalScale = baseScale * userZoom
                            val worldX = (tap.x - initOffX - offsetX) / currentTotalScale
                            val worldY = (tap.y - initOffY - offsetY) / currentTotalScale

                            destinationPoint = Offset(worldX, worldY)
                            destinationName = null
                            if (isUserInCampus && userLocation != null) isFollowingUser = true
                        }
                    }
            ) {
                // ── DRAWING MAP ELEMENTS ──
                withTransform({
                    translate(initOffX + offsetX, initOffY + offsetY)
                    scale(totalScale, totalScale, pivot = Offset.Zero)
                }) {
                    // 1. Streets
                    cachedStreetPaths.forEach { p ->
                        drawPath(p, Color(0xFFB0B0A8), style = Stroke(width = 10f / totalScale, join = StrokeJoin.Round))
                        drawPath(p, Color(0xFFF0F0E8), style = Stroke(width = 7f / totalScale, join = StrokeJoin.Round))
                    }
                    // 2. Buildings
                    cachedBuildingPaths.forEachIndexed { i, p ->
                        drawPath(p, Color(edificios[i].color))
                        drawPath(p, Color.Black.copy(alpha = 0.1f), style = Stroke(width = 1f / totalScale))
                    }
                    // 3. Route
                    currentRoute?.let { pts ->
                        if (pts.size >= 2) {
                            val path = Path().apply { moveTo(pts[0].x, pts[0].y); for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y) }
                            drawPath(path, UamBlue.copy(alpha = 0.2f), style = Stroke(width = 20f / totalScale, join = StrokeJoin.Round))
                            drawPath(path, UamBlue, style = Stroke(width = 10f / totalScale, join = StrokeJoin.Round))
                        }
                    }
                }

                // ── DRAWING POI ICONS (Directly in Canvas for Performance) ──
                pois.forEach { poi ->
                    val n = MapDataLoader.nodos.find { it.id == poi.nodoAsociado } ?: return@forEach
                    val c = Offset(n.x * totalScale + initOffX + offsetX, n.y * totalScale + initOffY + offsetY)
                    if (userZoom >= 0.8f) {
                        val iconSize = (18f * userZoom.coerceIn(0.8f, 2f))
                        val painter = poiPainters[poi.id]
                        if (painter != null) {
                            withTransform({
                                translate(c.x - iconSize / 2, c.y - iconSize / 2)
                            }) {
                                with(painter) { draw(size = androidx.compose.ui.geometry.Size(iconSize, iconSize), colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(UamBlue)) }
                            }
                        }
                    }
                }

                // ── DRAWING LABELS (Improved Collision Detection) ──
                if (userZoom >= 2.2f) {
                    val occupiedRects = mutableListOf<androidx.compose.ui.geometry.Rect>()
                    edificios.forEach { ed ->
                        val c = Offset(ed.centroid.x * totalScale + initOffX + offsetX, ed.centroid.y * totalScale + initOffY + offsetY)
                        val style = TextStyle(
                            color = Color(0xFF444444),
                            fontSize = (7f * (userZoom / 2.2f).coerceIn(0.9f, 1.3f)).sp,
                            fontWeight = FontWeight.SemiBold,
                            shadow = Shadow(Color.White.copy(0.8f), blurRadius = 2f)
                        )
                        val m = textMeasurer.measure(ed.name, style)
                        val labelRect = androidx.compose.ui.geometry.Rect(
                            left = c.x - m.size.width / 2f - 4f,
                            top = c.y - m.size.height / 2f - 2f,
                            right = c.x + m.size.width / 2f + 4f,
                            bottom = c.y + m.size.height / 2f + 2f
                        )

                        val isOverlapping = occupiedRects.any { it.overlaps(labelRect) }

                        if (!isOverlapping || ed.name.contains("Baño", true)) {
                            drawText(m, topLeft = Offset(c.x - m.size.width / 2f, c.y - m.size.height / 2f))
                            if (!ed.name.contains("Baño", true)) {
                                occupiedRects.add(labelRect)
                            }
                        }
                    }
                }

                // --- GPS AND TARGET ---
                if (isUserInCampus && userLocation != null) {
                    val c = Offset(smoothedUserPos.x * totalScale + initOffX + offsetX, smoothedUserPos.y * totalScale + initOffY + offsetY)
                    drawCircle(Color(0xFF2196F3).copy(pAlpha), 35f * pScale * userZoom.coerceIn(0.8f, 1.5f), c)
                    drawCircle(Color.White, 9f, c); drawCircle(Color(0xFF2196F3), 7f, c)
                }
                destinationPoint?.let { dp ->
                    val c = Offset(dp.x * totalScale + initOffX + offsetX, dp.y * totalScale + initOffY + offsetY)
                    drawCircle(UamGold, 8f, c); drawCircle(Color.White, 4f, c)
                }
            }

            // ── UI OVERLAYS ──────────────────────────────────────────────────
            SearchOverlay(searchQuery, onQueryChange = { searchQuery = it }, searchResults = searchResults, onResultClick = { ed -> destinationPoint = ed.centroid; destinationName = ed.name; searchQuery = ""; if (isUserInCampus && userLocation != null) isFollowingUser = true })

            // Indicador de conexión a la API
            ApiConnectionIndicator()

            if (destinationPoint != null) {
                DestIndicator(destinationName, onCancel = { destinationPoint = null; currentRoute = null; isFollowingUser = false; destinationName = null })
            }

            ControlButtons(
                isFollowing = isFollowingUser,
                isGpsEnabled = isGpsEnabled && isUserInCampus,
                onZoomIn = { userZoom = (userZoom * 1.3f).coerceAtMost(8f) },
                onZoomOut = { userZoom = (userZoom / 1.3f).coerceAtLeast(0.4f) },
                onReset = { userZoom = 1.5f; offsetX = 0f; offsetY = 0f },
                onLocationClick = {
                    val hasF = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    if (!hasF) { /* Permissions logic here or via launcher in parent */ } 
                    else if (isUserInCampus && isGpsEnabled) { isFollowingUser = true; userZoom = 3.0f }
                    else { isFollowingUser = false; userZoom = 1.5f; offsetX = 0f; offsetY = 0f }
                }
            )

            StatusMessages(isGpsEnabled, userLocation, isUserInCampus, destinationPoint != null)
        }
    }
}

@Composable
private fun SearchOverlay(query: String, onQueryChange: (String) -> Unit, searchResults: List<com.example.uammap.model.Edificio>, onResultClick: (com.example.uammap.model.Edificio) -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column {
            Surface(modifier = Modifier.fillMaxWidth(), color = Color.White.copy(0.95f), shape = RoundedCornerShape(12.dp), shadowElevation = 8.dp) {
                TextField(value = query, onValueChange = onQueryChange, placeholder = { Text("Buscar edificio...") }, leadingIcon = { Icon(Icons.Default.Search, null, tint = UamBlue) }, trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Default.Close, null) } }, colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent), singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            AnimatedVisibility(searchResults.isNotEmpty(), enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Surface(modifier = Modifier.fillMaxWidth().padding(top = 4.dp).heightIn(max = 200.dp), color = Color.White, shape = RoundedCornerShape(12.dp), shadowElevation = 8.dp) {
                    androidx.compose.foundation.lazy.LazyColumn { 
                        items(searchResults.size) { i -> 
                            val ed = searchResults[i]
                            ListItem(
                                headlineContent = { Text(ed.name, fontWeight = FontWeight.Bold) }, 
                                supportingContent = { Text("Edificio del Campus") }, 
                                leadingContent = { Icon(Icons.Default.Info, null, tint = UamBlue) }, 
                                trailingContent = {
                                    Surface(color = UamBlue, shape = RoundedCornerShape(8.dp), modifier = Modifier.clickable { onResultClick(ed) }) {
                                        Text("IR", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                },
                                modifier = Modifier.clickable { onResultClick(ed) }
                            ) 
                        } 
                    }
                }
            }
        }
    }
}

@Composable
private fun DestIndicator(name: String?, onCancel: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Surface(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp), color = UamBlue, shape = RoundedCornerShape(20.dp), shadowElevation = 6.dp) { 
            Text("Destino: ${name ?: "Punto en el mapa"}", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) 
        }
        SmallFloatingActionButton(onClick = onCancel, modifier = Modifier.align(Alignment.BottomStart).padding(16.dp), containerColor = Color.White, contentColor = Color.Red) { Icon(Icons.Default.Close, null) }
    }
}

@Composable
private fun ControlButtons(isFollowing: Boolean, isGpsEnabled: Boolean, onZoomIn: () -> Unit, onZoomOut: () -> Unit, onReset: () -> Unit, onLocationClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(end = 12.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
        ZoomButton("+", onZoomIn)
        Spacer(Modifier.height(6.dp))
        ZoomButton("−", onZoomOut)
        Spacer(Modifier.height(6.dp))
        ZoomButton("⌂", onReset)
        Spacer(Modifier.height(6.dp))
        FilledTonalIconButton(onClick = onLocationClick, modifier = Modifier.size(38.dp), colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = if (isFollowing) UamBlue else Color.White.copy(0.92f), contentColor = if (isFollowing) Color.White else UamBlue)) { 
            Icon(if (isGpsEnabled) Icons.Default.MyLocation else Icons.Default.Info, null, Modifier.size(20.dp)) 
        }
    }
}

@Composable
private fun StatusMessages(gps: Boolean, loc: android.location.Location?, inCampus: Boolean, hasDest: Boolean) {
    val showErr = (!gps || (hasDest && (loc == null || !inCampus)))
    Box(modifier = Modifier.fillMaxSize().padding(bottom = 32.dp), contentAlignment = Alignment.BottomCenter) {
        if (showErr) {
            Surface(color = Color(0xFFD32F2F).copy(0.9f), shape = RoundedCornerShape(16.dp), shadowElevation = 4.dp) {
                val msg = when { !gps -> "El GPS está desactivado"; loc == null -> "Buscando ubicación..."; else -> "Debes estar en el campus para navegar" }
                Text(msg, modifier = Modifier.padding(12.dp), color = Color.White, fontWeight = FontWeight.Bold)
            }
        } else if (loc != null && !inCampus) {
            Surface(color = UamBlue.copy(0.9f), shape = RoundedCornerShape(16.dp)) { Text("Te encuentras fuera del campus UAM", modifier = Modifier.padding(10.dp), color = Color.White) }
        }
    }
}

@Composable
private fun ApiConnectionIndicator() {
    val isConnected = MapDataLoader.isConnectedToApi
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Surface(
            color = if (isConnected) Color(0xFF4CAF50).copy(alpha = 0.9f) else Color(0xFFF44336).copy(alpha = 0.9f),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 4.dp,
            modifier = Modifier.padding(top = 70.dp) // Debajo de la barra de búsqueda
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.White, CircleShape)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isConnected) "API Conectada" else "Modo Offline",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ZoomButton(label: String, onClick: () -> Unit) {
    FilledTonalIconButton(onClick = onClick, modifier = Modifier.size(38.dp), colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color.White.copy(0.92f))) { Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF333333)) }
}
