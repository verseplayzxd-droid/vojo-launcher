@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
package net.kdt.pojavlaunch.ui.vojo

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ThemeMode { DARK, LIGHT, SYSTEM }
enum class Phase { LOADING, CONTENT, EMPTY, OFFLINE, ERROR }
data class LauncherSettings(val theme: ThemeMode = ThemeMode.DARK, val welcomed: Boolean = false)
data class SettingsState(val phase: Phase = Phase.LOADING, val value: LauncherSettings = LauncherSettings())
data class ProfileCard(val name: String, val version: String, val loader: String, val ram: String)
data class HomeState(val phase: Phase = Phase.LOADING, val profile: ProfileCard? = null,
    val recent: List<ProfileCard> = emptyList(), val unlockLabel: String = "Locked")

private val Context.vojoPreferences by preferencesDataStore(name = "vojo_interface")

@Singleton
class LauncherSettingsRepository @Inject constructor(@ApplicationContext context: Context) {
    private val store = context.applicationContext.vojoPreferences
    private val themeKey = stringPreferencesKey("theme")
    private val welcomeKey = booleanPreferencesKey("welcome_complete")
    val state: Flow<SettingsState> = store.data.map { prefs ->
        val theme = ThemeMode.entries.firstOrNull { it.name == prefs[themeKey] } ?: ThemeMode.DARK
        SettingsState(Phase.CONTENT, LauncherSettings(theme, prefs[welcomeKey] ?: false))
    }.catch { error ->
        if (error is CancellationException) throw error
        emit(SettingsState(Phase.ERROR))
    }
    suspend fun setTheme(theme: ThemeMode) { store.edit { it[themeKey] = theme.name } }
    suspend fun welcome() { store.edit { it[welcomeKey] = true } }
}

@Singleton
class NetworkMonitor @Inject constructor(@ApplicationContext context: Context) {
    private val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    @Suppress("DEPRECATION")
    private fun connected(): Boolean = if (Build.VERSION.SDK_INT >= 23) {
        manager.getNetworkCapabilities(manager.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
    } else manager.activeNetworkInfo?.isConnected == true
    val online: StateFlow<Boolean> = kotlinx.coroutines.flow.callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(connected()) }
            override fun onLost(network: Network) { trySend(connected()) }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { trySend(connected()) }
        }
        manager.registerNetworkCallback(NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(), callback)
        trySend(connected())
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), false)
}

@HiltViewModel
class ShellViewModel @Inject constructor(private val settings: LauncherSettingsRepository, network: NetworkMonitor) : ViewModel() {
    private val reload = MutableStateFlow(0)
    val state = reload.flatMapLatest { settings.state }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsState())
    val online = network.online
    fun retry() { reload.value++ }
}

@HiltViewModel
class FirstRunViewModel @Inject constructor(private val settings: LauncherSettingsRepository) : ViewModel() {
    val error = MutableStateFlow<String?>(null)
    val saving = MutableStateFlow(false)
    fun continueToHome() { viewModelScope.launch {
        saving.value = true
        try { settings.welcome() } catch (e: Exception) {
            if (e is CancellationException) throw e
            error.value = "Couldn't save your choice. Please try again."
        } finally { saving.value = false }
    } }
}

@HiltViewModel
class HomeViewModel @Inject constructor(network: NetworkMonitor) : ViewModel() {
    // Profiles are connected in M4. Never substitute preview data for installed profiles.
    val state = network.online.map { HomeState(if (it) Phase.EMPTY else Phase.OFFLINE) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeState())
}

open class FeatureViewModel(network: NetworkMonitor) : ViewModel() {
    val phase = network.online.map { if (it) Phase.EMPTY else Phase.OFFLINE }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Phase.LOADING)
}
@HiltViewModel class VersionsViewModel @Inject constructor(network: NetworkMonitor) : FeatureViewModel(network)
@HiltViewModel class ModsViewModel @Inject constructor(network: NetworkMonitor) : FeatureViewModel(network)
@HiltViewModel class ControlsViewModel @Inject constructor(network: NetworkMonitor) : FeatureViewModel(network)
@HiltViewModel class AboutViewModel @Inject constructor() : ViewModel()

@HiltViewModel
class SettingsViewModel @Inject constructor(private val settings: LauncherSettingsRepository) : ViewModel() {
    val error = MutableStateFlow<String?>(null)
    fun theme(mode: ThemeMode) { viewModelScope.launch {
        try { settings.setTheme(mode); error.value = null } catch (e: Exception) {
            if (e is CancellationException) throw e
            error.value = "Couldn't save the appearance setting. Please try again."
        }
    } }
}

data class DocumentState(val phase: Phase = Phase.LOADING, val text: String = "")
@HiltViewModel
class DocumentViewModel @Inject constructor(@param:ApplicationContext private val context: Context) : ViewModel() {
    val state = MutableStateFlow(DocumentState())
    private var readJob: Job? = null
    fun read(asset: String) {
        readJob?.cancel()
        readJob = viewModelScope.launch {
        state.value = DocumentState()
        state.value = try {
            val text = withContext(Dispatchers.IO) { context.assets.open(asset).bufferedReader().use { it.readText() } }
            DocumentState(Phase.CONTENT, text)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            DocumentState(Phase.ERROR, "Couldn't open this document. Please try again.")
        }
    } }
}
