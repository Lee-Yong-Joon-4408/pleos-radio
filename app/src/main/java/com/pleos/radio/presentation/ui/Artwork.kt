package com.pleos.radio.presentation.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pleos.radio.domain.Station
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pleos.radio.domain.Band
import com.pleos.radio.presentation.ui.theme.RadioColors
import kotlin.math.PI
import kotlin.math.sin

/**
 * Now-playing artwork: the station's logo on a card once it has loaded, otherwise (no logo, still loading,
 * offline, or blocked) the band artwork. A small level meter in the corner keeps the "playing" cue.
 */
@Composable
fun StationArtwork(station: Station?, band: Band, playing: Boolean, modifier: Modifier = Modifier) {
    val url = station?.logoUrl
    var loaded by remember(url) { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Box(modifier) {
        if (!loaded) BandArtwork(band, playing, Modifier.matchParentSize())
        if (url != null) {
            val cardColor = if (station.logoOnDark) Color(0xFF16263D) else Color(0xFFF7F9FC)
            Box(
                Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(if (loaded) cardColor else Color.Transparent)
                    .border(1.dp, if (loaded) Color(0xFF3A74B8).copy(alpha = 0.6f) else Color.Transparent, shape),
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(url).crossfade(true).build(),
                    contentDescription = "${station.name} logo",
                    contentScale = ContentScale.Fit,
                    onSuccess = { loaded = true },
                    onError = { loaded = false },
                    modifier = Modifier.fillMaxSize().padding(22.dp),
                )
                if (loaded && playing) {
                    LevelBars(
                        animate = true,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp).size(width = 30.dp, height = 22.dp),
                        color = RadioColors.Accent,
                    )
                }
            }
        }
    }
}

/** Fallback artwork: animated level bars over the band name. */
@Composable
private fun BandArtwork(band: Band, playing: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .background(Brush.linearGradient(listOf(Color(0xFF0B2240), Color(0xFF1A5FB4), Color(0xFF2C7BE0))), shape)
            .border(1.dp, Color(0xFF3A74B8).copy(alpha = 0.6f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LevelBars(playing, Modifier.size(width = 104.dp, height = 82.dp))
            Spacer(Modifier.height(10.dp))
            Text(band.label, color = Color.White, fontSize = 50.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
    }
}

private val BAR_BASE = floatArrayOf(0.55f, 0.75f, 1.0f, 0.62f, 0.35f, 0.55f)

/** Six vertical bars; they gently pulse while audio is playing and freeze when paused. */
@Composable
fun LevelBars(animate: Boolean, modifier: Modifier = Modifier, color: Color = Color.White) {
    val transition = rememberInfiniteTransition(label = "bars")
    val phase by transition.animateFloat(
        initialValue = 0f, targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1_600, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    Canvas(modifier) {
        val n = BAR_BASE.size
        val gap = size.width / (n * 2.2f)
        val barW = (size.width - gap * (n - 1)) / n
        for (i in 0 until n) {
            val wobble = if (animate) 0.18f * sin(phase * (1 + i % 3) + i * 1.3f) else 0f
            val h = size.height * (BAR_BASE[i] + wobble).coerceIn(0.15f, 1f)
            val x = i * (barW + gap)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(color, color.copy(alpha = 0.35f)),
                    startY = size.height - h, endY = size.height,
                ),
                topLeft = Offset(x, size.height - h),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2),
            )
        }
    }
}

/** Classic 4-step signal strength indicator. */
@Composable
fun SignalBars(level: Int, modifier: Modifier = Modifier, size: Dp = 26.dp, active: Color = RadioColors.Text) {
    Canvas(modifier.size(size)) {
        val bars = 4
        val gap = this.size.width * 0.1f
        val w = (this.size.width - gap * (bars - 1)) / bars
        for (i in 0 until bars) {
            val h = this.size.height * (0.35f + 0.65f * (i + 1) / bars)
            drawRoundRect(
                color = if (i < level) active else active.copy(alpha = 0.2f),
                topLeft = Offset(i * (w + gap), this.size.height - h),
                size = Size(w, h),
                cornerRadius = CornerRadius(w * 0.25f),
            )
        }
    }
}

/** Night landscape behind the now-playing area: dusk horizon, distant hills and a glowing dune. */
@Composable
fun NightLandscape(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val horizon = h * 0.52f

        drawRect(Brush.verticalGradient(
            0f to Color(0xFF04070D), 0.45f to Color(0xFF0C2041), 0.52f to Color(0xFF1E4677),
            0.6f to Color(0xFF0A1830), 1f to Color(0xFF04070D),
        ))
        // warm dusk glow on the horizon
        drawOval(
            brush = Brush.radialGradient(
                listOf(Color(0xFFD9956E).copy(alpha = 0.45f), Color(0xFF6C5A8A).copy(alpha = 0.15f), Color.Transparent),
                center = Offset(w * 0.42f, horizon), radius = w * 0.32f,
            ),
            topLeft = Offset(w * 0.1f, horizon - h * 0.16f),
            size = Size(w * 0.64f, h * 0.32f),
        )
        // distant hills
        val hills = Path().apply {
            moveTo(0f, horizon + h * 0.02f)
            cubicTo(w * 0.15f, horizon - h * 0.03f, w * 0.3f, horizon + h * 0.01f, w * 0.45f, horizon - h * 0.01f)
            cubicTo(w * 0.6f, horizon - h * 0.03f, w * 0.75f, horizon - h * 0.07f, w, horizon - h * 0.04f)
            lineTo(w, h); lineTo(0f, h); close()
        }
        drawPath(hills, Color(0xFF081325))
        // large dune rising to the right
        fun crest(dy: Float) = Path().apply {
            moveTo(w * 0.05f, h * 0.95f + dy)
            cubicTo(w * 0.35f, h * 0.78f + dy, w * 0.6f, h * 0.62f + dy, w * 0.8f, h * 0.47f + dy * 0.6f)
            cubicTo(w * 0.88f, h * 0.42f + dy * 0.5f, w * 0.95f, h * 0.45f + dy * 0.5f, w, h * 0.47f + dy * 0.5f)
        }
        val dune = crest(0f).apply { lineTo(w, h); lineTo(w * 0.05f, h); close() }
        drawPath(dune, Brush.verticalGradient(listOf(Color(0xFF0B1A33), Color(0xFF050A14)), startY = h * 0.45f, endY = h))
        // flowing contour lines
        for (i in 1..16) {
            val dy = i * h * 0.028f
            drawPath(crest(dy), Color(0xFF3F7FD6).copy(alpha = 0.22f * (1f - i / 18f)), style = Stroke(width = 1.2f))
        }
        drawPath(crest(0f), Color(0xFF5AA9FF).copy(alpha = 0.18f), style = Stroke(width = 10f))
        drawPath(crest(0f), Color(0xFF7FC0FF).copy(alpha = 0.75f), style = Stroke(width = 2.2f))

        // fade into the UI background on the left, top and bottom
        drawRect(Brush.horizontalGradient(0f to RadioColors.Background, 0.38f to Color.Transparent))
        drawRect(Brush.verticalGradient(0f to RadioColors.Background, 0.18f to Color.Transparent, 0.82f to Color.Transparent, 1f to RadioColors.Background))
    }
}

/** Small "on air" equaliser used in lists. */
@Composable
fun OnAirIndicator(playing: Boolean, modifier: Modifier = Modifier) {
    LevelBars(playing, modifier.size(width = 26.dp, height = 22.dp), color = RadioColors.AccentBright)
}

@Composable
fun Pill(text: String, modifier: Modifier = Modifier, color: Color = RadioColors.AccentBright) {
    Box(
        modifier
            .border(1.5.dp, color, RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = color, fontSize = 21.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp))
    }
}
