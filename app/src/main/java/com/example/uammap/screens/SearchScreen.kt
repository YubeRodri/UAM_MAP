package com.example.uammap.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.uammap.navigation.Screen
import com.example.uammap.utils.MapDataLoader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { MapDataLoader.load(context) }

    val edificios = MapDataLoader.edificios
    val filtrados = if (query.isEmpty()) {
        edificios.map { it.name to edificios.indexOf(it).toString() }
    } else {
        edificios.filter { it.name.contains(query, ignoreCase = true) }
            .map { it.name to edificios.indexOf(it).toString() }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            LazyColumn {
                items(filtrados) { (nombre, nodoId) ->
                    ListItem(
                        headlineContent = { Text(nombre) },
                        modifier = Modifier.clickable {
                            val origenId = "0"
                            val origen = edificios.firstOrNull()?.name ?: "Origen"
                            navController.navigate(
                                Screen.Route.createRoute(origenId, nodoId, origen, nombre)
                            )
                        }
                    )
                }
            }
        }
    }
}