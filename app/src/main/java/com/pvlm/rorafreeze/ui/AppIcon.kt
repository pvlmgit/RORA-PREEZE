package com.pvlm.rorafreeze.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pvlm.rorafreeze.utils.AppUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * App icon that shows a grey placeholder immediately and decodes the real icon
 * on a background thread only when this row is visible. Avoids decoding every
 * installed app icon up front, which causes lag with long app lists.
 */
@Composable
fun AppIcon(
    packageName: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    fallbackIcon: androidx.compose.ui.graphics.ImageBitmap? = null
) {
    val context = LocalContext.current
    var bitmap by remember(packageName) { mutableStateOf(fallbackIcon) }

    LaunchedEffect(packageName) {
        if (bitmap == null) {
            bitmap = withContext(Dispatchers.IO) {
                AppUtil.getAppIconAsync(context, packageName).asImageBitmap()
            }
        }
    }

    val current = bitmap
    if (current != null) {
        Image(
            bitmap = current,
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Box(
            modifier = modifier.clip(RoundedCornerShape(14.dp)).background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
        )
    }
}