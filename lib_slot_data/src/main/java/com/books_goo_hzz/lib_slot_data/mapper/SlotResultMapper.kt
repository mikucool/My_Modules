package com.books_goo_hzz.lib_slot_data.mapper

import com.books_goo_hzz.lib_database.entities.slot.SlotResultEntity
import com.books_goo_hzz.lib_slot_core.GameConfiguration
import com.books_goo_hzz.lib_slot_core.SlotMachine
import com.books_goo_hzz.lib_slot_core.SlotSymbol
import com.books_goo_hzz.lib_slot_core.SpinResult

/**
 * 将从数据库获取的 SlotResultEntity 转换为业务逻辑层的 SpinResult 对象。
 */
object SlotResultMapper {

    private const val TOTAL_BET = 2000L // 这个值需要与生成器中的总押注额保持一致

    private val slotMachine = SlotMachine()

    // 创建一个从字符串名称到SlotSymbol对象的映射，用于高效反序列化
    private val symbolMap: Map<String, SlotSymbol> by lazy {
        (GameConfiguration.goldCoinSymbols +
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
    }


    /**
     * 执行从实体到领域模型的转换。
     *
     * @param entity 从数据库查询到的实体对象。
     * @return 一个包含完整中奖信息的 SpinResult 对象。
     */
    fun mapToSpinResult(entity: SlotResultEntity): SpinResult {
        val grid = deserializeGrid(entity.grid)
        // 运行时重算中奖结果，以获取完整的 WinningLineInfo
        return slotMachine.calculateWinnings(grid, TOTAL_BET)
    }

    /**
     * 将存储在数据库中的字符串反序列化为 5x3 的符号网格。
     *
     * @param gridString 格式为 "ROW1;ROW2;ROW3"，其中 ROW 是 "SYM1,SYM2,..."
     * @return List<List<SlotSymbol>>
     */
    private fun deserializeGrid(gridString: String): List<List<SlotSymbol>> {
        return gridString.split(";").map { rowString ->
            rowString.split(",").map { symbolName ->
                symbolMap[symbolName] ?: throw IllegalArgumentException("Unknown symbol name: $symbolName")
            }
        }
    }
}
