package com.books_goo_hzz.feat_compose_playground.graphic.shadow

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFCC00
)
@Composable
fun NeoBrutalShadows2() {
    MaterialTheme {
        val dropShadowColor = Color(0xFF007AFF)
        val borderColor = Color(0xFFFF2D55)
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .width(300.dp)
                    .height(200.dp)
                    .align(Alignment.Center)
                    .dropShadow(
                        shape = RoundedCornerShape(0.dp),
                        shadow = Shadow(
                            radius = 0.dp,
                            spread = 0.dp,
                            color = dropShadowColor,
                            offset = DpOffset(x = 8.dp, 8.dp)
                        )
                    )
                    .border(
                        8.dp, borderColor
                    )
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(0.dp)
                    )
            ) {
                Text(
                    "Neobrutal Shadows",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}