package com.example.campusconnect.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.campusconnect.R

// Centralized URLs for light and dark themed background images (can be swapped later without touching screens)
private const val LIGHT_BG_URL = "https://lh3.googleusercontent.com/aida-public/AB6AXuCKN6n27dZKh-1Ttz3ZmuIsEeAsGi7AmXkmgFFD7P48WObgNwCWBPtp9iTf1AQqqea19KL3SC-Ch72mzcv0G7vrtNQjNCABmf9KKnuIYx5-b3LIfG0LS9kcVVu-yeMeGehv4ih3V76-gEoTub8JPiJyLv7XqK4FF3wZqv9Q5sKJTXTf1WlzbWqYODeYYGMsS0RquUeEUjq1IE2LspUGPXAuf0qzuWFNPbuAb3qIO-CRN9x4RpF7k3YX9OKKJPLgJEjviKAbu5MiMo8"
private const val DARK_BG_URL = "https://lh3.googleusercontent.com/aida-public/AB6AXuBieLfdTYmbVKy71lkl40Jc-VTR-xlfYi6bKbyosvMAR8_MhaSDU06R_9TteL-dCwdGWBuJHElMH2gqAiHwvef72NOrnLql6DzdS6EsmFjAIY7HPDGQ11RwyHNxRRmWee-3aQzLRKPjSrAx2NEYDc_LG_gQvs2Kl6HfozmTi2z7sRN_mrtF1haVoLX44_5dGRK8NrfdKxS6c5JJOG1YlRBYjMLvF0goPvhWct0ukq6jVb78fjvsJyk-ERKdlo0CkElhThGqF6mkIQA"

@Composable
fun ThemedBackgroundImage(
    modifier: Modifier = Modifier,
    blur: Dp = 12.dp,
    contentScale: ContentScale = ContentScale.Crop,
    overlayBrush: Brush? = null,
) {
    val isDark = isSystemInDarkTheme()
    val imageUrl = if (isDark) DARK_BG_URL else LIGHT_BG_URL
    val localRes = if (isDark) R.drawable.welcome_bg_dark else R.drawable.welcome_bg_light

    // Default overlay gradient if none provided
    val defaultOverlay = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF1F2937).copy(alpha = 0.98f), Color(0xFF1F2937).copy(alpha = 0.85f), Color.Transparent),
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFF6F7F8).copy(alpha = 0.95f), Color(0xFFF6F7F8).copy(alpha = 0.6f), Color.Transparent),
        )
    }

    Box(modifier = modifier) {
        // Always show a local drawable first so we never have an empty background
        Image(
            painter = painterResource(id = localRes),
            contentDescription = null,
            contentScale = contentScale,
            modifier = Modifier
                .fillMaxSize()
                .blur(blur)
        )

        // Attempt to load a network image over the local one; only show when fully loaded
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = contentScale,
            modifier = Modifier
                .fillMaxSize()
                .blur(blur)
        ) {
            if (painter.state is AsyncImagePainter.State.Success) {
                SubcomposeAsyncImageContent()
            }
        }

        // Overlay gradient to help content readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(overlayBrush ?: defaultOverlay)
        )
    }
}

