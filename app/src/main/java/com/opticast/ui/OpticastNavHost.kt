package com.opticast.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun OpticastNavHost(vm: StreamViewModel) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "live") {
        composable("live") { LiveScreen(vm, onConnections = { nav.navigate("connections") }) }
        composable("connections") { ConnectionsScreen(vm, onBack = { nav.popBackStack() }) }
    }
}
