# Vojo Microsoft registration — M3

Use your own registration. Never enter an ID copied from another launcher. The client ID is a public identifier, not a client secret; keeping it at build time makes its ownership and configuration explicit.

## Exact settings for this implementation

| Setting | Value |
| --- | --- |
| Display name | `Vojo Launcher` |
| Supported account types | **Personal Microsoft accounts only** (`PersonalMicrosoftAccount`) |
| Authority | `https://login.microsoftonline.com/consumers` |
| Platform | **Mobile and desktop applications** (custom redirect URI; this app uses a system browser, not MSAL or a broker) |
| Debug redirect URI | `io.github.verseplayzxd-droid.vojo.debug://oauth2redirect` |
| Release redirect URI | `io.github.verseplayzxd-droid.vojo://oauth2redirect` |
| Allow public client flows | **Yes**. The implementation uses authorization code + PKCE; it does not implement password or device-code grants. |
| Implicit access tokens / ID tokens | Both **unchecked** |
| Client secret / certificate | **None**; native public clients cannot keep a client secret |
| OAuth scopes, exact request string | `XboxLive.signin offline_access` |
| Microsoft Graph permissions | Not requested; remove the default `User.Read` permission if present |
| Admin consent | Not requested by this personal-account flow |

Register both exact redirect URIs in the same Vojo registration if you want debug and release to share that ID. They contain hyphens, not the underscore in the Android application ID: underscores are invalid in an OAuth URI scheme. The Android package IDs remain unchanged. These are **native client** redirects, not Web/SPA redirects, and there is no hosted callback endpoint to deploy.

The client sends `response_type=code`, `response_mode=query`, `code_challenge_method=S256`, a fresh cryptographic state and PKCE challenge, and `prompt=select_account`. Token exchange uses the matching redirect URI, client ID, authorization code and verifier; refresh uses the same registration. No ID token is requested, so `openid`, `profile` and `email` are unnecessary. `offline_access` requests refresh capability; it does not permit offline game ownership bypass.

`XboxLive.signin` is requested dynamically against the personal-account authority; it is not a Microsoft Graph permission to add from the Graph list. Azure settings alone do not grant Minecraft Services access: the app must also pass Mojang's AppID review. If the API returns `Invalid app registration`, Vojo shows the waiting-for-approval message and saves no new account.

## Build-time configuration

Supply the Application (client) ID GUID using one of:

```powershell
$env:VOJO_MICROSOFT_CLIENT_ID = '<your own Application (client) ID GUID>'
.\gradlew.bat :app_pojavlauncher:assembleDebug
```

or a local Gradle property (for example, your ignored Gradle user-home properties):

```properties
vojoMicrosoftClientId=<your own Application (client) ID GUID>
```

The Gradle property takes precedence over the environment variable. An omitted ID builds a UI/test APK with sign-in disabled and a clear setup message. A malformed nonempty ID fails configuration. There is no fallback ID and no client secret setting. Generated BuildConfig values are `MICROSOFT_CLIENT_ID` and `MICROSOFT_REDIRECT_URI`.

For CI, add the public repository Actions variable `VOJO_MICROSOFT_CLIENT_ID` after registration. It is optional for compile/unit/APK checks. Do not put refresh/access tokens or a client secret in Actions variables, Gradle properties, issue reports or source files.

## Sources and verification limits

- [Microsoft: platform and custom redirect configuration](https://learn.microsoft.com/en-us/entra/identity-platform/how-to-add-redirect-uri) explicitly supports Mobile/desktop for mobile apps that do not use MSAL or a broker.
- [Microsoft: authorization code and PKCE](https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-auth-code-flow).
- [Microsoft: public client flow setting](https://learn.microsoft.com/en-us/entra/identity-platform/scenario-desktop-app-registration#enable-public-client-flow).
- [Mojang AppID Review form](https://forms.cloud.microsoft/Pages/ResponsePage.aspx?id=v4j5cvGGr0GRqy180BHbR-ajEQ1td1ROpz00KtS8Gd5UNVpPTkVLNFVROVQxNkdRMEtXVjNQQjdXVC4u), read directly on 15 October 2026. No form was submitted.

Microsoft's public identity documentation does not fully document Minecraft's private service contract. Xbox/Minecraft request formats are checked against the inherited implementation and primary open-source implementation references; compatibility with this new registration must be verified after owner registration/approval. End-to-end authentication and device browser routing: **test later**.
