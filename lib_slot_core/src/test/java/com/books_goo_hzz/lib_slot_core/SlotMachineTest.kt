package com.books_goo_hzz.lib_slot_core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SlotMachineTest {

    private val slotMachine = SlotMachine()
    private val totalBet: Long = 2000L

    private fun emptyGrid(): MutableList<MutableList<SlotSymbol>> =
        MutableList(3) { MutableList(5) { SlotSymbol.GreenBill } }

    /**
     * 构造一个网格：中间行按给定的5列符号排布，其它行用不参与连线计算的符号填充。
     */
    private fun gridWithMiddleRow(vararg middleRow: SlotSymbol): List<List<SlotSymbol>> {
        require(middleRow.size == 5)
        val grid = emptyGrid()
        for (x in 0 until 5) {
            grid[1][x] = middleRow[x]
        }
        return grid.map { it.toList() }
    }

    @Test
    fun no_win_when_first_symbol_not_gold_or_wild() {
        // 首个符号是 GreenBill，则该线直接判定不中奖
        val grid = gridWithMiddleRow(
            SlotSymbol.GreenBill,
            SlotSymbol.Wild,
            SlotSymbol.Wild,
            SlotSymbol.Wild,
            SlotSymbol.Wild
        )

        val result = slotMachine.calculateWinnings(grid, totalBet)
        assertTrue(result.winningLines.isEmpty())
        assertEquals(0L, result.totalWinAmount)
    }

    @Test
    fun three_in_a_row_gold_coin_wins_with_expected_amount() {
        val symbol01 = GameConfiguration.goldCoinSymbols.first { it.name == "symbol01" }
        // 中间行前三列为 symbol01，后两列为非参与符号
        val grid = gridWithMiddleRow(
            symbol01,
            symbol01,
            symbol01,
            SlotSymbol.GreenBill,
            SlotSymbol.GreenBill
        )

        val result = slotMachine.calculateWinnings(grid, totalBet)
        // 期望命中1条线（第2条：中间行），3连赔率=10 → 单线赢分 = (2000*10)/20 = 1000
        assertEquals(1, result.winningLines.size)
        assertEquals(1000L, result.totalWinAmount)
        val win = result.winningLines.first()
        assertEquals(3, win.count)
        assertEquals(symbol01, win.symbol)
    }

    @Test
    fun wild_can_substitute_gold_coin_in_streak() {
        val symbol02 = GameConfiguration.goldCoinSymbols.first { it.name == "symbol02" }
        // 形如: symbol02, Wild, symbol02, x, x → 从左到右连续=3（中间的 Wild 替代）
        val grid = gridWithMiddleRow(
            symbol02,
            SlotSymbol.Wild,
            symbol02,
            SlotSymbol.GreenBill,
            SlotSymbol.GreenBill
        )

        val result = slotMachine.calculateWinnings(grid, totalBet)
        // symbol02 三连赔率=15 → 单线赢分 = (2000*15)/20 = 1500
        assertEquals(1, result.winningLines.size)
        assertEquals(1500L, result.totalWinAmount)
        val win = result.winningLines.first()
        assertEquals(3, win.count)
        assertEquals(symbol02, win.symbol)
    }

    @Test
    fun only_wilds_without_any_gold_coin_is_not_a_win() {
        // 全是 Wild 但线中没有任何金币，则无法确定实际中奖符号，应不计奖
        val grid = gridWithMiddleRow(
            SlotSymbol.Wild,
            SlotSymbol.Wild,
            SlotSymbol.Wild,
            SlotSymbol.Wild,
            SlotSymbol.Wild
        )

        val result = slotMachine.calculateWinnings(grid, totalBet)
        assertTrue(result.winningLines.isEmpty())
        assertEquals(0L, result.totalWinAmount)
    }

    @Test
    fun five_in_a_row_uses_highest_payout_only() {
        val symbol03 = GameConfiguration.goldCoinSymbols.first { it.name == "symbol03" }
        // 连续5个 symbol03，应按5连赔率结算，不叠加3/4连
        val grid = gridWithMiddleRow(
            symbol03,
            symbol03,
            symbol03,
            symbol03,
            symbol03
        )

        val result = slotMachine.calculateWinnings(grid, totalBet)
        // symbol03 五连赔率=490 → 单线赢分 = (2000*490)/20 = 49,000
        assertEquals(1, result.winningLines.size)
        assertEquals(49_000L, result.totalWinAmount)
        val win = result.winningLines.first()
        assertEquals(5, win.count)
        assertEquals(symbol03, win.symbol)
    }
}


