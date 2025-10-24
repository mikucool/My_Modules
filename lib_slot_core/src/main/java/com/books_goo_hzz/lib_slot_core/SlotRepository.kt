package com.books_goo_hzz.lib_slot_core

import kotlinx.coroutines.flow.Flow

interface SlotRepository {
    fun spin(): Flow<SpinResult>
}
