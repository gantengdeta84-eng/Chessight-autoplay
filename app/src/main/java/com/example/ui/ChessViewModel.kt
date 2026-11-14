package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.chess.BoardValidation
import com.example.chess.ChessGameRules
import com.example.chess.ChessMove
import com.example.chess.ChessPosition
import com.example.chess.EngineAnalysisResult
import com.example.chess.EngineLine
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.chess.Square
import com.example.data.engine.AnalysisCoordinator
import com.example.data.local.EnginePreferences
import com.example.data.local.HistoryRepository
import com.example.data.local.SavedPositionEntity
import com.example.data.local.UserPreferencesRepository
import com.example.data.scanner.FirebaseBoardScanner
import com.example.data.scanner.ImageUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "ChessViewModel"

sealed interface ScanUiState {
    data object Idle : ScanUiState
    data class Scanning(val message: String) : ScanUiState
    data class Success(val note: String) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

sealed interface AnalysisUiState {
    data object Idle : AnalysisUiState
    data object Analyzing : AnalysisUiState
    data class Ready(val result: EngineAnalysisResult) : AnalysisUiState
    data class Error(val message: String) : AnalysisUiState
}

sealed interface ExplanationUiState {
    data object Idle : ExplanationUiState
    data class Streaming(val text: String) : ExplanationUiState
    data class Ready(val text: String) : ExplanationUiState
    data class Error(val message: String) : ExplanationUiState
}

data class MainUiState(
    val position: ChessPosition = ChessPosition.starting(),
    val basePosition: ChessPosition = ChessPosition.starting(),
    val moveHistory: List<Pair<ChessPosition, ChessMove?>> = emptyList(),
    val isFlipped: Boolean = false,
    val selectedSquare: Square? = null,
    val legalTargets: List<Square> = emptyList(),
    val pendingPromotion: Pair<Square, Square>? = null,
    val lastMove: ChessMove? = null,
    val scanState: ScanUiState = ScanUiState.Idle,
    val analysisState: AnalysisUiState = AnalysisUiState.Idle,
    val explanationState: ExplanationUiState = ExplanationUiState.Idle,
    val showArrows: Boolean = true
)

class ChessViewModel(
    private val scanner: FirebaseBoardScanner,
    private val analysisCoordinator: AnalysisCoordinator,
    private val historyRepository: HistoryRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val preferences: StateFlow<EnginePreferences> = preferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EnginePreferences())

    val savedPositions: StateFlow<List<SavedPositionEntity>> = historyRepository.recentPositions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var analysisJob: Job? = null
    private var explanationJob: Job? = null

    init {
        viewModelScope.launch {
            preferencesRepository.preferences.collectLatest { prefs ->
                _uiState.update { state ->
                    if (state.moveHistory.isEmpty() && state.position == ChessPosition.starting()) {
                        state.copy(isFlipped = !prefs.defaultWhiteBottom)
                    } else {
                        state
                    }
                }
                triggerAnalysis(_uiState.value.position, prefs)
            }
        }
    }

    fun onSquareTapped(square: Square) {
        val current = _uiState.value
        val selected = current.selectedSquare

        // Check if tapping a legal target from previously selected piece
        if (selected != null && square in current.legalTargets) {
            val movingPiece = current.position.pieceAt(selected)
            val isPromotion = movingPiece?.type == PieceType.PAWN &&
                ((movingPiece.color == PieceColor.WHITE && square.row == 0) ||
                    (movingPiece.color == PieceColor.BLACK && square.row == 7))

            if (isPromotion) {
                _uiState.update { it.copy(pendingPromotion = selected to square) }
                return
            }

            executeMove(ChessMove(selected, square))
            return
        }

        // Otherwise select piece belonging to side-to-move
        val tappedPiece = current.position.pieceAt(square)
        if (tappedPiece != null && tappedPiece.color == current.position.turn && selected != square) {
            val targets = ChessGameRules.generateLegalMovesFrom(current.position, square).map { it.to }
            _uiState.update {
                it.copy(
                    selectedSquare = square,
                    legalTargets = targets
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    selectedSquare = null,
                    legalTargets = emptyList()
                )
            }
        }
    }

    fun onPromotionSelected(pieceType: PieceType) {
        val pending = _uiState.value.pendingPromotion ?: return
        _uiState.update { it.copy(pendingPromotion = null) }
        executeMove(ChessMove(pending.first, pending.second, pieceType))
    }

    fun dismissPromotion() {
        _uiState.update { it.copy(pendingPromotion = null, selectedSquare = null, legalTargets = emptyList()) }
    }

    private fun executeMove(move: ChessMove) {
        val current = _uiState.value
        val nextPos = ChessGameRules.applyMove(current.position, move)
        val updatedHistory = current.moveHistory + (current.position to current.lastMove)

        _uiState.update {
            it.copy(
                position = nextPos,
                moveHistory = updatedHistory,
                selectedSquare = null,
                legalTargets = emptyList(),
                lastMove = move,
                explanationState = ExplanationUiState.Idle
            )
        }
        triggerAnalysis(nextPos, preferences.value)
    }

    fun playContinuationMove(line: EngineLine, moveIndex: Int) {
        var cursorPos = _uiState.value.position
        val historyAdditions = mutableListOf<Pair<ChessPosition, ChessMove?>>()
        var lastPlayedMove = _uiState.value.lastMove

        for (i in 0..moveIndex) {
            val san = line.continuationSan.getOrNull(i) ?: break
            val move = if (i == 0) {
                ChessGameRules.uciToChessMove(cursorPos, line.bestMoveUci)
                    ?: ChessGameRules.sanToChessMove(cursorPos, san)
            } else {
                ChessGameRules.sanToChessMove(cursorPos, san)
            } ?: break

            historyAdditions.add(cursorPos to lastPlayedMove)
            cursorPos = ChessGameRules.applyMove(cursorPos, move)
            lastPlayedMove = move
        }

        if (historyAdditions.isEmpty()) return

        _uiState.update {
            it.copy(
                position = cursorPos,
                moveHistory = it.moveHistory + historyAdditions,
                selectedSquare = null,
                legalTargets = emptyList(),
                lastMove = lastPlayedMove,
                explanationState = ExplanationUiState.Idle
            )
        }
        triggerAnalysis(cursorPos, preferences.value)
    }

    fun undoMove() {
        val current = _uiState.value
        if (current.moveHistory.isEmpty()) return
        val (prevPos, prevLastMove) = current.moveHistory.last()
        _uiState.update {
            it.copy(
                position = prevPos,
                moveHistory = current.moveHistory.dropLast(1),
                selectedSquare = null,
                legalTargets = emptyList(),
                lastMove = prevLastMove,
                explanationState = ExplanationUiState.Idle
            )
        }
        triggerAnalysis(prevPos, preferences.value)
    }

    fun resetToBasePosition() {
        val base = _uiState.value.basePosition
        _uiState.update {
            it.copy(
                position = base,
                moveHistory = emptyList(),
                selectedSquare = null,
                legalTargets = emptyList(),
                lastMove = null,
                explanationState = ExplanationUiState.Idle
            )
        }
        triggerAnalysis(base, preferences.value)
    }

    fun resetToStartingBoard() {
        val start = ChessPosition.starting()
        _uiState.update {
            it.copy(
                position = start,
                basePosition = start,
                moveHistory = emptyList(),
                selectedSquare = null,
                legalTargets = emptyList(),
                lastMove = null,
                scanState = ScanUiState.Idle,
                explanationState = ExplanationUiState.Idle
            )
        }
        triggerAnalysis(start, preferences.value)
    }

    fun flipBoard() {
        _uiState.update { it.copy(isFlipped = !it.isFlipped) }
    }

    fun toggleArrows() {
        _uiState.update { it.copy(showArrows = !it.showArrows) }
    }

    fun toggleTurn() {
        val current = _uiState.value.position
        val updated = current.copy(turn = current.turn.opposite(), enPassantTarget = null)
        _uiState.update {
            it.copy(
                position = updated,
                selectedSquare = null,
                legalTargets = emptyList(),
                explanationState = ExplanationUiState.Idle
            )
        }
        triggerAnalysis(updated, preferences.value)
    }

    fun applyEditedPosition(edited: ChessPosition) {
        _uiState.update {
            it.copy(
                position = edited,
                basePosition = edited,
                moveHistory = emptyList(),
                selectedSquare = null,
                legalTargets = emptyList(),
                lastMove = null,
                scanState = ScanUiState.Success("Posisi editor diterapkan."),
                explanationState = ExplanationUiState.Idle
            )
        }
        triggerAnalysis(edited, preferences.value)
    }

    fun scanFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(scanState = ScanUiState.Scanning("Memuat gambar dari galeri…")) }
            val bitmap = ImageUtils.decodeAndScaleFromUri(context, uri)
            if (bitmap == null) {
                _uiState.update { it.copy(scanState = ScanUiState.Error("Gagal membuka gambar yang dipilih.")) }
                return@launch
            }
            val currentTurn = _uiState.value.position.turn
            val whiteBottom = !_uiState.value.isFlipped
            scanBitmapInternal(bitmap, currentTurn, whiteBottom, sourceLabel = "Galeri")
        }
    }

    fun scanFromCameraBitmap(bitmap: Bitmap, sideToMove: PieceColor, whiteAtBottom: Boolean) {
        viewModelScope.launch {
            scanBitmapInternal(bitmap, sideToMove, whiteAtBottom, sourceLabel = "Kamera")
        }
    }

    private suspend fun scanBitmapInternal(
        bitmap: Bitmap,
        sideToMove: PieceColor,
        whiteAtBottom: Boolean,
        sourceLabel: String
    ) {
        _uiState.update {
            it.copy(
                scanState = ScanUiState.Scanning("Membaca 64 petak papan dengan Gemini AI…"),
                explanationState = ExplanationUiState.Idle
            )
        }

        val scanResult = scanner.scanBoard(
            bitmap = bitmap,
            sideToMove = sideToMove,
            whiteAtBottomHint = whiteAtBottom
        )

        scanResult.fold(
            onSuccess = { parsed ->
                val newPos = ChessPosition.fromFen(parsed.fen)
                if (newPos == null) {
                    _uiState.update {
                        it.copy(scanState = ScanUiState.Error("Hasil FEN tidak valid. Silakan koreksi melalui Editor."))
                    }
                    return
                }
                val flipped = parsed.bottomSide == PieceColor.BLACK
                _uiState.update {
                    it.copy(
                        position = newPos,
                        basePosition = newPos,
                        moveHistory = emptyList(),
                        isFlipped = flipped,
                        selectedSquare = null,
                        legalTargets = emptyList(),
                        lastMove = null,
                        scanState = ScanUiState.Success(parsed.confidenceNote)
                    )
                }

                // Analyze & auto-save scan to local history
                val prefs = preferences.value
                val result = runAnalysisSuspend(newPos, prefs)
                if (result != null) {
                    val top = result.lines.firstOrNull()
                    historyRepository.savePosition(
                        fen = newPos.toFen(),
                        label = "Pindaian $sourceLabel",
                        bestMoveSan = top?.bestMoveSan ?: "-",
                        evalText = top?.formattedEval() ?: "0.0",
                        sourceLabel = sourceLabel
                    )
                }
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(
                        scanState = ScanUiState.Error(
                            error.localizedMessage ?: "Gagal memindai gambar papan catur."
                        )
                    )
                }
            }
        )
    }

    fun dismissScanBanner() {
        _uiState.update { it.copy(scanState = ScanUiState.Idle) }
    }

    fun triggerCurrentAnalysis() {
        triggerAnalysis(_uiState.value.position, preferences.value)
    }

    private fun triggerAnalysis(position: ChessPosition, prefs: EnginePreferences) {
        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            runAnalysisSuspend(position, prefs)
        }
    }

    private suspend fun runAnalysisSuspend(
        position: ChessPosition,
        prefs: EnginePreferences
    ): EngineAnalysisResult? {
        when (val validation = ChessGameRules.validatePosition(position)) {
            is BoardValidation.Invalid -> {
                _uiState.update { it.copy(analysisState = AnalysisUiState.Error(validation.reason)) }
                return null
            }
            BoardValidation.Valid -> Unit
        }

        _uiState.update { it.copy(analysisState = AnalysisUiState.Analyzing) }
        return try {
            val result = analysisCoordinator.analyze(
                position = position,
                depth = prefs.engineDepth,
                variants = prefs.variantsCount,
                offlineOnly = prefs.offlineMode
            )
            _uiState.update { it.copy(analysisState = AnalysisUiState.Ready(result)) }
            result
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Analysis failed", e)
            _uiState.update {
                it.copy(analysisState = AnalysisUiState.Error(e.localizedMessage ?: "Analisis gagal."))
            }
            null
        }
    }

    fun requestMoveExplanation() {
        val ready = _uiState.value.analysisState as? AnalysisUiState.Ready ?: return
        val topLine = ready.result.lines.firstOrNull() ?: return
        val currentFen = _uiState.value.position.toFen()

        explanationJob?.cancel()
        explanationJob = viewModelScope.launch {
            _uiState.update { it.copy(explanationState = ExplanationUiState.Streaming("")) }
            var latestText = ""
            try {
                scanner.explainMoveStream(
                    fen = currentFen,
                    bestMoveSan = topLine.bestMoveSan,
                    continuationSan = topLine.continuationSan,
                    evalText = topLine.formattedEval()
                ).collect { accumulated ->
                    latestText = accumulated
                    _uiState.update { it.copy(explanationState = ExplanationUiState.Streaming(accumulated)) }
                }
                _uiState.update {
                    it.copy(explanationState = ExplanationUiState.Ready(latestText))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Explanation failed", e)
                _uiState.update {
                    it.copy(
                        explanationState = ExplanationUiState.Error(
                            e.localizedMessage ?: "Gagal memuat penjelasan taktis Gemini."
                        )
                    )
                }
            }
        }
    }

    fun saveCurrentToHistory() {
        viewModelScope.launch {
            val pos = _uiState.value.position
            val ready = _uiState.value.analysisState as? AnalysisUiState.Ready
            val top = ready?.result?.lines?.firstOrNull()
            historyRepository.savePosition(
                fen = pos.toFen(),
                label = "Posisi Tersimpan",
                bestMoveSan = top?.bestMoveSan ?: "-",
                evalText = top?.formattedEval() ?: "0.0",
                sourceLabel = "Manual"
            )
            _uiState.update { it.copy(scanState = ScanUiState.Success("Posisi disimpan ke riwayat.")) }
        }
    }

    fun loadSavedPosition(entity: SavedPositionEntity) {
        val pos = ChessPosition.fromFen(entity.fen) ?: return
        _uiState.update {
            it.copy(
                position = pos,
                basePosition = pos,
                moveHistory = emptyList(),
                selectedSquare = null,
                legalTargets = emptyList(),
                lastMove = null,
                explanationState = ExplanationUiState.Idle
            )
        }
        triggerAnalysis(pos, preferences.value)
    }

    fun deleteSavedPosition(id: Long) {
        viewModelScope.launch { historyRepository.deletePosition(id) }
    }

    fun clearHistory() {
        viewModelScope.launch { historyRepository.clearAll() }
    }

    fun updateDepth(depth: Int) {
        viewModelScope.launch { preferencesRepository.updateEngineDepth(depth) }
    }

    fun updateVariants(count: Int) {
        viewModelScope.launch { preferencesRepository.updateVariantsCount(count) }
    }

    fun updateOfflineMode(offline: Boolean) {
        viewModelScope.launch { preferencesRepository.updateOfflineMode(offline) }
    }

    fun updateDefaultWhiteBottom(whiteBottom: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateDefaultWhiteBottom(whiteBottom)
            _uiState.update { it.copy(isFlipped = !whiteBottom) }
        }
    }

    class Factory(
        private val scanner: FirebaseBoardScanner,
        private val analysisCoordinator: AnalysisCoordinator,
        private val historyRepository: HistoryRepository,
        private val preferencesRepository: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChessViewModel(
                scanner = scanner,
                analysisCoordinator = analysisCoordinator,
                historyRepository = historyRepository,
                preferencesRepository = preferencesRepository
            ) as T
        }
    }
}
