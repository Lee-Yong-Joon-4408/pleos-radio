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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.pleos.radio.data.translate.WhisperModelStore
import com.pleos.radio.domain.caption.CaptionEngineType
import com.pleos.radio.domain.caption.CaptionLanguage
import com.pleos.radio.domain.caption.WhisperModel
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

/** Media is out of scope for this app; it keeps a mini player so radio stays controllable. */
@Composable
fun PlaceholderScreen(section: Section, ui: RadioUiState, vm: RadioViewModel) {
    val icon = when (section) {
        Section.MEDIA -> Icons.Filled.MusicNote
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
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 52.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SettingRow("Live caption engine", "On-device is free and works offline; Google Cloud is paid and needs an API key") {
            ChipRow(CaptionEngineType.entries, ui.captionEngine, { it.label }, vm::setCaptionEngine)
        }
        if (ui.captionEngine == CaptionEngineType.ON_DEVICE) {
            ModelRow(ui, vm)
        } else {
            ApiKeyRow(ui.apiKeySource, vm::saveApiKey)
        }
        SettingRow("Caption language", "Translation target for DAB / DRM live captions") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CaptionLanguage.entries.forEach { lang ->
                    val selected = lang == ui.captionLanguage
                    Text(
                        lang.label,
                        color = if (selected) RadioColors.Text else RadioColors.Muted,
                        fontSize = 19.sp,
                        modifier = Modifier
                            .background(if (selected) RadioColors.AccentDeep else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(18.dp))
                            .border(1.dp, if (selected) RadioColors.Accent else RadioColors.Outline, RoundedCornerShape(18.dp))
                            .clickable(role = Role.RadioButton) { vm.setCaptionLanguage(lang) }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            }
        }
        SettingRow("FM / AM transcript", "Show Korean speech as text in the caption panel") {
            ChipRow(listOf(true, false), ui.transcriptEnabled, { if (it) "On" else "Off" }, vm::setTranscriptEnabled)
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
private fun <T> ChipRow(items: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            val on = item == selected
            Text(
                label(item),
                color = if (on) RadioColors.Text else RadioColors.Muted,
                fontSize = 19.sp,
                modifier = Modifier
                    .background(if (on) RadioColors.AccentDeep else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(18.dp))
                    .border(1.dp, if (on) RadioColors.Accent else RadioColors.Outline, RoundedCornerShape(18.dp))
                    .clickable(role = Role.RadioButton) { onSelect(item) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
}

/** whisper model choice + download / delete for free on-device captions. */
@Composable
private fun ModelRow(ui: RadioUiState, vm: RadioViewModel) {
    val model = ui.whisperModel
    val state = ui.modelStates[model] ?: WhisperModelStore.State.Missing
    val shape = RoundedCornerShape(12.dp)
    Column(Modifier.fillMaxWidth().background(RadioColors.Surface, shape).padding(horizontal = 28.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Speech model (whisper.cpp)", color = RadioColors.Text, fontSize = 25.sp)
                Text(
                    when (state) {
                        WhisperModelStore.State.Ready -> "Downloaded · captions work offline"
                        is WhisperModelStore.State.Downloading -> "Downloading… ${(state.progress * 100).toInt()}%"
                        is WhisperModelStore.State.Failed -> "Download failed: ${state.message}"
                        WhisperModelStore.State.Missing -> "Not downloaded · Base is recommended (Tiny is faster but less accurate)"
                    },
                    color = if (state is WhisperModelStore.State.Failed) androidx.compose.ui.graphics.Color(0xFFFF7A85) else RadioColors.Muted,
                    fontSize = 18.sp,
                )
            }
            ChipRow(WhisperModel.entries, model, { it.label }, vm::setWhisperModel)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            when (state) {
                WhisperModelStore.State.Ready -> SmallButton("Delete") { vm.deleteModel(model) }
                is WhisperModelStore.State.Downloading -> SmallButton("Cancel") { vm.deleteModel(model) }
                else -> SmallButton("Download ${model.sizeMb} MB") { vm.downloadModel(model) }
            }
        }
    }
}

@Composable
private fun SmallButton(text: String, onClick: () -> Unit) {
    Text(
        text, color = RadioColors.Text, fontSize = 19.sp,
        modifier = Modifier.border(1.dp, RadioColors.Outline, RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
    )
}

/** Google Cloud API key (Speech-to-Text + Cloud Translation) for live captions; stored on the device. */
@Composable
private fun ApiKeyRow(source: String?, onSave: (String) -> Unit) {
    var draft by remember { mutableStateOf("") }
    val shape = RoundedCornerShape(12.dp)
    Column(Modifier.fillMaxWidth().background(RadioColors.Surface, shape).padding(horizontal = 28.dp, vertical = 14.dp)) {
        Text("Google Cloud API key", color = RadioColors.Text, fontSize = 25.sp)
        Text(
            when (source) {
                "settings" -> "Using the key saved on this device. Enable Speech-to-Text and Cloud Translation for it."
                "build" -> "Using the key from local.properties (GOOGLE_CLOUD_API_KEY)."
                else -> "No key yet – live captions for DAB / DRM need one (Speech-to-Text + Cloud Translation)."
            },
            color = RadioColors.Muted, fontSize = 18.sp,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                singleLine = true,
                placeholder = { Text("AIza…", fontSize = 18.sp) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp, color = RadioColors.Text),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "Save", color = RadioColors.Text, fontSize = 20.sp,
                modifier = Modifier.border(1.dp, RadioColors.Outline, RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button) { onSave(draft); draft = "" }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            )
            if (source == "settings") {
                Spacer(Modifier.width(8.dp))
                Text(
                    "Clear", color = RadioColors.Muted, fontSize = 20.sp,
                    modifier = Modifier.clickable(role = Role.Button) { onSave("") }.padding(horizontal = 12.dp, vertical = 10.dp),
                )
            }
        }
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
