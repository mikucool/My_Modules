package com.books_goo_hzz.feature_slot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.books_goo_hzz.feature_slot.mapper.toDrawableResId
import com.books_goo_hzz.feature_slot.model.UiSlotItem
import com.books_goo_hzz.lib_slot_core.Payline
import com.books_goo_hzz.lib_slot_core.Point
import com.books_goo_hzz.lib_slot_core.SlotRepository
import com.books_goo_hzz.lib_slot_core.SpinResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SlotViewModel @Inject constructor(
    private val slotRepository: SlotRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SlotUiState>(SlotUiState.Idle)
    val uiState: StateFlow<SlotUiState> = _uiState.asStateFlow()

    fun spin() {
        viewModelScope.launch {
            _uiState.value = SlotUiState.Loading
            val testPayouts = (5..1000 step 5).toList()
            val randomPayout = testPayouts.random()

            slotRepository.getResultByPayout(randomPayout)
                .map { result ->
                    // 1. 找出所有中奖符号的位置
                    val winningPositions = findWinningPositions(result)

                    // 2. 将业务模型转换为UI模型，并标记中奖符号
                    val uiGrid = result.grid.mapIndexed { y, row ->
                        row.mapIndexed { x, symbol ->
                            UiSlotItem(
                                imageResId = symbol.toDrawableResId(),
                                isWinning = winningPositions.contains(Point(x, y))
                            )
                        }
                    }
                    SlotUiState.Success(
                        grid = uiGrid,
                        totalWinAmount = result.totalWinAmount,
                        winningLinesCount = result.winningLines.size,
                        requestedPayout = randomPayout
                    )
                }
                .catch { e ->
                    _uiState.value = SlotUiState.Error(e.message ?: "An unknown error occurred")
                }
                .collect { successState ->
                    _uiState.value = successState
                }
        }
    }

    /**
     * 从SpinResult中提取所有参与中奖的符号坐标。
     */
    private fun findWinningPositions(result: SpinResult): Set<Point> {
        val positions = mutableSetOf<Point>()
        result.winningLines.forEach { winningLine ->
            val payline = Payline.allPaylines[winningLine.lineId]
            for (i in 0 until winningLine.count) {
                positions.add(payline[i])
            }
        }
        return positions
    }
}

// UI State 现在直接反映UI需要展示的内容
sealed interface SlotUiState {
    object Idle : SlotUiState
    object Loading : SlotUiState
    data class Success(
        val grid: List<List<UiSlotItem>>,
        val totalWinAmount: Long,
        val winningLinesCount: Int,
        val requestedPayout: Int
    ) : SlotUiState
    data class Error(val message: String) : SlotUiState
}
