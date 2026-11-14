package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.chess.EngineLine
import com.example.chess.EngineSource
import com.example.chess.PieceColor
import com.example.ui.AnalysisUiState
import com.example.ui.ChessViewModel
import com.example.ui.ExplanationUiState
import com.example.ui.MainUiState
import com.example.ui.ScanUiState
import com.example.ui.components.ChessBoardView
import com.example.ui.components.PromotionPickerDialog
import com.example.ui.components.VerticalEvalBar
import com.example.ui.theme.AmberNotice
import com.example.ui.theme.CoralError
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.TacticalBlue
import com.example.ui.theme.TournamentEmerald

@Composable
fun AnalysisScreen(
    viewModel: ChessViewModel,
    onOpenCamera: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val history by viewModel.savedPositions.collectAsStateWithLifecycle()

    var showEditorSheet by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.scanFromUri(context, uri)
        }
    }

    val launchGallery = {
        galleryLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    Scaffold(
        containerColor = ObsidianBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            TopMinimalHeader(
                analysisState = uiState.analysisState,
                engineDepth = preferences.engineDepth,
                onSaveClick = { viewModel.saveCurrentToHistory() },
                onHistoryClick = { showHistorySheet = true },
                onSettingsClick = { showSettingsSheet = true }
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp)
            ) {
                val isWide = maxWidth >= 680.dp

                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            BoardWithEvalSection(
                                uiState = uiState,
                                onSquareTapped = viewModel::onSquareTapped,
                                onToggleTurn = viewModel::toggleTurn,
                                onToggleArrows = viewModel::toggleArrows,
                                onUndo = viewModel::undoMove,
                                onResetBase = viewModel::resetToBasePosition,
                                onFlip = viewModel::flipBoard
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ScanStatusBanner(
                                scanState = uiState.scanState,
                                onEditClick = { showEditorSheet = true },
                                onDismiss = viewModel::dismissScanBanner
                            )
                            PrimaryActionStrip(
                                isScanning = uiState.scanState is ScanUiState.Scanning,
                                onCameraClick = onOpenCamera,
                                onGalleryClick = launchGallery,
                                onEditClick = { showEditorSheet = true }
                            )
                            BestMoveAnalysisPanel(
                                uiState = uiState,
                                onLineMoveClick = viewModel::playContinuationMove,
                                onRetryAnalysis = viewModel::triggerCurrentAnalysis,
                                onExplainMove = viewModel::requestMoveExplanation,
                                onResetStartBoard = viewModel::resetToStartingBoard
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .widthIn(max = 600.dp)
                            .align(Alignment.TopCenter)
                            .padding(bottom = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ScanStatusBanner(
                            scanState = uiState.scanState,
                            onEditClick = { showEditorSheet = true },
                            onDismiss = viewModel::dismissScanBanner
                        )

                        BoardWithEvalSection(
                            uiState = uiState,
                            onSquareTapped = viewModel::onSquareTapped,
                            onToggleTurn = viewModel::toggleTurn,
                            onToggleArrows = viewModel::toggleArrows,
                            onUndo = viewModel::undoMove,
                            onResetBase = viewModel::resetToBasePosition,
                            onFlip = viewModel::flipBoard
                        )

                        PrimaryActionStrip(
                            isScanning = uiState.scanState is ScanUiState.Scanning,
                            onCameraClick = onOpenCamera,
                            onGalleryClick = launchGallery,
                            onEditClick = { showEditorSheet = true }
                        )

                        BestMoveAnalysisPanel(
                            uiState = uiState,
                            onLineMoveClick = viewModel::playContinuationMove,
                            onRetryAnalysis = viewModel::triggerCurrentAnalysis,
                            onExplainMove = viewModel::requestMoveExplanation,
                            onResetStartBoard = viewModel::resetToStartingBoard
                        )
                    }
                }
            }
        }
    }

    if (uiState.pendingPromotion != null) {
        PromotionPickerDialog(
            color = uiState.position.turn,
            onSelect = viewModel::onPromotionSelected,
            onDismiss = viewModel::dismissPromotion
        )
    }

    if (showEditorSheet) {
        BoardEditorSheet(
            initialPosition = uiState.position,
            isFlipped = uiState.isFlipped,
            onApply = { edited ->
                viewModel.applyEditedPosition(edited)
                showEditorSheet = false
            },
            onDismiss = { showEditorSheet = false }
        )
    }

    if (showHistorySheet) {
        HistoryBottomSheet(
            items = history,
            onSelect = { entity ->
                viewModel.loadSavedPosition(entity)
                showHistorySheet = false
            },
            onDelete = viewModel::deleteSavedPosition,
            onClearAll = viewModel::clearHistory,
            onDismiss = { showHistorySheet = false }
        )
    }

    if (showSettingsSheet) {
        SettingsBottomSheet(
            preferences = preferences,
            onDepthChange = viewModel::updateDepth,
            onVariantsChange = viewModel::updateVariants,
            onOfflineChange = viewModel::updateOfflineMode,
            onDefaultWhiteBottomChange = viewModel::updateDefaultWhiteBottom,
            onDismiss = { showSettingsSheet = false }
        )
    }
}

@Composable
private fun TopMinimalHeader(
    analysisState: AnalysisUiState,
    engineDepth: Int,
    onSaveClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall
                )
                EngineStatusChip(analysisState = analysisState, depth = engineDepth)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onSaveClick,
                modifier = Modifier.testTag("header_save_button")
            ) {
                Icon(
                    imageVector = Icons.Default.BookmarkAdd,
                    contentDescription = "Simpan posisi",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onHistoryClick,
                modifier = Modifier.testTag("header_history_button")
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = stringResource(R.string.action_history),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.testTag("header_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.action_settings),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EngineStatusChip(analysisState: AnalysisUiState, depth: Int) {
    val (label, dotColor) = when (analysisState) {
        AnalysisUiState.Analyzing -> stringResource(R.string.engine_analyzing) to AmberNotice
        is AnalysisUiState.Ready -> {
            val res = analysisState.result
            val text = if (res.source == EngineSource.STOCKFISH_CLOUD) {
                "Stockfish D${res.depth}"
            } else {
                "Lokal D${res.depth}"
            }
            val color = if (res.source == EngineSource.STOCKFISH_CLOUD) TournamentEmerald else TacticalBlue
            text to color
        }
        is AnalysisUiState.Error -> "Periksa Papan" to CoralError
        AnalysisUiState.Idle -> "Stockfish D$depth" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@Composable
private fun ScanStatusBanner(
    scanState: ScanUiState,
    onEditClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = scanState !is ScanUiState.Idle,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        when (scanState) {
            is ScanUiState.Scanning -> {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = TournamentEmerald
                            )
                            Text(
                                text = scanState.message,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = TournamentEmerald,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }

            is ScanUiState.Success -> {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TournamentEmerald.copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = scanState.note,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TournamentEmerald,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = TournamentEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            is ScanUiState.Error -> {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = scanState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onEditClick) {
                            Text("Edit", color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            ScanUiState.Idle -> Unit
        }
    }
}

@Composable
private fun BoardWithEvalSection(
    uiState: MainUiState,
    onSquareTapped: (com.example.chess.Square) -> Unit,
    onToggleTurn: () -> Unit,
    onToggleArrows: () -> Unit,
    onUndo: () -> Unit,
    onResetBase: () -> Unit,
    onFlip: () -> Unit
) {
    val lines = (uiState.analysisState as? AnalysisUiState.Ready)?.result?.lines.orEmpty()
    val topLine = lines.firstOrNull()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VerticalEvalBar(
                topLine = topLine,
                isFlipped = uiState.isFlipped,
                modifier = Modifier.fillMaxHeight()
            )

            ChessBoardView(
                position = uiState.position,
                isFlipped = uiState.isFlipped,
                selectedSquare = uiState.selectedSquare,
                legalTargets = uiState.legalTargets,
                lastMove = uiState.lastMove,
                engineLines = lines,
                showArrows = uiState.showArrows,
                onSquareTapped = onSquareTapped,
                modifier = Modifier.weight(1f)
            )
        }

        // Compact Board Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Turn Toggle Pill
            val isWhiteTurn = uiState.position.turn == PieceColor.WHITE
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50))
                    .clickable(onClick = onToggleTurn)
                    .padding(horizontal = 12.dp, vertical = 7.dp)
                    .testTag("turn_toggle_pill"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isWhiteTurn) Color(0xFFF0F6FC) else Color(0xFF21262D))
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
                Text(
                    text = if (isWhiteTurn) stringResource(R.string.turn_white) else stringResource(R.string.turn_black),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleArrows,
                    modifier = Modifier.testTag("toggle_arrows_button")
                ) {
                    Icon(
                        imageVector = if (uiState.showArrows) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Tampilkan panah best move",
                        tint = if (uiState.showArrows) TournamentEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onUndo,
                    enabled = uiState.moveHistory.isNotEmpty(),
                    modifier = Modifier.testTag("undo_move_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Mundur satu langkah"
                    )
                }
                IconButton(
                    onClick = onResetBase,
                    enabled = uiState.moveHistory.isNotEmpty(),
                    modifier = Modifier.testTag("reset_base_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Kembali ke posisi awal pindaian"
                    )
                }
                IconButton(
                    onClick = onFlip,
                    modifier = Modifier.testTag("flip_board_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FlipCameraAndroid,
                        contentDescription = stringResource(R.string.action_flip_board)
                    )
                }
            }
        }
    }
}

@Composable
private fun PrimaryActionStrip(
    isScanning: Boolean,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onCameraClick,
            enabled = !isScanning,
            modifier = Modifier
                .weight(1.15f)
                .height(48.dp)
                .testTag("scan_camera_button"),
            colors = ButtonDefaults.buttonColors(containerColor = TournamentEmerald),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.action_scan_camera), fontWeight = FontWeight.SemiBold, maxLines = 1)
        }

        FilledTonalButton(
            onClick = onGalleryClick,
            enabled = !isScanning,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("scan_gallery_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.action_scan_gallery), maxLines = 1)
        }

        OutlinedButton(
            onClick = onEditClick,
            modifier = Modifier
                .weight(0.85f)
                .height(48.dp)
                .testTag("edit_board_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.action_edit_board), maxLines = 1)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BestMoveAnalysisPanel(
    uiState: MainUiState,
    onLineMoveClick: (EngineLine, Int) -> Unit,
    onRetryAnalysis: () -> Unit,
    onExplainMove: () -> Unit,
    onResetStartBoard: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (val analysis = uiState.analysisState) {
                AnalysisUiState.Idle, AnalysisUiState.Analyzing -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = TournamentEmerald
                        )
                        Text(
                            text = "Menghitung langkah terbaik…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is AnalysisUiState.Error -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = analysis.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CoralError
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onRetryAnalysis) {
                                Text("Coba Lagi")
                            }
                            TextButton(onClick = onResetStartBoard) {
                                Text("Reset Awal")
                            }
                        }
                    }
                }

                is AnalysisUiState.Ready -> {
                    val result = analysis.result
                    if (result.lines.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_moves_available),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val top = result.lines.first()

                        // Hero Best Move Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = stringResource(R.string.best_move_label),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TournamentEmerald
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = top.bestMoveSan,
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 26.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .clickable { onLineMoveClick(top, 0) }
                                            .testTag("best_move_primary_san")
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(TournamentEmerald.copy(alpha = 0.16f))
                                            .padding(horizontal = 9.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = top.formattedEval(),
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = TournamentEmerald,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }

                            FilledTonalButton(
                                onClick = onExplainMove,
                                enabled = uiState.explanationState !is ExplanationUiState.Streaming,
                                modifier = Modifier
                                    .height(42.dp)
                                    .testTag("explain_move_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = TournamentEmerald
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Jelaskan", fontSize = 13.sp)
                            }
                        }

                        if (!result.fallbackNotice.isNullOrBlank()) {
                            Text(
                                text = result.fallbackNotice,
                                style = MaterialTheme.typography.labelMedium,
                                color = AmberNotice
                            )
                        }

                        // Principal Variations List
                        result.lines.forEach { line ->
                            val isPrimary = line.rank == 1
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isPrimary) 0.7f else 0.4f))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "#${line.rank}  ${line.bestMoveSan}",
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPrimary) TournamentEmerald else TacticalBlue,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Text(
                                        text = "${line.formattedEval()} • ${line.winChanceWhite.toInt()}% Putih",
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    line.continuationSan.forEachIndexed { idx, san ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.surface)
                                                .clickable { onLineMoveClick(line, idx) }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                                .testTag("pv_${line.rank}_move_$idx")
                                        ) {
                                            Text(
                                                text = san,
                                                fontFamily = JetBrainsMonoFamily,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Gemini Tactical Move Explanation
                        when (val exp = uiState.explanationState) {
                            ExplanationUiState.Idle -> Unit
                            is ExplanationUiState.Streaming -> {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = TournamentEmerald.copy(alpha = 0.09f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = exp.text.ifBlank { "Menyusun penjelasan taktis…" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                            is ExplanationUiState.Ready -> {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = TournamentEmerald.copy(alpha = 0.12f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = exp.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                            is ExplanationUiState.Error -> {
                                Text(
                                    text = exp.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CoralError
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
