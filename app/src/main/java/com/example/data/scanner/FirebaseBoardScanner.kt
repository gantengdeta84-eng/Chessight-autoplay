package com.example.data.scanner

import android.graphics.Bitmap
import android.util.Log
import com.example.chess.ChessGameRules
import com.example.chess.ChessPosition
import com.example.chess.PieceColor
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "FirebaseBoardScanner"

data class BoardScanResult(
    val fen: String,
    val bottomSide: PieceColor,
    val confidenceNote: String
)

class FirebaseBoardScanner {

    private val boardSchema = Schema.obj(
        properties = mapOf(
            "isBoardDetected" to Schema.boolean(
                description = "True if the image clearly contains a chess board (physical or digital screenshot)"
            ),
            "rank8" to Schema.string(description = "FEN string for rank 8 (top row when White is at bottom, e.g. 'rnbqkbnr' or '4k3')"),
            "rank7" to Schema.string(description = "FEN string for rank 7"),
            "rank6" to Schema.string(description = "FEN string for rank 6"),
            "rank5" to Schema.string(description = "FEN string for rank 5"),
            "rank4" to Schema.string(description = "FEN string for rank 4"),
            "rank3" to Schema.string(description = "FEN string for rank 3"),
            "rank2" to Schema.string(description = "FEN string for rank 2"),
            "rank1" to Schema.string(description = "FEN string for rank 1 (bottom row when White is at bottom)"),
            "suggestedTurn" to Schema.enumeration(
                values = listOf("w", "b"),
                description = "Side to move: 'w' for White or 'b' for Black"
            ),
            "bottomPerspective" to Schema.enumeration(
                values = listOf("white", "black"),
                description = "Which side's pieces are closer to the bottom of the image"
            ),
            "notes" to Schema.string(
                description = "Brief note in Indonesian about board clarity or any uncertain squares"
            )
        ),
        optionalProperties = listOf("notes")
    )

    private val scannerModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = boardSchema
                temperature = 0.1f
            }
        )
    }

    private val coachModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.35f
            }
        )
    }

    suspend fun scanBoard(
        bitmap: Bitmap,
        sideToMove: PieceColor,
        whiteAtBottomHint: Boolean
    ): Result<BoardScanResult> {
        return try {
            val perspectiveLabel = if (whiteAtBottomHint) "White pieces at the bottom (rank 1 at bottom, rank 8 at top)" else "Black pieces at the bottom (rank 8 at bottom, rank 1 at top)"
            val turnLabel = if (sideToMove == PieceColor.WHITE) "White ('w')" else "Black ('b')"

            val promptText = """
                You are an expert chess board optical scanner. Carefully inspect every square of the 8x8 chess board in this image (whether a physical wooden/plastic board or a digital chess app screenshot).
                Orientation hint from user: $perspectiveLabel.
                Turn to move hint: $turnLabel.

                Always output rank8 (a8..h8) through rank1 (a1..h1) in standard FEN notation from file 'a' on the left to file 'h' on the right (remember: if Black is at the bottom of the photo, flip your reading so rank8..rank1 are still canonical FEN ranks a8..h8 to a1..h1!).
                Use standard FEN piece letters:
                - White uppercase: K, Q, R, B, N, P
                - Black lowercase: k, q, r, b, n, p
                - Consecutive empty squares: digits 1..8 (e.g. '8' for an empty rank, '4p3' for pawn on e-file).
                Every rank string MUST represent exactly 8 squares.
                Double-check Knight ('N'/'n') vs Bishop ('B'/'b') and Queen ('Q'/'q') vs King ('K'/'k') shapes carefully.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                scannerModel.generateContent(
                    content {
                        image(bitmap)
                        text(promptText)
                    }
                )
            }

            val jsonText = response.text
            if (jsonText.isNullOrBlank()) {
                return Result.failure(IllegalStateException("Gemini tidak mengembalikan data papan."))
            }

            val json = JSONObject(jsonText)
            val detected = json.optBoolean("isBoardDetected", false)
            if (!detected) {
                val note = json.optString("notes").takeIf { it.isNotBlank() }
                    ?: "Papan catur tidak terdeteksi dengan jelas pada gambar."
                return Result.failure(IllegalStateException(note))
            }

            val rows = listOf(
                json.optString("rank8", "8"),
                json.optString("rank7", "8"),
                json.optString("rank6", "8"),
                json.optString("rank5", "8"),
                json.optString("rank4", "8"),
                json.optString("rank3", "8"),
                json.optString("rank2", "8"),
                json.optString("rank1", "8")
            )

            val normalizedFen = ChessGameRules.normalizeRankRowsToFen(
                rows = rows,
                turn = sideToMove
            ) ?: return Result.failure(IllegalStateException("Format petak papan dari pemindai tidak valid."))

            // Verify parsed position is structurally sound
            ChessPosition.fromFen(normalizedFen)
                ?: return Result.failure(IllegalStateException("Gagal mengonversi hasil pindaian ke FEN."))

            val perspective = if (json.optString("bottomPerspective") == "black") {
                PieceColor.BLACK
            } else {
                PieceColor.WHITE
            }
            val notes = json.optString("notes", "Papan berhasil dipindai.")

            Result.success(
                BoardScanResult(
                    fen = normalizedFen,
                    bottomSide = perspective,
                    confidenceNote = notes
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Board scan timed out", e)
            Result.failure(IllegalStateException("Pemindaian papan melebihi batas waktu 60 detik. Coba foto ulang lebih dekat."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI board scan failed", e)
            Result.failure(IllegalStateException(e.localizedMessage ?: "Gagal memindai papan dengan Gemini AI."))
        }
    }

    fun explainMoveStream(
        fen: String,
        bestMoveSan: String,
        continuationSan: List<String>,
        evalText: String
    ): Flow<String> = flow {
        val lineStr = continuationSan.joinToString(" ")
        val prompt = """
            Kamu adalah pelatih catur Grandmaster yang ringkas.
            Posisi FEN: $fen
            Langkah terbaik Stockfish: $bestMoveSan (Evaluasi: $evalText)
            Garis lanjutan: $lineStr

            Jelaskan dalam maksimal 2–3 kalimat singkat berbahasa Indonesia yang jelas mengapa langkah $bestMoveSan kuat di posisi ini (misal ancaman taktis, kontrol petak pusat, keselamatan raja, atau pengembangan bidak). Langsung ke inti tanpa pembukaan basa-basi.
        """.trimIndent()

        val acc = StringBuilder()
        try {
            withTimeout(60_000L) {
                coachModel.generateContentStream(prompt).collect { chunk ->
                    val part = chunk.text.orEmpty()
                    if (part.isNotEmpty()) {
                        acc.append(part)
                        emit(acc.toString())
                    }
                }
            }
            if (acc.isBlank()) {
                throw IllegalStateException("Penjelasan AI kosong.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Move explanation timed out", e)
            throw IllegalStateException("Permintaan penjelasan melebihi batas waktu.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Move explanation failed", e)
            throw e
        }
    }
}
