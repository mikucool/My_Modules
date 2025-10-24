package com.books_goo_hzz.lib_slot_core

import java.sql.Connection
import java.sql.DriverManager
import kotlin.random.Random

const val TARGET_DB_NAME = "slot_results.db"
const val TOTAL_BET = 2000L // 总押注额，与calculateWinnings的输入保持一致
const val MAX_ATTEMPTS_PER_COMBINATION = 100 // 为单个赢奖组合尝试生成有效盘面的最大次数
const val MAX_SOLUTIONS_TO_FIND_PER_PAYOUT = 100 // 熔断机制：为每个赔率最多寻找多少种理论组合

// --- 分层生成策略定义 ---
data class GenerationStrategy(val maxCombinationSize: Int, val maxGridsPerPayout: Int)

private val STRATEGY_A = GenerationStrategy(maxCombinationSize = 5, maxGridsPerPayout = 10)
private val STRATEGY_B = GenerationStrategy(maxCombinationSize = 20, maxGridsPerPayout = 5)
private val STRATEGY_C = GenerationStrategy(maxCombinationSize = 20, maxGridsPerPayout = 1)

/** 根据赔率选择不同的生成策略 */
private fun selectStrategy(payout: Int): GenerationStrategy {
    return when {
        payout <= 1000 -> STRATEGY_A
        payout <= 2000 -> STRATEGY_B
        else -> STRATEGY_C
    }
}

/**
 * 数据类，用于封装一个具体的赢奖组合。
 * 例如: symbol01的3连，赔率为10。
 */
data class WinningCombination(val symbol: SlotSymbol.GoldCoin, val count: Int, val payout: Int)

// 提前计算好所有可能的赢奖组合，并按赔率降序排列，有助于优化搜索。
private val allPossibleWins: List<WinningCombination> by lazy {
    GameConfiguration.goldCoinSymbols.flatMap { coin ->
        coin.payouts.map { (count, payout) ->
            WinningCombination(coin, count, payout)
        }
    }.sortedByDescending { it.payout }
}

fun main() {
    println("Starting result grid generation...")
    val startTime = System.currentTimeMillis()

    val connection = DriverManager.getConnection("jdbc:sqlite:$TARGET_DB_NAME")
    createTables(connection)

    val slotMachine = SlotMachine()
    var totalGridsGenerated = 0

    // --- 核心生成逻辑 ---
    // 我们将为 5 到 10000 范围内的所有赔率（步长为5）生成结果盘
    for (targetPayout in 5..1000 step 5) {
        val strategy = selectStrategy(targetPayout)
        println("\n--- Processing target payout: $targetPayout (Strategy: maxCombo=${strategy.maxCombinationSize}, maxGrids=${strategy.maxGridsPerPayout}) ---")

        val combinations = findWinningCombinations(targetPayout, strategy.maxCombinationSize)
        println("Found ${combinations.size} theoretical combinations.")

        var gridsForThisPayout = 0
        // 遍历每一种赢奖组合方案
        for (combo in combinations) {
            if (gridsForThisPayout >= strategy.maxGridsPerPayout) {
                println("  Reached max grids limit for payout $targetPayout. Moving to next payout.")
                break
            }

            val remainingLimit = strategy.maxGridsPerPayout - gridsForThisPayout
            val validGrids = generateValidGridsForCombination(combo, targetPayout, slotMachine, remainingLimit)

            if (validGrids.isNotEmpty()) {
                println("  Successfully generated ${validGrids.size} valid grid(s) for combination: ${combo.map { it.payout }}")
                validGrids.forEach { grid ->
                    insertGrid(connection, grid, targetPayout)
                    gridsForThisPayout++
                    totalGridsGenerated++
                }
            }
        }
        println("Generated a total of $gridsForThisPayout grid(s) for payout $targetPayout.")
    }

    connection.close()
    val duration = (System.currentTimeMillis() - startTime) / 1000.0
    println("\nGeneration finished in ${duration}s. Total grids in DB: $totalGridsGenerated.")
    println("Database '$TARGET_DB_NAME' is ready.")
}

/**
 * 为一个给定的赢奖组合(combination)，尝试生成多个有效的结果盘。
 * @param limit 最多生成多少个有效盘面。
 * @return 一个包含多个有效结果盘的列表。
 */
fun generateValidGridsForCombination(
    combination: List<WinningCombination>,
    targetPayout: Int,
    slotMachine: SlotMachine,
    limit: Int
): List<List<List<SlotSymbol>>> {
    val validGrids = mutableListOf<List<List<SlotSymbol>>>()

    // 尝试多次，以寻找不同的随机填充结果
    for (attempt in 1..MAX_ATTEMPTS_PER_COMBINATION) {
        val grid: MutableList<MutableList<SlotSymbol>> = MutableList(3) { MutableList(5) { SlotSymbol.Empty } }
        val placementSuccessful = placeWinsRecursive(combination, grid, Payline.allPaylines.indices.toMutableSet())

        if (placementSuccessful) {
            padGrid(grid) // 用随机非赢奖符号填充空白区域

            // 最终校验：调用我们的规则引擎来计算最终赢分
            val result = slotMachine.calculateWinnings(grid, TOTAL_BET)
            val finalPayout = (result.totalWinAmount * 20 / TOTAL_BET).toInt()

            // 如果最终赢分与目标完全一致，则此盘面有效！
            if (finalPayout == targetPayout) {
                validGrids.add(grid.map { it.toList() }) // 保存盘面快照
                if (validGrids.size >= limit) {
                    break
                }
            }
        }
    }
    return validGrids
}

/**
 * 递归函数，尝试将赢奖组合列表不冲突地摆放到盘面上。
 * @return 如果成功找到一种摆法，返回true；否则返回false。
 */
private fun placeWinsRecursive(
    winsToPlace: List<WinningCombination>,
    grid: MutableList<MutableList<SlotSymbol>>,
    availablePaylines: MutableSet<Int>
): Boolean {
    if (winsToPlace.isEmpty()) {
        return true // 所有赢奖都已成功摆放
    }

    val win = winsToPlace.first()
    val remainingWins = winsToPlace.drop(1)

    for (lineId in availablePaylines.shuffled(Random)) {
        val payline = Payline.allPaylines[lineId]
        val positionsToPlace = payline.take(win.count)

        val canPlace = positionsToPlace.all { point -> grid[point.y][point.x] == SlotSymbol.Empty || grid[point.y][point.x] == win.symbol }

        if (canPlace) {
            val originalSymbols = positionsToPlace.map { point -> grid[point.y][point.x] }
            positionsToPlace.forEach { point -> grid[point.y][point.x] = win.symbol }

            val nextAvailablePaylines = availablePaylines.toMutableSet().apply { remove(lineId) }
            if (placeWinsRecursive(remainingWins, grid, nextAvailablePaylines)) {
                return true
            }

            positionsToPlace.forEachIndexed { index, point ->
                grid[point.y][point.x] = originalSymbols[index]
            }
        }
    }
    return false
}

private fun padGrid(grid: MutableList<MutableList<SlotSymbol>>) {
    val paddingSymbols = listOf(SlotSymbol.GreenBill, SlotSymbol.BonusGame, SlotSymbol.FreeGame)
    for (r in grid.indices) {
        for (c in grid[r].indices) {
            if (grid[r][c] == SlotSymbol.Empty) {
                grid[r][c] = paddingSymbols.random()
            }
        }
    }
}

fun findWinningCombinations(targetPayout: Int, maxCombinationSize: Int): List<List<WinningCombination>> {
    val allCombinations = mutableListOf<List<WinningCombination>>()
    findCombinationsRecursive(targetPayout, 0, mutableListOf(), allCombinations, maxCombinationSize, MAX_SOLUTIONS_TO_FIND_PER_PAYOUT)
    return allCombinations
}

private fun findCombinationsRecursive(
    target: Int,
    startIndex: Int,
    currentCombination: MutableList<WinningCombination>,
    allCombinations: MutableList<List<WinningCombination>>,
    maxCombinationSize: Int,
    solutionLimit: Int // 新增熔断参数
) {
    // 关键优化：如果已经找到了足够多的组合，则立即停止整个搜索过程
    if (allCombinations.size >= solutionLimit) return

    when {
        target == 0 -> {
            allCombinations.add(currentCombination.toList())
            return
        }
        target < 0 -> return
        currentCombination.size >= maxCombinationSize -> return
    }

    for (i in startIndex until allPossibleWins.size) {
        val win = allPossibleWins[i]
        if (win.payout > target) continue

        currentCombination.add(win)
        findCombinationsRecursive(target - win.payout, i, currentCombination, allCombinations, maxCombinationSize, solutionLimit)
        currentCombination.removeLast()
    }
}

fun createTables(connection: Connection) {
    connection.createStatement().use { statement ->
        statement.execute("DROP TABLE IF EXISTS spin_results")
        statement.execute(
            """
            CREATE TABLE spin_results (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                payout INTEGER NOT NULL,
                grid TEXT NOT NULL
            );
            """
        )
        statement.execute("CREATE INDEX idx_payout ON spin_results (payout);")
    }
}

fun insertGrid(connection: Connection, grid: List<List<SlotSymbol>>, payout: Int) {
    val gridString = grid.joinToString(";") { row ->
        row.joinToString(",") { symbol ->
            when (symbol) {
                is SlotSymbol.GoldCoin -> symbol.name
                is SlotSymbol.Wild -> "WILD"
                is SlotSymbol.GreenBill -> "GREEN_BILL"
                is SlotSymbol.BonusGame -> "BONUS"
                is SlotSymbol.FreeGame -> "FREE_GAME"
                is SlotSymbol.Empty -> "EMPTY"
            }
        }
    }
    connection.prepareStatement("INSERT INTO spin_results (payout, grid) VALUES (?, ?)").use { statement ->
        statement.setInt(1, payout)
        statement.setString(2, gridString)
        statement.executeUpdate()
    }
}
