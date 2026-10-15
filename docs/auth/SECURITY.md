# M3 authentication boundaries

## Identity and authorization

Vojo uses its own owner-supplied client ID, the personal-account authority, system browser, code grant and S256 PKCE. The registration guide defines the exact redirects/scopes. The owner must obtain Mojang approval; an Azure app registration alone is insufficient. Neither a missing ID nor a rejected app registration falls back to another launcher ID.

Each attempt uses independent 256-bit random state and verifier values. The callback must match the exact configured URI, have a valid one-time state, contain no duplicate query fields or fragment, and arrive within ten minutes of the recorded monotonic time. Reboot/elapsed-clock reset invalidates the attempt. PKCE/state are encrypted so process recreation can resume safely. A startup barrier prevents callback handling before vault load. State is durably consumed before exchanging the authorization code. A process dying after consumption requires a new sign-in; it cannot replay the code.

The service chain is Microsoft → Xbox Live → XSTS → Minecraft Services → Java entitlements → Java profile. Entitlement items must explicitly identify `game_minecraft` or `product_minecraft`; a profile alone, Bedrock-only item or unknown item is not enough. An unexpected/new entitlement shape fails closed for review rather than granting access. No demo/offline fallback exists.

The inherited login/entitlement endpoint contract is retained, with requests authored in Kotlin and checked against existing source plus primary implementation references. An unauthenticated GET to `/entitlements/mcstore` returned HTTP 401 on 15 October 2026; that only confirms a protected endpoint response, not successful entitlement semantics. Live compatibility requires the owner's approved registration and an entitled account. Current Prism sources use a different launcher/license API contract, so this implementation must not be described as a copy of Prism's latest full flow. No source code or client ID was copied from Prism.

`requireLaunchSession()` always makes a fresh online entitlement/profile check. It refreshes expired/rejected tokens, checks the profile ID matches the saved account, and returns a redacted-string `VerifiedLaunchSession` only after success. M5 must wire this explicitly into launch. The inherited `Tools.launchMinecraft` route currently rejects all legacy account/demo/offline launches, preventing an old plaintext account from bypassing the new boundary.

## Credential persistence

Only profile ID/name, verification/expiry metadata, Microsoft refresh token, Minecraft access token and a short-lived pending PKCE/state record are persisted, inside one authenticated encrypted vault. Intermediate Microsoft access, Xbox and XSTS tokens remain transient. Secret-bearing models have redacted string representations and never enter UI state, a saved Bundle, log, analytics or a backend request.

- API 23+: AES-256/GCM with a non-exportable AndroidKeyStore key and randomized IV.
- API 21–22: random AES-256 data key wrapped with an AndroidKeyStore RSA-2048 key, then AES/GCM with a fresh 96-bit IV. RSA/PKCS1 wrapping supports the retained API 21 lane; AES key bytes only reach disk as ciphertext.
- Vault header/version/mode are checked, with authenticated additional data, bounded file size, atomic writes, and no plaintext fallback on key loss/corruption. Hardware backing depends on the device and is not claimed as tested.
- Files live below `noBackupFilesDir`; cloud/transfer backup rules exclude app data, and backup is disabled in the manifest.
- Rotated refresh tokens for existing accounts are saved before downstream services can fail. One rejected cached Minecraft token may trigger one refresh/recheck; there is no infinite retry loop.
- Local removal rewrites the encrypted account list. Explicit reset deletes vault/wrapped-key files and Keystore aliases. This is local sign-out, not a claim that all Microsoft sessions/consents have been revoked.

Keystore invalidation or a registration change produces a recoverable storage error. The user chooses Retry or explicitly confirms a reset. No old plaintext account is auto-imported, and no existing user file is silently erased. If an experimental inherited build wrote credentials/logs, revoke that old app's consent and remove its data separately; M3 cannot retroactively make historical files or Git history private.

## Transport and diagnostics

Requests use platform-validated HTTPS to fixed Microsoft/Xbox/Minecraft endpoints, finite connect/read timeouts, no cache and no automatic redirects. Minecraft bearer tokens are attached only to profile/entitlement requests. The `http://auth.xboxlive.com` and `rp://api.minecraftservices.com/` relying-party values are protocol audience strings, not plaintext network requests.

Responses are bounded to 128 KiB and JSON nesting to 32 levels. Error handling maps allowlisted codes to plain messages and discards original bodies/causes/URLs, preventing a server echo from reaching a crash report. No token/request/response/callback logging is added. Cancellation disconnects the active request and invalidates pending state; coroutine cancellation is checked before credential commits.

## Validation limits

Host tests cover RFC PKCE, state/URI/expiry rejection, parser bounds, refresh parsing, XSTS errors, app-approval rejection, Java entitlement checks and service ordering. They use synthetic responses only. API 21/22 RSA wrapping, API 23+ Keystore AES, actual browser return, process death, consent, account switching/removal, real ownership/child-account errors, TalkBack and hardware behavior: **test later**.

References: [Microsoft authorization code/PKCE](https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-auth-code-flow), [Android Keystore](https://developer.android.com/privacy-and-security/keystore), [primary Xbox request reference](https://github.com/PrismLauncher/PrismLauncher/blob/develop/launcher/minecraft/auth/steps/XboxUserStep.cpp), [primary current entitlement reference](https://github.com/PrismLauncher/PrismLauncher/blob/develop/launcher/minecraft/auth/steps/EntitlementsStep.cpp). Protocol facts were checked; no source was transplanted or relicensed.
