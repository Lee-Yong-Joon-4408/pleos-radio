package com.pleos.radio.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pleos.radio.presentation.RadioViewModel
import com.pleos.radio.presentation.Section
import com.pleos.radio.presentation.ui.theme.RadioColors
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Root: left rail + top bar + section content, scaled so the 1280×768 design fits any cockpit display. */
@Composable
fun RadioApp(vm: RadioViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    ScaledDesign(designWidth = 1280.dp, designHeight = 768.dp) {
        Box(Modifier.fillMaxSize().background(RadioColors.Background)) {
            Row(Modifier.fillMaxSize()) {
                NavRail(selected = ui.section, onHome = vm::goHome, onSelect = vm::selectSection)
                Box(Modifier.width(1.dp).fillMaxHeight().background(RadioColors.Divider))
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    TopBar(path = breadcrumb(ui.section, ui.band.label))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(RadioColors.Divider))
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        when (ui.section) {
                            Section.RADIO -> RadioScreen(ui, vm)
                            Section.SETTINGS -> SettingsScreen(ui, vm)
                            else -> PlaceholderScreen(ui.section, ui, vm)
                        }
                    }
                }
            }

            StationListPanel(ui, vm)

            AnimatedVisibility(
                visible = ui.message != null,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 140.dp),
            ) {
                Text(
                    ui.message.orEmpty(),
                    color = RadioColors.Text,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .background(RadioColors.SurfaceHigh, RoundedCornerShape(28.dp))
                        .clickable(onClick = vm::dismissMessage)
                        .padding(horizontal = 28.dp, vertical = 14.dp),
                )
            }
        }
    }
}

private fun breadcrumb(section: Section, band: String): List<String> = when (section) {
    Section.RADIO -> listOf("Radio", band)
    Section.MEDIA -> listOf("Media")
    Section.SETTINGS -> listOf("Settings")
}

/**
 * Lays content out in a fixed design space ([designWidth]×[designHeight] dp) and scales it uniformly
 * to the real window, so a 12.3"/2560×1440 Pleos display and a small emulator look the same.
 */
@Composable
fun ScaledDesign(designWidth: Dp, designHeight: Dp, content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val base = LocalDensity.current
        val scale = minOf(maxWidth / designWidth, maxHeight / designHeight)
        CompositionLocalProvider(LocalDensity provides Density(base.density * scale, 1f)) {
            content()
        }
    }
}

// --- left navigation rail -----------------------------------------------------------------

private val RAIL_ITEM_HEIGHT = 140.dp

private data class RailItem(val section: Section, val label: String, val icon: ImageVector)

private val RAIL_ITEMS = listOf(
    RailItem(Section.RADIO, "Radio", Icons.Filled.Radio),
    RailItem(Section.MEDIA, "Media", Icons.Filled.MusicNote),
)

private val SETTINGS_ITEM = RailItem(Section.SETTINGS, "Settings", Icons.Filled.Settings)

@Composable
private fun NavRail(selected: Section, onHome: () -> Unit, onSelect: (Section) -> Unit) {
    Column(Modifier.width(120.dp).fillMaxHeight().background(RadioColors.Rail)) {
        Box(Modifier.fillMaxWidth().height(82.dp).clickable(onClick = onHome), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Home, contentDescription = "Home", tint = RadioColors.TextSecondary, modifier = Modifier.size(40.dp))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(RadioColors.Divider))
        Spacer(Modifier.height(18.dp))
        // Radio and Media at the top, Settings pinned to the bottom.
        RAIL_ITEMS.forEach { item ->
            RailButton(item, item.section == selected, Modifier.height(RAIL_ITEM_HEIGHT)) { onSelect(item.section) }
        }
        Spacer(Modifier.weight(1f))
        RailButton(SETTINGS_ITEM, SETTINGS_ITEM.section == selected, Modifier.height(RAIL_ITEM_HEIGHT)) { onSelect(Section.SETTINGS) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun RailButton(item: RailItem, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val tint = if (selected) RadioColors.Text else RadioColors.Muted
    Box(
        modifier
            .fillMaxWidth()
            .then(
                if (selected) Modifier.background(Brush.horizontalGradient(listOf(Color(0xFF0F2C55), Color(0xFF0A1830))))
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                Modifier.align(Alignment.CenterStart).width(5.dp).fillMaxHeight()
                    .background(RadioColors.AccentBright, RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(item.icon, contentDescription = null, tint = if (selected) RadioColors.AccentBright else tint, modifier = Modifier.size(44.dp))
            Text(item.label, color = tint, fontSize = 20.sp)
        }
    }
}

// --- top status bar -----------------------------------------------------------------------

@Composable
private fun TopBar(path: List<String>) {
    Row(
        Modifier.fillMaxWidth().height(82.dp).padding(start = 42.dp, end = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        path.forEachIndexed { i, part ->
            val last = i == path.lastIndex
            if (i > 0) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = RadioColors.Muted,
                    modifier = Modifier.padding(horizontal = 14.dp).size(30.dp),
                )
            }
            Text(part, color = if (last) RadioColors.Text else RadioColors.Muted, fontSize = 29.sp,
                fontWeight = if (last) FontWeight.Medium else FontWeight.Normal)
        }
        Spacer(Modifier.weight(1f))
        Icon(Icons.Filled.Bluetooth, "Bluetooth connected", tint = RadioColors.Text, modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(22.dp))
        Icon(Icons.Filled.SignalCellularAlt, "Signal", tint = RadioColors.Text, modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(36.dp))
        Clock()
    }
}

@Composable
private fun Clock() {
    val formatter = remember { DateTimeFormatter.ofPattern("h:mm a", Locale.US) }
    val time by produceState(LocalTime.now().format(formatter)) {
        while (true) {
            value = LocalTime.now().format(formatter)
            delay(1_000L)
        }
    }
    Text(time, color = RadioColors.Text, fontSize = 31.sp)
}
