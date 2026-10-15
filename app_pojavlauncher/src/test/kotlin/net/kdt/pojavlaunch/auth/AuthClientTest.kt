package net.kdt.pojavlaunch.auth

import org.junit.Assert.*
import org.junit.Test

class AuthClientTest {
    private class FixtureTransport(private val owned: Boolean = true, private val malformedXbox: Boolean = false) : AuthTransport {
        val calls = mutableListOf<AuthService>()
        override fun exchange(service: AuthService, payload: String?, form: Boolean, bearer: String?): String {
            calls += service
            return when (service) {
                AuthService.MICROSOFT -> { assertTrue(form); assertTrue(payload!!.contains("code_verifier=")); """{"access_token":"ms-fixture","refresh_token":"refresh-fixture"}""" }
                AuthService.XBOX -> { assertTrue(payload!!.contains("d=ms-fixture")); """{"Token":"xbox-fixture"}""" }
                AuthService.XSTS -> if (malformedXbox) """{"Token":"xsts-fixture","DisplayClaims":{"xui":[]}}""" else """{"Token":"xsts-fixture","DisplayClaims":{"xui":[{"uhs":"hash-fixture"}]}}"""
                AuthService.MINECRAFT -> { assertTrue(payload!!.contains("XBL3.0 x=hash-fixture;xsts-fixture")); """{"access_token":"game-fixture","expires_in":3600}""" }
                AuthService.ENTITLEMENTS -> { assertEquals("game-fixture", bearer); if (owned) """{"items":[{"name":"game_minecraft"}]}""" else """{"items":[]}""" }
                AuthService.PROFILE -> { assertEquals("game-fixture", bearer); """{"id":"0123456789abcdef0123456789abcdef","name":"FixturePlayer"}""" }
            }
        }
    }
    @Test fun fullChainUsesAllServicesAndChecksOwnershipBeforeProfile() {
        val transport = FixtureTransport(); val client = AuthClient(transport)
        val tokens = client.exchangeCode("11111111-2222-4333-8444-555555555555", "native://callback", "synthetic-code", "v".repeat(43))
        val (game, profile) = client.signIn(tokens, {1000}, {})
        assertEquals("FixturePlayer", profile.name); assertEquals("game-fixture", game.access)
        assertEquals(AuthService.entries.toList(), transport.calls)
    }
    @Test fun validLookingProfileCannotBypassEmptyEntitlements() {
        val transport = FixtureTransport(owned = false)
        val failure = assertThrows(AuthFailure::class.java) { AuthClient(transport).signIn(MicrosoftTokens("ms-fixture","r"), {1000}, {}) }
        assertEquals(AuthProblem.NOT_OWNED, failure.problem); assertFalse(transport.calls.contains(AuthService.PROFILE))
    }
    @Test fun malformedXboxResponseNeverReachesMinecraft() {
        val transport = FixtureTransport(malformedXbox = true)
        assertThrows(AuthFailure::class.java) { AuthClient(transport).signIn(MicrosoftTokens("ms-fixture","r"), {1000}, {}) }
        assertFalse(transport.calls.contains(AuthService.MINECRAFT))
    }
    @Test fun approvalRejectionStopsBeforeEntitlementOrProfileRequests() {
        val calls = mutableListOf<AuthService>()
        val delegate = FixtureTransport()
        val rejecting = object : AuthTransport {
            override fun exchange(service: AuthService, payload: String?, form: Boolean, bearer: String?): String {
                calls += service
                if (service == AuthService.MINECRAFT) throw AuthProtocol.failure(service,403,"""{"errorMessage":"Invalid app registration"}""")
                return delegate.exchange(service,payload,form,bearer)
            }
        }
        assertEquals(AuthProblem.APPROVAL_PENDING, assertThrows(AuthFailure::class.java) {
            AuthClient(rejecting).signIn(MicrosoftTokens("ms-fixture","r"), {1000}, {})
        }.problem)
        assertFalse(calls.contains(AuthService.ENTITLEMENTS)); assertFalse(calls.contains(AuthService.PROFILE))
    }
}
