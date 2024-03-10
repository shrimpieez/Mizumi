package com.dokja.mizumi.presentation.utils

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

@Composable
fun LazyListState.isAtTop(threshold: Dp) = run {

    val density by rememberUpdatedState(LocalDensity.current)

    return@run remember {
        derivedStateOf {
            if (firstVisibleItemIndex > 0) return@derivedStateOf false
            val item = layoutInfo.visibleItemsInfo.firstOrNull()
                ?: return@derivedStateOf true
            with(density) { item.offset.toDp() } > -threshold
        }
    }
}

@Composable
fun ScrollState.isAtTop(threshold: Dp) = run {

    val density by rememberUpdatedState(LocalDensity.current)

    return@run remember(threshold) {
        derivedStateOf {
            val valueDp = with(density) { value.toDp() }
            valueDp < threshold
        }
    }
}
