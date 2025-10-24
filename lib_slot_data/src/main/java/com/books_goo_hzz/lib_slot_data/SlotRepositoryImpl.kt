package com.books_goo_hzz.lib_slot_data

import com.books_goo_hzz.lib_database.dao.slot.SlotResultDao
import com.books_goo_hzz.lib_slot_core.SlotRepository
import com.books_goo_hzz.lib_slot_core.SpinResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SlotRepositoryImpl @Inject constructor(
    private val slotResultDao: SlotResultDao
) : SlotRepository {

    override fun spin(): Flow<SpinResult> {
        TODO("Not yet implemented")
    }
}
