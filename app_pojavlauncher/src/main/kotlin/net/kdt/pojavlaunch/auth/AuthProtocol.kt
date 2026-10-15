package net.kdt.pojavlaunch.auth

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom

enum class AuthProblem(val message: String) {
    NOT_CONFIGURED("Microsoft sign-in isn't configured in this build. The developer must supply Vojo's own client ID."),
    BAD_REGISTRATION("Vojo's Microsoft registration settings need attention. Ask the developer to check the client ID, account type and redirect URI."),
    APPROVAL_PENDING("Vojo is waiting for Mojang approval. Minecraft Services has not approved this app registration yet. Your Microsoft account may be fine. Please try again after Vojo's developer receives approval."),
    CANCELLED("Sign-in was cancelled. Your saved accounts have not changed."),
    CONSENT_DENIED("Microsoft sign-in permission was declined. Sign in again if you want to continue."),
    INVALID_CALLBACK("The sign-in response could not be verified. Start a new sign-in from Vojo."),
    EXPIRED_FLOW("This sign-in request expired. Start a new sign-in from Vojo."),
    NO_XBOX_PROFILE("This Microsoft account needs an Xbox profile. Create one on xbox.com, then try again."),
    CHILD_APPROVAL("This child account needs family approval or an Xbox family profile. Ask a parent or family organizer to review its permissions."),
    REGION_UNAVAILABLE("Xbox sign-in isn't available for this account's region."),
    ADULT_VERIFICATION("Xbox requires age or adult verification for this account. Complete it with Microsoft, then try again."),
    ACCOUNT_RESTRICTED("Xbox account or family restrictions are preventing sign-in. Review the account's Microsoft and Xbox settings with the family organizer if applicable."),
    NOT_OWNED("Java Edition ownership could not be verified for this account. Sign in with the account that owns Java Edition or has an eligible active entitlement."),
    NO_JAVA_PROFILE("This account has no Java Edition profile yet. Set up your Java profile through Minecraft's official website, then try again."),
    REAUTH_REQUIRED("Your saved sign-in is no longer valid. Sign in with Microsoft again."),
    OFFLINE("Couldn't connect to Microsoft or Minecraft Services. Check your internet connection and try again. Playing requires an online ownership check."),
    SERVICE_UNAVAILABLE("The sign-in service is unavailable or rejected this request. Please try again later."),
    RATE_LIMITED("Too many sign-in requests. Please wait a little before trying again."),
    INVALID_RESPONSE("The sign-in service sent an unexpected response. Please try again later."),
    STORAGE("Saved sign-ins could not be opened or updated securely. Retry, or reset local sign-ins if the problem continues."),
    ACCOUNT_LIMIT("Vojo can save up to 20 accounts. Remove a saved account before adding another."),
    NO_BROWSER("No browser could open Microsoft sign-in. Install or enable a browser and try again.")
}

// Never retain server bodies, tokens, callback URIs or exception causes in errors.
class AuthFailure(val problem: AuthProblem) : Exception(problem.message)
enum class AuthService { MICROSOFT, XBOX, XSTS, MINECRAFT, ENTITLEMENTS, PROFILE }

class PendingSignIn(val verifier: String, val state: String, val startedElapsedMs: Long) {
    override fun toString() = "PendingSignIn(<redacted>)"
}
class MicrosoftTokens(val access: String, val refresh: String) {
    override fun toString() = "MicrosoftTokens(<redacted>)"
}
class GameToken(val access: String, val expiresAtMs: Long) {
    override fun toString() = "GameToken(<redacted>)"
}
data class JavaProfile(val id: String, val name: String)

object AuthProtocol {
    const val SCOPES = "XboxLive.signin offline_access"
    const val AUTHORIZE = "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize"
    const val TOKEN = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token"
    const val XBOX = "https://user.auth.xboxlive.com/user/authenticate"
    const val XSTS = "https://xsts.auth.xboxlive.com/xsts/authorize"
    const val MINECRAFT = "https://api.minecraftservices.com/authentication/login_with_xbox"
    const val ENTITLEMENTS = "https://api.minecraftservices.com/entitlements/mcstore"
    const val PROFILE = "https://api.minecraftservices.com/minecraft/profile"
    private val random = SecureRandom()
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

    // java.util.Base64 is API 26; use a small RFC 4648 URL-safe encoder on API 21.
    fun base64Url(bytes: ByteArray): String = buildString {
        var bits = 0; var accumulator = 0
        for (byte in bytes) {
            accumulator = (accumulator shl 8) or (byte.toInt() and 255); bits += 8
            while (bits >= 6) { bits -= 6; append(ALPHABET[(accumulator ushr bits) and 63]) }
        }
        if (bits > 0) append(ALPHABET[(accumulator shl (6 - bits)) and 63])
    }
    fun pending(elapsedMs: Long) = PendingSignIn(base64Url(ByteArray(32).also(random::nextBytes)),
        base64Url(ByteArray(32).also(random::nextBytes)), elapsedMs)
    fun challenge(verifier: String) = base64Url(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))
    fun form(fields: Map<String, String>) = fields.entries.joinToString("&") { encode(it.key) + "=" + encode(it.value) }
    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")
    fun authorizationUrl(clientId: String, redirect: String, pending: PendingSignIn): String {
        if (!clientId.matches(Regex("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}"))) throw AuthFailure(AuthProblem.NOT_CONFIGURED)
        return AUTHORIZE + "?" + form(linkedMapOf("client_id" to clientId, "redirect_uri" to redirect,
            "response_type" to "code", "response_mode" to "query", "scope" to SCOPES,
            "code_challenge" to challenge(pending.verifier), "code_challenge_method" to "S256",
            "state" to pending.state, "prompt" to "select_account"))
    }
    fun callbackCode(callback: String, redirect: String, pending: PendingSignIn, elapsedMs: Long): String {
        if (elapsedMs < pending.startedElapsedMs || elapsedMs - pending.startedElapsedMs > 600_000) throw AuthFailure(AuthProblem.EXPIRED_FLOW)
        try {
            if (callback.length > 16_384) throw AuthFailure(AuthProblem.INVALID_CALLBACK)
            val uri = URI(callback); val expected = URI(redirect)
            if (uri.scheme != expected.scheme || uri.rawAuthority != expected.rawAuthority || uri.rawPath != expected.rawPath || uri.rawFragment != null)
                throw AuthFailure(AuthProblem.INVALID_CALLBACK)
            val fields = linkedMapOf<String,String>()
            val pairs = uri.rawQuery?.split('&') ?: throw AuthFailure(AuthProblem.INVALID_CALLBACK)
            if (pairs.size > 12) throw AuthFailure(AuthProblem.INVALID_CALLBACK)
            for (pair in pairs) {
                val parts = pair.split('=', limit = 2)
                if (parts.size != 2) throw AuthFailure(AuthProblem.INVALID_CALLBACK)
                val key = URLDecoder.decode(parts[0], "UTF-8")
                if (fields.put(key, URLDecoder.decode(parts[1], "UTF-8")) != null) throw AuthFailure(AuthProblem.INVALID_CALLBACK)
            }
            val receivedState = fields["state"] ?: throw AuthFailure(AuthProblem.INVALID_CALLBACK)
            if (!MessageDigest.isEqual(receivedState.toByteArray(Charsets.UTF_8), pending.state.toByteArray(Charsets.UTF_8)))
                throw AuthFailure(AuthProblem.INVALID_CALLBACK)
            if (fields.containsKey("error")) throw AuthFailure(if (fields["error"] == "access_denied") AuthProblem.CONSENT_DENIED else AuthProblem.BAD_REGISTRATION)
            return fields["code"]?.takeIf { it.isNotBlank() && it.length <= 8192 } ?: throw AuthFailure(AuthProblem.INVALID_CALLBACK)
        } catch (failure: AuthFailure) { throw failure }
        catch (_: Exception) { throw AuthFailure(AuthProblem.INVALID_CALLBACK) }
    }
    fun json(body: String): JsonObject = try {
        if (body.length > 131_072) throw AuthFailure(AuthProblem.INVALID_RESPONSE)
        var depth = 0; var quoted = false; var escaped = false
        for (char in body) {
            if (quoted) { if (escaped) escaped = false else if (char == '\\') escaped = true else if (char == '"') quoted = false }
            else when (char) {
                '"' -> quoted = true
                '{', '[' -> { depth++; if (depth > 32) throw AuthFailure(AuthProblem.INVALID_RESPONSE) }
                '}', ']' -> { depth--; if (depth < 0) throw AuthFailure(AuthProblem.INVALID_RESPONSE) }
            }
        }
        val element = JsonParser.parseString(body)
        if (!element.isJsonObject) throw AuthFailure(AuthProblem.INVALID_RESPONSE)
        element.asJsonObject
    } catch (_: Exception) { throw AuthFailure(AuthProblem.INVALID_RESPONSE) }
    fun string(json: JsonObject, key: String): String = try {
        val value = json.get(key)
        if (!value.isJsonPrimitive || !value.asJsonPrimitive.isString) throw AuthFailure(AuthProblem.INVALID_RESPONSE)
        value.asString.takeIf { it.isNotBlank() } ?: throw AuthFailure(AuthProblem.INVALID_RESPONSE)
    } catch (_: Exception) { throw AuthFailure(AuthProblem.INVALID_RESPONSE) }
    fun integer(json: JsonObject, key: String): Long = try {
        val value = json.get(key)
        if (!value.isJsonPrimitive || !value.asJsonPrimitive.isNumber || !value.asString.matches(Regex("[0-9]+"))) throw AuthFailure(AuthProblem.INVALID_RESPONSE)
        value.asString.toLong()
    } catch (_: Exception) { throw AuthFailure(AuthProblem.INVALID_RESPONSE) }
    fun microsoft(body: String, previousRefresh: String? = null): MicrosoftTokens {
        val json = json(body)
        val refresh = if (json.has("refresh_token")) string(json, "refresh_token") else previousRefresh ?: throw AuthFailure(AuthProblem.INVALID_RESPONSE)
        return MicrosoftTokens(string(json, "access_token"), refresh)
    }
    fun gameToken(body: String, nowMs: Long): GameToken {
        val json = json(body); val seconds = integer(json, "expires_in")
        if (seconds !in 1..604_800L || nowMs < 0 || nowMs > Long.MAX_VALUE - seconds * 1000) throw AuthFailure(AuthProblem.INVALID_RESPONSE)
        return GameToken(string(json, "access_token"), nowMs + seconds * 1000)
    }
    fun usable(expiresAtMs: Long, nowMs: Long): Boolean = nowMs >= 0 && expiresAtMs > nowMs && expiresAtMs - nowMs in 60_001..604_800_000L
    fun requireOwnership(body: String) {
        val json = json(body)
        val items = json.get("items")
        if (items == null || !items.isJsonArray) throw AuthFailure(AuthProblem.INVALID_RESPONSE)
        val owned = items.asJsonArray.any { item ->
            item.isJsonObject && item.asJsonObject.get("name")?.let { name ->
                name.isJsonPrimitive && name.asJsonPrimitive.isString && name.asString in setOf("game_minecraft", "product_minecraft")
            } == true
        }
        if (!owned) throw AuthFailure(AuthProblem.NOT_OWNED)
    }
    fun profile(body: String): JavaProfile {
        val json = json(body); val id = string(json, "id"); val name = string(json, "name")
        if (!id.matches(Regex("[0-9a-fA-F]{32}")) || !name.matches(Regex("[A-Za-z0-9_]{1,16}"))) throw AuthFailure(AuthProblem.INVALID_RESPONSE)
        return JavaProfile(id.lowercase(), name)
    }
    fun failure(service: AuthService, status: Int, body: String): AuthFailure {
        val json = try { json(body) } catch (_: AuthFailure) { JsonObject() }
        fun text(key: String) = try { string(json, key) } catch (_: AuthFailure) { "" }
        if (service == AuthService.MINECRAFT && body.contains("Invalid app registration", ignoreCase = true)) return AuthFailure(AuthProblem.APPROVAL_PENDING)
        if (service == AuthService.XSTS) {
            val xerr = try { integer(json, "XErr") } catch (_: AuthFailure) { -1 }
            when (xerr) {
                2148916233L -> return AuthFailure(AuthProblem.NO_XBOX_PROFILE)
                2148916235L -> return AuthFailure(AuthProblem.REGION_UNAVAILABLE)
                2148916236L -> return AuthFailure(AuthProblem.ADULT_VERIFICATION)
                2148916237L -> return AuthFailure(AuthProblem.ACCOUNT_RESTRICTED)
                2148916238L -> return AuthFailure(AuthProblem.CHILD_APPROVAL)
            }
        }
        if (service == AuthService.MICROSOFT) when (text("error")) {
            "invalid_grant", "interaction_required", "login_required" -> return AuthFailure(AuthProblem.REAUTH_REQUIRED)
            "invalid_client", "unauthorized_client", "invalid_scope", "invalid_request" -> return AuthFailure(AuthProblem.BAD_REGISTRATION)
            "access_denied" -> return AuthFailure(AuthProblem.CONSENT_DENIED)
        }
        return AuthFailure(when {
            status == 429 -> AuthProblem.RATE_LIMITED
            service == AuthService.PROFILE && status == 404 -> AuthProblem.NO_JAVA_PROFILE
            status == 401 -> AuthProblem.REAUTH_REQUIRED
            else -> AuthProblem.SERVICE_UNAVAILABLE
        })
    }
}
