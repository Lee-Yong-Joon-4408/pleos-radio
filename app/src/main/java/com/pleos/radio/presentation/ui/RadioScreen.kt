package com.pleos.radio.presentation.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pleos.radio.audio.AudioStatus
import com.pleos.radio.domain.Band
import com.pleos.radio.domain.RadioLogic
import com.pleos.radio.domain.Station
import com.pleos.radio.presentation.RadioUiState
import com.pleos.radio.presentation.RadioViewModel
import com.pleos.radio.presentation.ui.theme.RadioColors

/** The main (launch) screen from the design reference. */
@Composable
fun RadioScreen(ui: RadioUiState, vm: RadioViewModel) {
    Box(Modifier.fillMaxSize()) {
        NightLandscape(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 118.dp)
                .fillMaxWidth(0.66f)
                .height(470.dp)
        )
        Column(Modifier.fillMaxSize().padding(start = 52.dp, end = 34.dp)) {
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth().height(80.dp), verticalAlignment = Alignment.CenterVertically) {
                BandTabs(ui.band, vm::selectBand, Modifier.weight(1f).fillMaxHeight())
                Spacer(Modifier.width(24.dp))
                StationListButton(vm::openStationList)
            }
            Spacer(Modifier.height(12.dp))
            NowPlaying(ui, vm::retry)
            PlaybackControls(ui, vm, Modifier.padding(start = 310.dp))
            Spacer(Modifier.weight(1f))
            // Live captions: translated for DAB / DRM, speech-to-text for FM / AM.
            if (ui.caption.available) {
                CaptionPanel(ui.caption, vm::setCaptionLanguage, vm::setTranscriptEnabled, onDownloadModel = { vm.downloadModel() })
                Spacer(Modifier.height(8.dp))
            }
            PresetBar(ui, vm)
            Spacer(Modifier.height(10.dp))
        }
    }
}

// --- band tabs ----------------------------------------------------------------------------

@Composable
private fun BandTabs(selected: Band, onSelect: (Band) -> Unit, modifier: Modifier) {
    Box(modifier) {
        // baseline that fades out toward the station list button
        Box(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().height(1.5.dp)
                .background(Brush.horizontalGradient(listOf(RadioColors.Outline, RadioColors.Outline, Color.Transparent)))
        )
        Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.Bottom) {
            Band.entries.forEach { band ->
                BandTab(band.label, band == selected) { onSelect(band) }
            }
        }
    }
}

@Composable
private fun BandTab(label: String, selected: Boolean, onClick: () -> Unit) {
    val textColor by animateColorAsState(if (selected) RadioColors.Text else RadioColors.Muted, label = "tab")
    val shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
    Box(
        Modifier
            .width(150.dp)
            .height(72.dp)
            .then(
                if (selected) Modifier
                    .background(Brush.verticalGradient(listOf(Color(0xFF0E2A4F), Color(0xFF0B1E3A))), shape)
                    .border(1.dp, Color(0xFF1D4679), shape)
                    .drawBehind {
                        // glowing underline
                        val y = size.height - 2.dp.toPx()
                        drawLine(RadioColors.AccentBright.copy(alpha = 0.25f), Offset(8f, y), Offset(size.width - 8f, y), strokeWidth = 10.dp.toPx())
                        drawLine(RadioColors.AccentBright, Offset(8f, y), Offset(size.width - 8f, y), strokeWidth = 3.dp.toPx())
                    }
                else Modifier
            )
            .clickable(role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = textColor, fontSize = 29.sp, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal)
    }
}

@Composable
private fun StationListButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        Modifier
            .height(68.dp)
            .border(1.5.dp, RadioColors.Outline, shape)
            .background(RadioColors.Background.copy(alpha = 0.6f), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 26.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.AutoMirrored.Filled.List, null, tint = RadioColors.Text, modifier = Modifier.size(38.dp))
        Spacer(Modifier.width(18.dp))
        Text("Station List", color = RadioColors.Text, fontSize = 26.sp)
    }
}

// --- now playing --------------------------------------------------------------------------

@Composable
private fun NowPlaying(ui: RadioUiState, onRetry: () -> Unit) {
    val station = ui.current
    Row(Modifier.height(250.dp)) {
        StationArtwork(station, ui.band, ui.playing, Modifier.size(width = 258.dp, height = 250.dp))
        Spacer(Modifier.width(52.dp))
        if (station == null) {
            Text("No station – open Station List and scan", color = RadioColors.Muted, fontSize = 26.sp,
                modifier = Modifier.align(Alignment.CenterVertically))
            return@Row
        }
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill(station.badge)
                Spacer(Modifier.width(14.dp))
                if (ui.playing) AudioStatusChip(ui.audioStatus, onRetry)
            }
            Spacer(Modifier.height(14.dp))
            Text(station.name, color = RadioColors.Text, fontSize = 48.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Text(station.frequencyLabel, color = RadioColors.TextSecondary.copy(alpha = 0.8f), fontSize = 29.sp)
            Spacer(Modifier.height(18.dp))
            Text(station.programme, color = RadioColors.Text, fontSize = 28.sp)
            Spacer(Modifier.height(4.dp))
            AnimatedContent(
                targetState = station.infoLine(ui.labelIndex),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "dls",
            ) { line ->
                Text(line, color = RadioColors.Muted, fontSize = 25.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun PlaybackControls(ui: RadioUiState, vm: RadioViewModel, modifier: Modifier) {
    Row(modifier.height(88.dp), verticalAlignment = Alignment.CenterVertically) {
        RoundControl(Icons.Filled.SkipPrevious, "Previous station", 72.dp) { vm.seek(-1) }
        Spacer(Modifier.width(52.dp))
        RoundControl(
            if (ui.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            if (ui.playing) "Pause" else "Play",
            88.dp,
            highlighted = true,
            onClick = vm::togglePlay,
        )
        Spacer(Modifier.width(52.dp))
        RoundControl(Icons.Filled.SkipNext, "Next station", 72.dp) { vm.seek(+1) }
        Spacer(Modifier.width(62.dp))
        RoundControl(
            if (ui.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
            if (ui.isFavorite) "Remove favorite" else "Add favorite",
            68.dp,
            tint = if (ui.isFavorite) RadioColors.Favorite else RadioColors.Text,
        ) { vm.toggleFavorite() }
    }
}

@Composable
private fun RoundControl(
    icon: ImageVector,
    description: String,
    size: Dp,
    highlighted: Boolean = false,
    tint: Color = RadioColors.Text,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(size)
            .then(if (highlighted) Modifier.shadow(18.dp, CircleShape, ambientColor = RadioColors.Accent, spotColor = RadioColors.Accent) else Modifier)
            .background(if (highlighted) Color(0xFF0A1424) else RadioColors.SurfaceHigh.copy(alpha = 0.9f), CircleShape)
            .border(
                if (highlighted) 3.dp else 1.dp,
                if (highlighted) RadioColors.AccentBright else RadioColors.Outline.copy(alpha = 0.7f),
                CircleShape,
            )
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

/** LIVE / Connecting / error (tap to retry) / simulated, next to the service badge. */
@Composable
private fun AudioStatusChip(status: AudioStatus, onRetry: () -> Unit) {
    when (status) {
        AudioStatus.Live -> Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).background(Color(0xFFFF4D5E), CircleShape))
            Spacer(Modifier.width(8.dp))
            Text("LIVE", color = RadioColors.Text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
        }
        AudioStatus.Connecting -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(18.dp), color = RadioColors.AccentBright, strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
            Text("Connecting…", color = RadioColors.Muted, fontSize = 20.sp)
        }
        is AudioStatus.Error -> Text(
            "${status.message} · Retry",
            color = Color(0xFFFF7A85),
            fontSize = 20.sp,
            modifier = Modifier.clickable(role = Role.Button, onClickLabel = "Retry", onClick = onRetry),
        )
        AudioStatus.Simulated -> Text("Simulated audio", color = RadioColors.Muted.copy(alpha = 0.7f), fontSize = 18.sp)
        AudioStatus.Idle -> Unit
    }
}

// --- presets ------------------------------------------------------------------------------

@Composable
private fun PresetBar(ui: RadioUiState, vm: RadioViewModel) {
    val perPage = RadioLogic.PRESETS_PER_PAGE
    val first = ui.presetPage * perPage
    Row(Modifier.fillMaxWidth().height(72.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f).presetSwipe(onNext = { vm.changePresetPage(+1) }, onPrevious = { vm.changePresetPage(-1) }),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            for (slot in first until first + perPage) {
                PresetChip(
                    number = slot + 1,
                    station = ui.presets.getOrNull(slot),
                    active = ui.presets.getOrNull(slot)?.id == ui.current?.id && ui.current != null,
                    playing = ui.playing,
                    modifier = Modifier.weight(1f),
                    onClick = { vm.selectPreset(slot) },
                    onLongClick = { vm.savePreset(slot) },
                )
            }
        }
        // Next page (wraps from the last page to the first) with a page indicator underneath.
        Column(
            Modifier.width(64.dp).fillMaxHeight().clickable(role = Role.Button, onClickLabel = "Next preset page") { vm.changePresetPage(+1) },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Preset page ${ui.presetPage + 1} of ${ui.presetPageCount}",
                tint = RadioColors.TextSecondary,
                modifier = Modifier.size(44.dp),
            )
            Text("${ui.presetPage + 1} / ${ui.presetPageCount}", color = RadioColors.Muted, fontSize = 16.sp)
        }
    }
}

/** Horizontal swipe across the preset row flips pages (left = next, right = previous). */
private fun Modifier.presetSwipe(onNext: () -> Unit, onPrevious: () -> Unit): Modifier = pointerInput(Unit) {
    var total = 0f
    val threshold = 60.dp.toPx()
    detectHorizontalDragGestures(
        onDragStart = { total = 0f },
        onDragEnd = {
            if (total <= -threshold) onNext() else if (total >= threshold) onPrevious()
        },
    ) { change, dx ->
        change.consume()
        total += dx
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PresetChip(
    number: Int,
    station: Station?,
    active: Boolean,
    playing: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier
            .fillMaxHeight()
            .background(
                if (active) Brush.verticalGradient(listOf(Color(0xFF0F2B52), Color(0xFF0A1A33)))
                else Brush.verticalGradient(listOf(RadioColors.Surface, RadioColors.Surface)),
                shape,
            )
            .border(if (active) 2.dp else 1.dp, if (active) RadioColors.AccentBright else RadioColors.Divider, shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick, onLongClickLabel = "Save current station here")
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.align(Alignment.CenterStart)) {
            Text("$number", color = if (active) RadioColors.Text else RadioColors.Muted, fontSize = 20.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                station?.name ?: "Empty",
                color = if (station == null) RadioColors.Muted.copy(alpha = 0.6f) else RadioColors.TextSecondary,
                fontSize = 22.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (active) {
            SignalBars(
                station?.signal ?: 0,
                Modifier.align(Alignment.TopEnd).padding(top = 4.dp),
                active = if (playing) RadioColors.Text else RadioColors.Muted,
            )
        }
    }
}
