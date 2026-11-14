package com.example.data.engine

import android.util.Log
import com.example.chess.ChessGameRules
import com.example.chess.ChessPosition
import com.example.chess.EngineAnalysisResult
import com.example.chess.EngineLine
import com.example.chess.EngineSource
import com.example.chess.LocalMinimaxEngine
import com.example.chess.PieceColor
import com.example.chess.Square
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout

private const val TAG = "AnalysisCoordinator"

class AnalysisCoordinator(
    private val stockfishApi: StockfishApiService = StockfishApiService.create()
) {
    suspend fun analyze(
        position: ChessPosition,
        depth: Int,
        variants: Int,
        offlineOnly: Boolean
    ): EngineAnalysisResult {
        val legalMoves = ChessGameRules.generateLegalMoves(position)
        if (legalMoves.isEmpty()) {
            return EngineAnalysisResult(
                fen = position.toFen(),
                depth = 0,
                source = if (offlineOnly) EngineSource.LOCAL_MINIMAX else EngineSource.STOCKFISH_CLOUD,
                lines = emptyList()
            )
        }

        if (offlineOnly) {
            return LocalMinimaxEngine.analyze(
                position = position,
                maxLines = variants,
                searchDepth = 3
            )
        }

        return try {
            withTimeout(9_000L) {
                fetchStockfishLines(position, depth.coerceIn(8, 18), variants.coerceIn(1, 3))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Stockfish API unavailable, falling back to local engine: ${e.message}")
            LocalMinimaxEngine.analyze(
                position = position,
                maxLines = variants,
                searchDepth = 3,
                fallbackNotice = "Mode offline aktif — menggunakan mesin lokal"
            )
        }
    }

    private suspend fun fetchStockfishLines(
        position: ChessPosition,
        depth: Int,
        variants: Int
    ): EngineAnalysisResult = coroutineScope {
        val fen = position.toFen()
        val primaryResp = stockfishApi.evaluatePosition(
            ChessApiRequest(
                fen = fen,
                depth = depth,
                maxThinkingTime = 80,
                variants = 1
            )
        )

        if (!primaryResp.error.isNullOrBlank()) {
            throw IllegalStateException(primaryResp.error)
        }

        val primaryLine = mapResponseToEngineLine(position, primaryResp, rank = 1)
            ?: throw IllegalStateException("Stockfish tidak mengembalikan langkah valid.")

        val lines = mutableListOf(primaryLine)

        if (variants > 1) {
            val legalMoves = ChessGameRules.generateLegalMoves(position)
            val remainingMoves = legalMoves.filter { it.toUci() != primaryLine.bestMoveUci }
            if (remainingMoves.isNotEmpty()) {
                val secondDeferred = async {
                    try {
                        val searchFilter = remainingMoves.take(25).joinToString(" ") { it.toUci() }
                        val resp2 = stockfishApi.evaluatePosition(
                            ChessApiRequest(
                                fen = fen,
                                depth = (depth - 2).coerceAtLeast(8),
                                maxThinkingTime = 60,
                                variants = 1,
                                searchmoves = searchFilter
                            )
                        )
                        mapResponseToEngineLine(position, resp2, rank = 2)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        null
                    }
                }

                val secondLine = secondDeferred.await()
                if (secondLine != null) {
                    lines.add(secondLine)
                    if (variants > 2) {
                        val remainingForThird = remainingMoves.filter { it.toUci() != secondLine.bestMoveUci }
                        if (remainingForThird.isNotEmpty()) {
                            try {
                                val searchFilter3 = remainingForThird.take(25).joinToString(" ") { it.toUci() }
                                val resp3 = stockfishApi.evaluatePosition(
                                    ChessApiRequest(
                                        fen = fen,
                                        depth = (depth - 3).coerceAtLeast(8),
                                        maxThinkingTime = 50,
                                        variants = 1,
                                        searchmoves = searchFilter3
                                    )
                                )
                                mapResponseToEngineLine(position, resp3, rank = 3)?.let { lines.add(it) }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                // Ignore optional 3rd line failure
                            }
                        }
                    }
                }
            }
        }

        EngineAnalysisResult(
            fen = fen,
            depth = primaryResp.depth ?: depth,
            source = EngineSource.STOCKFISH_CLOUD,
            lines = lines
        )
    }

    private fun mapResponseToEngineLine(
        position: ChessPosition,
        resp: ChessApiResponse,
        rank: Int
    ): EngineLine? {
        val uci = resp.move ?: return null
        val chessMove = ChessGameRules.uciToChessMove(position, uci) ?: return null
        val fromSq = resp.from?.let { Square.fromAlgebraic(it) } ?: chessMove.from
        val toSq = resp.to?.let { Square.fromAlgebraic(it) } ?: chessMove.to
        val san = resp.san?.takeIf { it.isNotBlank() } ?: ChessGameRules.moveToSan(position, chessMove)

        val cpInt = resp.centipawns?.toIntOrNull()
        val evalPawns = resp.evalScore ?: cpInt?.let { it / 100.0 }
        val winChanceWhite = resp.winChance?.let { raw ->
            if (position.turn == PieceColor.WHITE) raw else (100.0 - raw)
        } ?: run {
            val whiteCp = ((evalPawns ?: 0.0) * 100.0).toInt()
            LocalMinimaxEngine.centipawnsToWinChance(whiteCp)
        }

        val continuation = buildList {
            add(san)
            resp.continuationArr.orEmpty().forEach { step ->
                if (step.isNotBlank() && step != san) add(step)
            }
        }.take(6)

        return EngineLine(
            rank = rank,
            bestMoveUci = chessMove.toUci(),
            bestMoveSan = san,
            fromSquare = fromSq,
            toSquare = toSq,
            evalScorePawns = evalPawns,
            mateIn = resp.mate,
            winChanceWhite = winChanceWhite.coerceIn(1.0, 99.0),
            continuationSan = continuation,
            summaryText = resp.text.orEmpty()
        )
    }
}
