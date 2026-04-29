package com.example.uammap.screens

//La clase contiene el mapa interactivo del campus.
//Muestra un grafo que permite arrastrar para desplazarse sobre el mapa, o tocar sobre un edificio para calcular la ruta desde el punto de partida, el cuál, es considerado la caja (A).

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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.uammap.model.TipoNodo
import com.example.uammap.navigation.Screen
import com.example.uammap.utils.GrafoCampus
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val nodos = GrafoCampus.nodos
    val aristas = GrafoCampus.aristas
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    var offset by remember { mutableStateOf(Offset.Zero) }
    var mensajePorDefecto by remember { mutableStateOf<String?>(null) }

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
                    .pointerInput(Unit) {
                        detectTapGestures { tapOffset ->
                            val densidad = density.density
                            val radioPx = 20f * densidad
                            var nodoTocado: String? = null
                            for (nodo in nodos) {
                                val nx = nodo.x * densidad + offset.x
                                val ny = nodo.y * densidad + offset.y
                                val dx = tapOffset.x - nx
                                val dy = tapOffset.y - ny
                                if (sqrt(dx * dx + dy * dy) <= radioPx) {
                                    nodoTocado = nodo.id
                                    break
                                }
                            }
                            if (nodoTocado != null) {
                                val origenId = "A"
                                val origenNombre = nodos.find { it.id == origenId }?.nombre ?: "Caja"
                                val destinoNombre = nodos.find { it.id == nodoTocado }?.nombre ?: nodoTocado
                                navController.navigate(
                                    Screen.Route.createRoute(origenId, nodoTocado, origenNombre, destinoNombre)
                                )
                            } else {
                                mensajePorDefecto = "No se encontró edificio en esa posición"
                            }
                        }
                    }
            ) {
                val densidad = density.density

                for (arista in aristas) {
                    val origen = nodos.find { it.id == arista.origen } ?: continue
                    val destino = nodos.find { it.id == arista.destino } ?: continue
                    drawLine(
                        color = Color.Gray,
                        start = Offset(
                            origen.x * densidad + offset.x,
                            origen.y * densidad + offset.y
                        ),
                        end = Offset(
                            destino.x * densidad + offset.x,
                            destino.y * densidad + offset.y
                        ),
                        strokeWidth = 4f
                    )
                }

                for (nodo in nodos) {
                    val nx = nodo.x * densidad + offset.x
                    val ny = nodo.y * densidad + offset.y
                    val circleColor = when (nodo.tipo) {
                        TipoNodo.CAJA -> Color(0xFFFFA000)
                        TipoNodo.BIBLIOTECA -> Color(0xFF1E88E5)
                        TipoNodo.CAFETERIA -> Color(0xFFE53935)
                        else -> Color(0xFF43A047)
                    }
                    drawCircle(
                        color = circleColor,
                        radius = 20f * densidad,
                        center = Offset(nx, ny)
                    )

                    val textLayoutResult = textMeasurer.measure(
                        text = nodo.id,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
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
            }
            
            nodos.filter { it.tipo != TipoNodo.EDIFICIO }.forEach { nodo ->
                Text(
                    text = nodo.nombre,
                    modifier = Modifier
                        .offset(
                            x = (nodo.x + offset.x / density.density).dp,
                            y = (nodo.y + 20 + offset.y / density.density).dp
                        )
                        .background(Color.White.copy(alpha = 0.8f))
                        .padding(2.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Button(
                onClick = { navController.navigate("search") },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text("Buscar destino")
            }

            mensajePorDefecto?.let {
                AlertDialog(
                    onDismissRequest = { mensajePorDefecto = null },
                    title = { Text("Información") },
                    text = { Text(it) },
                    confirmButton = {
                        TextButton(onClick = { mensajePorDefecto = null }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}