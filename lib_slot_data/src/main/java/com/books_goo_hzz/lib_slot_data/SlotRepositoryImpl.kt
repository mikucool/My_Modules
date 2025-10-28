package com.books_goo_hzz.lib_slot_data

import com.books_goo_hzz.lib_database.dao.slot.SlotResultDao
import com.books_goo_hzz.lib_slot_core.SlotRepository
import com.books_goo_hzz.lib_slot_core.SpinResult
import com.books_goo_hzz.lib_slot_data.mapper.SlotResultMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SlotRepositoryImpl @Inject constructor(
    private val slotResultDao: SlotResultDao
) : SlotRepository {

    override fun getResultByPayout(payout: Int): Flow<SpinResult> {
        // 从DAO获取Flow<SlotResultEntity?>
        return slotResultDao.findByPayout(payout)
            .map { entity ->
                // 如果entity为null（数据库中没有对应的payout），则抛出异常
                requireNotNull(entity) { "No pre-generated result found for payout: $payout" }
                // 使用Mapper将Entity转换为业务对象SpinResult
                SlotResultMapper.mapToSpinResult(entity)
            }
    }
}
