package com.example.campusconnect.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.material3.MaterialTheme
import com.example.campusconnect.ui.theme.Transparent

private const val LIGHT_BG_URL = "https://lh3.googleusercontent.com/aida-public/AB6AXuCKN6n27dZKh-1Ttz3ZmuIsEeAsGi7AmXkmgFFD7P48WObgNwCWBPtp9iTf1AQqqea19KL3SC-Ch72mzcv0G7vrtNQjNCABmf9KKnuIYx5-b3LIfG0LS9kcVVu-yeMeGehv4ih3V76-gEoTub8JPiJyLv7XqK4FF3wZqv9Q5sKJTXTf1WlzbWqYODeYYGMsS0RquUeEUjq1IE2LspUGPXAuf0qzuWFNPbuAb3qIO-CRN9x4RpF7k3YX9OKKJPLgJEjviKAbu5MiMo8"
private const val DARK_BG_URL = "https://lh3.googleusercontent.com/aida-public/AB6AXuBieLfdTYmbVKy71lkl40Jc-VTR-xlfYi6bKbyosvMAR8_MhaSDU06R_9TteL-dCwdGWBuJHElMH2gqAiHwvef72NOrnLql6DzdS6EsmFjAIY7HPDGQ11RwyHNxRRmWee-3aQzLRKPjSrAx2NEYDc_LG_gQvs2Kl6HfozmTi2z7sRN_mrtF1haVoLX44_5dGRK8NrfdKxS6c5JJOG1YlRBYjMLvF0goPvhWct0ukq6jVb78fjvsJyk-ERKdlo0CkElhThGqF6mkIQA"

@Composable
fun ThemedBackgroundImage(
    modifier: Modifier = Modifier,
    blur: Dp = 1.dp, // reduced default blur (even sharper)
    isDarkOverride: Boolean? = null,
    contentScale: ContentScale = ContentScale.Crop,
    overlayBrush: Brush? = null,
) {
    val isDark = isDarkOverride ?: isSystemInDarkTheme()
    val imageUrl = if (isDark) DARK_BG_URL else LIGHT_BG_URL
    val localRes = if (isDark) R.drawable.welcome_bg_dark else R.drawable.welcome_bg_light

    val colorScheme = MaterialTheme.colorScheme

    val defaultOverlay = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(colorScheme.background.copy(alpha = 0.98f), colorScheme.background.copy(alpha = 0.85f), Transparent),
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(colorScheme.background.copy(alpha = 0.95f), colorScheme.background.copy(alpha = 0.6f), Transparent),
        )
    }

    Box(modifier = modifier) {
        Image(
            painter = painterResource(id = localRes),
            contentDescription = null,
            contentScale = contentScale,
            modifier = Modifier
                .fillMaxSize()
                .blur(blur)
        )

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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(overlayBrush ?: defaultOverlay)
        )
    }
}
