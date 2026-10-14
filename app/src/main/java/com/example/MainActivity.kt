package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.GameScreen
import com.example.ui.screens.GarageScreen
import com.example.ui.screens.SimulationLabScreen
import com.example.ui.theme.ApexCarTheme
import com.example.ui.theme.DarkNavy
import com.example.viewmodel.AppScreen
import com.example.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ApexCarTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkNavy
                ) {
                    CarAppRoot(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun CarAppRoot(viewModel: GameViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    // Handle back button when on sub-screens
    if (uiState.currentScreen != AppScreen.GAME) {
        BackHandler {
            viewModel.navigateTo(AppScreen.GAME)
        }
    }

    when (uiState.currentScreen) {
        AppScreen.GAME -> {
            GameScreen(
                viewModel = viewModel,
                onNavigateToGarage = { viewModel.navigateTo(AppScreen.GARAGE) },
                onNavigateToAdmin = { viewModel.navigateTo(AppScreen.ADMIN_PANEL) },
                onNavigateToLab = { viewModel.navigateTo(AppScreen.AI_LAB) }
            )
        }
        AppScreen.GARAGE -> {
            GarageScreen(
                viewModel = viewModel,
                onBackToGame = { viewModel.navigateTo(AppScreen.GAME) }
            )
        }
        AppScreen.ADMIN_PANEL -> {
            AdminPanelScreen(
                viewModel = viewModel,
                onBackToGame = { viewModel.navigateTo(AppScreen.GAME) }
            )
        }
        AppScreen.AI_LAB -> {
            SimulationLabScreen(
                viewModel = viewModel,
                onBackToGame = { viewModel.navigateTo(AppScreen.GAME) }
            )
        }
    }
}
