package com.books_goo_hzz.lib_slot_core

import java.sql.Connection
import java.sql.DriverManager
import kotlin.random.Random

const val TARGET_DB_NAME = "slot_results.db"
const val TOTAL_BET = 2000L // 总押注额，与calculateWinnings的输入保持一致
const val MAX_ATTEMPTS_PER_COMBINATION = 100 // 为单个赢奖组合尝试生成有效盘面的最大次数
const val MAX_SOLUTIONS_TO_FIND_PER_PAYOUT = 100 // 熔断机制：为每个赔率最多寻找多少种理论组合
// ⭐ WILD使用策略已改为冲突驱动，不再使用固定概率

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
    for (targetPayout in 5..10000 step 5) {
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

        // ⭐ 冲突驱动WILD策略：根据位置冲突情况智能生成符号序列
        // 先快速检查是否有冲突，如果没有冲突则使用简单策略
        val hasConflict = positionsToPlace.any { point ->
            val existing = grid[point.y][point.x]
            existing is SlotSymbol.GoldCoin && existing != win.symbol
        }
        
        val symbolsForLine = if (hasConflict) {
            // 有冲突：使用冲突驱动策略
            generateOptimalSymbolSequence(win, positionsToPlace, grid)
        } else {
            // 无冲突：使用简单策略（全部使用金币符号）
            List(win.count) { win.symbol }
        }

        // Check if the target positions can be placed with the generated sequence
        // WILD可以与任何符号兼容（它可以替代任何金币符号）
        val canPlace = positionsToPlace.zip(symbolsForLine).all { (point, symbolToPlace) ->
            val existingSymbol = grid[point.y][point.x]
            when {
                existingSymbol == SlotSymbol.Empty -> true  // 空位置总是可以放置
                existingSymbol == symbolToPlace -> true     // 符号匹配
                existingSymbol is SlotSymbol.Wild -> true   // 已有WILD，可以替代任何符号
                symbolToPlace is SlotSymbol.Wild -> true    // 要放置WILD，可以替代任何金币符号
                else -> false  // 其他情况不兼容（如非参与符号位置）
            }
        }

        if (canPlace) {
            val originalSymbols = positionsToPlace.map { point -> grid[point.y][point.x] }

            // Place the generated sequence (which might include WILDs)
            // 如果位置已有WILD且要放置的也是WILD，保持现有WILD（避免不必要的覆盖）
            positionsToPlace.zip(symbolsForLine).forEach { (point, symbolToPlace) ->
                val existing = grid[point.y][point.x]
                if (existing is SlotSymbol.Wild && symbolToPlace is SlotSymbol.Wild) {
                    // 保持现有WILD
                } else {
                    grid[point.y][point.x] = symbolToPlace
                }
            }

            val nextAvailablePaylines = availablePaylines.toMutableSet().apply { remove(lineId) }
            if (placeWinsRecursive(remainingWins, grid, nextAvailablePaylines)) {
                return true
            }

            // Backtrack if the recursive call failed
            positionsToPlace.forEachIndexed { index, point ->
                grid[point.y][point.x] = originalSymbols[index]
            }
        }
    }
    return false
}

/**
 * ⭐ 冲突驱动WILD策略（优化版）：根据位置冲突情况智能生成符号序列
 * 简化逻辑，减少计算开销
 */
private fun generateOptimalSymbolSequence(
    win: WinningCombination,
    positions: List<Point>,
    grid: MutableList<MutableList<SlotSymbol>>
): List<SlotSymbol> {
    val sequence = mutableListOf<SlotSymbol>()
    var hasGoldCoin = false
    val goldCoinSymbol = win.symbol
    var firstEmptyIndex = -1  // 记录第一个空位置索引，用于最终替换
    
    for ((index, point) in positions.withIndex()) {
        val existing = grid[point.y][point.x]
        
        when {
            // 情况1：空位置 - 优先使用金币符号
            existing == SlotSymbol.Empty -> {
                sequence.add(goldCoinSymbol)
                hasGoldCoin = true
                if (firstEmptyIndex == -1) firstEmptyIndex = index
            }
            
            // 情况2：已匹配 - 保持现有符号
            existing == goldCoinSymbol -> {
                sequence.add(existing)
                hasGoldCoin = true
            }
            
            // 情况3：已有WILD - 保持WILD
            existing is SlotSymbol.Wild -> {
                sequence.add(SlotSymbol.Wild)
                if (firstEmptyIndex == -1) firstEmptyIndex = index
            }
            
            // 情况4：冲突 - 必须使用WILD（位置已有其他金币符号）
            existing is SlotSymbol.GoldCoin && existing != goldCoinSymbol -> {
                sequence.add(SlotSymbol.Wild)
            }
            
            // 情况5：非参与符号 - 使用金币符号
            else -> {
                sequence.add(goldCoinSymbol)
                hasGoldCoin = true
            }
        }
    }
    
    // 最终检查：至少保留一个金币符号（不能全是WILD）
    if (!hasGoldCoin) {
        // 优先在空位置或已有WILD的位置替换为金币
        val replaceIndex = if (firstEmptyIndex >= 0) firstEmptyIndex else 
            sequence.indices.firstOrNull { 
                val existing = grid[positions[it].y][positions[it].x]
                existing is SlotSymbol.Wild || existing == SlotSymbol.Empty
            } ?: 0
        
        sequence[replaceIndex] = goldCoinSymbol
    }
    
    return sequence
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

    // ⭐ Fast path 1: 单一符号大满贯方案（5连/4连/3连的同符号多线）
    outer@ for (count in intArrayOf(5, 4, 3)) {
        for (coin in GameConfiguration.goldCoinSymbols) {
            val payout = coin.payouts[count] ?: continue
            if (payout == 0) continue
            if (targetPayout % payout != 0) continue
            val lines = targetPayout / payout
            if (lines in 1..20) {
                allCombinations.add(List(lines) { WinningCombination(coin, count = count, payout = payout) })
                if (allCombinations.size >= MAX_SOLUTIONS_TO_FIND_PER_PAYOUT) break@outer
            }
        }
    }

    // ⭐ Fast path 2: 混合快速路径 - 处理难产区间（如 1365 = 260×5 + 65）
    // 只在以下情况使用：1) 完全没有找到组合 2) 高倍率区间（1000+）且组合数少
    // 对于低倍率（<1000），如果快速路径1找到了组合，就不使用混合路径，避免增加搜索空间
    if (allCombinations.isEmpty() || (targetPayout >= 1000 && allCombinations.size < 3)) {
        findMixedCombinations(targetPayout, allCombinations)
    }

    findCombinationsRecursive(targetPayout, 0, mutableListOf(), allCombinations, maxCombinationSize, MAX_SOLUTIONS_TO_FIND_PER_PAYOUT)
    return allCombinations
}

/**
 * ⭐ 混合快速路径：尝试两个符号的混合组合，用于处理难产区间
 * 例如：1365 = 260×5 + 65 (symbol02×5 + symbol08×1)
 */
private fun findMixedCombinations(
    targetPayout: Int,
    allCombinations: MutableList<List<WinningCombination>>
) {
    // ⭐ 优化：限制混合组合的数量和范围，避免搜索空间爆炸
    // 只尝试5连的组合，且限制每个符号最多5条线（避免过多5连导致物理不可行）
    val fiveLinkPayouts = GameConfiguration.goldCoinSymbols.mapNotNull { coin ->
        val payout = coin.payouts[5]
        if (payout != null && payout > 0) {
            coin to payout
        } else null
    }

    // 限制：最多只找5个混合组合，避免搜索空间过大
    val maxMixedCombos = 5
    var foundCount = 0

    // 尝试两个符号的混合（优先尝试高赔率符号）
    for ((coin1, p1) in fiveLinkPayouts) {
        if (foundCount >= maxMixedCombos) break
        if (allCombinations.size >= MAX_SOLUTIONS_TO_FIND_PER_PAYOUT) break
        
        for ((coin2, p2) in fiveLinkPayouts) {
            if (coin1 == coin2) continue
            if (foundCount >= maxMixedCombos) break
            if (allCombinations.size >= MAX_SOLUTIONS_TO_FIND_PER_PAYOUT) break
            
            // ⭐ 优化：限制线数范围，避免过多组合
            // 只尝试较小的线数组合（n1和n2都≤5），物理上更可行
            for (n1 in 1..minOf(5, targetPayout / p1)) {
                if (foundCount >= maxMixedCombos) break
                
                val remainder = targetPayout - p1 * n1
                if (remainder < 0) break
                if (remainder % p2 != 0) continue
                
                val n2 = remainder / p2
                // ⭐ 限制：总线数不超过10，避免过多5连导致物理不可行
                if (n2 in 1..5 && n1 + n2 <= 10) {
                    val combo = List(n1) { WinningCombination(coin1, count = 5, payout = p1) } +
                                List(n2) { WinningCombination(coin2, count = 5, payout = p2) }
                    allCombinations.add(combo)
                    foundCount++
                    if (foundCount >= maxMixedCombos) return
                }
            }
        }
    }
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
        statement.execute("DROP TABLE IF EXISTS slot_results")
        statement.execute(
            """
            CREATE TABLE slot_results (
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                payout INTEGER NOT NULL,
                grid TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            );
            """
        )
        statement.execute("CREATE INDEX idx_payout ON slot_results (payout);")
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
    connection.prepareStatement("INSERT INTO slot_results (payout, grid, timestamp) VALUES (?, ?, ?)").use { statement ->
        statement.setInt(1, payout)
        statement.setString(2, gridString)
        statement.setLong(3, System.currentTimeMillis())
        statement.executeUpdate()
    }
}
