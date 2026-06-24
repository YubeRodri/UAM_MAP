package com.example.uammap.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
}