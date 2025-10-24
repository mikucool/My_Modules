package com.books_goo_hzz.lib_database.entities.slot

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.books_goo_hzz.lib_slot_core.SlotSymbol

@Entity(tableName = "slot_results")
data class SlotResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val symbols: List<SlotSymbol>,
    val isWinning: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)