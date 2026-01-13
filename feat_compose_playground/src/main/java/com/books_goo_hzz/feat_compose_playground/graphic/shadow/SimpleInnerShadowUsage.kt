package com.books_goo_hzz.feat_compose_playground.graphic.shadow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SimpleInnerShadowUsage() {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .width(300.dp)
                .height(200.dp)
                .align(Alignment.Center)
                // note that the background needs to be defined before defining the inner shadow
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp)
                )
                .innerShadow(
                    shape = RoundedCornerShape(20.dp),
                    shadow = Shadow(
                        radius = 10.dp,
                        spread = 2.dp,
                        color = Color(0x40000000),
                        offset = DpOffset(x = 6.dp, 7.dp)
                    )
                )

        ) {
            Text(
                "Inner Shadow",
                modifier = Modifier.align(Alignment.Center),
                fontSize = 32.sp
            )
        }
    }
}

@Preview
@Composable
fun PreviewSimpleInnerShadowUsage() {
    SimpleInnerShadowUsage()
}