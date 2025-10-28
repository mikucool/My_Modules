package com.books_goo_hzz.lib_database.entities.slot

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "slot_results",
    indices = [Index(value = ["payout"], name = "idx_payout")]
)
data class SlotResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val payout: Int,
    val grid: String,
    val timestamp: Long = System.currentTimeMillis()
)
