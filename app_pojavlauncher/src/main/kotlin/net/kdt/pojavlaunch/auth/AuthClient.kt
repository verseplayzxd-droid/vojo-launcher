package net.kdt.pojavlaunch.auth

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import javax.inject.Inject

interface AuthTransport {
    fun exchange(service: AuthService, payload: String? = null, form: Boolean = false, bearer: String? = null): String
}

class AuthClient(private val transport: AuthTransport) {
    @Inject constructor(http: AuthHttp) : this(http as AuthTransport)

    fun exchangeCode(clientId: String, redirect: String, code: String, verifier: String): MicrosoftTokens =
        AuthProtocol.microsoft(transport.exchange(AuthService.MICROSOFT, AuthProtocol.form(linkedMapOf(
            "client_id" to clientId, "redirect_uri" to redirect, "code" to code, "code_verifier" to verifier,
            "grant_type" to "authorization_code", "scope" to AuthProtocol.SCOPES)), form = true))

    fun refresh(clientId: String, refresh: String): MicrosoftTokens =
        AuthProtocol.microsoft(transport.exchange(AuthService.MICROSOFT, AuthProtocol.form(linkedMapOf(
            "client_id" to clientId, "refresh_token" to refresh, "grant_type" to "refresh_token",
            "scope" to AuthProtocol.SCOPES)), form = true), refresh)

    fun signIn(tokens: MicrosoftTokens, nowMs: () -> Long, stage: (String) -> Unit): Pair<GameToken, JavaProfile> {
        stage("Signing in to Xbox Live")
        val xboxProperties = JsonObject().apply {
            addProperty("AuthMethod", "RPS"); addProperty("SiteName", "user.auth.xboxlive.com"); addProperty("RpsTicket", "d=${tokens.access}")
        }
        val xbox = AuthProtocol.json(transport.exchange(AuthService.XBOX, request(xboxProperties, "http://auth.xboxlive.com").toString()))
        val xboxToken = AuthProtocol.string(xbox, "Token")
        stage("Checking Xbox account permissions")
        val xstsProperties = JsonObject().apply {
            addProperty("SandboxId", "RETAIL"); add("UserTokens", JsonArray().apply { add(xboxToken) })
        }
        val xsts = AuthProtocol.json(transport.exchange(AuthService.XSTS, request(xstsProperties, "rp://api.minecraftservices.com/").toString()))
        val userHash = try {
            AuthProtocol.string(xsts.getAsJsonObject("DisplayClaims").getAsJsonArray("xui")[0].asJsonObject, "uhs")
        } catch (_: Exception) { throw AuthFailure(AuthProblem.INVALID_RESPONSE) }
        val xstsToken = AuthProtocol.string(xsts, "Token")
        stage("Signing in to Minecraft Services")
        val body = JsonObject().apply { addProperty("identityToken", "XBL3.0 x=$userHash;$xstsToken") }
        val game = AuthProtocol.gameToken(transport.exchange(AuthService.MINECRAFT, body.toString()), nowMs())
        return game to verify(game.access, stage)
    }

    fun verify(access: String, stage: (String) -> Unit): JavaProfile {
        // Always check entitlements, including when a profile already exists.
        stage("Verifying Java Edition ownership")
        AuthProtocol.requireOwnership(transport.exchange(AuthService.ENTITLEMENTS, bearer = access))
        stage("Loading your Java Edition profile")
        return AuthProtocol.profile(transport.exchange(AuthService.PROFILE, bearer = access))
    }

    private fun request(properties: JsonObject, relyingParty: String) = JsonObject().apply {
        add("Properties", properties); addProperty("RelyingParty", relyingParty); addProperty("TokenType", "JWT")
    }
}
