package com.books_goo_hzz.feature_slot.mapper

import com.books_goo_hzz.feature_slot.R
import com.books_goo_hzz.lib_slot_core.SlotSymbol

/**
 * 将业务逻辑层的 SlotSymbol 映射到 UI 层的可绘制资源 ID。
 */
fun SlotSymbol.toDrawableResId(): Int {
    return when (this) {
        is SlotSymbol.GoldCoin -> when (this.name) {
            "symbol01" -> R.drawable.icon_symbol_01
            "symbol02" -> R.drawable.icon_symbol_02
            "symbol03" -> R.drawable.icon_symbol_03
            "symbol04" -> R.drawable.icon_symbol_04
            "symbol05" -> R.drawable.icon_symbol_05
            "symbol06" -> R.drawable.icon_symbol_06
            "symbol07" -> R.drawable.icon_symbol_07
            "symbol08" -> R.drawable.icon_symbol_08
            "symbol09" -> R.drawable.icon_symbol_09
            "symbol10" -> R.drawable.icon_symbol_10
            else -> android.R.color.transparent
        }
        is SlotSymbol.Wild -> R.drawable.icon_symbol_wild
        is SlotSymbol.GreenBill -> R.drawable.icon_symbol_green_bill
        is SlotSymbol.BonusGame -> R.drawable.icon_symbol_bonus
        is SlotSymbol.FreeGame -> R.drawable.icon_symbol_free_game
        is SlotSymbol.Empty -> android.R.color.transparent
    }
}
