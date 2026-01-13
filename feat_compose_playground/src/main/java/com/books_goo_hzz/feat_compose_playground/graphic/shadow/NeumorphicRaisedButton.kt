package com.books_goo_hzz.feat_compose_playground.graphic.shadow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

@Preview
// [START android_compose_graphics_neumorphic_shadow]
@Composable
fun NeumorphicRaisedButton(
    shape: RoundedCornerShape = RoundedCornerShape(30.dp)
) {
    val bgColor = Color(0xFFe0e0e0)
    val lightShadow = Color(0xFFFFFFFF)
    val darkShadow = Color(0xFFb1b1b1)
    val upperOffset = -10.dp
    val lowerOffset = 10.dp
    val radius = 15.dp
    val spread = 0.dp
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .wrapContentSize(Alignment.Center)
            .size(240.dp)
            .dropShadow(
                shape,
                shadow = Shadow(
                    radius = radius,
                    color = lightShadow,
                    spread = spread,
                    offset = DpOffset(upperOffset, upperOffset)
                ),
            )
            .dropShadow(
                shape,
                shadow = Shadow(
                    radius = radius,
                    color = darkShadow,
                    spread = spread,
                    offset = DpOffset(lowerOffset, lowerOffset)
                ),

            )
            .background(bgColor, shape)
    )
}