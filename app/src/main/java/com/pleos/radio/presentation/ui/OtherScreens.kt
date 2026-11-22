package com.pleos.radio.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pleos.radio.presentation.RadioUiState
import com.pleos.radio.presentation.RadioViewModel
import com.pleos.radio.presentation.Section
import com.pleos.radio.presentation.ui.theme.RadioColors

/** Media / Phone / Navigation are out of scope for this app; they keep a mini player so radio stays controllable. */
@Composable
fun PlaceholderScreen(section: Section, ui: RadioUiState, vm: RadioViewModel) {
    val icon = when (section) {
        Section.MEDIA -> Icons.Filled.MusicNote
        Section.PHONE -> Icons.Filled.Phone
        Section.NAVIGATION -> Icons.Filled.Navigation
        else -> Icons.Filled.Settings
    }
    Column(Modifier.fillMaxSize().padding(52.dp)) {
        Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = RadioColors.Outline, modifier = Modifier.size(120.dp))
            Spacer(Modifier.height(20.dp))
            Text(section.name.lowercase().replaceFirstChar { it.uppercase() }, color = RadioColors.Text, fontSize = 36.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            Text("This demo implements the Radio app. Radio keeps playing in the background.", color = RadioColors.Muted, fontSize = 22.sp)
        }
        MiniPlayer(ui, vm)
    }
}

@Composable
private fun MiniPlayer(ui: RadioUiState, vm: RadioViewModel) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().height(110.dp)
            .background(RadioColors.Surface, shape)
            .border(1.dp, RadioColors.Divider, shape)
            .clickable(onClickLabel = "Open radio", onClick = vm::goHome)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Radio, null, tint = RadioColors.AccentBright, modifier = Modifier.size(48.dp))
        Spacer(Modifier.width(22.dp))
        Column(Modifier.weight(1f)) {
            Text(ui.current?.name ?: "Radio", color = RadioColors.Text, fontSize = 26.sp, fontWeight = FontWeight.Medium)
            Text("${ui.band.label} · ${ui.current?.frequencyLabel.orEmpty()}", color = RadioColors.Muted, fontSize = 20.sp)
        }
        Box(
            Modifier.size(72.dp).border(2.dp, RadioColors.AccentBright, CircleShape)
                .clickable(role = Role.Button, onClick = vm::togglePlay),
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (ui.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (ui.playing) "Pause" else "Play",
                tint = RadioColors.Text, modifier = Modifier.size(38.dp))
        }
    }
}

@Composable
fun SettingsScreen(ui: RadioUiState, vm: RadioViewModel) {
    Column(Modifier.fillMaxSize().padding(horizontal = 52.dp, vertical = 36.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingRow("Simulated audio (ATSC)", "FM, AM, DAB and DRM always play the broadcaster's live internet stream; ATSC uses a generated tone") {
            Switch(
                checked = ui.audioEnabled,
                onCheckedChange = vm::setAudioEnabled,
                colors = SwitchDefaults.colors(checkedTrackColor = RadioColors.Accent),
            )
        }
        SettingRow("Reset presets", "Restore the factory preset list for every band") {
            Text(
                "Reset",
                color = RadioColors.Text,
                fontSize = 22.sp,
                modifier = Modifier
                    .border(1.dp, RadioColors.Outline, RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = vm::resetPresets)
                    .padding(horizontal = 24.dp, vertical = 10.dp),
            )
        }
        SettingRow("Favorites", "${ui.favorites.size} station(s) saved on this device") {}
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, trailing: @Composable () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth().height(104.dp).background(RadioColors.Surface, shape).padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = RadioColors.Text, fontSize = 25.sp)
            Text(subtitle, color = RadioColors.Muted, fontSize = 19.sp)
        }
        trailing()
    }
}
