package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.chess.ChessGameRules
import com.example.chess.ChessMove
import com.example.chess.ChessPiece
import com.example.chess.ChessPosition
import com.example.chess.EngineLine
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.chess.Square
import com.example.ui.theme.BoardCheckSquare
import com.example.ui.theme.BoardCoordinateDark
import com.example.ui.theme.BoardCoordinateLight
import com.example.ui.theme.BoardDarkSquare
import com.example.ui.theme.BoardHighlightMove
import com.example.ui.theme.BoardLightSquare
import com.example.ui.theme.BoardSelectedSquare
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.TacticalBlue
import com.example.ui.theme.TournamentEmerald
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ChessBoardView(
    position: ChessPosition,
    isFlipped: Boolean,
    selectedSquare: Square?,
    legalTargets: List<Square>,
    lastMove: ChessMove?,
    engineLines: List<EngineLine>,
    onSquareTapped: (Square) -> Unit,
    modifier: Modifier = Modifier,
    showArrows: Boolean = true
) {
    val boardCd = stringResource(R.string.cd_chessboard)
    val textMeasurer = rememberTextMeasurer()
    val kingInCheckSq = if (ChessGameRules.isKingInCheck(position, position.turn)) {
        ChessGameRules.findKingSquare(position, position.turn)
    } else {
        null
    }

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            .semantics { contentDescription = boardCd }
            .testTag("chessboard_view")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isFlipped) {
                    detectTapGestures { offset ->
                        val sqSize = size.width / 8f
                        val visualCol = (offset.x / sqSize).toInt().coerceIn(0, 7)
                        val visualRow = (offset.y / sqSize).toInt().coerceIn(0, 7)
                        val boardRow = if (isFlipped) 7 - visualRow else visualRow
                        val boardCol = if (isFlipped) 7 - visualCol else visualCol
                        onSquareTapped(Square(boardRow, boardCol))
                    }
                }
        ) {
            val sqSize = size.width / 8f

            // 1. Squares & Highlights
            for (vr in 0..7) {
                for (vc in 0..7) {
                    val br = if (isFlipped) 7 - vr else vr
                    val bc = if (isFlipped) 7 - vc else vc
                    val sq = Square(br, bc)
                    val isLight = (br + bc) % 2 == 0
                    val baseColor = if (isLight) BoardLightSquare else BoardDarkSquare
                    val topLeft = Offset(vc * sqSize, vr * sqSize)

                    drawRect(color = baseColor, topLeft = topLeft, size = Size(sqSize, sqSize))

                    if (lastMove != null && (sq == lastMove.from || sq == lastMove.to)) {
                        drawRect(color = BoardHighlightMove, topLeft = topLeft, size = Size(sqSize, sqSize))
                    }

                    if (sq == selectedSquare) {
                        drawRect(color = BoardSelectedSquare, topLeft = topLeft, size = Size(sqSize, sqSize))
                    }

                    if (sq == kingInCheckSq) {
                        val center = Offset(topLeft.x + sqSize / 2f, topLeft.y + sqSize / 2f)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(BoardCheckSquare, Color.Transparent),
                                center = center,
                                radius = sqSize * 0.65f
                            ),
                            center = center,
                            radius = sqSize * 0.65f
                        )
                    }
                }
            }

            // 2. Coordinates along edges
            drawBoardCoordinates(textMeasurer, sqSize, isFlipped)

            // 3. Pieces
            for (vr in 0..7) {
                for (vc in 0..7) {
                    val br = if (isFlipped) 7 - vr else vr
                    val bc = if (isFlipped) 7 - vc else vc
                    val piece = position.pieceAt(br, bc) ?: continue
                    val pad = sqSize * 0.08f
                    drawChessPiece(
                        piece = piece,
                        width = sqSize - 2 * pad,
                        height = sqSize - 2 * pad,
                        topLeft = Offset(vc * sqSize + pad, vr * sqSize + pad)
                    )
                }
            }

            // 4. Legal move indicators
            for (target in legalTargets) {
                val vr = if (isFlipped) 7 - target.row else target.row
                val vc = if (isFlipped) 7 - target.col else target.col
                val center = Offset((vc + 0.5f) * sqSize, (vr + 0.5f) * sqSize)
                val hasEnemy = position.pieceAt(target) != null
                if (hasEnemy) {
                    drawCircle(
                        color = Color(0x660D1117),
                        radius = sqSize * 0.42f,
                        center = center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = sqSize * 0.09f)
                    )
                } else {
                    drawCircle(
                        color = Color(0x550D1117),
                        radius = sqSize * 0.16f,
                        center = center
                    )
                }
            }

            // 5. Best Move Arrows (drawn secondary first, #1 primary on top)
            if (showArrows && engineLines.isNotEmpty()) {
                val sortedArrows = engineLines.sortedByDescending { it.rank }
                for (line in sortedArrows) {
                    val arrowColor = when (line.rank) {
                        1 -> TournamentEmerald.copy(alpha = 0.88f)
                        2 -> TacticalBlue.copy(alpha = 0.68f)
                        else -> TacticalBlue.copy(alpha = 0.40f)
                    }
                    val strokeFactor = if (line.rank == 1) 0.16f else 0.11f
                    drawMoveArrow(
                        from = line.fromSquare,
                        to = line.toSquare,
                        sqSize = sqSize,
                        isFlipped = isFlipped,
                        color = arrowColor,
                        strokeWidth = sqSize * strokeFactor
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawBoardCoordinates(
    textMeasurer: TextMeasurer,
    sqSize: Float,
    isFlipped: Boolean
) {
    val fontSize = (sqSize * 0.19f / density).coerceIn(8f, 12f).sp
    for (i in 0..7) {
        // Rank labels on left column (vc == 0)
        val br = if (isFlipped) 7 - i else i
        val rankText = ('8' - br).toString()
        val isLightLeft = (br + 0) % 2 == 0
        val rankColor = if (isLightLeft) BoardCoordinateLight else BoardCoordinateDark
        drawText(
            textMeasurer = textMeasurer,
            text = rankText,
            topLeft = Offset(sqSize * 0.07f, i * sqSize + sqSize * 0.04f),
            style = TextStyle(
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                color = rankColor
            )
        )

        // File labels on bottom row (vr == 7)
        val bc = if (isFlipped) 7 - i else i
        val fileText = ('a' + bc).toString()
        val bottomBr = if (isFlipped) 0 else 7
        val isLightBottom = (bottomBr + bc) % 2 == 0
        val fileColor = if (isLightBottom) BoardCoordinateLight else BoardCoordinateDark
        val layout = textMeasurer.measure(
            text = fileText,
            style = TextStyle(
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                color = fileColor
            )
        )
        drawText(
            textLayoutResult = layout,
            topLeft = Offset(
                (i + 1) * sqSize - layout.size.width - sqSize * 0.07f,
                8f * sqSize - layout.size.height - sqSize * 0.04f
            )
        )
    }
}

private fun DrawScope.drawMoveArrow(
    from: Square,
    to: Square,
    sqSize: Float,
    isFlipped: Boolean,
    color: Color,
    strokeWidth: Float
) {
    val fromVr = if (isFlipped) 7 - from.row else from.row
    val fromVc = if (isFlipped) 7 - from.col else from.col
    val toVr = if (isFlipped) 7 - to.row else to.row
    val toVc = if (isFlipped) 7 - to.col else to.col

    val start = Offset((fromVc + 0.5f) * sqSize, (fromVr + 0.5f) * sqSize)
    val end = Offset((toVc + 0.5f) * sqSize, (toVr + 0.5f) * sqSize)

    val angle = atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
    val headLength = strokeWidth * 2.15f
    val headHalfWidth = strokeWidth * 1.35f

    val shaftEnd = Offset(
        end.x - (headLength * 0.72f * cos(angle)).toFloat(),
        end.y - (headLength * 0.72f * sin(angle)).toFloat()
    )

    drawLine(
        color = color,
        start = start,
        end = shaftEnd,
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )

    val tip = end
    val baseCenter = Offset(
        end.x - (headLength * cos(angle)).toFloat(),
        end.y - (headLength * sin(angle)).toFloat()
    )
    val perpAngle = angle + Math.PI / 2.0
    val leftCorner = Offset(
        baseCenter.x + (headHalfWidth * cos(perpAngle)).toFloat(),
        baseCenter.y + (headHalfWidth * sin(perpAngle)).toFloat()
    )
    val rightCorner = Offset(
        baseCenter.x - (headHalfWidth * cos(perpAngle)).toFloat(),
        baseCenter.y - (headHalfWidth * sin(perpAngle)).toFloat()
    )

    val arrowHead = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(leftCorner.x, leftCorner.y)
        lineTo(rightCorner.x, rightCorner.y)
        close()
    }
    drawPath(arrowHead, color = color)
}

@Composable
fun MiniChessBoardPreview(
    position: ChessPosition,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sq = size.width / 8f
            for (r in 0..7) {
                for (c in 0..7) {
                    val isLight = (r + c) % 2 == 0
                    drawRect(
                        color = if (isLight) BoardLightSquare else BoardDarkSquare,
                        topLeft = Offset(c * sq, r * sq),
                        size = Size(sq, sq)
                    )
                    val piece = position.pieceAt(r, c)
                    if (piece != null) {
                        val pad = sq * 0.08f
                        drawChessPiece(
                            piece = piece,
                            width = sq - 2 * pad,
                            height = sq - 2 * pad,
                            topLeft = Offset(c * sq + pad, r * sq + pad)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PromotionPickerDialog(
    color: PieceColor,
    onSelect: (PieceType) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Pilih Bidak Promosi",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                options.forEach { pieceType ->
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onSelect(pieceType) }
                            .padding(8.dp)
                            .testTag("promo_${pieceType.fenLetter.lowercaseChar()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        ChessPieceIcon(
                            piece = ChessPiece(color, pieceType),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
