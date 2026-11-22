package com.pleos.radio.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pleos.radio.domain.caption.CaptionEngineType
import com.pleos.radio.domain.caption.CaptionLanguage
import com.pleos.radio.domain.caption.CaptionState
import com.pleos.radio.domain.caption.CaptionStatus
import com.pleos.radio.presentation.ui.theme.RadioColors

/** Caption body: five 18sp lines (23sp line height). */
private const val CAPTION_LINES = 5
private val CAPTION_BODY_HEIGHT = (23 * CAPTION_LINES).dp

/** Height reserved on the main screen: header + five caption lines. */
val CAPTION_PANEL_HEIGHT = 158.dp

/**
 * Live translation captions for foreign-language stations (DAB / DRM): a header with the
 * "source → target" pair, status and language menu, and the latest five lines of translated speech.
 */
@Composable
fun CaptionPanel(
    state: CaptionState,
    onSelectLanguage: (CaptionLanguage) -> Unit,
    onDownloadModel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier
            .fillMaxWidth()
            .height(CAPTION_PANEL_HEIGHT)
            .background(RadioColors.Surface.copy(alpha = 0.88f), shape)
            .border(1.dp, RadioColors.Divider, shape)
            .padding(horizontal = 18.dp, vertical = 8.dp),
    ) {
        Row(Modifier.height(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Translate, null, tint = RadioColors.AccentBright, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                if (state.target == CaptionLanguage.OFF) "Live translation · ${state.sourceLabel}"
                else "Live translation · ${state.sourceLabel} → ${state.target.label}",
                color = RadioColors.TextSecondary, fontSize = 17.sp, fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                if (state.engine == CaptionEngineType.ON_DEVICE) "On-device" else "Cloud",
                color = RadioColors.Muted, fontSize = 13.sp,
                modifier = Modifier.border(1.dp, RadioColors.Outline, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp),
            )
            Spacer(Modifier.width(12.dp))
            StatusTag(state)
            Spacer(Modifier.weight(1f))
            LanguageMenu(state.target, onSelectLanguage)
        }
        when (state.status) {
            CaptionStatus.NEED_MODEL -> ModelPrompt(state, onDownloadModel)
            CaptionStatus.DOWNLOADING -> ModelProgress(state)
            else -> CaptionLines(state)
        }
    }
}

@Composable
private fun ModelPrompt(state: CaptionState, onDownload: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(CAPTION_BODY_HEIGHT), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Free on-device captions need a one-time speech model download (${state.modelSizeMb} MB). " +
                "Translation models (~30 MB per language) download automatically on first use.",
            color = RadioColors.Muted, fontSize = 18.sp, lineHeight = 24.sp, modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            "Download", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier
                .background(RadioColors.Accent, RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = onDownload)
                .padding(horizontal = 22.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun ModelProgress(state: CaptionState) {
    Column(Modifier.fillMaxWidth().height(CAPTION_BODY_HEIGHT), verticalArrangement = Arrangement.Center) {
        Text(
            "Downloading speech model… ${(state.progress * 100).toInt()}% of ${state.modelSizeMb} MB",
            color = RadioColors.TextSecondary, fontSize = 18.sp,
        )
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { state.progress },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = RadioColors.AccentBright,
            trackColor = RadioColors.Divider,
        )
    }
}

@Composable
private fun StatusTag(state: CaptionState) {
    when (state.status) {
        CaptionStatus.TRANSLATING -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(14.dp), color = RadioColors.AccentBright, strokeWidth = 2.dp)
            Spacer(Modifier.width(6.dp))
            Text("Translating…", color = RadioColors.Muted, fontSize = 15.sp)
        }
        CaptionStatus.LISTENING -> Text("Listening", color = RadioColors.Muted, fontSize = 15.sp)
        else -> Unit
    }
}

@Composable
private fun CaptionLines(state: CaptionState) {
    // Five 18sp lines; the text scrolls so the newest words are always at the bottom.
    val scroll = rememberScrollState()
    val (text, color) = when (state.status) {
        CaptionStatus.OFF ->
            (if (state.target == CaptionLanguage.OFF) "Captions are off. Choose a language on the right." else "Captions start when playback starts.") to RadioColors.Muted
        CaptionStatus.NEED_MODEL, CaptionStatus.DOWNLOADING -> "" to RadioColors.Muted
        CaptionStatus.NEED_KEY ->
            "Add a Google Cloud API key (Speech-to-Text + Translation) in Settings to see live captions." to RadioColors.Muted
        CaptionStatus.ERROR -> (state.error ?: "Captions paused") to Color(0xFFFF7A85)
        else -> state.text.ifBlank { "Waiting for speech…" } to if (state.text.isBlank()) RadioColors.Muted else RadioColors.Text
    }
    LaunchedEffect(text) { scroll.animateScrollTo(scroll.maxValue) }
    Box(Modifier.fillMaxWidth().height(CAPTION_BODY_HEIGHT).padding(top = 2.dp)) {
        Text(text, color = color, fontSize = 18.sp, lineHeight = 23.sp, modifier = Modifier.verticalScroll(scroll))
    }
}

@Composable
private fun LanguageMenu(current: CaptionLanguage, onSelect: (CaptionLanguage) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .border(1.dp, RadioColors.Outline, RoundedCornerShape(14.dp))
                .clickable(role = Role.DropdownList, onClickLabel = "Caption language") { open = true }
                .padding(start = 12.dp, end = 4.dp, top = 1.dp, bottom = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(current.label, color = RadioColors.Text, fontSize = 16.sp)
            Icon(Icons.Filled.ArrowDropDown, null, tint = RadioColors.TextSecondary, modifier = Modifier.size(22.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            CaptionLanguage.entries.forEach { lang ->
                DropdownMenuItem(
                    text = { Text(lang.label, fontSize = 18.sp, fontWeight = if (lang == current) FontWeight.SemiBold else FontWeight.Normal) },
                    onClick = { open = false; onSelect(lang) },
                )
            }
        }
    }
}
