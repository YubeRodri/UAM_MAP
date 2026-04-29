package com.example.uammap.screens

//La pantalla muestra el resultado de la ruta calculada.
//Muestra los datos de origen, destino, distancia total y la secuencia de nodos seguidas para llegar desde el punto destino hasta el punto final.

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.uammap.navigation.Screen
import com.example.uammap.utils.CalculadorRutas
import com.example.uammap.utils.GrafoCampus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteScreen(
    navController: NavController,
    origenId: String,
    destinoId: String,
    origenNombre: String,
    destinoNombre: String
) {
    val ruta = remember {
        CalculadorRutas.calcularRuta(GrafoCampus.nodos, GrafoCampus.aristas, origenId, destinoId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ruta encontrada") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            if (ruta == null) {
                Text("No se pudo calcular una ruta entre $origenNombre y $destinoNombre")
                return@Column
            }
            Text("Desde: $origenNombre", style = MaterialTheme.typography.titleMedium)
            Text("Hasta: $destinoNombre", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Distancia total: ${"%.0f".format(ruta.distanciaTotal)} m")
            Spacer(modifier = Modifier.height(8.dp))
            Text("Recorrido: ${ruta.nodos.joinToString(" → ")}")
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    val nodosStr = ruta.nodos.joinToString(",")
                    navController.navigate(Screen.Navigation.createRoute(nodosStr, origenId))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Iniciar navegación")
            }
            OutlinedButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text("Volver al mapa")
            }
        }
    }
}