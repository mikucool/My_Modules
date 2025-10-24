package com.books_goo_hzz.lib_slot_core

import kotlin.collections.map

/**
 * 封装了老虎机核心算法的类。
 */
class SlotMachine {

    /**
     * 根据给定的5x3盘面和总押注，计算并返回详细的赢分结果。
     * 这个方法是老虎机游戏最核心的“规则引擎”和“校验器”。
     *
     * @param grid 要计算的 5x3 符号盘面。
     * @param totalBet 本次 spin 的总押注额。
     * @return 一个 [SpinResult] 对象，包含了所有赢奖线路的详情和总赢分。
     */
    fun calculateWinnings(grid: List<List<SlotSymbol>>, totalBet: Long): SpinResult {
        val winningLines = mutableListOf<WinningLineInfo>()

        // 遍历所有20条payline
        Payline.allPaylines.forEachIndexed { lineId, paylineCoordinates ->
            // 1. 从盘面(grid)中提取出当前payline上的5个符号
            val lineSymbols = paylineCoordinates.map { point -> grid[point.y][point.x] }

            // 2. 检查连线是否可能中奖：必须以金币符号或Wild符号开头
            val firstSymbol = lineSymbols.first()
            if (firstSymbol !is SlotSymbol.GoldCoin && firstSymbol !is SlotSymbol.Wild) {
                return@forEachIndexed // 如果不是，这条线不可能中奖，直接跳到下一条线
            }

            // 3. 确定这条线的中奖符号是什么。
            // Wild可以替代任何金币符号，所以我们需要找到这条线上第一个出现的“具体”金币符号。
            val actualSymbol: SlotSymbol.GoldCoin? = lineSymbols.filterIsInstance<SlotSymbol.GoldCoin>().firstOrNull()

            // 如果一条线上全是Wild，或者只有Wild和不参与赢奖的符号（绿钞等），则无法确定中奖符号，不计算中奖。
            // 这是一个合理的业务假设，因为文档没有定义全Wild的赔率。
            if (actualSymbol == null) {
                return@forEachIndexed
            }

            // 4. 从左到右计算连续的符号数量
            var consecutiveCount = 0
            for (symbol in lineSymbols) {
                // 如果当前符号与中奖符号相同，或是Wild，则连线继续
                if (symbol == actualSymbol || symbol is SlotSymbol.Wild) {
                    consecutiveCount++
                } else {
                    break // 连线中断
                }
            }

            // 5. 根据文档，至少3连才算中奖
            if (consecutiveCount >= 3) {
                // 根据连线数量(consecutiveCount)获取赔率
                val payout = actualSymbol.payouts[consecutiveCount]

                if (payout != null) {
                    // 根据文档公式计算这条线的赢分
                    val lineWin = (totalBet * payout) / 20L

                    // 记录这条中奖线路的详细信息
                    winningLines.add(
                        WinningLineInfo(
                            lineId = lineId,
                            symbol = actualSymbol,
                            count = consecutiveCount,
                            winAmount = lineWin
                        )
                    )
                }
            }
        }

        // 6. 将所有中奖线路的赢分相加，得到总赢分
        val totalWinAmount = winningLines.sumOf { it.winAmount }

        // 7. 返回包含所有信息的最终结果
        return SpinResult(
            grid = grid,
            winningLines = winningLines,
            totalWinAmount = totalWinAmount
        )
    }
}
