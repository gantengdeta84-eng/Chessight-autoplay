package com.example.chess

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.math.exp
import kotlin.math.max

/**
 * Pure-Kotlin offline chess engine using Negamax + Alpha-Beta pruning,
 * MVV-LVA move ordering, and Piece-Square Tables as an instant offline fallback.
 */
object LocalMinimaxEngine {

    private const val INF = 1_000_000
    private const val MATE_SCORE = 100_000

    private val pawnPst = intArrayOf(
        0,  0,  0,  0,  0,  0,  0,  0,
        50, 50, 50, 50, 50, 50, 50, 50,
        10, 10, 20, 30, 30, 20, 10, 10,
        5,  5, 10, 25, 25, 10,  5,  5,
        0,  0,  0, 22, 22,  0,  0,  0,
        5, -5,-10,  0,  0,-10, -5,  5,
        5, 10, 10,-20,-20, 10, 10,  5,
        0,  0,  0,  0,  0,  0,  0,  0
    )

    private val knightPst = intArrayOf(
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  0,  0,  0,-20,-40,
        -30,  0, 10, 15, 15, 10,  0,-30,
        -30,  5, 15, 20, 20, 15,  5,-30,
        -30,  0, 15, 20, 20, 15,  0,-30,
        -30,  5, 10, 15, 15, 10,  5,-30,
        -40,-20,  0,  5,  5,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50
    )

    private val bishopPst = intArrayOf(
        -20,-10,-10,-10,-10,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5, 10, 10,  5,  0,-10,
        -10,  5,  5, 10, 10,  5,  5,-10,
        -10,  0, 10, 10, 10, 10,  0,-10,
        -10, 10, 10, 10, 10, 10, 10,-10,
        -10,  5,  0,  0,  0,  0,  5,-10,
        -20,-10,-10,-10,-10,-10,-10,-20
    )

    private val rookPst = intArrayOf(
        0,  0,  0,  0,  0,  0,  0,  0,
        5, 10, 10, 10, 10, 10, 10,  5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        0,  0,  0,  5,  5,  0,  0,  0
    )

    private val queenPst = intArrayOf(
        -20,-10,-10, -5, -5,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5,  5,  5,  5,  0,-10,
        -5,  0,  5,  5,  5,  5,  0, -5,
        0,  0,  5,  5,  5,  5,  0, -5,
        -10,  5,  5,  5,  5,  5,  0,-10,
        -10,  0,  5,  0,  0,  0,  0,-10,
        -20,-10,-10, -5, -5,-10,-10,-20
    )

    private val kingMidPst = intArrayOf(
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -20,-30,-30,-40,-40,-30,-30,-20,
        -10,-20,-20,-20,-20,-20,-20,-10,
        20, 20,  0,  0,  0,  0, 20, 20,
        20, 30, 10,  0,  0, 10, 30, 20
    )

    suspend fun analyze(
        position: ChessPosition,
        maxLines: Int = 2,
        searchDepth: Int = 3,
        fallbackNotice: String? = null
    ): EngineAnalysisResult = withContext(Dispatchers.Default) {
        val legalMoves = orderMoves(position, ChessGameRules.generateLegalMoves(position))
        if (legalMoves.isEmpty()) {
            return@withContext EngineAnalysisResult(
                fen = position.toFen(),
                depth = searchDepth,
                source = EngineSource.LOCAL_MINIMAX,
                lines = emptyList(),
                fallbackNotice = fallbackNotice
            )
        }

        val scored = mutableListOf<Pair<ChessMove, Int>>()
        for (move in legalMoves) {
            currentCoroutineContext().ensureActive()
            val next = ChessGameRules.applyMove(position, move)
            val scoreFromSide = -negamax(next, searchDepth - 1, -INF, INF, 1)
            scored.add(move to scoreFromSide)
        }

        val topScored = scored.sortedByDescending { it.second }.take(maxLines.coerceIn(1, 3))
        val lines = topScored.mapIndexed { idx, (move, scoreFromSide) ->
            val whiteCp = if (position.turn == PieceColor.WHITE) scoreFromSide else -scoreFromSide
            val mateIn = if (kotlin.math.abs(whiteCp) > MATE_SCORE - 500) {
                val plies = (MATE_SCORE - kotlin.math.abs(whiteCp)).coerceAtLeast(1)
                val fullMoves = (plies + 1) / 2
                if (whiteCp > 0) fullMoves else -fullMoves
            } else {
                null
            }
            val evalPawns = if (mateIn == null) whiteCp / 100.0 else null
            val winChance = centipawnsToWinChance(whiteCp)
            val pv = extractPvLine(position, move, maxPlies = 4)
            val san = ChessGameRules.moveToSan(position, move)

            EngineLine(
                rank = idx + 1,
                bestMoveUci = move.toUci(),
                bestMoveSan = san,
                fromSquare = move.from,
                toSquare = move.to,
                evalScorePawns = evalPawns,
                mateIn = mateIn,
                winChanceWhite = winChance,
                continuationSan = pv,
                summaryText = "Rekomendasi mesin lokal ($san)"
            )
        }

        EngineAnalysisResult(
            fen = position.toFen(),
            depth = searchDepth,
            source = EngineSource.LOCAL_MINIMAX,
            lines = lines,
            fallbackNotice = fallbackNotice
        )
    }

    private suspend fun negamax(
        position: ChessPosition,
        depth: Int,
        alphaInit: Int,
        beta: Int,
        ply: Int
    ): Int {
        currentCoroutineContext().ensureActive()
        if (depth <= 0) {
            return quiescence(position, alphaInit, beta, 2)
        }

        val moves = orderMoves(position, ChessGameRules.generateLegalMoves(position))
        if (moves.isEmpty()) {
            return if (ChessGameRules.isKingInCheck(position, position.turn)) {
                -(MATE_SCORE - ply)
            } else {
                0
            }
        }

        var alpha = alphaInit
        var best = -INF
        for (move in moves) {
            val next = ChessGameRules.applyMove(position, move)
            val score = -negamax(next, depth - 1, -beta, -alpha, ply + 1)
            best = max(best, score)
            alpha = max(alpha, score)
            if (alpha >= beta) break
        }
        return best
    }

    private suspend fun quiescence(
        position: ChessPosition,
        alphaInit: Int,
        beta: Int,
        remaining: Int
    ): Int {
        currentCoroutineContext().ensureActive()
        val standPat = evaluateStatic(position)
        if (standPat >= beta) return beta
        var alpha = max(alphaInit, standPat)
        if (remaining <= 0) return alpha

        val captures = ChessGameRules.generateLegalMoves(position)
            .filter { position.pieceAt(it.to) != null || it.promotion != null }
        val ordered = orderMoves(position, captures)
        for (move in ordered) {
            val next = ChessGameRules.applyMove(position, move)
            val score = -quiescence(next, -beta, -alpha, remaining - 1)
            if (score >= beta) return beta
            alpha = max(alpha, score)
        }
        return alpha
    }

    private fun extractPvLine(
        start: ChessPosition,
        firstMove: ChessMove,
        maxPlies: Int
    ): List<String> {
        val result = mutableListOf<String>()
        var current = start
        result.add(ChessGameRules.moveToSan(current, firstMove))
        current = ChessGameRules.applyMove(current, firstMove)

        repeat(maxPlies - 1) {
            val legal = orderMoves(current, ChessGameRules.generateLegalMoves(current))
            val bestReply = legal.maxByOrNull { move ->
                val next = ChessGameRules.applyMove(current, move)
                -evaluateStatic(next)
            } ?: return result
            result.add(ChessGameRules.moveToSan(current, bestReply))
            current = ChessGameRules.applyMove(current, bestReply)
        }
        return result
    }

    private fun orderMoves(position: ChessPosition, moves: List<ChessMove>): List<ChessMove> {
        return moves.sortedByDescending { move ->
            var score = 0
            val captured = position.pieceAt(move.to)
            val attacker = position.pieceAt(move.from)
            if (captured != null && attacker != null) {
                score += 10 * captured.type.materialValue - attacker.type.materialValue
            }
            if (move.promotion != null) {
                score += move.promotion.materialValue
            }
            score
        }
    }

    private fun evaluateStatic(position: ChessPosition): Int {
        var whiteScore = 0
        var blackScore = 0

        for (r in 0..7) {
            for (c in 0..7) {
                val p = position.pieceAt(r, c) ?: continue
                val pstRow = if (p.color == PieceColor.WHITE) r else 7 - r
                val pstIdx = pstRow * 8 + c
                val pstBonus = when (p.type) {
                    PieceType.PAWN -> pawnPst[pstIdx]
                    PieceType.KNIGHT -> knightPst[pstIdx]
                    PieceType.BISHOP -> bishopPst[pstIdx]
                    PieceType.ROOK -> rookPst[pstIdx]
                    PieceType.QUEEN -> queenPst[pstIdx]
                    PieceType.KING -> kingMidPst[pstIdx]
                }
                val value = p.type.materialValue + pstBonus
                if (p.color == PieceColor.WHITE) whiteScore += value else blackScore += value
            }
        }

        val diff = whiteScore - blackScore
        return if (position.turn == PieceColor.WHITE) diff else -diff
    }

    fun centipawnsToWinChance(whiteCp: Int): Double {
        val clamped = whiteCp.coerceIn(-1500, 1500)
        return (50.0 + 50.0 * (2.0 / (1.0 + exp(-0.00368208 * clamped)) - 1.0))
            .coerceIn(1.0, 99.0)
    }
}
