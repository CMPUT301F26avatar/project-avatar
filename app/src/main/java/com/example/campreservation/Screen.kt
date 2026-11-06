package com.example.campreservation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage


abstract class Screen {

    abstract val role: UserRole

    var onNavigate: (String) -> Unit = {}

    abstract fun refresh()

    @Composable
    abstract fun Content()

    fun navigate(route: String) = onNavigate(route)
    var onChangeRole: () -> Unit = {}

    @Composable
    fun ShowImage(url: String?, modifier: Modifier = Modifier) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}
//git push