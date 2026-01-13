package com.books_goo_hzz.feat_compose_playground.graphic.shadow

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// Define breathing states
enum class BreathingState {
    Inhaling,
    Exhaling
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF
)
@Composable
fun GradientBasedShadowAnimation() {
    MaterialTheme {
        val colors = listOf(
            Color(0xFF4cc9f0),
            Color(0xFFf72585),
            Color(0xFFb5179e),
            Color(0xFF7209b7),
            Color(0xFF560bad),
            Color(0xFF480ca8),
            Color(0xFF3a0ca3),
            Color(0xFF3f37c9),
            Color(0xFF4361ee),
            Color(0xFF4895ef),
            Color(0xFF4cc9f0)
        )

        // ..

        // State for the breathing animation
        var breathingState by remember { mutableStateOf(BreathingState.Inhaling) }

        // Create transition based on breathing state
        val transition = updateTransition(
            targetState = breathingState,
            label = "breathing_transition"
        )

        // Animate spread based on breathing state
        val animatedSpread by transition.animateFloat(
            transitionSpec = {
                tween(
                    durationMillis = 5000,
                    easing = FastOutSlowInEasing
                )
            },
            label = "spread_animation"
        ) { state ->
            when (state) {
                BreathingState.Inhaling -> 10f
                BreathingState.Exhaling -> 2f
            }
        }

        // Animate alpha based on breathing state (optional)
        val animatedAlpha by transition.animateFloat(
            transitionSpec = {
                tween(
                    durationMillis = 2000,
                    easing = FastOutSlowInEasing
                )
            },
            label = "alpha_animation"
        ) { state ->
            when (state) {
                BreathingState.Inhaling -> 1f
                BreathingState.Exhaling -> 1f
            }
        }

        // Get text based on current state
        val breathingText = when (breathingState) {
            BreathingState.Inhaling -> "Inhale"
            BreathingState.Exhaling -> "Exhale"
        }

        // Switch states when animation completes
        LaunchedEffect(breathingState) {
            delay(5000) // Wait for animation to complete
            breathingState = when (breathingState) {
                BreathingState.Inhaling -> BreathingState.Exhaling
                BreathingState.Exhaling -> BreathingState.Inhaling
            }
        }

        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // [START android_compose_graphics_gradient_shadow]
            Box(
                modifier = Modifier
                    .width(240.dp)
                    .height(200.dp)
                    .dropShadow(
                        shape = RoundedCornerShape(70.dp),
                        shadow = Shadow(
                            radius = 10.dp,
                            spread = animatedSpread.dp,
                            brush = Brush.sweepGradient(
                                colors
                            ),
                            offset = DpOffset(x = 0.dp, y = 0.dp),
                            alpha = animatedAlpha
                        )
                    )
                    .clip(RoundedCornerShape(70.dp))
                    .background(Color(0xEDFFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = breathingText,
                    color = Color.Black,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            // [END android_compose_graphics_gradient_shadow]
        }
    }
}
