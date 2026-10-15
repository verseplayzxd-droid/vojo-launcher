# Mojang AppID review — paste-ready draft

The live form was inspected on 15 October 2026. Replace every bracketed value with your own registration details. This document does not claim an approval, and nothing has been submitted.

## Field 1 — EULA / Usage Guidelines acknowledgement

Read the linked EULA and Usage Guidelines yourself; choose Yes only if the acknowledgement is true. This cannot be answered on your behalf by a code change or a prepared draft.

## Field 2 — Contact Information

`[Your valid email address matching the contact details in your Azure registration]`

## Field 3 — Request type

`New AppID for Approval`

## Field 4 — Application Name

`Vojo Launcher`

## Field 5 — Application ID

`[Your Application (client) ID GUID — not the Android package name or Azure Object ID]`

## Field 6 — Tenant ID

`[Your Directory (tenant) ID GUID from the registration Overview — not the consumers authority name]`

## Field 7 — Associated website or domain

`https://github.com/verseplayzxd-droid/vojo-launcher`

## Field 8 — Justification

Vojo Launcher is an open-source Android launcher project for users who have legitimate access to Minecraft: Java Edition. It is derived from Amethyst and PojavLauncher and is being developed in public at https://github.com/verseplayzxd-droid/vojo-launcher.

I request approval for my own Microsoft Entra Application (client) ID so Vojo can complete Microsoft OAuth, Xbox Live, XSTS and Minecraft Services authentication, verify Java Edition entitlements, and retrieve the authenticated player's Java profile. Only a successfully verified entitled account with a valid Java profile may receive a launch session. Vojo provides no offline/cracked account mode, no demo fallback and no borrowed launcher client ID.

The Android app uses the system browser with OAuth authorization code + S256 PKCE, a validated one-time state and registered native redirect URIs. Microsoft passwords are never entered into Vojo. Tokens are stored in app-private, Android Keystore-backed encrypted storage, excluded from backups, and are not logged, shared with analytics or sent to a Vojo backend. Intermediate Xbox/XSTS credentials are held only during authentication. Users can switch between their own verified accounts and remove local credentials.

The current milestone implements authentication; version installation and game launch integration follow in later milestones. Game client files, libraries and assets are not bundled with Vojo. The planned download pipeline obtains authorized game files at runtime and verifies their hashes. Vojo uses original branding and displays "Not affiliated with Mojang or Microsoft". I am requesting review before distributing a production launcher.

## Field 9 — Additional information

Android application ID: io.github.verseplayzxd_droid.vojo (debug suffix .debug). Minimum Android API level currently 21.

Native public client redirect URIs:
io.github.verseplayzxd-droid.vojo://oauth2redirect
io.github.verseplayzxd-droid.vojo.debug://oauth2redirect

Requested OAuth scopes: XboxLive.signin offline_access. No client secret, Graph access, organizational account access or Microsoft password collection is requested. If Minecraft Services rejects this registration, Vojo explains that the app is waiting for approval and stops authentication.

The current development build contains no ad SDK or ad unlock backend. The public roadmap includes launcher-only monetization: three rewarded ads for a 24-hour launcher unlock, with additional referral/creator/community unlock paths and possible future Premium. This is separate from game ownership: it must never grant a Java entitlement or bypass Microsoft/Minecraft checks. Ads would not appear inside the game. I disclose this planned behavior for review before that milestone is implemented.

The source preserves inherited notices and includes an unresolved component-license inventory requiring review before release. The privacy notice is a development draft. I will provide updated production policy/contact information and any additional technical details requested during review.

## Before submitting

Check that the draft still matches the actual build and current roadmap, replace placeholders, verify both GUIDs and Azure contact details, and provide a real contact/privacy URL if requested. The form says duplicate submissions do not speed review; retain your original submission/approval correspondence. Do not paste passwords, client secrets, access tokens or refresh tokens into the form.
