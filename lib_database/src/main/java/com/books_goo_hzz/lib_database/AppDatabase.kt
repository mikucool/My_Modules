package com.books_goo_hzz.lib_database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.books_goo_hzz.lib_database.dao.slot.SlotResultDao
import com.books_goo_hzz.lib_database.entities.slot.SlotResultEntity

@Database(entities = [SlotResultEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun slotResultDao(): SlotResultDao
}
