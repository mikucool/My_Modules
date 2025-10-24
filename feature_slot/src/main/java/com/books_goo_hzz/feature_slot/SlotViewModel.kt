package com.books_goo_hzz.feature_slot

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.books_goo_hzz.lib_slot_core.SlotRepository
import com.books_goo_hzz.lib_slot_core.SlotResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SlotViewModel @Inject constructor(
    private val slotRepository: SlotRepository
) : ViewModel() {

    private val _uiState = mutableStateOf<SlotUiState>(SlotUiState.Idle)
    val uiState: State<SlotUiState> = _uiState

    fun spin() {

    }
}

sealed interface SlotUiState {
    object Idle : SlotUiState
    object Loading : SlotUiState
    data class Success(val result: SlotResult) : SlotUiState
    data class Error(val message: String) : SlotUiState
}
