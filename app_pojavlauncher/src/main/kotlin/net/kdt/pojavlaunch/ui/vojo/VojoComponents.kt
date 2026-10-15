@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package net.kdt.pojavlaunch.ui.vojo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.kdt.pojavlaunch.R
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VojoLogo(modifier: Modifier = Modifier) {
    Canvas(modifier.size(48.dp)) {
        fun ribbon(points: List<Pair<Float,Float>>, color: Color) {
            val path = Path().apply {
                moveTo(size.width * points[0].first, size.height * points[0].second)
                points.drop(1).forEach { lineTo(size.width * it.first, size.height * it.second) }; close()
            }; drawPath(path, color)
        }
        ribbon(listOf(.10f to .22f,.30f to .22f,.52f to .72f,.42f to .93f),Color(0xFF7DD3FC))
        ribbon(listOf(.42f to .93f,.31f to .68f,.70f to .12f,.95f to .12f),Color(0xFFCDEEFF))
        ribbon(listOf(.69f to .12f,.95f to .12f,.85f to .27f),Color(0xFFA5B4FC))
    }
}

@Composable
fun CoverArt(modifier: Modifier = Modifier, alternate: Boolean = false) {
    Canvas(modifier.clip(RoundedCornerShape(12.dp)).background(Brush.verticalGradient(
        if (alternate) listOf(Color(0xFF212548),Color(0xFF34425E)) else listOf(Color(0xFF142F48),Color(0xFF234C5E))))) {
        drawCircle(Color(0xFFA8DDF4), size.width * .065f, Offset(size.width * .76f, size.height * .28f))
        listOf(Color(0xFF345C75),Color(0xFF21445D),Color(0xFF15334A)).forEachIndexed { layer,color ->
            val path = Path().apply {
                moveTo(0f,size.height)
                for (i in 0..24) {
                    val base = .63f + layer * .2f
                    val y = size.height * (base - .18f * sin(i * .35f + layer * 1.4f + if(alternate) 1 else 0) - .08f * cos(i * .8f + layer))
                    lineTo(size.width * i / 24, y)
                }; lineTo(size.width,size.height);close()
            }; drawPath(path,color)
        }
    }
}

@Composable
fun VojoIcon(resource: Int, description: String? = null, modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.primary) {
    Icon(painterResource(resource), description, modifier.size(24.dp), tint = tint)
}

@Composable
fun StateNotice(phase: Phase, retry: () -> Unit = {}) {
    when (phase) {
        Phase.LOADING -> {
            Text("Loading…", style = MaterialTheme.typography.bodyMedium)
            if (!rememberReducedMotion()) LinearProgressIndicator(Modifier.fillMaxWidth().semantics { contentDescription = "Loading" })
        }
        Phase.ERROR -> Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Couldn't load this page. Please try again.", color = MaterialTheme.colorScheme.onErrorContainer)
                OutlinedButton(onClick = retry, modifier = Modifier.heightIn(min = 48.dp)) { Text("Retry") }
            }
        }
        Phase.OFFLINE -> Text("You're offline. Local settings and information are still available.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        else -> Unit
    }
}

@Composable
fun ProfileChip(text: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp)) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun UnlockPill(label: String, unlocked: Boolean = false) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            VojoIcon(if (unlocked) R.drawable.vojo_check else R.drawable.vojo_lock,
                modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun ProfileHero(profile: ProfileCard?, unlock: String, onPlay: () -> Unit) {
    val dark = MaterialTheme.colorScheme.background.luminance() < .5f
    val gradient = if (dark) listOf(Color(0xFF172B3D),Color(0xFF1A354A)) else listOf(Color(0xFFE3EDF7),Color(0xFFD6E8F3))
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brush.horizontalGradient(gradient)).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("SELECTED PROFILE", style = MaterialTheme.typography.labelMedium)
                Text(profile?.name ?: "Your next adventure", style = MaterialTheme.typography.headlineSmall)
            }
            if (LocalDensity.current.fontScale <= 1.3f) CoverArt(Modifier.width(112.dp).height(104.dp))
        }
        if (profile != null) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileChip(profile.version); ProfileChip(profile.loader); ProfileChip(profile.ram)
            }
            Text("Renderer: Auto", style = MaterialTheme.typography.bodySmall)
        } else Text("Choose a profile to set your version, loader and memory.", style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onPlay, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp), shape = RoundedCornerShape(12.dp)) {
            VojoIcon(R.drawable.vojo_play, tint = MaterialTheme.colorScheme.onPrimary)
            Spacer(Modifier.width(10.dp))
            Text(if (profile == null) "Choose a profile" else "Play", style = MaterialTheme.typography.titleMedium)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            UnlockPill(unlock, unlock.startsWith("Unlocked"))
            Text("Unlock options arrive in Milestone 9", Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun SectionTitle(text: String) { Text(text, style = MaterialTheme.typography.titleMedium) }

@Composable
fun InfoCard(title: String, body: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionTitle(title)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
