package com.pleos.radio.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pleos.radio.domain.Station
import com.pleos.radio.presentation.RadioUiState
import com.pleos.radio.presentation.RadioViewModel
import com.pleos.radio.presentation.ui.theme.RadioColors

/** Right-hand sheet opened by "Station List": all / favourite services of the band, plus a band scan. */
@Composable
fun StationListPanel(ui: RadioUiState, vm: RadioViewModel) {
    AnimatedVisibility(ui.stationListOpen, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f))
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = vm::closeStationList)
        )
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
        AnimatedVisibility(
            ui.stationListOpen,
            enter = slideInHorizontally { it },
            exit = slideOutHorizontally { it },
        ) {
            Column(
                Modifier
                    .width(560.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF0A1019), RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp))
                    .border(1.dp, RadioColors.Divider, RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp))
                    .clickable(remember { MutableInteractionSource() }, indication = null) {} // swallow taps
                    .padding(horizontal = 28.dp, vertical = 22.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Station List", color = RadioColors.Text, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(14.dp))
                    Pill(ui.band.label)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = vm::closeStationList, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Filled.Close, "Close", tint = RadioColors.Text, modifier = Modifier.size(34.dp))
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip("All (${ui.stations.size})", !ui.favoritesOnly) { vm.setFavoritesOnly(false) }
                    FilterChip("Favorites (${ui.stations.count { it.id in ui.favorites }})", ui.favoritesOnly) { vm.setFavoritesOnly(true) }
                    Spacer(Modifier.weight(1f))
                    ScanButton(ui.scanning, vm::scan)
                }
                Spacer(Modifier.height(14.dp))
                if (ui.scanning) {
                    LinearProgressIndicator(
                        progress = { ui.scanProgress },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = RadioColors.AccentBright,
                        trackColor = RadioColors.Divider,
                    )
                    Text("Scanning ${ui.band.label}…", color = RadioColors.Muted, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
                } else {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(RadioColors.Divider))
                }
                val list = ui.listedStations
                if (list.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if (ui.favoritesOnly) "No favorites yet.\nTap ☆ to add a station." else "No stations. Tap Scan.",
                            color = RadioColors.Muted, fontSize = 22.sp,
                        )
                    }
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(list, key = { it.id }) { station ->
                            StationRow(
                                station = station,
                                current = station.id == ui.current?.id,
                                playing = ui.playing,
                                favorite = station.id in ui.favorites,
                                onClick = { vm.tune(station); vm.closeStationList() },
                                onToggleFavorite = { vm.toggleFavorite(station) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Text(
        text,
        color = if (selected) RadioColors.Text else RadioColors.Muted,
        fontSize = 20.sp,
        modifier = Modifier
            .background(if (selected) RadioColors.AccentDeep else Color.Transparent, shape)
            .border(1.dp, if (selected) RadioColors.Accent else RadioColors.Outline, shape)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
    )
}

@Composable
private fun ScanButton(scanning: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        Modifier
            .border(1.dp, RadioColors.Outline, shape)
            .clickable(enabled = !scanning, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Refresh, null, tint = if (scanning) RadioColors.Muted else RadioColors.Text, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(8.dp))
        Text(if (scanning) "Scanning" else "Scan", color = if (scanning) RadioColors.Muted else RadioColors.Text, fontSize = 20.sp)
    }
}

@Composable
private fun StationRow(
    station: Station,
    current: Boolean,
    playing: Boolean,
    favorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(82.dp)
            .background(if (current) RadioColors.AccentDeep.copy(alpha = 0.6f) else Color.Transparent, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(40.dp), contentAlignment = Alignment.Center) {
            if (current) OnAirIndicator(playing) else SignalBars(station.signal, size = 24.dp, active = RadioColors.TextSecondary)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(station.name, color = if (current) RadioColors.AccentBright else RadioColors.Text, fontSize = 24.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${station.frequencyLabel}  ·  ${station.genre}", color = RadioColors.Muted, fontSize = 18.sp, maxLines = 1)
        }
        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(56.dp)) {
            Icon(
                if (favorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = if (favorite) "Remove ${station.name} from favorites" else "Add ${station.name} to favorites",
                tint = if (favorite) RadioColors.Favorite else RadioColors.Muted,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}
