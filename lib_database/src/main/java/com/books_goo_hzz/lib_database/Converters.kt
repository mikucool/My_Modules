package com.books_goo_hzz.lib_database

import androidx.room.TypeConverter
import com.books_goo_hzz.lib_slot_core.SlotSymbol

class Converters {
    @TypeConverter
    fun fromSymbolList(symbols: List<SlotSymbol>): String {
        return symbols.joinToString(",") { it.name }
    }

    @TypeConverter
    fun toSymbolList(data: String): List<SlotSymbol> {
        return data.split(",").map { SlotSymbol.valueOf(it) }
    }
}
