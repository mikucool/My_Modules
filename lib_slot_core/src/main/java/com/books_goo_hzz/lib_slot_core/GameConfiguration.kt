package com.books_goo_hzz.lib_slot_core

/**
 * 一个单例对象，用于存放游戏中所有静态的配置数据，例如符号列表和赔率。
 */
object GameConfiguration {

    /**
     * 包含了所有10个可赢钱的金币符号的完整列表。
     * 数据来源于设计文档。
     */
    val goldCoinSymbols: List<SlotSymbol.GoldCoin> = listOf(
        SlotSymbol.GoldCoin("symbol01", mapOf(3 to 10, 4 to 60, 5 to 150)),
        SlotSymbol.GoldCoin("symbol02", mapOf(3 to 15, 4 to 65, 5 to 260)),
        SlotSymbol.GoldCoin("symbol03", mapOf(3 to 20, 4 to 70, 5 to 490)),
        SlotSymbol.GoldCoin("symbol04", mapOf(3 to 25, 4 to 75, 5 to 500)),
        SlotSymbol.GoldCoin("symbol05", mapOf(3 to 10, 4 to 55, 5 to 80)),
        SlotSymbol.GoldCoin("symbol06", mapOf(3 to 5, 4 to 30, 5 to 55)),
        SlotSymbol.GoldCoin("symbol07", mapOf(3 to 5, 4 to 35, 5 to 60)),
        SlotSymbol.GoldCoin("symbol08", mapOf(3 to 5, 4 to 40, 5 to 65)),
        SlotSymbol.GoldCoin("symbol09", mapOf(3 to 5, 4 to 45, 5 to 70)),
        SlotSymbol.GoldCoin("symbol10", mapOf(3 to 5, 4 to 50, 5 to 75))
    )

    /**
     * 所有参与连线计算的符号（金币 + Wild）。
     */
    val winningSymbols: List<SlotSymbol> = goldCoinSymbols + SlotSymbol.Wild

    /**
     * 所有可能出现在转轴上的符号。
     */
    val allSymbols: List<SlotSymbol> = goldCoinSymbols + listOf(
        SlotSymbol.Wild,
        SlotSymbol.GreenBill,
        SlotSymbol.BonusGame,
        SlotSymbol.FreeGame
    )
}
