package com.books_goo_hzz.feature_slot

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.books_goo_hzz.lib_slot_core.SlotRepository
import com.books_goo_hzz.lib_slot_core.SpinResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SlotViewModel @Inject constructor(
    private val slotRepository: SlotRepository
) : ViewModel() {

    private val _uiState = mutableStateOf<SlotUiState>(SlotUiState.Idle)
    val uiState: State<SlotUiState> = _uiState

    fun spin() {
        viewModelScope.launch {
            _uiState.value = SlotUiState.Loading
            // For debugging, let's request a random payout from a predefined list of valid payouts.
            // According to Generator.kt, it generates for (5..1000 step 5)
            val testPayouts = (5..100 step 5).toList()
            val randomPayout = testPayouts.random()

            slotRepository.getResultByPayout(randomPayout)
                .catch { e ->
                    _uiState.value = SlotUiState.Error(e.message ?: "An unknown error occurred")
                    Log.e("SlotViewModel", "Error: ${e.message}", e)
                }
                .collect { result ->
                    _uiState.value = SlotUiState.Success(result, randomPayout)
                }
        }
    }
}

sealed interface SlotUiState {
    object Idle : SlotUiState
    object Loading : SlotUiState
    data class Success(val result: SpinResult, val requestedPayout: Int) : SlotUiState
    data class Error(val message: String) : SlotUiState
}
