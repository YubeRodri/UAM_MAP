package com.example.uammap.screens

//Es una pantalla encargada de simular la navegación en tiempo real.
//Muestra el grafo del campus, junto a un punto verde animado, que avanza cada 2 segundos al siguiente nodo de la ruta.
//Muestra un mensaje de confirmación al llegar al final de la ruta.

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.uammap.utils.GrafoCampus
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationScreen(navController: NavController, rutaNodesStr: String, origenId: String) {
    val nodeIds = remember { rutaNodesStr.split(",") }
    var currentIndex by remember { mutableIntStateOf(0) }
    val nodos = GrafoCampus.nodos
    val currentNodo = nodos.find { it.id == nodeIds[currentIndex] } ?: return
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

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
                val densityPx = density.density

                for (arista in GrafoCampus.aristas) {
                    val o = nodos.find { it.id == arista.origen } ?: continue
                    val d = nodos.find { it.id == arista.destino } ?: continue
                    drawLine(
                        Color.Gray,
                        Offset(o.x * densityPx, o.y * densityPx),
                        Offset(d.x * densityPx, d.y * densityPx),
                        strokeWidth = 4f
                    )
                }

                for (nodo in nodos) {
                    val nx = nodo.x * densityPx
                    val ny = nodo.y * densityPx
                    drawCircle(Color.LightGray, radius = 16f * densityPx, center = Offset(nx, ny))
                    val textLayoutResult = textMeasurer.measure(
                        text = nodo.id,
                        style = TextStyle(
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    )
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(
                            nx - textLayoutResult.size.width / 2f,
                            ny - textLayoutResult.size.height / 2f
                        )
                    )
                }

                drawCircle(
                    Color.Green,
                    radius = 24f * densityPx,
                    center = Offset(currentNodo.x * densityPx, currentNodo.y * densityPx)
                )
            }

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