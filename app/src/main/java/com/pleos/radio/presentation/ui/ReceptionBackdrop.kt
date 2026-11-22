package com.pleos.radio.presentation.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pleos.radio.audio.ReceptionPath
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

private val WaveBlue = Color(0xFF5AA9FF)
private val BeaconRed = Color(0xFFFF5A5F)
private val IpCyan = Color(0xFF3DDBD9)

/**
 * Animated layer over the night landscape showing how the audio is arriving:
 * radio waves from a transmitter for the broadcast network, data packets flowing through a network
 * into a receiver for the IP network. Static (no layer) while not on air.
 */
@Composable
fun ReceptionBackdrop(path: ReceptionPath, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = path,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        modifier = modifier,
        label = "reception",
    ) { p ->
        when (p) {
            ReceptionPath.BROADCAST -> BroadcastWaves(Modifier.fillMaxSize())
            ReceptionPath.IP -> IpStream(Modifier.fillMaxSize())
            ReceptionPath.NONE -> Unit
        }
    }
}

@Composable
private fun BroadcastWaves(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "waves")
    val phase by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(2_600, easing = LinearEasing), RepeatMode.Restart), label = "phase",
    )
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val base = Offset(w * 0.86f, h * 0.70f)
        val top = Offset(w * 0.86f, h * 0.30f)
        drawTower(base, top)

        // Expanding wave fronts from the antenna, sweeping toward the listener (left).
        val maxR = w * 0.62f
        val rings = 5
        for (i in 0 until rings) {
            val f = (phase + i.toFloat() / rings) % 1f
            val r = 18f + f * maxR
            val alpha = (1f - f).pow(1.6f) * 0.55f
            drawArc(
                color = WaveBlue.copy(alpha = alpha),
                startAngle = 115f, sweepAngle = 130f, useCenter = false,
                topLeft = Offset(top.x - r, top.y - r), size = Size(r * 2, r * 2),
                style = Stroke(width = 3.2f, cap = StrokeCap.Round),
            )
        }
        // Carrier waves flowing across the sky.
        for (k in 0 until 3) {
            val y = h * (0.20f + 0.13f * k)
            val amp = h * 0.018f
            val wavelength = w * 0.12f
            val shift = phase * wavelength * (1.4f + 0.3f * k)
            val path = Path()
            var x = w * 0.30f
            path.moveTo(x, y)
            while (x <= top.x - 20f) {
                path.lineTo(x, y + amp * sin(((x + shift) / wavelength) * 2 * PI).toFloat())
                x += 6f
            }
            drawPath(path, WaveBlue.copy(alpha = 0.10f + 0.05f * k), style = Stroke(width = 2f))
        }
        // Blinking aviation light.
        val blink = if (phase < 0.5f) 1f else 0.25f
        drawCircle(BeaconRed.copy(alpha = 0.35f * blink), radius = 14f, center = top)
        drawCircle(BeaconRed.copy(alpha = blink), radius = 5f, center = top)
    }
}

private fun DrawScope.drawTower(base: Offset, top: Offset) {
    val half = size.width * 0.028f
    val color = Color(0xFF8FB8E8).copy(alpha = 0.55f)
    val left = Offset(base.x - half, base.y)
    val right = Offset(base.x + half, base.y)
    drawLine(color, left, top, strokeWidth = 2.5f)
    drawLine(color, right, top, strokeWidth = 2.5f)
    // Lattice cross-bracing.
    val steps = 6
    for (i in 0 until steps) {
        val t0 = i.toFloat() / steps
        val t1 = (i + 1).toFloat() / steps
        fun lerp(a: Offset, b: Offset, t: Float) = Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
        val l0 = lerp(left, top, t0); val r0 = lerp(right, top, t0)
        val l1 = lerp(left, top, t1); val r1 = lerp(right, top, t1)
        drawLine(color, l0, r0, strokeWidth = 1.5f)
        drawLine(color, l0, r1, strokeWidth = 1.2f)
        drawLine(color, r0, l1, strokeWidth = 1.2f)
    }
    drawLine(color, top, Offset(top.x, top.y - size.height * 0.05f), strokeWidth = 2.5f)
}

/** Network mesh (normalised coordinates) and the receiver node packets flow into. */
private val NODES = listOf(
    0.92f to 0.20f, 0.80f to 0.14f, 0.70f to 0.28f, 0.88f to 0.40f, 0.76f to 0.50f,
    0.94f to 0.62f, 0.82f to 0.72f, 0.66f to 0.66f, 0.60f to 0.42f, 0.72f to 0.84f,
)
private val EDGES = listOf(0 to 1, 1 to 2, 0 to 3, 2 to 3, 3 to 4, 2 to 8, 4 to 8, 3 to 5, 4 to 6, 5 to 6, 6 to 7, 7 to 8, 6 to 9, 7 to 9, 4 to 7)
private val UPLINKS = listOf(7, 9, 6, 4)          // nodes connected to the receiver
// Receiver sits in the open lower-right area, clear of the playback controls.
private const val RX_X = 0.64f
private const val RX_Y = 0.90f

@Composable
private fun IpStream(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "ip")
    val phase by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(2_200, easing = LinearEasing), RepeatMode.Restart), label = "phase",
    )
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        fun pt(i: Int) = Offset(NODES[i].first * w, NODES[i].second * h)
        val rx = Offset(RX_X * w, RX_Y * h)

        // Mesh links and nodes.
        EDGES.forEach { (a, b) -> drawLine(IpCyan.copy(alpha = 0.16f), pt(a), pt(b), strokeWidth = 1.5f) }
        UPLINKS.forEach { n -> drawLine(IpCyan.copy(alpha = 0.22f), pt(n), rx, strokeWidth = 1.8f) }
        NODES.indices.forEach { i ->
            val pulse = 0.5f + 0.5f * sin((phase + i * 0.17f) * 2 * PI).toFloat()
            drawCircle(IpCyan.copy(alpha = 0.18f + 0.22f * pulse), radius = 9f, center = pt(i))
            drawCircle(IpCyan.copy(alpha = 0.85f), radius = 3.5f, center = pt(i))
        }

        // Packets travelling along the mesh, then down the uplinks into the receiver.
        EDGES.forEachIndexed { k, (a, b) ->
            val t = (phase * 1.3f + k * 0.37f) % 1f
            val p = Offset(pt(a).x + (pt(b).x - pt(a).x) * t, pt(a).y + (pt(b).y - pt(a).y) * t)
            drawCircle(IpCyan.copy(alpha = 0.55f), radius = 2.6f, center = p)
        }
        UPLINKS.forEachIndexed { k, n ->
            for (j in 0 until 3) {
                val t = (phase + k * 0.21f + j / 3f) % 1f
                val from = pt(n)
                val p = Offset(from.x + (rx.x - from.x) * t, from.y + (rx.y - from.y) * t)
                drawCircle(Color.White.copy(alpha = 0.85f * (1f - t * 0.4f)), radius = 3.2f, center = p)
                drawCircle(IpCyan.copy(alpha = 0.30f), radius = 7f, center = p)
            }
        }

        // Receiver: steady core with "data received" ripples.
        for (i in 0 until 3) {
            val f = (phase + i / 3f) % 1f
            drawCircle(IpCyan.copy(alpha = (1f - f) * 0.45f), radius = 10f + f * hypot(w, h) * 0.05f, center = rx, style = Stroke(2f))
        }
        drawCircle(IpCyan.copy(alpha = 0.35f), radius = 13f, center = rx)
        drawCircle(Color.White, radius = 5f, center = rx)
    }
}
