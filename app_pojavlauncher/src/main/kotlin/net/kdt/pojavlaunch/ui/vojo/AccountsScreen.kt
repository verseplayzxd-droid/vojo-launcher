package net.kdt.pojavlaunch.ui.vojo

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import net.kdt.pojavlaunch.auth.*

@HiltViewModel
class AccountsViewModel @Inject constructor(private val repository: AccountRepository, monitor: NetworkMonitor) : ViewModel() {
    val state = repository.state
    val online = monitor.online
    val browser = MutableStateFlow<String?>(null)
    private var starting = false
    fun signIn() {
        if (starting) return
        starting = true
        viewModelScope.launch {
            try { browser.value = repository.beginSignIn() }
            catch (failure: AuthFailure) { repository.display(failure.problem) }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { repository.display(AuthProblem.STORAGE) }
            finally { starting = false }
        }
    }
    fun opened() { browser.value = null }
    fun browserFailed() { browser.value = null; repository.browserFailed() }
    fun cancel() = repository.cancel()
    fun select(id: String) = repository.select(id)
    fun remove(id: String) = repository.remove(id)
    fun reset() = repository.reset()
    fun retry() = repository.reload()
}

@Composable
fun AccountsRoute(vm: AccountsViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val online by vm.online.collectAsStateWithLifecycle()
    val browser by vm.browser.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(browser) {
        val address = browser ?: return@LaunchedEffect
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(address)).addCategory(Intent.CATEGORY_BROWSABLE))
            vm.opened()
        } catch (_: ActivityNotFoundException) { vm.browserFailed() }
        catch (_: SecurityException) { vm.browserFailed() }
    }
    AccountsScreen(state, online, vm::signIn, vm::cancel, vm::select, vm::remove, vm::retry, vm::reset)
}

@Composable
fun AccountsScreen(state: AccountsState, online: Boolean, signIn: () -> Unit, cancel: () -> Unit,
    select: (String) -> Unit, remove: (String) -> Unit, retry: () -> Unit, reset: () -> Unit) {
    var removal by remember { mutableStateOf<AccountSummary?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    Page("Accounts") {
        Text("Sign in with the Microsoft account that owns Java Edition. Vojo checks your entitlement and Java profile before saving the account.")
        Text("Your password stays in Microsoft's browser sign-in. Vojo stores tokens securely on this device.", style = MaterialTheme.typography.bodySmall)
        if (!online) StateNotice(Phase.OFFLINE)
        if (state.loading) StateNotice(Phase.LOADING)
        state.problem?.let {
            InfoCard(if (it == AuthProblem.APPROVAL_PENDING) "Waiting for app approval" else "Sign-in information", it.message)
        }
        if (state.busy || state.waitingForBrowser) {
            Text(state.stage, style = MaterialTheme.typography.titleMedium)
            if (state.busy && !rememberReducedMotion()) LinearProgressIndicator(Modifier.fillMaxWidth())
            OutlinedButton(cancel, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Cancel sign-in or account check") }
        } else {
            Button(signIn, Modifier.fillMaxWidth().heightIn(min = 56.dp),
                enabled = !state.loading && !state.storageBroken && state.problem != AuthProblem.NOT_CONFIGURED) {
                Text(if (state.accounts.isEmpty()) "Sign in with Microsoft" else "Add Microsoft account")
            }
        }
        if (state.storageBroken) {
            OutlinedButton(retry, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Retry secure storage") }
            OutlinedButton({ confirmReset = true }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Reset saved sign-ins") }
        }
        SectionTitle("Saved accounts")
        if (!state.loading && state.accounts.isEmpty()) InfoCard("No saved accounts", "Only accounts with verified Java Edition access appear here. Existing legacy account files are not imported.")
        state.accounts.forEach { account ->
            val selected = state.selectedId == account.id
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
                Column(Modifier.fillMaxWidth().padding(16.dp).semantics {
                    stateDescription = if (selected) "Selected account" else "Not selected"
                }, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                            Text(account.name.take(1).uppercase(), Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                        }
                        Text(account.name, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    }
                    Text(if (selected) "Selected · Previously verified Java Edition" else "Previously verified Java Edition")
                    Text("Playing requires a fresh online ownership check. Removing an account clears its saved credentials from Vojo, not your Microsoft account.", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton({ select(account.id) }, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = !state.busy && !state.waitingForBrowser && !state.storageBroken) {
                        Text(if (selected) "Verify ${account.name} again" else "Verify and select ${account.name}")
                    }
                    OutlinedButton({ removal = account }, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = !state.busy && !state.waitingForBrowser && !state.storageBroken) { Text("Remove ${account.name}") }
                }
            }
        }
    }
    removal?.let { account -> AlertDialog(onDismissRequest = { removal = null }, title = { Text("Remove ${account.name}?") },
        text = { Text("This removes this account's saved sign-in from Vojo. It does not delete your Microsoft account or sign you out of your browser.") },
        confirmButton = { TextButton({ removal = null; remove(account.id) }, Modifier.heightIn(min = 48.dp)) { Text("Remove account") } },
        dismissButton = { TextButton({ removal = null }, Modifier.heightIn(min = 48.dp)) { Text("Keep account") } }) }
    if (confirmReset) AlertDialog(onDismissRequest = { confirmReset = false }, title = { Text("Reset saved sign-ins?") },
        text = { Text("This removes all Vojo saved sign-ins and their encryption keys on this device. You will need to sign in again.") },
        confirmButton = { TextButton({ confirmReset = false; reset() }, Modifier.heightIn(min = 48.dp)) { Text("Reset local sign-ins") } },
        dismissButton = { TextButton({ confirmReset = false }, Modifier.heightIn(min = 48.dp)) { Text("Cancel") } })
}

@VojoPreviews @Composable private fun AccountsEmptyPreview() { VojoTheme(ThemeMode.SYSTEM) { Surface { AccountsScreen(AccountsState(loading = false), true, {}, {}, {}, {}, {}, {}) } } }
@VojoPreviews @Composable private fun AccountsSavedPreview() { VojoTheme(ThemeMode.SYSTEM) { Surface { AccountsScreen(AccountsState(loading = false, accounts = listOf(AccountSummary("1", "ExamplePlayer", 1)), selectedId = "1"), true, {}, {}, {}, {}, {}, {}) } } }
@VojoPreviews @Composable private fun AccountsApprovalPreview() { VojoTheme(ThemeMode.SYSTEM) { Surface { AccountsScreen(AccountsState(loading = false, problem = AuthProblem.APPROVAL_PENDING), true, {}, {}, {}, {}, {}, {}) } } }
@VojoPreviews @Composable private fun AccountsLoadingPreview() { VojoTheme(ThemeMode.SYSTEM) { Surface { AccountsScreen(AccountsState(), true, {}, {}, {}, {}, {}, {}) } } }
@VojoPreviews @Composable private fun AccountsOfflinePreview() { VojoTheme(ThemeMode.SYSTEM) { Surface { AccountsScreen(AccountsState(loading = false, problem = AuthProblem.OFFLINE), false, {}, {}, {}, {}, {}, {}) } } }
