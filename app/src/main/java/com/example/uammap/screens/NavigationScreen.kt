package com.example.uammap.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.uammap.utils.MapDataLoader
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationScreen(navController: NavController, rutaNodesStr: String, origenId: String) {
    val context = LocalContext.current
    val textMeasurer = rememberTextMeasurer()

    // Cargar datos si es necesario
    if (MapDataLoader.nodos.isEmpty()) {
        LaunchedEffect(Unit) { MapDataLoader.load(context) }
        return
    }

    val nodeIds = remember { rutaNodesStr.split(",") }
    var currentIndex by remember { mutableIntStateOf(0) }
    val nodos = MapDataLoader.nodos
    val currentNodo = nodos.find { it.id == nodeIds[currentIndex] } ?: return

    // Avance automático
    LaunchedEffect(currentIndex) {
        if (currentIndex < nodeIds.lastIndex) {
            delay(2000)
            currentIndex++
        }
    }

    val siguienteNodoId = if (currentIndex < nodeIds.lastIndex) nodeIds[currentIndex + 1] else null
    val siguienteNodo = nodos.find { it.id == siguienteNodoId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Navegación en vivo") },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text("Salir")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Calcular escala igual que en HomeScreen para que se vea todo el campus
                val worldWidth = nodos.maxOf { it.x } - nodos.minOf { it.x }
                val worldHeight = nodos.maxOf { it.y } - nodos.minOf { it.y }
                if (worldWidth == 0f || worldHeight == 0f) return@Canvas
                val scale = minOf(canvasWidth / worldWidth, canvasHeight / worldHeight) * 0.9f

                // Dibujar todos los nodos (círculos grises)
                for (nodo in nodos) {
                    val x = nodo.x * scale
                    val y = nodo.y * scale
                    drawCircle(Color.LightGray, radius = 12f, center = Offset(x, y))
                    // Etiqueta pequeña
                    val text = nodo.nombre.take(10)
                    val style = TextStyle(color = Color.Black, fontSize = 8.sp)
                    val textLayout = textMeasurer.measure(text, style)
                    drawText(textLayout, topLeft = Offset(x - textLayout.size.width/2f, y - 16f))
                }

                // Dibujar aristas de toda la red (tenue)
                for (arista in MapDataLoader.aristas) {
                    val o = nodos.find { it.id == arista.origen } ?: continue
                    val d = nodos.find { it.id == arista.destino } ?: continue
                    drawLine(
                        Color.Gray.copy(alpha = 0.3f),
                        Offset(o.x * scale, o.y * scale),
                        Offset(d.x * scale, d.y * scale),
                        strokeWidth = 2f
                    )
                }

                // Punto verde del usuario (más grande)
                drawCircle(
                    Color.Green,
                    radius = 16f,
                    center = Offset(currentNodo.x * scale, currentNodo.y * scale)
                )
            }

            // Instrucciones abajo
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
            ) {
                if (siguienteNodo != null) {
                    Text("Dirígete hacia: ${siguienteNodo.nombre}")
                } else {
                    Text("¡Has llegado a tu destino: ${currentNodo.nombre}!")
                }
            }
        }
    }
}