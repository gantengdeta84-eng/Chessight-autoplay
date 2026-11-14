package com.example.chess

import kotlin.math.abs

object ChessGameRules {

    fun validatePosition(position: ChessPosition): BoardValidation {
        var whiteKings = 0
        var blackKings = 0

        for (r in 0..7) {
            for (c in 0..7) {
                val piece = position.pieceAt(r, c) ?: continue
                if (piece.type == PieceType.KING) {
                    if (piece.color == PieceColor.WHITE) whiteKings++ else blackKings++
                }
                if (piece.type == PieceType.PAWN && (r == 0 || r == 7)) {
                    return BoardValidation.Invalid("Pion tidak boleh berada di baris 1 atau 8.")
                }
            }
        }

        if (whiteKings != 1 || blackKings != 1) {
            return BoardValidation.Invalid("Papan harus memiliki tepat satu Raja Putih dan satu Raja Hitam.")
        }

        val opponent = position.turn.opposite()
        if (isKingInCheck(position, opponent)) {
            return BoardValidation.Invalid("Raja lawan tidak boleh sudah dalam posisi skak saat bukan gilirannya.")
        }

        return BoardValidation.Valid
    }

    /**
     * Normalizes 8 rank strings (e.g. from Gemini vision output) into a valid FEN placement string.
     */
    fun normalizeRankRowsToFen(
        rows: List<String>,
        turn: PieceColor,
        requestedCastling: String = "KQkq"
    ): String? {
        if (rows.size != 8) return null
        val normalizedRanks = rows.map { rawRow ->
            val cleaned = rawRow.trim()
            val expanded = StringBuilder()
            for (ch in cleaned) {
                if (ch.isDigit()) {
                    val count = ch.digitToInt().coerceIn(1, 8)
                    repeat(count) { expanded.append('.') }
                } else if (ChessPiece.fromFenChar(ch) != null) {
                    expanded.append(ch)
                }
            }
            // Pad or truncate to exactly 8 squares
            while (expanded.length < 8) expanded.append('.')
            val eightChars = expanded.substring(0, 8)

            // Re-compress into canonical FEN rank
            val fenRank = StringBuilder()
            var empty = 0
            for (sq in eightChars) {
                if (sq == '.') {
                    empty++
                } else {
                    if (empty > 0) {
                        fenRank.append(empty)
                        empty = 0
                    }
                    fenRank.append(sq)
                }
            }
            if (empty > 0) fenRank.append(empty)
            fenRank.toString()
        }

        val placement = normalizedRanks.joinToString("/")
        val basePos = ChessPosition.fromFen("$placement ${turn.toFenChar()} - - 0 1") ?: return null
        val inferredCastling = inferValidCastling(basePos, requestedCastling)
        return basePos.copy(castling = inferredCastling).toFen()
    }

    fun inferValidCastling(position: ChessPosition, hint: String = "KQkq"): CastlingRights {
        val wk = position.pieceAt(7, 4) == ChessPiece(PieceColor.WHITE, PieceType.KING)
        val bk = position.pieceAt(0, 4) == ChessPiece(PieceColor.BLACK, PieceType.KING)
        return CastlingRights(
            whiteKingSide = wk && hint.contains('K') &&
                position.pieceAt(7, 7) == ChessPiece(PieceColor.WHITE, PieceType.ROOK),
            whiteQueenSide = wk && hint.contains('Q') &&
                position.pieceAt(7, 0) == ChessPiece(PieceColor.WHITE, PieceType.ROOK),
            blackKingSide = bk && hint.contains('k') &&
                position.pieceAt(0, 7) == ChessPiece(PieceColor.BLACK, PieceType.ROOK),
            blackQueenSide = bk && hint.contains('q') &&
                position.pieceAt(0, 0) == ChessPiece(PieceColor.BLACK, PieceType.ROOK)
        )
    }

    fun findKingSquare(position: ChessPosition, color: PieceColor): Square? {
        for (r in 0..7) {
            for (c in 0..7) {
                val p = position.pieceAt(r, c)
                if (p != null && p.color == color && p.type == PieceType.KING) {
                    return Square(r, c)
                }
            }
        }
        return null
    }

    fun isKingInCheck(position: ChessPosition, color: PieceColor): Boolean {
        val kingSq = findKingSquare(position, color) ?: return false
        return isSquareAttacked(position, kingSq, color.opposite())
    }

    fun isSquareAttacked(position: ChessPosition, target: Square, byColor: PieceColor): Boolean {
        // Pawn attacks
        val pawnDir = if (byColor == PieceColor.WHITE) 1 else -1
        val pawnRow = target.row + pawnDir
        for (dc in listOf(-1, 1)) {
            val p = position.pieceAt(pawnRow, target.col + dc)
            if (p != null && p.color == byColor && p.type == PieceType.PAWN) return true
        }

        // Knight attacks
        val knightOffsets = listOf(
            -2 to -1, -2 to 1, -1 to -2, -1 to 2,
            1 to -2, 1 to 2, 2 to -1, 2 to 1
        )
        for ((dr, dc) in knightOffsets) {
            val p = position.pieceAt(target.row + dr, target.col + dc)
            if (p != null && p.color == byColor && p.type == PieceType.KNIGHT) return true
        }

        // King attacks
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val p = position.pieceAt(target.row + dr, target.col + dc)
                if (p != null && p.color == byColor && p.type == PieceType.KING) return true
            }
        }

        // Rook / Queen orthogonal rays
        val orthoDirs = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
        for ((dr, dc) in orthoDirs) {
            var r = target.row + dr
            var c = target.col + dc
            while (r in 0..7 && c in 0..7) {
                val p = position.pieceAt(r, c)
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                r += dr
                c += dc
            }
        }

        // Bishop / Queen diagonal rays
        val diagDirs = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
        for ((dr, dc) in diagDirs) {
            var r = target.row + dr
            var c = target.col + dc
            while (r in 0..7 && c in 0..7) {
                val p = position.pieceAt(r, c)
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                r += dr
                c += dc
            }
        }

        return false
    }

    fun generateLegalMoves(position: ChessPosition): List<ChessMove> {
        val pseudo = generatePseudoLegalMoves(position)
        return pseudo.filter { move ->
            val next = applyMoveUnchecked(position, move)
            !isKingInCheck(next, position.turn)
        }
    }

    fun generateLegalMovesFrom(position: ChessPosition, from: Square): List<ChessMove> {
        return generateLegalMoves(position).filter { it.from == from }
    }

    private fun generatePseudoLegalMoves(position: ChessPosition): List<ChessMove> {
        val moves = mutableListOf<ChessMove>()
        val color = position.turn
        val opponent = color.opposite()

        for (r in 0..7) {
            for (c in 0..7) {
                val piece = position.pieceAt(r, c) ?: continue
                if (piece.color != color) continue
                val from = Square(r, c)

                when (piece.type) {
                    PieceType.PAWN -> {
                        val dir = if (color == PieceColor.WHITE) -1 else 1
                        val startRow = if (color == PieceColor.WHITE) 6 else 1
                        val promoRow = if (color == PieceColor.WHITE) 0 else 7

                        val oneStep = r + dir
                        if (oneStep in 0..7 && position.pieceAt(oneStep, c) == null) {
                            if (oneStep == promoRow) {
                                for (promo in listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
                                    moves.add(ChessMove(from, Square(oneStep, c), promo))
                                }
                            } else {
                                moves.add(ChessMove(from, Square(oneStep, c)))
                                val twoStep = r + 2 * dir
                                if (r == startRow && position.pieceAt(twoStep, c) == null) {
                                    moves.add(ChessMove(from, Square(twoStep, c)))
                                }
                            }
                        }

                        for (dc in listOf(-1, 1)) {
                            val tc = c + dc
                            if (oneStep in 0..7 && tc in 0..7) {
                                val targetSq = Square(oneStep, tc)
                                val targetPiece = position.pieceAt(targetSq)
                                val isEnPassant = targetSq == position.enPassantTarget
                                if ((targetPiece != null && targetPiece.color == opponent) || isEnPassant) {
                                    if (oneStep == promoRow) {
                                        for (promo in listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
                                            moves.add(ChessMove(from, targetSq, promo))
                                        }
                                    } else {
                                        moves.add(ChessMove(from, targetSq))
                                    }
                                }
                            }
                        }
                    }

                    PieceType.KNIGHT -> {
                        val offsets = listOf(
                            -2 to -1, -2 to 1, -1 to -2, -1 to 2,
                            1 to -2, 1 to 2, 2 to -1, 2 to 1
                        )
                        for ((dr, dc) in offsets) {
                            val nr = r + dr
                            val nc = c + dc
                            if (nr in 0..7 && nc in 0..7) {
                                val target = position.pieceAt(nr, nc)
                                if (target == null || target.color == opponent) {
                                    moves.add(ChessMove(from, Square(nr, nc)))
                                }
                            }
                        }
                    }

                    PieceType.BISHOP -> addSlidingMoves(
                        position, from, color,
                        listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1), moves
                    )

                    PieceType.ROOK -> addSlidingMoves(
                        position, from, color,
                        listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1), moves
                    )

                    PieceType.QUEEN -> addSlidingMoves(
                        position, from, color,
                        listOf(
                            -1 to -1, -1 to 1, 1 to -1, 1 to 1,
                            -1 to 0, 1 to 0, 0 to -1, 0 to 1
                        ), moves
                    )

                    PieceType.KING -> {
                        for (dr in -1..1) {
                            for (dc in -1..1) {
                                if (dr == 0 && dc == 0) continue
                                val nr = r + dr
                                val nc = c + dc
                                if (nr in 0..7 && nc in 0..7) {
                                    val target = position.pieceAt(nr, nc)
                                    if (target == null || target.color == opponent) {
                                        moves.add(ChessMove(from, Square(nr, nc)))
                                    }
                                }
                            }
                        }

                        // Castling
                        if (!isKingInCheck(position, color)) {
                            if (color == PieceColor.WHITE && r == 7 && c == 4) {
                                if (position.castling.whiteKingSide &&
                                    position.pieceAt(7, 5) == null &&
                                    position.pieceAt(7, 6) == null &&
                                    position.pieceAt(7, 7) == ChessPiece(PieceColor.WHITE, PieceType.ROOK) &&
                                    !isSquareAttacked(position, Square(7, 5), opponent) &&
                                    !isSquareAttacked(position, Square(7, 6), opponent)
                                ) {
                                    moves.add(ChessMove(from, Square(7, 6)))
                                }
                                if (position.castling.whiteQueenSide &&
                                    position.pieceAt(7, 3) == null &&
                                    position.pieceAt(7, 2) == null &&
                                    position.pieceAt(7, 1) == null &&
                                    position.pieceAt(7, 0) == ChessPiece(PieceColor.WHITE, PieceType.ROOK) &&
                                    !isSquareAttacked(position, Square(7, 3), opponent) &&
                                    !isSquareAttacked(position, Square(7, 2), opponent)
                                ) {
                                    moves.add(ChessMove(from, Square(7, 2)))
                                }
                            } else if (color == PieceColor.BLACK && r == 0 && c == 4) {
                                if (position.castling.blackKingSide &&
                                    position.pieceAt(0, 5) == null &&
                                    position.pieceAt(0, 6) == null &&
                                    position.pieceAt(0, 7) == ChessPiece(PieceColor.BLACK, PieceType.ROOK) &&
                                    !isSquareAttacked(position, Square(0, 5), opponent) &&
                                    !isSquareAttacked(position, Square(0, 6), opponent)
                                ) {
                                    moves.add(ChessMove(from, Square(0, 6)))
                                }
                                if (position.castling.blackQueenSide &&
                                    position.pieceAt(0, 3) == null &&
                                    position.pieceAt(0, 2) == null &&
                                    position.pieceAt(0, 1) == null &&
                                    position.pieceAt(0, 0) == ChessPiece(PieceColor.BLACK, PieceType.ROOK) &&
                                    !isSquareAttacked(position, Square(0, 3), opponent) &&
                                    !isSquareAttacked(position, Square(0, 2), opponent)
                                ) {
                                    moves.add(ChessMove(from, Square(0, 2)))
                                }
                            }
                        }
                    }
                }
            }
        }
        return moves
    }

    private fun addSlidingMoves(
        position: ChessPosition,
        from: Square,
        color: PieceColor,
        dirs: List<Pair<Int, Int>>,
        out: MutableList<ChessMove>
    ) {
        val opponent = color.opposite()
        for ((dr, dc) in dirs) {
            var r = from.row + dr
            var c = from.col + dc
            while (r in 0..7 && c in 0..7) {
                val target = position.pieceAt(r, c)
                if (target == null) {
                    out.add(ChessMove(from, Square(r, c)))
                } else {
                    if (target.color == opponent) {
                        out.add(ChessMove(from, Square(r, c)))
                    }
                    break
                }
                r += dr
                c += dc
            }
        }
    }

    fun applyMove(position: ChessPosition, move: ChessMove): ChessPosition {
        return applyMoveUnchecked(position, move)
    }

    private fun applyMoveUnchecked(position: ChessPosition, move: ChessMove): ChessPosition {
        val board = position.board.toMutableList()
        val movingPiece = board[move.from.index] ?: return position
        val capturedPiece = board[move.to.index]

        board[move.from.index] = null

        // Handle En Passant capture
        if (movingPiece.type == PieceType.PAWN && move.to == position.enPassantTarget && capturedPiece == null) {
            val capturedPawnRow = move.from.row
            board[Square(capturedPawnRow, move.to.col).index] = null
        }

        // Handle Castling rook move
        if (movingPiece.type == PieceType.KING && abs(move.to.col - move.from.col) == 2) {
            val row = move.from.row
            if (move.to.col == 6) {
                board[Square(row, 5).index] = board[Square(row, 7).index]
                board[Square(row, 7).index] = null
            } else if (move.to.col == 2) {
                board[Square(row, 3).index] = board[Square(row, 0).index]
                board[Square(row, 0).index] = null
            }
        }

        val placedPiece = if (move.promotion != null && movingPiece.type == PieceType.PAWN) {
            ChessPiece(movingPiece.color, move.promotion)
        } else {
            movingPiece
        }
        board[move.to.index] = placedPiece

        // Update castling rights
        var castling = position.castling
        if (movingPiece.type == PieceType.KING) {
            castling = if (movingPiece.color == PieceColor.WHITE) {
                castling.copy(whiteKingSide = false, whiteQueenSide = false)
            } else {
                castling.copy(blackKingSide = false, blackQueenSide = false)
            }
        }
        if (move.from == Square(7, 7) || move.to == Square(7, 7)) {
            castling = castling.copy(whiteKingSide = false)
        }
        if (move.from == Square(7, 0) || move.to == Square(7, 0)) {
            castling = castling.copy(whiteQueenSide = false)
        }
        if (move.from == Square(0, 7) || move.to == Square(0, 7)) {
            castling = castling.copy(blackKingSide = false)
        }
        if (move.from == Square(0, 0) || move.to == Square(0, 0)) {
            castling = castling.copy(blackQueenSide = false)
        }

        // Update en passant square
        val newEnPassant = if (movingPiece.type == PieceType.PAWN && abs(move.to.row - move.from.row) == 2) {
            Square((move.from.row + move.to.row) / 2, move.from.col)
        } else {
            null
        }

        val isResetHalfMove = movingPiece.type == PieceType.PAWN || capturedPiece != null
        val newHalfMove = if (isResetHalfMove) 0 else position.halfMoveClock + 1
        val newFullMove = if (position.turn == PieceColor.BLACK) position.fullMoveNumber + 1 else position.fullMoveNumber

        return ChessPosition(
            board = board,
            turn = position.turn.opposite(),
            castling = castling,
            enPassantTarget = newEnPassant,
            halfMoveClock = newHalfMove,
            fullMoveNumber = newFullMove
        )
    }

    fun uciToChessMove(position: ChessPosition, uci: String): ChessMove? {
        val trimmed = uci.trim()
        if (trimmed.length < 4) return null
        val from = Square.fromAlgebraic(trimmed.substring(0, 2)) ?: return null
        val to = Square.fromAlgebraic(trimmed.substring(2, 4)) ?: return null
        val promo = if (trimmed.length >= 5) PieceType.fromChar(trimmed[4]) else null
        val legal = generateLegalMoves(position)
        return legal.firstOrNull {
            it.from == from && it.to == to && (promo == null || it.promotion == promo)
        }
    }

    fun sanToChessMove(position: ChessPosition, sanRaw: String): ChessMove? {
        val clean = sanRaw.trim().replace("+", "").replace("#", "")
        val legal = generateLegalMoves(position)
        return legal.firstOrNull { move ->
            val moveSan = moveToSan(position, move).replace("+", "").replace("#", "")
            moveSan == clean
        }
    }

    fun moveToSan(position: ChessPosition, move: ChessMove): String {
        val piece = position.pieceAt(move.from) ?: return move.toUci()
        val after = applyMove(position, move)
        val isCheck = isKingInCheck(after, after.turn)
        val hasReply = generateLegalMoves(after).isNotEmpty()
        val checkSuffix = when {
            isCheck && !hasReply -> "#"
            isCheck -> "+"
            else -> ""
        }

        if (piece.type == PieceType.KING && abs(move.to.col - move.from.col) == 2) {
            val castle = if (move.to.col == 6) "O-O" else "O-O-O"
            return "$castle$checkSuffix"
        }

        val isCapture = position.pieceAt(move.to) != null ||
            (piece.type == PieceType.PAWN && move.to == position.enPassantTarget)

        val sb = StringBuilder()
        if (piece.type == PieceType.PAWN) {
            if (isCapture) {
                sb.append('a' + move.from.col)
                sb.append('x')
            }
            sb.append(move.to.toAlgebraic())
            if (move.promotion != null) {
                sb.append('=')
                sb.append(move.promotion.sanLetter)
            }
        } else {
            sb.append(piece.type.sanLetter)
            val others = generateLegalMoves(position).filter { other ->
                other != move &&
                    other.to == move.to &&
                    position.pieceAt(other.from) == piece
            }
            if (others.isNotEmpty()) {
                val sameFile = others.any { it.from.col == move.from.col }
                val sameRank = others.any { it.from.row == move.from.row }
                if (!sameFile) {
                    sb.append('a' + move.from.col)
                } else if (!sameRank) {
                    sb.append('8' - move.from.row)
                } else {
                    sb.append(move.from.toAlgebraic())
                }
            }
            if (isCapture) sb.append('x')
            sb.append(move.to.toAlgebraic())
        }
        sb.append(checkSuffix)
        return sb.toString()
    }
}
