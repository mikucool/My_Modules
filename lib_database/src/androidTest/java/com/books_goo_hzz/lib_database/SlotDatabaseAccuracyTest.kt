package com.books_goo_hzz.lib_database

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.books_goo_hzz.lib_database.dao.slot.SlotResultDao
import com.books_goo_hzz.lib_database.entities.slot.SlotResultEntity
import com.books_goo_hzz.lib_slot_core.GameConfiguration
import com.books_goo_hzz.lib_slot_core.SlotMachine
import com.books_goo_hzz.lib_slot_core.SlotSymbol
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * 验证数据库中存储的结果网格的准确性。
 * 
 * 测试内容：
 * 1. 验证数据库中存储的payout是否准确（使用SlotMachine.calculateWinnings重新计算）
 * 2. 验证网格格式正确（5x3，有效的SlotSymbol）
 * 3. 验证不同payout范围的覆盖情况（采样测试）
 */
@RunWith(AndroidJUnit4::class)
class SlotDatabaseAccuracyTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: SlotResultDao
    private val slotMachine = SlotMachine()
    private val TOTAL_BET = 2000L

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "my_modules_database"
        )
        .createFromAsset("slot_results.db") // 从assets目录加载预生成的数据库
        .build()
        dao = database.slotResultDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    /**
     * 测试1：验证网格格式（5x3，有效符号）
     */
    @Test
    fun test_grid_format_is_valid() = runBlocking {
        // 采样测试多个payout值
        val testPayouts = listOf(5, 50, 100, 500, 1000, 2000, 5000)
        
        for (payout in testPayouts) {
            val entity = dao.findByPayout(payout).first()
            
            if (entity != null) {
                val grid = deserializeGrid(entity.grid)
                
                // 验证网格尺寸：必须是3行5列
                assertEquals("Grid should have 3 rows for payout $payout", 3, grid.size)
                grid.forEachIndexed { rowIndex, row ->
                    assertEquals("Row $rowIndex should have 5 columns for payout $payout", 5, row.size)
                }
                
                // 验证所有符号都是有效的
                grid.forEach { row ->
                    row.forEach { symbol ->
                        assertNotNull("Symbol should not be null for payout $payout", symbol)
                        assertTrue(
                            "Symbol should be valid (GoldCoin, Wild, GreenBill, BonusGame, FreeGame, or Empty) for payout $payout",
                            symbol is SlotSymbol.GoldCoin ||
                            symbol is SlotSymbol.Wild ||
                            symbol is SlotSymbol.GreenBill ||
                            symbol is SlotSymbol.BonusGame ||
                            symbol is SlotSymbol.FreeGame ||
                            symbol is SlotSymbol.Empty
                        )
                    }
                }
            }
        }
    }

    /**
     * 测试2：验证payout准确性（重新计算应该等于存储的payout）
     */
    @Test
    fun test_payout_accuracy() = runBlocking {
        // 采样测试多个payout值，覆盖不同范围
        val testPayouts = listOf(
            5, 10, 15, 20, 25,  // 低倍率
            50, 100, 150, 200, 250,  // 中倍率
            500, 1000, 1500, 2000,  // 高倍率
            5000, 10000  // 超高倍率（如果有）
        )
        
        var totalTested = 0
        var totalPassed = 0
        
        for (payout in testPayouts) {
            val entity = dao.findByPayout(payout).first()
            
            if (entity != null) {
                totalTested++
                
                val grid = deserializeGrid(entity.grid)
                val result = slotMachine.calculateWinnings(grid, TOTAL_BET)
                
                // 计算实际的payout：totalWinAmount * 20 / TOTAL_BET
                val actualPayout = (result.totalWinAmount * 20 / TOTAL_BET).toInt()
                
                assertEquals(
                    "Stored payout ($payout) should match calculated payout ($actualPayout) for grid",
                    payout,
                    actualPayout
                )
                
                totalPassed++
            }
        }
        
        println("Payout accuracy test: $totalPassed/$totalTested passed")
        assertTrue("At least some payout values should be tested", totalTested > 0)
    }

    /**
     * 测试3：验证payout范围的覆盖情况
     */
    @Test
    fun test_payout_coverage() = runBlocking {
        // 检查几个关键的payout范围是否有数据
        val ranges = listOf(
            5..50 step 5,      // 低倍率范围
            100..500 step 50,  // 中倍率范围
            1000..5000 step 500, // 高倍率范围
        )
        
        val coverage = mutableMapOf<String, Pair<Int, Int>>() // range -> (found, total)
        
        for (range in ranges) {
            var found = 0
            var total = 0
            val rangeName = "${range.first}..${range.last}"
            
            for (payout in range) {
                total++
                val entity = dao.findByPayout(payout).first()
                if (entity != null) {
                    found++
                }
            }
            
            coverage[rangeName] = found to total
            val coverageRate = if (total > 0) (found * 100.0 / total) else 0.0
            println("Payout range $rangeName: $found/$total (${String.format("%.1f", coverageRate)}%)")
        }
        
        // 至少低倍率范围应该有较高的覆盖率
        val lowRangeCoverage = coverage["5..50"] ?: (0 to 1)
        assertTrue(
            "Low payout range (5..50) should have good coverage",
            lowRangeCoverage.first > lowRangeCoverage.second * 0.5 // 至少50%覆盖率
        )
    }

    /**
     * 测试4：验证每个网格都至少有一个中奖线（payout > 0时）
     */
    @Test
    fun test_non_zero_payout_has_winning_lines() = runBlocking {
        val testPayouts = listOf(5, 10, 50, 100, 500, 1000)
        
        for (payout in testPayouts) {
            if (payout == 0) continue // 跳过0 payout
            
            val entity = dao.findByPayout(payout).first()
            
            if (entity != null) {
                val grid = deserializeGrid(entity.grid)
                val result = slotMachine.calculateWinnings(grid, TOTAL_BET)
                
                assertTrue(
                    "Payout $payout should have at least one winning line",
                    result.winningLines.isNotEmpty()
                )
                
                assertTrue(
                    "Payout $payout should have positive win amount",
                    result.totalWinAmount > 0
                )
            }
        }
    }

    /**
     * 辅助函数：反序列化网格字符串
     * 与Generator.kt中的序列化格式保持一致
     */
    private fun deserializeGrid(gridString: String): List<List<SlotSymbol>> {
        val symbolMap: Map<String, SlotSymbol> = (
            GameConfiguration.goldCoinSymbols +
            listOf(
                SlotSymbol.Wild,
                SlotSymbol.GreenBill,
                SlotSymbol.BonusGame,
                SlotSymbol.FreeGame,
                SlotSymbol.Empty
            )
        ).associateBy {
            when (it) {
                is SlotSymbol.GoldCoin -> it.name
                is SlotSymbol.Wild -> "WILD"
                is SlotSymbol.GreenBill -> "GREEN_BILL"
                is SlotSymbol.BonusGame -> "BONUS"
                is SlotSymbol.FreeGame -> "FREE_GAME"
                is SlotSymbol.Empty -> "EMPTY"
            }
        }
        
        return gridString.split(";").map { rowString ->
            rowString.split(",").map { symbolName ->
                symbolMap[symbolName] ?: throw IllegalArgumentException("Unknown symbol name: $symbolName")
            }
        }
    }
}

