package com.books_goo_hzz.lib_slot_data

import com.books_goo_hzz.lib_database.dao.slot.SlotResultDao
import com.books_goo_hzz.lib_database.entities.slot.SlotResultEntity
import com.books_goo_hzz.lib_slot_core.SlotRepository
import com.books_goo_hzz.lib_slot_core.SlotResult
import com.books_goo_hzz.lib_slot_core.SlotSymbol
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class SlotRepositoryImpl @Inject constructor(
    private val slotResultDao: SlotResultDao
) : SlotRepository {

    private val symbols = SlotSymbol.entries.filter { it != SlotSymbol.EMPTY }

    override fun spin(): Flow<SlotResult> = flow {
        // Simulate network delay or heavy computation
        delay(1000)

        // Generate a random result
        val resultSymbols = List(3) { symbols.random() }
        val isWinning = resultSymbols.all { it == resultSymbols.first() }

        // Save the result to the database
        slotResultDao.insertResult(
            SlotResultEntity(
                symbols = resultSymbols,
                isWinning = isWinning
            )
        )

        emit(SlotResult(symbols = resultSymbols, isWinning = isWinning))
    }
}
