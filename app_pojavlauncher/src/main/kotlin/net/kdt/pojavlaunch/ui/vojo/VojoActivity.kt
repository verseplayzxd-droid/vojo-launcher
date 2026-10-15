package net.kdt.pojavlaunch.ui.vojo

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import net.kdt.pojavlaunch.R

@AndroidEntryPoint
class VojoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VojoShell() }
    }
}

private enum class Destination(val label: String, val icon: Int) {
    HOME("Home", R.drawable.vojo_home), VERSIONS("Versions", R.drawable.vojo_versions),
    MODS("Mods", R.drawable.vojo_mods), CONTROLS("Controls", R.drawable.vojo_controls), SETTINGS("Settings", R.drawable.vojo_settings)
}

@Composable
private fun VojoShell(shell: ShellViewModel = viewModel()) {
    val settings by shell.state.collectAsStateWithLifecycle()
    var destination by rememberSaveable { mutableStateOf(Destination.HOME.name) }
    var page by rememberSaveable { mutableStateOf("") }
    var dialog by rememberSaveable { mutableStateOf("") }
    val configuration = LocalConfiguration.current
    val rail = configuration.screenWidthDp >= 600 || configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current
    fun openDocument(id: String) { page = id }
    BackHandler(page.isNotEmpty() || destination != Destination.HOME.name) {
        if (page.isNotEmpty()) page = "" else destination = Destination.HOME.name
    }
    VojoTheme(settings.value.theme) {
        val reducedMotion = rememberReducedMotion()
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (page.isNotEmpty()) IconButton({ page = "" }, Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                        VojoIcon(R.drawable.vojo_back, "Back")
                    } else VojoLogo()
                    Text("Vojo", Modifier.weight(1f).padding(start = 8.dp), style = MaterialTheme.typography.titleLarge)
                    IconButton({ dialog = "Microsoft sign-in and ownership checks arrive in Milestone 3. No account is connected in this interface preview." },
                        Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) { VojoIcon(R.drawable.vojo_account, "Account") }
                }
                Row(Modifier.weight(1f)) {
                    if (settings.value.welcomed && rail && page.isEmpty()) NavigationItems(destination, true) { destination = it }
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        when {
                            settings.phase != Phase.CONTENT -> Page("Getting ready") { StateNotice(settings.phase, shell::retry) }
                            page in legalDocuments -> {
                                val vm: DocumentViewModel = viewModel()
                                val doc = legalDocuments.getValue(page)
                                val state by vm.state.collectAsStateWithLifecycle()
                                LaunchedEffect(page) { vm.read(doc.asset) }
                                DocumentScreen(doc, state, { vm.read(doc.asset) }, ::openDocument)
                            }
                            !settings.value.welcomed -> {
                                val vm: FirstRunViewModel = viewModel()
                                val error by vm.error.collectAsStateWithLifecycle()
                                val saving by vm.saving.collectAsStateWithLifecycle()
                                FirstRunScreen(saving, error, vm::continueToHome, { openDocument("privacy") }, { openDocument("notice") })
                            }
                            page == "about" -> {
                                val vm: AboutViewModel = viewModel()
                                AboutScreen({
                                    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/verseplayzxd-droid/vojo-launcher"))) }
                                    catch (_: android.content.ActivityNotFoundException) { dialog = "No browser is installed. Source: github.com/verseplayzxd-droid/vojo-launcher" }
                                }, ::openDocument)
                            }
                            else -> Crossfade(destination, animationSpec = tween(if (reducedMotion) 0 else 220), label = "Page transition") { selected ->
                                when (Destination.valueOf(selected)) {
                                    Destination.HOME -> {
                                        val vm: HomeViewModel = viewModel()
                                        val state by vm.state.collectAsStateWithLifecycle()
                                        HomeScreen(state, { destination = Destination.VERSIONS.name }, { destination = Destination.CONTROLS.name },
                                            { dialog = "No downloads yet. Verified downloads and progress management arrive in Milestone 4." })
                                    }
                                    Destination.VERSIONS -> {
                                        val vm: VersionsViewModel = viewModel()
                                        val phase by vm.phase.collectAsStateWithLifecycle()
                                        FeatureScreen("Versions", phase, "Your library starts here", "No profiles are available in this interface preview. Version browsing, verified downloads and profile creation arrive in Milestone 4.")
                                    }
                                    Destination.MODS -> {
                                        val vm: ModsViewModel = viewModel()
                                        val phase by vm.phase.collectAsStateWithLifecycle()
                                        FeatureScreen("Mods", phase, "Make it yours", "Select a version profile first. Modrinth browsing, loader installation and compatibility checks arrive in Milestone 8.", "View profiles") { destination = Destination.VERSIONS.name }
                                    }
                                    Destination.CONTROLS -> {
                                        val vm: ControlsViewModel = viewModel()
                                        val phase by vm.phase.collectAsStateWithLifecycle()
                                        FeatureScreen("Controls", phase, "Find your way to play", "Touch layouts, JSON import and export, and gamepad mapping arrive in Milestone 7. This preview does not change your inherited control files.")
                                    }
                                    Destination.SETTINGS -> {
                                        val vm: SettingsViewModel = viewModel()
                                        val error by vm.error.collectAsStateWithLifecycle()
                                        SettingsScreen(settings.value.theme, error, vm::theme, { page = "about" }, { openDocument("privacy") }, { openDocument("notice") })
                                    }
                                }
                            }
                        }
                    }
                }
                if (settings.value.welcomed && !rail && page.isEmpty()) NavigationItems(destination, false) { destination = it }
            }
            if (dialog.isNotEmpty()) AlertDialog(onDismissRequest = { dialog = "" }, title = { Text("Interface preview") },
                text = { Text(dialog) }, confirmButton = { TextButton({ dialog = "" }, Modifier.heightIn(min = 48.dp)) { Text("Got it") } })
        }
    }
}

@Composable
private fun NavigationItems(selected: String, rail: Boolean, choose: (String) -> Unit) {
    val configuration = LocalConfiguration.current
    val largeText = LocalDensity.current.fontScale > 1.3f
    val width = if (rail || largeText) 120.dp else (configuration.screenWidthDp / 5f).dp
    val item: @Composable (Destination) -> Unit = { destination ->
        Surface(color = if (selected == destination.name) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.small) {
            Column(Modifier.width(width).heightIn(min = 64.dp).selectable(selected == destination.name,
                role = Role.Tab, onClick = { choose(destination.name) }).semantics(mergeDescendants = true) {}
                .padding(horizontal = 4.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                VojoIcon(destination.icon)
                Text(destination.label, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
    Surface(color = MaterialTheme.colorScheme.surface) {
        if (rail) Column(Modifier.fillMaxHeight().verticalScroll(rememberScrollState()).padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) { Destination.entries.forEach { item(it) } }
        else Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) { Destination.entries.forEach { item(it) } }
    }
}
