package com.example.nisnocu.Screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

@Composable
fun NavGraph(navController:NavHostController){
    NavHost(
        navController=navController,
        startDestination = "login"){
        composable("login") { LoginScreen(navController) }
        composable("register") { RegistrationScreen(navController) }
        composable("Map"){ MapScreen(navController) }
        composable("rangLista"){ LeaderboardScreen(navController) }
        composable(
            route = "Kafic/{cafeId}/{currentUserId}",
            arguments = listOf(
                navArgument("cafeId") { type = NavType.StringType },
                navArgument("currentUserId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val cafeId = backStackEntry.arguments?.getString("cafeId") ?: ""
            val currentUserId = backStackEntry.arguments?.getString("currentUserId") ?: ""
            CafeScreen(navController, cafeId, currentUserId)
        }
        composable(
            route = "User/{userId}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            UserScreen(navController, userId)
        }

        composable(
            route = "search/{currentUserId}/{userLat}/{userLon}",
            arguments = listOf(
                navArgument("currentUserId") { type = NavType.StringType },
                navArgument("userLat") { type = NavType.FloatType },
                navArgument("userLon") { type = NavType.FloatType }
            )
        ) { backStackEntry ->
            val currentUserId = backStackEntry.arguments?.getString("currentUserId") ?: ""
            val userLat = backStackEntry.arguments?.getFloat("userLat")?.toDouble() ?: 0.0
            val userLon = backStackEntry.arguments?.getFloat("userLon")?.toDouble() ?: 0.0
            SearchScreen(navController, currentUserId, userLat, userLon)
        }


    }
}