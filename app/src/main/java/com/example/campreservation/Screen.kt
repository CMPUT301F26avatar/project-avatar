package com.example.campreservation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage


// Screen is the base class for every screen in the app (for example EntrantView).
// It defines what each screen must provide (a role, a refresh action, and its UI content)
// and gives all screens shared tools for navigation, switching roles, and showing images.
abstract class Screen {

    // The kind of user this screen is for. Each subclass must set it.
    abstract val role: UserRole

    // A callback the app sets so a screen can request navigation to another route.
    // It does nothing by default until the app assigns a real handler.
    var onNavigate: (String) -> Unit = {}

    // Reloads whatever data the screen shows. Each subclass decides what that means.
    abstract fun refresh()

    // Draws the screen's UI. Each subclass must implement its own layout here.
    @Composable
    abstract fun Content()

    // Convenience wrapper so screens can call navigate("route") instead of onNavigate(...).
    fun navigate(route: String) = onNavigate(route)

    // A callback the app sets so a screen can send the user back to role selection.
    // It does nothing by default until the app assigns a real handler.
    var onChangeRole: () -> Unit = {}

    // A shared composable for loading and showing an image from a URL using Coil's
    // AsyncImage. ContentScale.Crop fills the given size by cropping any overflow, and
    // contentDescription is null because the image is decorative (no screen reader text).
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