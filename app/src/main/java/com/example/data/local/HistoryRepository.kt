package com.example.data.local

import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val dao: SavedPositionDao) {
    val recentPositions: Flow<List<SavedPositionEntity>> = dao.observeRecentPositions()

    suspend fun savePosition(
        fen: String,
        label: String,
        bestMoveSan: String,
        evalText: String,
        sourceLabel: String
    ) {
        dao.insertPosition(
            SavedPositionEntity(
                fen = fen,
                label = label,
                bestMoveSan = bestMoveSan,
                evalText = evalText,
                sourceLabel = sourceLabel
            )
        )
    }

    suspend fun deletePosition(id: Long) = dao.deleteById(id)

    suspend fun clearAll() = dao.clearAll()
}
