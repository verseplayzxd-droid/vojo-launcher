package net.kdt.pojavlaunch.auth

import android.os.SystemClock
import java.util.concurrent.CancellationException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.kdt.pojavlaunch.BuildConfig

data class AccountSummary(val id: String, val name: String, val verifiedAtMs: Long)
data class AccountsState(val loading: Boolean = true, val accounts: List<AccountSummary> = emptyList(),
    val selectedId: String? = null, val busy: Boolean = false, val waitingForBrowser: Boolean = false,
    val stage: String = "", val problem: AuthProblem? = null, val storageBroken: Boolean = false)

class VerifiedLaunchSession internal constructor(val profileId: String, val name: String, val accessToken: String) {
    override fun toString() = "VerifiedLaunchSession(<redacted>)"
}

@Singleton
class AccountRepository @Inject constructor(private val vault: CredentialVault, private val client: AuthClient, private val http: AuthHttp) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    @Volatile private var contents: VaultContents? = null
    @Volatile private var work: Job? = null
    private val mutable = MutableStateFlow(AccountsState())
    val state: StateFlow<AccountsState> = mutable.asStateFlow()
    private val clientId get() = BuildConfig.MICROSOFT_CLIENT_ID
    private val redirect get() = BuildConfig.MICROSOFT_REDIRECT_URI
    private val initialized = CompletableDeferred<Unit>()

    init { scope.launch { load(); initialized.complete(Unit) } }

    fun reload() { scope.launch { initialized.await(); load() } }
    private suspend fun load() { mutex.withLock {
        mutable.value = AccountsState()
        try {
            if (clientId.isBlank()) { mutable.value = AccountsState(loading = false, problem = AuthProblem.NOT_CONFIGURED); return@withLock }
            val loaded = vault.read() ?: VaultContents(clientId)
            if (loaded.clientId != clientId) throw AuthFailure(AuthProblem.STORAGE)
            contents = loaded
            val pending = loaded.pending
            if (pending != null && (SystemClock.elapsedRealtime() < pending.startedElapsedMs || SystemClock.elapsedRealtime() - pending.startedElapsedMs > 600_000)) {
                save(loaded.updated(pending = null)); publish(AuthProblem.EXPIRED_FLOW)
            } else publish()
        } catch (_: Exception) { contents = null; mutable.value = AccountsState(loading = false, problem = AuthProblem.STORAGE, storageBroken = true) }
    } }

    suspend fun beginSignIn(): String = withContext(Dispatchers.IO) { initialized.await(); mutex.withLock {
        val current = current()
        if (work?.isActive == true) throw AuthFailure(AuthProblem.CANCELLED)
        val pending = AuthProtocol.pending(SystemClock.elapsedRealtime())
        val url = AuthProtocol.authorizationUrl(clientId, redirect, pending)
        save(current.updated(pending = pending)); publish()
        url
    } }

    fun browserFailed() { cancel(AuthProblem.NO_BROWSER) }
    fun acceptRedirect(uri: String) = operation {
        val current = current()
        val pending = current.pending ?: throw AuthFailure(AuthProblem.INVALID_CALLBACK)
        val code = AuthProtocol.callbackCode(uri, redirect, pending, SystemClock.elapsedRealtime())
        // Consume state durably before exchanging the code, preventing callback replay.
        save(current.updated(pending = null))
        stage("Completing Microsoft sign-in")
        val tokens = client.exchangeCode(clientId, redirect, code, pending.verifier)
        val (game, profile) = client.signIn(tokens, System::currentTimeMillis, progress())
        currentCoroutineContext().ensureActive()
        val account = StoredAccount(profile.id, profile.name, tokens.refresh, game.access, game.expiresAtMs, System.currentTimeMillis())
        val latest = current()
        if (latest.accounts.size >= 20 && latest.accounts.none { it.id == account.id }) throw AuthFailure(AuthProblem.ACCOUNT_LIMIT)
        save(latest.updated(accounts = latest.accounts.filterNot { it.id == account.id } + account, selectedId = account.id))
        publish()
    }

    fun select(id: String) = operation {
        val account = current().accounts.firstOrNull { it.id == id } ?: throw AuthFailure(AuthProblem.REAUTH_REQUIRED)
        val verified = verify(account)
        currentCoroutineContext().ensureActive()
        val latest = current()
        save(latest.updated(accounts = latest.accounts.map { if (it.id == id) verified else it }, selectedId = id))
        publish()
    }

    fun remove(id: String) = operation {
        val current = current(); val accounts = current.accounts.filterNot { it.id == id }
        save(current.updated(accounts = accounts, selectedId = if (current.selectedId == id) null else current.selectedId))
        publish()
    }

    fun reset() = operation {
        vault.reset()
        contents = if (clientId.isBlank()) null else VaultContents(clientId)
        publish(if (clientId.isBlank()) AuthProblem.NOT_CONFIGURED else null)
    }

    fun cancel(problem: AuthProblem = AuthProblem.CANCELLED) {
        if (!mutable.value.busy && !mutable.value.waitingForBrowser) return
        work?.cancel(); http.cancel()
        scope.launch { initialized.await(); mutex.withLock {
            try { contents?.let { save(it.updated(pending = null)) }; publish(problem) }
            catch (_: Exception) { publish(AuthProblem.STORAGE) }
        } }
    }

    fun display(problem: AuthProblem) { publish(problem) }

    // M5 must obtain this immediately before launch. Saved profile metadata is
    // never authorization; even an unexpired access token requires an online check.
    suspend fun requireLaunchSession(): VerifiedLaunchSession = withContext(Dispatchers.IO) { initialized.await(); mutex.withLock {
        val account = current().accounts.firstOrNull { it.id == current().selectedId } ?: throw AuthFailure(AuthProblem.REAUTH_REQUIRED)
        try {
            val verified = verify(account)
            currentCoroutineContext().ensureActive()
            val latest = current()
            save(latest.updated(accounts = latest.accounts.map { if (it.id == verified.id) verified else it }))
            publish()
            VerifiedLaunchSession(verified.id, verified.name, verified.minecraftToken)
        } catch (failure: AuthFailure) { publish(failure.problem); throw failure }
    } }

    private suspend fun verify(account: StoredAccount): StoredAccount {
        var game = GameToken(account.minecraftToken, account.expiresAtMs)
        var refresh = account.refreshToken
        suspend fun refreshGame(): Pair<GameToken, JavaProfile> {
            stage("Refreshing Microsoft sign-in")
            val tokens = client.refresh(clientId, refresh)
            currentCoroutineContext().ensureActive()
            refresh = tokens.refresh
            // Persist refresh-token rotation before downstream services can fail.
            val latest = current()
            save(latest.updated(accounts = latest.accounts.map {
                if (it.id == account.id) StoredAccount(it.id, it.name, refresh, it.minecraftToken, it.expiresAtMs, it.verifiedAtMs) else it
            }))
            return client.signIn(tokens, System::currentTimeMillis, progress())
        }
        val refreshed = !AuthProtocol.usable(game.expiresAtMs, System.currentTimeMillis())
        val profile = if (refreshed) {
            val result = refreshGame(); game = result.first; result.second
        } else try { client.verify(game.access, progress()) }
        catch (failure: AuthFailure) {
            if (!refreshed && failure.problem == AuthProblem.REAUTH_REQUIRED) {
                val result = refreshGame(); game = result.first; result.second
            } else throw failure
        }
        if (profile.id != account.id) throw AuthFailure(AuthProblem.INVALID_RESPONSE)
        return StoredAccount(profile.id, profile.name, refresh, game.access, game.expiresAtMs, System.currentTimeMillis())
    }

    private fun operation(action: suspend () -> Unit) {
        if (work?.isActive == true) return
        work = scope.launch { initialized.await(); mutex.withLock {
            try { mutable.value = mutable.value.copy(loading = false, busy = true, problem = null); action() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: AuthFailure) { publish(failure.problem) }
            catch (_: Exception) { publish(AuthProblem.SERVICE_UNAVAILABLE) }
        } }
    }
    private suspend fun progress(): (String) -> Unit {
        val context = currentCoroutineContext()
        return { message -> context.ensureActive(); stage(message) }
    }
    private fun stage(message: String) { mutable.value = mutable.value.copy(busy = true, waitingForBrowser = false, stage = message, problem = null) }
    private fun current(): VaultContents {
        if (clientId.isBlank()) throw AuthFailure(AuthProblem.NOT_CONFIGURED)
        return contents ?: throw AuthFailure(AuthProblem.STORAGE)
    }
    private suspend fun save(next: VaultContents) {
        currentCoroutineContext().ensureActive()
        vault.write(next); contents = next
    }
    private fun publish(problem: AuthProblem? = null) {
        val current = contents
        mutable.value = AccountsState(loading = false,
            accounts = current?.accounts?.map { AccountSummary(it.id, it.name, it.verifiedAtMs) } ?: emptyList(),
            selectedId = current?.selectedId, waitingForBrowser = current?.pending != null,
            stage = if (current?.pending != null) "Finish sign-in in your browser, then return to Vojo." else "",
            problem = problem, storageBroken = problem == AuthProblem.STORAGE)
    }
}
