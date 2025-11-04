package com.books_goo_hzz.lib_slot_core

import kotlin.random.Random
import kotlinx.coroutines.*

/**
 * 动态结果盘生成器
 * 根据目标赔率动态生成对应的结果盘，与预生成逻辑完全分离
 * 只返回一个结果盘，在指定时间内完成生成
 */
object DynamicSlotGenerator {
    
    // 配置常量
    private const val TOTAL_BET = 2000L // 总押注额，与calculateWinnings的输入保持一致
    private const val MAX_ATTEMPTS_PER_COMBINATION = 100 // 为单个赢奖组合尝试生成有效盘面的最大次数
    private const val MAX_COMBINATIONS_TO_TRY = 20 // 最多尝试多少个理论组合
    private const val MAX_COMBINATION_SIZE = 20 // 单个组合最多包含多少个赢奖项
    
    /**
     * 数据类，用于封装一个具体的赢奖组合
     */
    private data class WinningCombination(val symbol: SlotSymbol.GoldCoin, val count: Int, val payout: Int)
    
    // 提前计算好所有可能的赢奖组合，并按赔率降序排列
    private val allPossibleWins: List<WinningCombination> by lazy {
        GameConfiguration.goldCoinSymbols.flatMap { coin ->
            coin.payouts.map { (count, payout) ->
                WinningCombination(coin, count, payout)
            }
        }.sortedByDescending { it.payout }
    }
    
    /**
     * 根据目标赔率动态生成结果盘
     * 
     * @param targetPayout 目标赔率
     * @param timeoutMs 超时时间（毫秒），默认10秒
     * @return 生成的结果盘，如果生成失败或超时则返回null
     */
    suspend fun findSlotResultByPayout(
        targetPayout: Int,
        timeoutMs: Long = 10_000L
    ): List<List<SlotSymbol>>? = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        println("[DynamicSlotGenerator] 开始生成结果盘，目标赔率: $targetPayout, 超时时间: ${timeoutMs}ms")
        
        val slotMachine = SlotMachine()
        
        // 使用超时机制
        val result = withTimeoutOrNull(timeoutMs) {
            generateGridForPayout(targetPayout, slotMachine)
        }
        
        val duration = System.currentTimeMillis() - startTime
        if (result != null) {
            println("[DynamicSlotGenerator] ✅ 成功生成结果盘，耗时: ${duration}ms")
        } else {
            println("[DynamicSlotGenerator] ❌ 生成失败或超时，耗时: ${duration}ms")
        }
        
        result
    }
    
    /**
     * 为指定倍率生成网格（挂起函数，支持协程超时）
     * 找到一个有效的结果盘就返回
     */
    private suspend fun generateGridForPayout(
        targetPayout: Int,
        slotMachine: SlotMachine
    ): List<List<SlotSymbol>>? {
        val combinations = findWinningCombinations(targetPayout)
        println("[DynamicSlotGenerator] 找到 ${combinations.size} 个理论组合，开始尝试生成")
        
        // 遍历每一种赢奖组合方案
        var comboIndex = 0
        for (combo in combinations) {
            comboIndex++
            // 定期让出控制权，使协程超时能够生效
            yield()
            
            val comboDescription = combo.joinToString(", ") { "${it.symbol.name}(${it.count}连×${combo.count { w -> w == it }})" }
            println("[DynamicSlotGenerator] 尝试组合 $comboIndex/${combinations.size}: $comboDescription")
            
            val grid = generateValidGridForCombination(combo, targetPayout, slotMachine)
            if (grid != null) {
                println("[DynamicSlotGenerator] ✅ 组合 $comboIndex 生成成功")
                return grid
            } else {
                println("[DynamicSlotGenerator] ❌ 组合 $comboIndex 生成失败，尝试下一个组合")
            }
        }
        
        println("[DynamicSlotGenerator] 所有组合尝试完毕，未找到有效结果盘")
        return null
    }
    
    /**
     * 为一个给定的赢奖组合，尝试生成一个有效的结果盘
     * @return 生成的有效结果盘，如果生成失败则返回null
     */
    private suspend fun generateValidGridForCombination(
        combination: List<WinningCombination>,
        targetPayout: Int,
        slotMachine: SlotMachine
    ): List<List<SlotSymbol>>? {
        // 尝试多次，以寻找不同的随机填充结果
        for (attempt in 1..MAX_ATTEMPTS_PER_COMBINATION) {
            // 定期让出控制权，使协程超时能够生效
            // 每10次尝试让出一次，避免过于频繁
            if (attempt % 10 == 0) {
                yield()
            }
            
            // 每50次尝试输出一次进度
            if (attempt % 50 == 0) {
                println("[DynamicSlotGenerator]    尝试进度: $attempt/$MAX_ATTEMPTS_PER_COMBINATION")
            }
            
            val grid: MutableList<MutableList<SlotSymbol>> = MutableList(3) { MutableList(5) { SlotSymbol.Empty } }
            val placementSuccessful = placeWinsRecursive(combination, grid, Payline.allPaylines.indices.toMutableSet())
            
            if (placementSuccessful) {
                padGrid(grid) // 用随机非赢奖符号填充空白区域
                
                // 最终校验：调用规则引擎来计算最终赢分
                val result = slotMachine.calculateWinnings(grid, TOTAL_BET)
                val finalPayout = (result.totalWinAmount * 20 / TOTAL_BET).toInt()
                
                // 如果最终赢分与目标完全一致，则此盘面有效！
                if (finalPayout == targetPayout) {
                    println("[DynamicSlotGenerator]    ✅ 第 $attempt 次尝试成功，最终赔率: $finalPayout")
                    return grid.map { it.toList() } // 返回盘面快照
                } else {
                    // 如果赔率不匹配，输出调试信息（仅在少数情况下输出，避免日志过多）
                    if (attempt <= 5 || attempt % 20 == 0) {
                        println("[DynamicSlotGenerator]    ⚠️  第 $attempt 次尝试赔率不匹配: 目标=$targetPayout, 实际=$finalPayout")
                    }
                }
            }
        }
        
        println("[DynamicSlotGenerator]    ❌ 已尝试 $MAX_ATTEMPTS_PER_COMBINATION 次，未找到有效结果盘")
        return null
    }
    
    /**
     * 递归函数，尝试将赢奖组合列表不冲突地摆放到盘面上
     * @return 如果成功找到一种摆法，返回true；否则返回false
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
            
            // 冲突驱动WILD策略：根据位置冲突情况智能生成符号序列
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
            
            // 检查目标位置是否可以放置生成的符号序列
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
                
                // 放置生成的符号序列（可能包含WILDs）
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
                
                // 回溯：如果递归调用失败，恢复原始状态
                positionsToPlace.forEachIndexed { index, point ->
                    grid[point.y][point.x] = originalSymbols[index]
                }
            }
        }
        return false
    }
    
    /**
     * 冲突驱动WILD策略（优化版）：根据位置冲突情况智能生成符号序列
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
    
    /**
     * 用随机非赢奖符号填充空白区域
     */
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
    
    /**
     * 查找所有可能的赢奖组合
     */
    private fun findWinningCombinations(targetPayout: Int): List<List<WinningCombination>> {
        val allCombinations = mutableListOf<List<WinningCombination>>()
        println("[DynamicSlotGenerator] 开始查找理论组合，目标赔率: $targetPayout")
        
        // Fast path 1: 单一符号大满贯方案（5连/4连/3连的同符号多线）
        outer@ for (count in intArrayOf(5, 4, 3)) {
            for (coin in GameConfiguration.goldCoinSymbols) {
                val payout = coin.payouts[count] ?: continue
                if (payout == 0) continue
                if (targetPayout % payout != 0) continue
                val lines = targetPayout / payout
                if (lines in 1..20) {
                    allCombinations.add(List(lines) { WinningCombination(coin, count = count, payout = payout) })
                    println("[DynamicSlotGenerator]   Fast path 1: 找到单一符号组合 - ${coin.name}($count 连) × $lines 线")
                    if (allCombinations.size >= MAX_COMBINATIONS_TO_TRY) break@outer
                }
            }
        }
        
        // Fast path 2: 混合快速路径 - 处理难产区间（如 1365 = 260×5 + 65）
        if (allCombinations.isEmpty()) {
            println("[DynamicSlotGenerator]   Fast path 1 未找到组合，尝试混合路径")
            findMixedCombinations(targetPayout, allCombinations)
        }
        
        // 如果还没有找到足够的组合，使用递归搜索
        if (allCombinations.size < MAX_COMBINATIONS_TO_TRY) {
            val beforeRecursive = allCombinations.size
            println("[DynamicSlotGenerator]   开始递归搜索，当前组合数: $beforeRecursive")
            findCombinationsRecursive(targetPayout, 0, mutableListOf(), allCombinations, MAX_COMBINATION_SIZE, MAX_COMBINATIONS_TO_TRY)
            val afterRecursive = allCombinations.size
            if (afterRecursive > beforeRecursive) {
                println("[DynamicSlotGenerator]   递归搜索完成，新增 ${afterRecursive - beforeRecursive} 个组合")
            }
        }
        
        println("[DynamicSlotGenerator] 组合查找完成，共找到 ${allCombinations.size} 个理论组合")
        return allCombinations
    }
    
    /**
     * 混合快速路径：尝试两个符号的混合组合，用于处理难产区间
     * 例如：1365 = 260×5 + 65 (symbol02×5 + symbol08×1)
     */
    private fun findMixedCombinations(
        targetPayout: Int,
        allCombinations: MutableList<List<WinningCombination>>
    ) {
        // 优化：限制混合组合的数量和范围，避免搜索空间爆炸
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
            if (allCombinations.size >= MAX_COMBINATIONS_TO_TRY) break
            
            for ((coin2, p2) in fiveLinkPayouts) {
                if (coin1 == coin2) continue
                if (foundCount >= maxMixedCombos) break
                if (allCombinations.size >= MAX_COMBINATIONS_TO_TRY) break
                
                // 优化：限制线数范围，避免过多组合
                // 只尝试较小的线数组合（n1和n2都≤5），物理上更可行
                for (n1 in 1..minOf(5, targetPayout / p1)) {
                    if (foundCount >= maxMixedCombos) break
                    
                    val remainder = targetPayout - p1 * n1
                    if (remainder < 0) break
                    if (remainder % p2 != 0) continue
                    
                    val n2 = remainder / p2
                    // 限制：总线数不超过10，避免过多5连导致物理不可行
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
    
    /**
     * 递归查找所有可能的赢奖组合
     */
    private fun findCombinationsRecursive(
        target: Int,
        startIndex: Int,
        currentCombination: MutableList<WinningCombination>,
        allCombinations: MutableList<List<WinningCombination>>,
        maxCombinationSize: Int,
        solutionLimit: Int
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
}

