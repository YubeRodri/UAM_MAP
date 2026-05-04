package com.example.uammap.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.uammap.navigation.Screen
import com.example.uammap.utils.CalculadorRutas
import com.example.uammap.utils.MapDataLoader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteScreen(
    navController: NavController,
    origenId: String,
    destinoId: String,
    origenNombre: String,
    destinoNombre: String
) {
    val context = LocalContext.current

    // Asegurar que los datos estén cargados (por si se accede directamente)
    LaunchedEffect(Unit) {
        MapDataLoader.load(context)
    }

    // Esperar a que los datos estén listos antes de calcular la ruta
    val ruta = remember(MapDataLoader.nodos, MapDataLoader.aristas, origenId, destinoId) {
        if (MapDataLoader.nodos.isNotEmpty()) {
            CalculadorRutas.calcularRuta(
                MapDataLoader.nodos,
                MapDataLoader.aristas,
                origenId,
                destinoId
            )
        } else null
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
                if (MapDataLoader.nodos.isEmpty()) {
                    Text("Cargando datos del mapa...")
                } else {
                    Text("No se pudo calcular una ruta entre $origenNombre y $destinoNombre")
                }
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
        }
    }
}