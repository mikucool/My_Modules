package com.books_goo_hzz.feature_slot

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.books_goo_hzz.feature_slot.model.UiSlotItem

@Composable
fun SlotScreen(viewModel: SlotViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            when (val state = uiState) {
                is SlotUiState.Idle -> {
                    Text(text = "Welcome to the Slot Machine!\nPress Spin to start.")
                }
                is SlotUiState.Loading -> {
                    CircularProgressIndicator()
                }
                is SlotUiState.Success -> {
                    Text("Debug Info", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Requested Payout: ${state.requestedPayout}")

                    Spacer(modifier = Modifier.height(16.dp))

                    // Display the Grid with actual images
                    Text("Result Grid:", style = MaterialTheme.typography.titleMedium)
                    GridDisplay(grid = state.grid)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Display Winning Info
                    Text("Total Win: ${state.totalWinAmount}")
                    Text("Winning Lines: ${state.winningLinesCount}")

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

@Composable
fun GridDisplay(grid: List<List<UiSlotItem>>) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .border(2.dp, Color.Gray)
            .padding(4.dp)
    ) {
        grid.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { item ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(68.dp) // A bit larger to show the background
                            .then(
                                if (item.isWinning) {
                                    Modifier
                                        .clip(CircleShape)
                                        .background(Color.Yellow.copy(alpha = 0.6f))
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        Image(
                            painter = painterResource(id = item.imageResId),
                            contentDescription = null, // Decorative image
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            }
        }
    }
}
