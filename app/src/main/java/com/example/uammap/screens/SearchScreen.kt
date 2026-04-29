package com.example.uammap.screens

//Es una pantalla de búsqueda, que cuenta con un campo de texto y una lista filtrable de todos los edificios y puntos de interés.

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.uammap.navigation.Screen
import com.example.uammap.utils.GrafoCampus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController) {
    var query by remember { mutableStateOf("") }
    val pois = GrafoCampus.puntosInteres
    val nodos = GrafoCampus.nodos

    val items = buildList {
        addAll(pois.map { it.nombre to it.nodoAsociado })
        addAll(nodos.map { it.nombre to it.id })
    }.distinctBy { it.first }

    val filtrados = if (query.isEmpty()) items else items.filter {
        it.first.contains(query, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Buscar destino") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Escribe un edificio o lugar") },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
            LazyColumn {
                items(filtrados) { (nombre, nodoId) ->
                    ListItem(
                        headlineContent = { Text(nombre) },
                        modifier = Modifier.clickable {
                            val origenId = "A"  // Origen por defecto (Caja)
                            val origenNombre = nodos.find { it.id == origenId }?.nombre ?: "Origen"
                            navController.navigate(
                                Screen.Route.createRoute(origenId, nodoId, origenNombre, nombre)
                            )
                        }
                    )
                }
            }
        }
    }
}