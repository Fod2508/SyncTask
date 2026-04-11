package com.phuc.synctask.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.phuc.synctask.model.Task

/**
 * Lớp cơ sở dữ liệu Room chính của ứng dụng SyncTask.
 * Singleton Pattern (Double-Checked Locking).
 */
@Database(entities = [Task::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    companion object {
        private const val DATABASE_NAME = "synctask_db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
