package com.example.uammap.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object Route : Screen("route/{origenId}/{destinoId}/{origenNombre}/{destinoNombre}") {
        fun createRoute(origenId: String, destinoId: String, origenNombre: String, destinoNombre: String) =
            "route/$origenId/$destinoId/$origenNombre/$destinoNombre"
    }
    object Navigation : Screen("navigation/{rutaNodes}/{origenId}") {
        fun createRoute(rutaNodes: String, origenId: String) =
            "navigation/$rutaNodes/$origenId"
    }
}