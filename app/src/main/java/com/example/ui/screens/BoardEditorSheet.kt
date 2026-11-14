package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.chess.BoardValidation
import com.example.chess.CastlingRights
import com.example.chess.ChessGameRules
import com.example.chess.ChessPiece
import com.example.chess.ChessPosition
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.ui.components.ChessBoardView
import com.example.ui.components.ChessPieceIcon
import com.example.ui.theme.CoralError
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.TournamentEmerald

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BoardEditorSheet(
    initialPosition: ChessPosition,
    isFlipped: Boolean,
    onApply: (ChessPosition) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboard = LocalClipboardManager.current

    var editingPosition by remember { mutableStateOf(initialPosition) }
    var fenText by remember { mutableStateOf(initialPosition.toFen()) }
    var selectedPalettePiece by remember {
        mutableStateOf<ChessPiece?>(ChessPiece(PieceColor.WHITE, PieceType.QUEEN))
    }
    var eraserSelected by remember { mutableStateOf(false) }

    val validation = ChessGameRules.validatePosition(editingPosition)

    fun syncFenFromBoard(newPos: ChessPosition) {
        editingPosition = newPos
        fenText = newPos.toFen()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.editor_title),
                style = MaterialTheme.typography.headlineSmall
            )

            // Interactive Board in Editor Mode
            ChessBoardView(
                position = editingPosition,
                isFlipped = isFlipped,
                selectedSquare = null,
                legalTargets = emptyList(),
                lastMove = null,
                engineLines = emptyList(),
                showArrows = false,
                onSquareTapped = { square ->
                    val updated = if (eraserSelected) {
                        editingPosition.withPieceAt(square, null)
                    } else {
                        val existing = editingPosition.pieceAt(square)
                        if (existing == selectedPalettePiece) {
                            editingPosition.withPieceAt(square, null)
                        } else {
                            editingPosition.withPieceAt(square, selectedPalettePiece)
                        }
                    }
                    syncFenFromBoard(updated)
                },
                modifier = Modifier
                    .fillMaxWidth(0.76f)
                    .align(Alignment.CenterHorizontally)
            )

            // Piece Palette - White & Black + Eraser
            val whitePieces = PieceType.entries.map { ChessPiece(PieceColor.WHITE, it) }
            val blackPieces = PieceType.entries.map { ChessPiece(PieceColor.BLACK, it) }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    whitePieces.forEach { piece ->
                        PaletteCell(
                            piece = piece,
                            selected = !eraserSelected && selectedPalettePiece == piece,
                            onClick = {
                                eraserSelected = false
                                selectedPalettePiece = piece
                            }
                        )
                    }
                    // Eraser cell
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (eraserSelected) CoralError.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = if (eraserSelected) 1.5.dp else 1.dp,
                                color = if (eraserSelected) CoralError else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { eraserSelected = true }
                            .testTag("editor_eraser"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backspace,
                            contentDescription = "Hapus Bidak",
                            tint = if (eraserSelected) CoralError else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    blackPieces.forEach { piece ->
                        PaletteCell(
                            piece = piece,
                            selected = !eraserSelected && selectedPalettePiece == piece,
                            onClick = {
                                eraserSelected = false
                                selectedPalettePiece = piece
                            }
                        )
                    }
                    Spacer(modifier = Modifier.size(44.dp))
                }
            }

            // Turn & Castling Row
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = editingPosition.turn == PieceColor.WHITE,
                    onClick = { syncFenFromBoard(editingPosition.copy(turn = PieceColor.WHITE)) },
                    label = { Text(stringResource(R.string.turn_white)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TournamentEmerald.copy(alpha = 0.22f),
                        selectedLabelColor = TournamentEmerald
                    )
                )
                FilterChip(
                    selected = editingPosition.turn == PieceColor.BLACK,
                    onClick = { syncFenFromBoard(editingPosition.copy(turn = PieceColor.BLACK)) },
                    label = { Text(stringResource(R.string.turn_black)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TournamentEmerald.copy(alpha = 0.22f),
                        selectedLabelColor = TournamentEmerald
                    )
                )
                CastlingChip("K", editingPosition.castling.whiteKingSide) {
                    syncFenFromBoard(
                        editingPosition.copy(
                            castling = editingPosition.castling.copy(whiteKingSide = !editingPosition.castling.whiteKingSide)
                        )
                    )
                }
                CastlingChip("Q", editingPosition.castling.whiteQueenSide) {
                    syncFenFromBoard(
                        editingPosition.copy(
                            castling = editingPosition.castling.copy(whiteQueenSide = !editingPosition.castling.whiteQueenSide)
                        )
                    )
                }
                CastlingChip("k", editingPosition.castling.blackKingSide) {
                    syncFenFromBoard(
                        editingPosition.copy(
                            castling = editingPosition.castling.copy(blackKingSide = !editingPosition.castling.blackKingSide)
                        )
                    )
                }
                CastlingChip("q", editingPosition.castling.blackQueenSide) {
                    syncFenFromBoard(
                        editingPosition.copy(
                            castling = editingPosition.castling.copy(blackQueenSide = !editingPosition.castling.blackQueenSide)
                        )
                    )
                }
            }

            // Quick Board Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { syncFenFromBoard(ChessPosition.starting()) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Awal")
                }
                OutlinedButton(
                    onClick = { syncFenFromBoard(ChessPosition.empty(editingPosition.turn)) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ClearAll, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Kosongkan")
                }
            }

            // Direct FEN Input + Copy/Paste
            OutlinedTextField(
                value = fenText,
                onValueChange = { value ->
                    fenText = value
                    ChessPosition.fromFen(value)?.let { parsed ->
                        editingPosition = parsed
                    }
                },
                label = { Text("FEN") },
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 12.sp
                ),
                trailingIcon = {
                    Row {
                        IconButton(
                            onClick = {
                                val clip = clipboard.getText()?.text
                                if (!clip.isNullOrBlank()) {
                                    fenText = clip.trim()
                                    ChessPosition.fromFen(fenText)?.let { editingPosition = it }
                                }
                            }
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Tempel FEN", modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(fenText))
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Salin FEN", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_fen_input")
            )

            if (validation is BoardValidation.Invalid) {
                Text(
                    text = validation.reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CoralError
                )
            }

            Button(
                onClick = { onApply(editingPosition) },
                enabled = validation is BoardValidation.Valid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("editor_apply_button"),
                colors = ButtonDefaults.buttonColors(containerColor = TournamentEmerald)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Terapkan & Analisis", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun PaletteCell(
    piece: ChessPiece,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) TournamentEmerald.copy(alpha = 0.24f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) TournamentEmerald else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(5.dp)
            .testTag("palette_${piece.toFenChar()}"),
        contentAlignment = Alignment.Center
    ) {
        ChessPieceIcon(piece = piece, modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun CastlingChip(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    FilterChip(
        selected = checked,
        onClick = onToggle,
        label = {
            Text(
                text = label,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold
            )
        }
    )
}
