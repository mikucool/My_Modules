package com.books_goo_hzz.lib_slot_core

/**
 * 描述单条中奖线路的详细信息。
 *
 * @param lineId 连线的ID (0-19)，对应 Payline.allPaylines 的索引。
 * @param symbol 形成中奖的符号 (只能是 GoldCoin)。
 * @param count 连续符号的数量 (3, 4, or 5)。
 * @param winAmount 这条线赢得的金额。
 */
data class WinningLineInfo(
    val lineId: Int,
    val symbol: SlotSymbol.GoldCoin,
    val count: Int,
    val winAmount: Long
)

/**
 * 封装了一次完整的 Spin 操作的结果。
 *
 * @param grid 最终生成的 5x3 符号盘面。
 * @param winningLines 一个列表，包含所有在此次 Spin 中中奖的线路信息。如果未中奖，则为空列表。
 * @param totalWinAmount 本次 Spin 赢得的总金额。
 */
data class SpinResult(
    val grid: List<List<SlotSymbol>>,
    val winningLines: List<WinningLineInfo>,
    val totalWinAmount: Long
)
