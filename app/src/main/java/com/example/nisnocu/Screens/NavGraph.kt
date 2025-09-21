package com.example.nisnocu.Screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun NavGraph(navController:NavHostController){
    NavHost(
        navController=navController,
        startDestination = "login"){
        composable("login") { LoginScreen(navController) }
        composable("register") { RegistrationScreen(navController) }
        composable("Map"){ MapScreen(navController) }

    }
}