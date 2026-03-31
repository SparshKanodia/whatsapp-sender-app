package com.internal.wamessenger.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.internal.wamessenger.core.CampaignForegroundService

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WAMessengerTheme {
                AppNavigation(viewModel)
            }
        }
    }
}

object Routes {
    const val HOME     = "home"
    const val PREVIEW  = "preview"
    const val CAMPAIGN = "campaign"
    const val SUMMARY  = "summary"
}

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val campaignState by viewModel.campaignState.collectAsState()

    // Auto-navigate to summary when campaign completes
    LaunchedEffect(campaignState.isComplete) {
        if (campaignState.isComplete) {
            navController.navigate(Routes.SUMMARY) {
                popUpTo(Routes.CAMPAIGN) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onPreview = { navController.navigate(Routes.PREVIEW) },
                onStartCampaign = {
                    navController.navigate(Routes.CAMPAIGN)
                }
            )
        }
        composable(Routes.PREVIEW) {
            PreviewScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onConfirm = {
                    navController.navigate(Routes.CAMPAIGN)
                }
            )
        }
        composable(Routes.CAMPAIGN) {
            CampaignScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSummary = {
                    navController.navigate(Routes.SUMMARY) {
                        popUpTo(Routes.CAMPAIGN) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.SUMMARY) {
            SummaryScreen(
                viewModel = viewModel,
                onNewCampaign = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
