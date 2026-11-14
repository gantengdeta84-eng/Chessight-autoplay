package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SavedPositionEntity::class], version = 1, exportSchema = false)
abstract class ChessSightDatabase : RoomDatabase() {
    abstract fun savedPositionDao(): SavedPositionDao

    companion object {
        @Volatile
        private var INSTANCE: ChessSightDatabase? = null

        fun getInstance(context: Context): ChessSightDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ChessSightDatabase::class.java,
                    "chesssight_local.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
