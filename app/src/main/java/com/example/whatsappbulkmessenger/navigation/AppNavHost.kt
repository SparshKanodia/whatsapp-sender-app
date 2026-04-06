package com.example.whatsappbulkmessenger.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.whatsappbulkmessenger.ui.upload.UploadScreenRoute

private object Routes {
    const val upload = "upload"
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.upload
    ) {
        composable(Routes.upload) {
            UploadScreenRoute()
        }
    }
}
