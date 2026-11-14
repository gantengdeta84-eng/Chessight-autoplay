package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.engine.AnalysisCoordinator
import com.example.data.firebase.configureAppCheck
import com.example.data.local.ChessSightDatabase
import com.example.data.local.HistoryRepository
import com.example.data.local.UserPreferencesRepository
import com.example.data.scanner.FirebaseBoardScanner
import com.example.ui.ChessViewModel
import com.example.ui.screens.AnalysisScreen
import com.example.ui.screens.CameraScannerScreen
import com.example.ui.theme.ChessSightTheme

private object Routes {
    const val ANALYSIS = "analysis"
    const val CAMERA = "camera"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        val db = ChessSightDatabase.getInstance(applicationContext)
        val factory = ChessViewModel.Factory(
            scanner = FirebaseBoardScanner(),
            analysisCoordinator = AnalysisCoordinator(),
            historyRepository = HistoryRepository(db.savedPositionDao()),
            preferencesRepository = UserPreferencesRepository(applicationContext)
        )

        setContent {
            ChessSightTheme {
                val navController = rememberNavController()
                val chessViewModel: ChessViewModel = viewModel(factory = factory)
                val uiState by chessViewModel.uiState.collectAsStateWithLifecycle()

                NavHost(
                    navController = navController,
                    startDestination = Routes.ANALYSIS
                ) {
                    composable(Routes.ANALYSIS) {
                        AnalysisScreen(
                            viewModel = chessViewModel,
                            onOpenCamera = {
                                navController.navigate(Routes.CAMERA) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable(Routes.CAMERA) {
                        CameraScannerScreen(
                            initialSideToMove = uiState.position.turn,
                            initialWhiteBottom = !uiState.isFlipped,
                            onCaptured = { bitmap, sideToMove, whiteAtBottom ->
                                chessViewModel.scanFromCameraBitmap(bitmap, sideToMove, whiteAtBottom)
                                navController.popBackStack()
                            },
                            onPickFromGallery = { uri ->
                                chessViewModel.scanFromUri(this@MainActivity, uri)
                                navController.popBackStack()
                            },
                            onBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}
