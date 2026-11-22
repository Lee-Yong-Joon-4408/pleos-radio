package com.pleos.radio.presentation.ui

import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import com.pleos.radio.presentation.ui.theme.RadioColors

/** Size of the TV video frame in the now-playing row (16:9). */
val VIDEO_WIDTH: Dp = 400.dp
val VIDEO_HEIGHT: Dp = 225.dp

/**
 * Renders [player]'s video into a TextureView; shows a loading state until the first frame arrives.
 * [stationKey] resets the loading state when the channel changes.
 */
@Composable
fun VideoSurface(player: Player, stationKey: String?, modifier: Modifier = Modifier) {
    var rendered by remember(stationKey) { mutableStateOf(false) }
    DisposableEffect(player, stationKey) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() { rendered = true }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    Box(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { ctx -> TextureView(ctx).also { player.setVideoTextureView(it) } },
            onRelease = { player.clearVideoTextureView(it) },
            modifier = Modifier.fillMaxSize().alpha(if (rendered) 1f else 0f),
        )
        if (!rendered) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp), color = RadioColors.AccentBright, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Loading video…", color = RadioColors.Muted, fontSize = 18.sp)
            }
        }
    }
}

/** Now-playing video frame with an enlarge button. */
@Composable
fun VideoPane(player: Player, stationKey: String?, onExpand: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .size(VIDEO_WIDTH, VIDEO_HEIGHT)
            .clip(shape)
            .border(1.dp, Color(0xFF3A74B8).copy(alpha = 0.6f), shape)
            .clickable(role = Role.Button, onClickLabel = "Enlarge video", onClick = onExpand),
    ) {
        VideoSurface(player, stationKey, Modifier.fillMaxSize())
        Icon(
            Icons.Filled.Fullscreen, contentDescription = null, tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).size(30.dp)
                .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(6.dp)).padding(3.dp),
        )
    }
}

/** Shown in place of the video while driving: audio continues. */
@Composable
fun VideoBlockedPane() {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier.size(VIDEO_WIDTH, VIDEO_HEIGHT).background(Color(0xFF0B1220), shape).border(1.dp, RadioColors.Outline, shape),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Tv, null, tint = RadioColors.Muted, modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(10.dp))
        Text("Video is paused while driving", color = RadioColors.TextSecondary, fontSize = 20.sp)
        Text("Audio continues · video returns when parked", color = RadioColors.Muted, fontSize = 16.sp)
    }
}

/** Large video view over the radio screen; tap anywhere to return. */
@Composable
fun ExpandedVideo(player: Player, stationKey: String?, title: String, onClose: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.94f))
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        VideoSurface(player, stationKey, Modifier.fillMaxHeight(0.88f).aspectRatio(16f / 9f))
        Row(Modifier.align(Alignment.TopStart).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Color.White, fontSize = 22.sp)
        }
        Icon(
            Icons.Filled.FullscreenExit, contentDescription = "Close large video", tint = Color.White,
            modifier = Modifier.align(Alignment.TopEnd).padding(14.dp).size(40.dp),
        )
    }
}
