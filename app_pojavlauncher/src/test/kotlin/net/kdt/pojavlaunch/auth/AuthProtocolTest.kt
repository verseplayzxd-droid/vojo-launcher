package net.kdt.pojavlaunch.auth

import org.junit.Assert.*
import org.junit.Test

class AuthProtocolTest {
    private val redirect = "io.github.verseplayzxd-droid.vojo.debug://oauth2redirect"
    private val pending = PendingSignIn("v".repeat(43), "s".repeat(43), 1000)
    private val state get() = pending.state
    private fun failure(problem: AuthProblem, action: () -> Unit) {
        assertEquals(problem, assertThrows(AuthFailure::class.java, action).problem)
    }

    @Test fun pkceMatchesRfc7636Vector() {
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", AuthProtocol.challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"))
    }
    @Test fun base64UrlHandlesPartialGroupsAndNoPadding() {
        listOf("" to "", "f" to "Zg", "fo" to "Zm8", "foo" to "Zm9v", "foobar" to "Zm9vYmFy").forEach {
            assertEquals(it.second, AuthProtocol.base64Url(it.first.toByteArray()))
        }
        assertEquals("__8", AuthProtocol.base64Url(byteArrayOf(-1,-1)))
    }
    @Test fun pendingRequestsHaveIndependentCryptographicState() {
        val a = AuthProtocol.pending(1); val b = AuthProtocol.pending(1)
        assertEquals(43, a.verifier.length); assertEquals(43, a.state.length)
        assertNotEquals(a.verifier, b.verifier); assertNotEquals(a.state, b.state); assertNotEquals(a.state, a.verifier)
    }
    @Test fun urlUsesOwnClientPkceAndNoImplicitGrant() {
        val url = AuthProtocol.authorizationUrl("11111111-2222-4333-8444-555555555555", redirect, pending)
        assertTrue(url.startsWith(AuthProtocol.AUTHORIZE)); assertTrue(url.contains("code_challenge_method=S256"))
        assertTrue(url.contains("response_type=code")); assertTrue(url.contains("offline_access"))
        assertFalse(url.contains("client_secret")); assertFalse(url.contains("response_type=token")); assertFalse(url.contains(pending.verifier))
    }
    @Test fun missingClientFailsBeforeNetwork() { failure(AuthProblem.NOT_CONFIGURED) { AuthProtocol.authorizationUrl("", redirect, pending) } }
    @Test fun callbackDecodesCodeOnlyAfterStateValidation() {
        assertEquals("a+b=c", AuthProtocol.callbackCode("$redirect?code=a%2Bb%3Dc&state=$state", redirect, pending, 1001))
    }
    @Test fun callbackRejectsDifferentSchemeHostPortPathAndFragment() {
        listOf("other://oauth2redirect", "${redirect}evil", "$redirect:123", "$redirect/extra", redirect).forEachIndexed { index, uri ->
            val suffix = if (index == 4) "#fragment" else ""
            failure(AuthProblem.INVALID_CALLBACK) { AuthProtocol.callbackCode("$uri?code=x&state=$state$suffix", redirect, pending, 1001) }
        }
    }
    @Test fun callbackRejectsMissingForgedAndDuplicateState() {
        listOf("code=x", "code=x&state=wrong", "code=x&state=$state&state=$state", "code=x&state=$state&%73tate=$state").forEach {
            failure(AuthProblem.INVALID_CALLBACK) { AuthProtocol.callbackCode("$redirect?$it", redirect, pending, 1001) }
        }
    }
    @Test fun callbackRejectsDuplicateCodeMissingCodeAndMalformedEncoding() {
        listOf("code=x&code=y&state=$state", "state=$state", "code=%ZZ&state=$state").forEach {
            failure(AuthProblem.INVALID_CALLBACK) { AuthProtocol.callbackCode("$redirect?$it", redirect, pending, 1001) }
        }
    }
    @Test fun callbackExpiresAndFailsAcrossElapsedClockReset() {
        listOf(999L, 601001L).forEach { now -> failure(AuthProblem.EXPIRED_FLOW) { AuthProtocol.callbackCode("$redirect?code=x&state=$state", redirect, pending, now) } }
    }
    @Test fun consentErrorStillNeedsValidState() {
        failure(AuthProblem.CONSENT_DENIED) { AuthProtocol.callbackCode("$redirect?error=access_denied&state=$state", redirect, pending, 1001) }
        failure(AuthProblem.INVALID_CALLBACK) { AuthProtocol.callbackCode("$redirect?error=access_denied&state=forged", redirect, pending, 1001) }
    }
    @Test fun refreshRotationAndMissingOptionalRotationAreHandled() {
        assertEquals("new", AuthProtocol.microsoft("""{"access_token":"a","refresh_token":"new"}""", "old").refresh)
        assertEquals("old", AuthProtocol.microsoft("""{"access_token":"a"}""", "old").refresh)
        failure(AuthProblem.INVALID_RESPONSE) { AuthProtocol.microsoft("""{"access_token":"a"}""") }
    }
    @Test fun missingEmptyAndWrongTypeTokensFailClosed() {
        listOf("{}", "[]", "null", "not-json", """{"access_token":42,"refresh_token":"r"}""", """{"access_token":"","refresh_token":"r"}""").forEach {
            failure(AuthProblem.INVALID_RESPONSE) { AuthProtocol.microsoft(it) }
        }
    }
    @Test fun deeplyNestedAndOversizedResponsesFailWithoutStackOverflow() {
        failure(AuthProblem.INVALID_RESPONSE) { AuthProtocol.json("[".repeat(1000) + "]".repeat(1000)) }
        failure(AuthProblem.INVALID_RESPONSE) { AuthProtocol.json(" ".repeat(131073)) }
    }
    @Test fun expiryUsesServerTtlAndSafetyMargin() {
        val token = AuthProtocol.gameToken("""{"access_token":"a","expires_in":3600}""", 1000)
        assertEquals(3601000L, token.expiresAtMs)
        assertTrue(AuthProtocol.usable(token.expiresAtMs, 1000))
        assertFalse(AuthProtocol.usable(61000,1000)); assertFalse(AuthProtocol.usable(1000,1000))
    }
    @Test fun zeroFractionalOverflowAndUnreasonableTtlFailClosed() {
        listOf("0", "-1", "1.5", "9999999999999999999999999999", "604801").forEach {
            failure(AuthProblem.INVALID_RESPONSE) { AuthProtocol.gameToken("""{"access_token":"a","expires_in":$it}""", 1000) }
        }
        failure(AuthProblem.INVALID_RESPONSE) { AuthProtocol.gameToken("""{"access_token":"a","expires_in":3600}""", Long.MAX_VALUE) }
    }
    @Test fun onlyRecognizedJavaEntitlementsPass() {
        listOf("game_minecraft", "product_minecraft").forEach { AuthProtocol.requireOwnership("""{"items":[{"name":"$it"}]}""") }
        listOf("""{"items":[]}""", """{"items":[{"name":"bedrock_only"}]}""", """{"items":[null]}""").forEach {
            failure(AuthProblem.NOT_OWNED) { AuthProtocol.requireOwnership(it) }
        }
        failure(AuthProblem.INVALID_RESPONSE) { AuthProtocol.requireOwnership("{}") }
    }
    @Test fun javaProfileRequiresValidIdentity() {
        assertEquals("player", AuthProtocol.profile("""{"id":"0123456789abcdef0123456789abcdef","name":"player"}""").name)
        listOf("{}", """{"id":"bad","name":"player"}""", """{"id":"0123456789abcdef0123456789abcdef","name":"../escape"}""").forEach {
            failure(AuthProblem.INVALID_RESPONSE) { AuthProtocol.profile(it) }
        }
    }
    @Test fun invalidRegistrationHasSpecificApprovalMessageOnlyForMinecraft() {
        listOf("Invalid app registration", """{"errorMessage":"Invalid app registration, see URL for more information"}""").forEach {
            assertEquals(AuthProblem.APPROVAL_PENDING, AuthProtocol.failure(AuthService.MINECRAFT,403,it).problem)
        }
        assertEquals(AuthProblem.SERVICE_UNAVAILABLE, AuthProtocol.failure(AuthService.MINECRAFT,403,"Forbidden").problem)
        assertEquals(AuthProblem.SERVICE_UNAVAILABLE, AuthProtocol.failure(AuthService.XBOX,403,"Invalid app registration").problem)
    }
    @Test fun xboxMissingProfileChildAndAgeErrorsAreDistinct() {
        mapOf(2148916233L to AuthProblem.NO_XBOX_PROFILE, 2148916235L to AuthProblem.REGION_UNAVAILABLE,
            2148916236L to AuthProblem.ADULT_VERIFICATION, 2148916237L to AuthProblem.ACCOUNT_RESTRICTED,
            2148916238L to AuthProblem.CHILD_APPROVAL).forEach { (code, problem) ->
            assertEquals(problem, AuthProtocol.failure(AuthService.XSTS,401,"""{"XErr":$code}""").problem)
        }
    }
    @Test fun serviceErrorsArePlainAndNeverContainServerSecrets() {
        val failure = AuthProtocol.failure(AuthService.MICROSOFT,400,"""{"error":"invalid_grant","error_description":"secret-token-fixture"}""")
        assertEquals(AuthProblem.REAUTH_REQUIRED, failure.problem); assertNull(failure.cause)
        assertFalse(failure.toString().contains("secret-token-fixture"))
        assertEquals(AuthProblem.NO_JAVA_PROFILE, AuthProtocol.failure(AuthService.PROFILE,404,"{}").problem)
        assertEquals(AuthProblem.RATE_LIMITED, AuthProtocol.failure(AuthService.PROFILE,429,"{}").problem)
    }
    @Test fun credentialModelsHaveRedactedStringRepresentations() {
        listOf(MicrosoftTokens("sensitive-marker", "sensitive-marker").toString(), GameToken("sensitive-marker",1).toString(),
            PendingSignIn("sensitive-marker", "sensitive-marker",1).toString(),
            VerifiedLaunchSession("id","player","sensitive-marker").toString()).forEach { assertFalse(it.contains("sensitive-marker")) }
    }
    @Test fun legacyOfflineDemoAndPlaintextLaunchCannotProceed() { assertThrows(SecurityException::class.java) { LegacyLaunchBoundary.rejectLegacyLaunch() } }
}
