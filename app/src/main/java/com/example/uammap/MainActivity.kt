package com.example.uammap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.uammap.navigation.Screen
import com.example.uammap.screens.*
import com.example.uammap.ui.theme.UAMMapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UAMMapTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = Screen.Home.route) {
                        composable(Screen.Home.route) { HomeScreen(navController) }
                        composable(Screen.Search.route) { SearchScreen(navController) }
                        composable(Screen.Route.route) { backStackEntry ->
                            val origenId = backStackEntry.arguments?.getString("origenId") ?: ""
                            val destinoId = backStackEntry.arguments?.getString("destinoId") ?: ""
                            val origenNombre = backStackEntry.arguments?.getString("origenNombre") ?: ""
                            val destinoNombre = backStackEntry.arguments?.getString("destinoNombre") ?: ""
                            RouteScreen(navController, origenId, destinoId, origenNombre, destinoNombre)
                        }
                        composable(Screen.Navigation.route) { backStackEntry ->
                            val rutaNodes = backStackEntry.arguments?.getString("rutaNodes") ?: ""
                            val origenId = backStackEntry.arguments?.getString("origenId") ?: ""
                            NavigationScreen(navController, rutaNodes, origenId)
                        }
                    }
                }
            }
        }
    }
}