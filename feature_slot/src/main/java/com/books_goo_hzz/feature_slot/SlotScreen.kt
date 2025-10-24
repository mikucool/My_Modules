package com.books_goo_hzz.feature_slot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SlotScreen(viewModel: SlotViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (val state = uiState) {
                is SlotUiState.Idle -> {
                    Text(text = "Welcome to the Slot Machine!")
                }
                is SlotUiState.Loading -> {
                    CircularProgressIndicator()
                }
                is SlotUiState.Success -> {
                    // todo
                }
                is SlotUiState.Error -> {
                    Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(onClick = { viewModel.spin() }, enabled = uiState !is SlotUiState.Loading) {
                Text(text = "Spin")
            }
        }
    }
}
