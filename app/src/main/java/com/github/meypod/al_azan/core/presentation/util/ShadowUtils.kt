package com.github.meypod.al_azan.core.presentation.util

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Stable
fun Modifier.dropShadow2(shape: Shape): Modifier =
    this
        .dropShadow(
            shape,
        ) {
            this.radius = 1.dp.toPx()
            this.offset = Offset(0f, 2f)
            this.spread = 0f
            this.alpha = 0.3f
        }
        .dropShadow(
            shape,
        ) {
            this.radius = 4.dp.toPx()
            this.offset = Offset(0f, 4f)
            this.spread = 1f
            this.alpha = 0.25f
        }

@Stable
fun Modifier.dropShadow2Up(shape: Shape): Modifier =
    this
        .dropShadow(
            shape,
        ) {
            this.radius = 1.dp.toPx()
            this.offset = Offset(0f, 0f)
            this.spread = 0f
            this.alpha = 0.3f
        }
        .dropShadow(
            shape,
        ) {
            this.radius = 4.dp.toPx()
            this.offset = Offset(0f, 0f)
            this.spread = 1f
            this.alpha = 0.25f
        }
