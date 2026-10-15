package net.kdt.pojavlaunch.ui.vojo

import android.content.res.Configuration
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "Glacier dark", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 412, heightDp = 915)
@Preview(name = "Glacier light", uiMode = Configuration.UI_MODE_NIGHT_NO, widthDp = 412, heightDp = 915)
@Target(AnnotationTarget.FUNCTION)
annotation class VojoPreviews

private val sample = ProfileCard("Northern trails", "1.21", "Fabric", "3 GB RAM")
private val home = HomeState(Phase.CONTENT, sample, listOf(sample, ProfileCard("Quiet horizons", "1.20.1", "Vanilla", "2 GB RAM")))

@Composable private fun PreviewPage(content: @Composable () -> Unit) { VojoTheme(ThemeMode.SYSTEM) { Surface(content = content) } }
@VojoPreviews @Composable private fun HomePreview() { PreviewPage { HomeScreen(home, {}, {}, {}) } }
@VojoPreviews @Composable private fun HomeEmptyPreview() { PreviewPage { HomeScreen(HomeState(Phase.EMPTY), {}, {}, {}) } }
@VojoPreviews @Composable private fun HomeOfflinePreview() { PreviewPage { HomeScreen(HomeState(Phase.OFFLINE), {}, {}, {}) } }
@Preview(name = "Home 200%", fontScale = 2f, widthDp = 360, heightDp = 800)
@Preview(name = "Home landscape", widthDp = 800, heightDp = 360)
@Composable private fun HomeAccessiblePreview() { PreviewPage { HomeScreen(home, {}, {}, {}) } }
@VojoPreviews @Composable private fun FirstRunPreview() { PreviewPage { FirstRunScreen(false, null, {}, {}, {}) } }
@VojoPreviews @Composable private fun VersionsPreview() { PreviewPage { FeatureScreen("Versions", Phase.EMPTY, "Your library starts here", "No profiles yet. Version browsing and profile creation arrive in Milestone 4.") } }
@VojoPreviews @Composable private fun ModsPreview() { PreviewPage { FeatureScreen("Mods", Phase.EMPTY, "Make it yours", "Select a profile first. Mod management arrives in Milestone 8.", "View profiles") } }
@VojoPreviews @Composable private fun ControlsPreview() { PreviewPage { FeatureScreen("Controls", Phase.EMPTY, "Find your way to play", "Touch layouts and gamepad mapping arrive in Milestone 7.") } }
@VojoPreviews @Composable private fun SettingsPreview() { PreviewPage { SettingsScreen(ThemeMode.DARK, null, {}, {}, {}, {}) } }
@VojoPreviews @Composable private fun AboutPreview() { PreviewPage { AboutScreen({}, {}) } }
@VojoPreviews @Composable private fun DocumentPreview() { PreviewPage { DocumentScreen(legalDocuments.getValue("notice"), DocumentState(Phase.CONTENT, "Vojo derives from Amethyst and PojavLauncher. Existing notices remain intact. No relicensing."), {}, {}) } }
@VojoPreviews @Composable private fun LoadingPreview() { PreviewPage { Page("Getting ready") { StateNotice(Phase.LOADING) } } }
@VojoPreviews @Composable private fun ErrorPreview() { PreviewPage { Page("Couldn't load this page") { StateNotice(Phase.ERROR) } } }
@VojoPreviews @Composable private fun UnlockedPreview() { PreviewPage { HomeScreen(home.copy(unlockLabel = "Unlocked 23h 10m"), {}, {}, {}) } }
