package com.example.uammap.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.uammap.navigation.Screen
import com.example.uammap.utils.MapDataLoader

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

    val edificios = MapDataLoader.edificios
    val error = MapDataLoader.lastError

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.Center)
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Cargando mapa...")
            }
        }
        return
    }

    if (error != null) {
        AlertDialog(
            onDismissRequest = { MapDataLoader.lastError = null },
            title = { Text("Error al cargar el mapa") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { MapDataLoader.lastError = null }) {
                    Text("OK")
                }
            }
        )
    }

    if (edificios.isEmpty() && error == null) {
        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Text("No se encontraron edificios en el mapa.",
                modifier = Modifier.align(Alignment.Center))
        }
        return
    }

    var offset by remember { mutableStateOf(Offset.Zero) }
    var scale by remember { mutableStateOf(1f) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Mapa UAM") }) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { },
                            onDragEnd = { },
                            onDragCancel = { },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                offset = Offset(offset.x + dragAmount.x, offset.y + dragAmount.y)
                            }
                        )
                    }
                    .pointerInput(edificios) {
                        detectTapGestures { tapOffset ->
                            val canvasSize = size

                            val adjusted = Offset(
                                (tapOffset.x - offset.x) / scale,
                                (tapOffset.y - offset.y) / scale
                            )
                            for (ed in edificios) {
                                if (isPointInPolygon(adjusted, ed.points)) {
                                    val origenId = "0"
                                    val origen = edificios.firstOrNull()?.name ?: "Origen"
                                    val destinoId = edificios.indexOf(ed).toString()
                                    navController.navigate(
                                        Screen.Route.createRoute(
                                            origenId,
                                            destinoId,
                                            origen,
                                            ed.name
                                        )
                                    )
                                    return@detectTapGestures
                                }
                            }
                        }
                    }
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val worldWidth = edificios.maxOf { it.points.maxOf { p -> p.x } } -
                        edificios.minOf { it.points.minOf { p -> p.x } }
                val worldHeight = edificios.maxOf { it.points.maxOf { p -> p.y } } -
                        edificios.minOf { it.points.minOf { p -> p.y } }
                scale = minOf(canvasWidth / worldWidth, canvasHeight / worldHeight) * 0.9f
                for (ed in edificios) {
                    val path = Path()
                    val pts = ed.points.map {
                        Offset(it.x * scale + offset.x, it.y * scale + offset.y)
                    }
                    if (pts.isNotEmpty()) {
                        path.moveTo(pts[0].x, pts[0].y)
                        for (i in 1 until pts.size) {
                            path.lineTo(pts[i].x, pts[i].y)
                        }
                        path.close()

                        drawPath(path, Color(ed.color).copy(alpha = 0.7f))
                        drawPath(path, Color.DarkGray, style = Stroke(width = 2f))
                    }
                    val cx = ed.centroid.x * scale + offset.x
                    val cy = ed.centroid.y * scale + offset.y
                    drawCircle(Color.Red, radius = 8f, center = Offset(cx, cy))
                }

                for (ed in edificios) {
                    val cx = ed.centroid.x * scale + offset.x
                    val cy = ed.centroid.y * scale + offset.y
                    val text = ed.name.take(15)
                    val style = TextStyle(color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    val textLayout = textMeasurer.measure(text, style)
                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(cx - textLayout.size.width / 2f, cy - textLayout.size.height / 2f)
                    )
                }
            }

            Button(
                onClick = { navController.navigate("search") },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text("Buscar destino")
            }
        }
    }
}

fun isPointInPolygon(point: Offset, polygon: List<Offset>): Boolean {
    var intersects = false
    var j = polygon.size - 1
    for (i in polygon.indices) {
        val xi = polygon[i].x
        val yi = polygon[i].y
        val xj = polygon[j].x
        val yj = polygon[j].y
        if ((yi > point.y) != (yj > point.y) &&
            point.x < (xj - xi) * (point.y - yi) / (yj - yi) + xi
        ) {
            intersects = !intersects
        }
        j = i
    }
    return intersects
}