@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package net.kdt.pojavlaunch.ui.vojo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import net.kdt.pojavlaunch.BuildConfig
import net.kdt.pojavlaunch.R

@Composable
fun Page(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        content()
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun HomeScreen(state: HomeState, profiles: () -> Unit, controls: () -> Unit, downloads: () -> Unit) {
    Page("Make room for play.") {
        StateNotice(state.phase)
        ProfileHero(state.profile, state.unlockLabel, profiles)
        SectionTitle("Jump back in")
        if (state.recent.isEmpty()) InfoCard("No recent profiles yet", "Your recently played profiles will appear here after you create and launch one.")
        else state.recent.forEachIndexed { index, profile ->
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
                Row(Modifier.fillMaxWidth().clickable(onClickLabel = "Select ${profile.name}", onClick = profiles).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    CoverArt(Modifier.size(64.dp), alternate = index % 2 == 1)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionTitle(profile.name)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ProfileChip(profile.version); ProfileChip(profile.loader); ProfileChip(profile.ram)
                        }
                    }
                }
            }
        }
        SectionTitle("Quick actions")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Action("Profiles", R.drawable.vojo_versions, profiles)
            Action("Downloads", R.drawable.vojo_download, downloads)
            Action("Controls", R.drawable.vojo_controls, controls)
        }
        InfoCard("A little headroom helps", "Start with a modest memory allocation. Leaving RAM for Android can keep your game more responsive.")
    }
}

@Composable
private fun Action(label: String, icon: Int, onClick: () -> Unit) {
    OutlinedButton(onClick, Modifier.heightIn(min = 48.dp)) {
        VojoIcon(icon); Spacer(Modifier.width(8.dp)); Text(label)
    }
}

@Composable
fun FeatureScreen(title: String, phase: Phase, heading: String, explanation: String,
    action: String? = null, onAction: () -> Unit = {}) {
    Page(title) {
        StateNotice(phase)
        InfoCard(heading, explanation)
        if (action != null) OutlinedButton(onAction, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(action) }
    }
}

@Composable
fun FirstRunScreen(saving: Boolean, error: String?, continueAction: () -> Unit, privacy: () -> Unit, licenses: () -> Unit) {
    Page("Welcome to Vojo") {
        VojoLogo(Modifier.size(72.dp))
        InfoCard("Your game. Your adventure.", "You need your own copy of Java Edition to play")
        Text("This build previews the new launcher interface. Microsoft sign-in and ownership checks are coming in Milestone 3.")
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
        Button(continueAction, Modifier.fillMaxWidth().heightIn(min = 56.dp), enabled = !saving) {
            Text(if (saving) "Saving…" else "Continue")
        }
        OutlinedButton(privacy, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Privacy notice · draft") }
        OutlinedButton(licenses, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Open-source licenses") }
    }
}

@Composable
fun SettingsScreen(mode: ThemeMode, error: String?, change: (ThemeMode) -> Unit, about: () -> Unit,
    privacy: () -> Unit, licenses: () -> Unit) {
    Page("Settings") {
        SectionTitle("Appearance")
        Text("Choose how Vojo looks. Your choice stays on this device.")
        Column(Modifier.selectableGroup()) { ThemeMode.entries.forEach { choice ->
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp)
                .selectable(selected = mode == choice, role = Role.RadioButton, onClick = { change(choice) }).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = mode == choice, onClick = null)
                Spacer(Modifier.width(12.dp))
                Text(when (choice) { ThemeMode.DARK -> "Dark"; ThemeMode.LIGHT -> "Light"; ThemeMode.SYSTEM -> "Follow system" })
            }
        } }
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
        InfoCard("More settings are on their way", "Accounts arrive in M3. Storage, runtime, renderer and log tools will follow in their planned milestones.")
        OutlinedButton(about, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("About Vojo") }
        OutlinedButton(privacy, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Privacy notice · draft") }
        OutlinedButton(licenses, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Open-source licenses") }
    }
}

@Composable
fun AboutScreen(source: () -> Unit, document: (String) -> Unit) {
    Page("About Vojo") {
        VojoLogo()
        Text("Vojo Launcher", style = MaterialTheme.typography.titleLarge)
        Text("Version ${BuildConfig.VERSION_NAME}")
        Text("Not affiliated with Mojang or Microsoft")
        Text("Built on Amethyst and PojavLauncher. Thanks to their contributors and the authors of the open-source libraries included here.")
        OutlinedButton(source, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("View source on GitHub") }
        OutlinedButton({ document("notice") }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Open-source licenses") }
        OutlinedButton({ document("unresolved") }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Unresolved licenses · review before release") }
        OutlinedButton({ document("privacy") }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Privacy notice · draft") }
    }
}

data class LegalDocument(val title: String, val asset: String)
val legalDocuments = mapOf(
    "notice" to LegalDocument("Open-source notices", "licenses/vojo/NOTICE.txt"),
    "lgpl" to LegalDocument("GNU LGPL 3.0", "licenses/vojo/LGPL-3.0.txt"),
    "gpl" to LegalDocument("GNU GPL 3.0", "licenses/vojo/GPL-3.0.txt"),
    "apache" to LegalDocument("Apache License 2.0", "licenses/vojo/Apache-2.0.txt"),
    "unresolved" to LegalDocument("Unresolved licenses", "licenses/vojo/UNRESOLVED.txt"),
    "privacy" to LegalDocument("Privacy notice · draft", "licenses/vojo/PRIVACY-DRAFT.txt"),
    "angle" to LegalDocument("ANGLE notice", "licenses/ANGLE_LICENSE"),
    "font" to LegalDocument("Font notice", "font/LICENSE_OFL.txt")
)

@Composable
fun DocumentScreen(document: LegalDocument, state: DocumentState, retry: () -> Unit, open: (String) -> Unit) {
    Page(document.title) {
        StateNotice(state.phase, retry)
        if (state.phase == Phase.CONTENT) SelectionContainer { Text(state.text, style = MaterialTheme.typography.bodyMedium) }
        if (document == legalDocuments["notice"]) {
            legalDocuments.filterKeys { it != "notice" && it != "privacy" }.forEach { (id, item) ->
                OutlinedButton({ open(id) }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(item.title) }
            }
        }
    }
}
