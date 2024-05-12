package com.dokja.mizumi.presentation.common.material

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dokja.mizumi.presentation.Dimens.IndicatorSize
import com.dokja.mizumi.presentation.theme.LightBlack

@Composable
fun PageIndicator(
    modifier: Modifier = Modifier,
    pageSize: Int,
    selectedPage: Int,
    selectedColor: Color = MaterialTheme.colorScheme.primary,
    unselectedColor: Color = LightBlack
) {

    Row (modifier = modifier, horizontalArrangement = Arrangement.SpaceBetween){
        repeat(times = pageSize) { page ->
            val indicatorSize by animateDpAsState(targetValue = if (page == selectedPage) 40.dp else IndicatorSize,
                label = "indicator",
                animationSpec = tween(500)
            )
            val color by animateColorAsState(targetValue = if (page == selectedPage) selectedColor else unselectedColor.copy(alpha = 0.5f), animationSpec = tween(600),
                label = "color"
            )
            Box(
                modifier = Modifier
                    .height(IndicatorSize)
                    .width(indicatorSize)
                    .clip(if (page == selectedPage) RoundedCornerShape(50) else CircleShape)
                    .background(color = color)
            )
        }
    }
}