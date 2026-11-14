package com.example

import com.example.chess.BoardValidation
import com.example.chess.ChessGameRules
import com.example.chess.ChessPosition
import com.example.chess.LocalMinimaxEngine
import com.example.chess.PieceColor
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChessRulesAndEngineTest {

    @Test
    fun fenRoundTrip_startingPosition_matches() {
        val pos = ChessPosition.starting()
        assertEquals(ChessPosition.STARTING_FEN, pos.toFen())
        assertEquals(BoardValidation.Valid, ChessGameRules.validatePosition(pos))
        assertEquals(20, ChessGameRules.generateLegalMoves(pos).size)
    }

    @Test
    fun normalizeRankRows_compressesConsecutiveEmptySquares() {
        val rows = listOf(
            "rnbqkbnr",
            "pppppppp",
            "8",
            "11111111",
            "4P3",
            "8",
            "PPPP1PPP",
            "RNBQKBNR"
        )
        val fen = ChessGameRules.normalizeRankRowsToFen(rows, PieceColor.BLACK)
        assertEquals(
            "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
            fen
        )
    }

    @Test
    fun foolsMate_detectedAsCheckmateWithZeroLegalMoves() {
        val foolsMateFen = "rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3"
        val pos = ChessPosition.fromFen(foolsMateFen)!!
        assertTrue(ChessGameRules.isKingInCheck(pos, PieceColor.WHITE))
        assertTrue(ChessGameRules.generateLegalMoves(pos).isEmpty())
    }

    @Test
    fun localMinimaxEngine_findsImmediateMateInOne() = runTest {
        // White Queen on h5, Bishop on c4 -> Qxf7#
        val fen = "r1bqkbnr/pppp1ppp/2n5/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4"
        val pos = ChessPosition.fromFen(fen)!!
        val result = LocalMinimaxEngine.analyze(pos, maxLines = 2, searchDepth = 2)
        assertTrue(result.lines.isNotEmpty())
        assertEquals("Qxf7#", result.lines.first().bestMoveSan)
    }
}
