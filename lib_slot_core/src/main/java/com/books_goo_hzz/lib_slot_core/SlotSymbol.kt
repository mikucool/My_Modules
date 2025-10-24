package com.books_goo_hzz.lib_slot_core

/**
 * 使用密封类来定义游戏中的所有符号。
 * 这比枚举更强大，因为它允许不同类型的符号拥有不同的属性。
 */
sealed class SlotSymbol {
    /**
     * 代表可参与连线计算并有赔率的金币符号。
     * @param name 符号的唯一标识符。
     * @param payouts 一个Map，Key是连线数量(3, 4, 5)，Value是对应的赔率。
     */
    data class GoldCoin(
        val name: String,
        val payouts: Map<Int, Int>
    ) : SlotSymbol()

    /**
     * 代表万能符号，它可以替代任何 GoldCoin 符号。
     */
    data object Wild : SlotSymbol()

    /**
     * 代表绿钞符号，不参与连线计算。
     */
    data object GreenBill : SlotSymbol()

    /**
     * 代表奖励游戏符号，不参与连线计算。
     */
    data object BonusGame : SlotSymbol()

    /**
     * 代表免费游戏符号，不参与连线计算。
     */
    data object FreeGame : SlotSymbol()

    /**
     * 代表一个空位置，用于盘面生成过程。
     */
    data object Empty : SlotSymbol()
}
