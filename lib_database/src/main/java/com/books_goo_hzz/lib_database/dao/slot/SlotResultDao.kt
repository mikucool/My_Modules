package com.books_goo_hzz.lib_database.dao.slot

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.books_goo_hzz.lib_database.entities.slot.SlotResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SlotResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: SlotResultEntity)

    /**
     * 根据给定的payout值，从数据库中随机查询一个匹配的结果。
     *
     * @param payout 要查询的赔率。
     * @return 一个包含单个 nullable SlotResultEntity 的 Flow。如果没有找到匹配项，则Flow将发出null。
     */
    @Query("SELECT * FROM slot_results WHERE payout = :payout ORDER BY RANDOM() LIMIT 1")
    fun findByPayout(payout: Int): Flow<SlotResultEntity?>
}