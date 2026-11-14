package com.example.chess

enum class PieceColor {
    WHITE,
    BLACK;

    fun opposite(): PieceColor = if (this == WHITE) BLACK else WHITE

    fun toFenChar(): String = if (this == WHITE) "w" else "b"
}

enum class PieceType(val fenLetter: Char, val sanLetter: String, val materialValue: Int) {
    PAWN('P', "", 100),
    KNIGHT('N', "N", 320),
    BISHOP('B', "B", 330),
    ROOK('R', "R", 500),
    QUEEN('Q', "Q", 900),
    KING('K', "K", 20000);

    companion object {
        fun fromChar(c: Char): PieceType? = when (c.uppercaseChar()) {
            'P' -> PAWN
            'N' -> KNIGHT
            'B' -> BISHOP
            'R' -> ROOK
            'Q' -> QUEEN
            'K' -> KING
            else -> null
        }
    }
}

data class ChessPiece(
    val color: PieceColor,
    val type: PieceType
) {
    fun toFenChar(): Char =
        if (color == PieceColor.WHITE) type.fenLetter.uppercaseChar() else type.fenLetter.lowercaseChar()

    companion object {
        fun fromFenChar(c: Char): ChessPiece? {
            val type = PieceType.fromChar(c) ?: return null
            val color = if (c.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
            return ChessPiece(color, type)
        }
    }
}

/**
 * Row 0 = rank 8, Row 7 = rank 1.
 * Col 0 = file 'a', Col 7 = file 'h'.
 */
data class Square(val row: Int, val col: Int) {
    val index: Int get() = row * 8 + col

    fun isValid(): Boolean = row in 0..7 && col in 0..7

    fun toAlgebraic(): String {
        val file = ('a' + col)
        val rank = ('8' - row)
        return "$file$rank"
    }

    companion object {
        fun fromIndex(index: Int): Square = Square(index / 8, index % 8)

        fun fromAlgebraic(coord: String): Square? {
            if (coord.length < 2) return null
            val col = coord[0].lowercaseChar() - 'a'
            val row = '8' - coord[1]
            val sq = Square(row, col)
            return if (sq.isValid()) sq else null
        }
    }
}

data class ChessMove(
    val from: Square,
    val to: Square,
    val promotion: PieceType? = null
) {
    fun toUci(): String {
        val promoChar = promotion?.fenLetter?.lowercaseChar()?.toString().orEmpty()
        return "${from.toAlgebraic()}${to.toAlgebraic()}$promoChar"
    }
}

data class CastlingRights(
    val whiteKingSide: Boolean = true,
    val whiteQueenSide: Boolean = true,
    val blackKingSide: Boolean = true,
    val blackQueenSide: Boolean = true
) {
    fun toFenString(): String {
        val sb = StringBuilder()
        if (whiteKingSide) sb.append('K')
        if (whiteQueenSide) sb.append('Q')
        if (blackKingSide) sb.append('k')
        if (blackQueenSide) sb.append('q')
        return if (sb.isEmpty()) "-" else sb.toString()
    }

    companion object {
        val NONE = CastlingRights(false, false, false, false)

        fun fromFenString(str: String): CastlingRights {
            if (str == "-" || str.isBlank()) return NONE
            return CastlingRights(
                whiteKingSide = str.contains('K'),
                whiteQueenSide = str.contains('Q'),
                blackKingSide = str.contains('k'),
                blackQueenSide = str.contains('q')
            )
        }
    }
}

data class ChessPosition(
    val board: List<ChessPiece?>,
    val turn: PieceColor = PieceColor.WHITE,
    val castling: CastlingRights = CastlingRights(),
    val enPassantTarget: Square? = null,
    val halfMoveClock: Int = 0,
    val fullMoveNumber: Int = 1
) {
    fun pieceAt(row: Int, col: Int): ChessPiece? {
        if (row !in 0..7 || col !in 0..7) return null
        return board[row * 8 + col]
    }

    fun pieceAt(square: Square): ChessPiece? = pieceAt(square.row, square.col)

    fun withPieceAt(square: Square, piece: ChessPiece?): ChessPosition {
        val mutable = board.toMutableList()
        mutable[square.index] = piece
        return copy(board = mutable)
    }

    fun toFen(): String {
        val rows = (0..7).joinToString("/") { r ->
            val sb = StringBuilder()
            var emptyCount = 0
            for (c in 0..7) {
                val p = pieceAt(r, c)
                if (p == null) {
                    emptyCount++
                } else {
                    if (emptyCount > 0) {
                        sb.append(emptyCount)
                        emptyCount = 0
                    }
                    sb.append(p.toFenChar())
                }
            }
            if (emptyCount > 0) sb.append(emptyCount)
            sb.toString()
        }
        val ep = enPassantTarget?.toAlgebraic() ?: "-"
        return "$rows ${turn.toFenChar()} ${castling.toFenString()} $ep $halfMoveClock $fullMoveNumber"
    }

    companion object {
        const val STARTING_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

        fun empty(turn: PieceColor = PieceColor.WHITE): ChessPosition = ChessPosition(
            board = List(64) { null },
            turn = turn,
            castling = CastlingRights.NONE
        )

        fun starting(): ChessPosition = fromFen(STARTING_FEN)!!

        fun fromFen(fen: String): ChessPosition? {
            val parts = fen.trim().split(Regex("\\s+"))
            if (parts.isEmpty()) return null
            val placement = parts[0]
            val rankStrings = placement.split('/')
            if (rankStrings.size != 8) return null

            val cells = MutableList<ChessPiece?>(64) { null }
            for (r in 0..7) {
                var col = 0
                for (ch in rankStrings[r]) {
                    if (ch.isDigit()) {
                        col += ch.digitToInt()
                    } else {
                        val piece = ChessPiece.fromFenChar(ch) ?: return null
                        if (col >= 8) return null
                        cells[r * 8 + col] = piece
                        col++
                    }
                }
                if (col != 8) return null
            }

            val turn = when (parts.getOrNull(1)) {
                "b" -> PieceColor.BLACK
                else -> PieceColor.WHITE
            }
            val castling = CastlingRights.fromFenString(parts.getOrNull(2) ?: "-")
            val ep = parts.getOrNull(3)?.takeIf { it != "-" }?.let { Square.fromAlgebraic(it) }
            val halfMove = parts.getOrNull(4)?.toIntOrNull() ?: 0
            val fullMove = parts.getOrNull(5)?.toIntOrNull()?.coerceAtLeast(1) ?: 1

            return ChessPosition(
                board = cells,
                turn = turn,
                castling = castling,
                enPassantTarget = ep,
                halfMoveClock = halfMove,
                fullMoveNumber = fullMove
            )
        }
    }
}

sealed interface BoardValidation {
    data object Valid : BoardValidation
    data class Invalid(val reason: String) : BoardValidation
}

enum class EngineSource(val label: String) {
    STOCKFISH_CLOUD("Stockfish Cloud"),
    LOCAL_MINIMAX("Mesin Lokal")
}

data class EngineLine(
    val rank: Int,
    val bestMoveUci: String,
    val bestMoveSan: String,
    val fromSquare: Square,
    val toSquare: Square,
    val evalScorePawns: Double?,
    val mateIn: Int?,
    val winChanceWhite: Double,
    val continuationSan: List<String>,
    val summaryText: String = ""
) {
    fun formattedEval(): String {
        if (mateIn != null) {
            return if (mateIn > 0) "M$mateIn" else "-M${kotlin.math.abs(mateIn)}"
        }
        val score = evalScorePawns ?: 0.0
        val sign = if (score > 0.0) "+" else ""
        return "$sign${String.format(java.util.Locale.US, "%.2f", score)}"
    }
}

data class EngineAnalysisResult(
    val fen: String,
    val depth: Int,
    val source: EngineSource,
    val lines: List<EngineLine>,
    val fallbackNotice: String? = null
)
