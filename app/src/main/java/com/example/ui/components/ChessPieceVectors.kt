package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.chess.ChessPiece
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.ui.theme.BlackPieceFill
import com.example.ui.theme.BlackPieceStroke
import com.example.ui.theme.WhitePieceFill
import com.example.ui.theme.WhitePieceStroke

@Composable
fun ChessPieceIcon(
    piece: ChessPiece,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawChessPiece(piece, size.width, size.height)
    }
}

fun DrawScope.drawChessPiece(
    piece: ChessPiece,
    width: Float,
    height: Float,
    topLeft: Offset = Offset.Zero
) {
    val fill = if (piece.color == PieceColor.WHITE) WhitePieceFill else BlackPieceFill
    val stroke = if (piece.color == PieceColor.WHITE) WhitePieceStroke else BlackPieceStroke
    val detail = if (piece.color == PieceColor.WHITE) WhitePieceStroke else Color(0xFF9EA7B3)

    val s = minOf(width, height) / 100f
    val ox = topLeft.x + (width - 100f * s) / 2f
    val oy = topLeft.y + (height - 100f * s) / 2f

    fun x(v: Float) = ox + v * s
    fun y(v: Float) = oy + v * s

    val strokeStyle = Stroke(width = (2.6f * s).coerceAtLeast(1.2f), cap = StrokeCap.Round, join = StrokeJoin.Round)
    val detailStyle = Stroke(width = (1.9f * s).coerceAtLeast(1f), cap = StrokeCap.Round, join = StrokeJoin.Round)

    when (piece.type) {
        PieceType.PAWN -> {
            val body = Path().apply {
                moveTo(x(27f), y(84f))
                lineTo(x(73f), y(84f))
                lineTo(x(70f), y(75f))
                lineTo(x(62f), y(71f))
                cubicTo(x(60f), y(58f), x(57f), y(48f), x(56f), y(43f))
                lineTo(x(44f), y(43f))
                cubicTo(x(43f), y(48f), x(40f), y(58f), x(38f), y(71f))
                lineTo(x(30f), y(75f))
                close()
            }
            drawPath(body, color = fill, style = Fill)
            drawPath(body, color = stroke, style = strokeStyle)

            // Collar ring
            drawRoundRect(
                color = fill,
                topLeft = Offset(x(36f), y(41f)),
                size = Size(28f * s, 6f * s),
                cornerRadius = CornerRadius(3f * s, 3f * s)
            )
            drawRoundRect(
                color = stroke,
                topLeft = Offset(x(36f), y(41f)),
                size = Size(28f * s, 6f * s),
                cornerRadius = CornerRadius(3f * s, 3f * s),
                style = detailStyle
            )

            // Head
            drawCircle(color = fill, radius = 13.5f * s, center = Offset(x(50f), y(28f)))
            drawCircle(color = stroke, radius = 13.5f * s, center = Offset(x(50f), y(28f)), style = strokeStyle)
        }

        PieceType.ROOK -> {
            val body = Path().apply {
                moveTo(x(22f), y(84f))
                lineTo(x(78f), y(84f))
                lineTo(x(75f), y(75f))
                lineTo(x(67f), y(70f))
                lineTo(x(64f), y(38f))
                lineTo(x(72f), y(31f))
                lineTo(x(72f), y(18f))
                lineTo(x(62f), y(18f))
                lineTo(x(62f), y(25f))
                lineTo(x(55f), y(25f))
                lineTo(x(55f), y(18f))
                lineTo(x(45f), y(18f))
                lineTo(x(45f), y(25f))
                lineTo(x(38f), y(25f))
                lineTo(x(38f), y(18f))
                lineTo(x(28f), y(18f))
                lineTo(x(28f), y(31f))
                lineTo(x(36f), y(38f))
                lineTo(x(33f), y(70f))
                lineTo(x(25f), y(75f))
                close()
            }
            drawPath(body, color = fill, style = Fill)
            drawPath(body, color = stroke, style = strokeStyle)

            drawLine(detail, Offset(x(36f), y(38f)), Offset(x(64f), y(38f)), strokeWidth = 1.9f * s)
            drawLine(detail, Offset(x(33f), y(70f)), Offset(x(67f), y(70f)), strokeWidth = 1.9f * s)
        }

        PieceType.BISHOP -> {
            val body = Path().apply {
                moveTo(x(23f), y(84f))
                lineTo(x(77f), y(84f))
                lineTo(x(73f), y(75f))
                lineTo(x(63f), y(70f))
                cubicTo(x(65f), y(56f), x(64f), y(43f), x(57f), y(30f))
                cubicTo(x(54f), y(24f), x(50f), y(21f), x(50f), y(21f))
                cubicTo(x(50f), y(21f), x(46f), y(24f), x(43f), y(30f))
                cubicTo(x(36f), y(43f), x(35f), y(56f), x(37f), y(70f))
                lineTo(x(27f), y(75f))
                close()
            }
            drawPath(body, color = fill, style = Fill)
            drawPath(body, color = stroke, style = strokeStyle)

            // Finial ball
            drawCircle(color = fill, radius = 4.8f * s, center = Offset(x(50f), y(16f)))
            drawCircle(color = stroke, radius = 4.8f * s, center = Offset(x(50f), y(16f)), style = detailStyle)

            // Miter slash & collar
            drawLine(detail, Offset(x(45f), y(47f)), Offset(x(56f), y(35f)), strokeWidth = 2.4f * s, cap = StrokeCap.Round)
            drawLine(detail, Offset(x(37f), y(69f)), Offset(x(63f), y(69f)), strokeWidth = 1.9f * s)
        }

        PieceType.KNIGHT -> {
            val body = Path().apply {
                moveTo(x(23f), y(84f))
                lineTo(x(77f), y(84f))
                lineTo(x(74f), y(74f))
                lineTo(x(68f), y(69f))
                cubicTo(x(74f), y(50f), x(70f), y(28f), x(53f), y(19f))
                lineTo(x(50f), y(12f))
                lineTo(x(43f), y(19f))
                lineTo(x(36f), y(14f))
                lineTo(x(35f), y(24f))
                lineTo(x(21f), y(43f))
                lineTo(x(25f), y(52f))
                lineTo(x(37f), y(47f))
                lineTo(x(44f), y(43f))
                cubicTo(x(41f), y(53f), x(34f), y(61f), x(32f), y(70f))
                lineTo(x(26f), y(74f))
                close()
            }
            drawPath(body, color = fill, style = Fill)
            drawPath(body, color = stroke, style = strokeStyle)

            // Eye and nostril accents
            drawCircle(color = detail, radius = 2.5f * s, center = Offset(x(36f), y(30f)))
            drawLine(detail, Offset(x(54f), y(27f)), Offset(x(62f), y(62f)), strokeWidth = 2.0f * s, cap = StrokeCap.Round)
            drawLine(detail, Offset(x(31f), y(72f)), Offset(x(69f), y(72f)), strokeWidth = 1.9f * s)
        }

        PieceType.QUEEN -> {
            val body = Path().apply {
                moveTo(x(23f), y(84f))
                lineTo(x(77f), y(84f))
                lineTo(x(73f), y(74f))
                lineTo(x(66f), y(68f))
                lineTo(x(78f), y(32f))
                lineTo(x(62f), y(50f))
                lineTo(x(64f), y(24f))
                lineTo(x(54f), y(48f))
                lineTo(x(50f), y(20f))
                lineTo(x(46f), y(48f))
                lineTo(x(36f), y(24f))
                lineTo(x(38f), y(50f))
                lineTo(x(22f), y(32f))
                lineTo(x(34f), y(68f))
                lineTo(x(27f), y(74f))
                close()
            }
            drawPath(body, color = fill, style = Fill)
            drawPath(body, color = stroke, style = strokeStyle)

            val crownPoints = listOf(
                Offset(x(22f), y(30f)),
                Offset(x(36f), y(22f)),
                Offset(x(50f), y(18f)),
                Offset(x(64f), y(22f)),
                Offset(x(78f), y(30f))
            )
            crownPoints.forEach { pt ->
                drawCircle(color = fill, radius = 3.6f * s, center = pt)
                drawCircle(color = stroke, radius = 3.6f * s, center = pt, style = detailStyle)
            }
            drawLine(detail, Offset(x(33f), y(70f)), Offset(x(67f), y(70f)), strokeWidth = 1.9f * s)
        }

        PieceType.KING -> {
            // Cross on top
            drawLine(stroke, Offset(x(50f), y(10f)), Offset(x(50f), y(26f)), strokeWidth = 3.2f * s, cap = StrokeCap.Round)
            drawLine(stroke, Offset(x(42f), y(16f)), Offset(x(58f), y(16f)), strokeWidth = 3.2f * s, cap = StrokeCap.Round)

            val body = Path().apply {
                moveTo(x(23f), y(84f))
                lineTo(x(77f), y(84f))
                lineTo(x(73f), y(74f))
                lineTo(x(67f), y(68f))
                cubicTo(x(79f), y(52f), x(81f), y(34f), x(67f), y(29f))
                cubicTo(x(58f), y(26f), x(53f), y(34f), x(50f), y(39f))
                cubicTo(x(47f), y(34f), x(42f), y(26f), x(33f), y(29f))
                cubicTo(x(19f), y(34f), x(21f), y(52f), x(33f), y(68f))
                lineTo(x(27f), y(74f))
                close()
            }
            drawPath(body, color = fill, style = Fill)
            drawPath(body, color = stroke, style = strokeStyle)

            drawLine(detail, Offset(x(50f), y(39f)), Offset(x(50f), y(67f)), strokeWidth = 1.9f * s)
            drawLine(detail, Offset(x(33f), y(69f)), Offset(x(67f), y(69f)), strokeWidth = 1.9f * s)
        }
    }
}
