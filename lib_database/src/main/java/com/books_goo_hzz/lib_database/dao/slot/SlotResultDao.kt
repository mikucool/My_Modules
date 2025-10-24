package com.books_goo_hzz.lib_database.dao.slot

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.books_goo_hzz.lib_database.entities.slot.SlotResultEntity

@Dao
interface SlotResultDao {
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertResult(result: SlotResultEntity)
}