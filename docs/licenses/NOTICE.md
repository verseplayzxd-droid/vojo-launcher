# Vojo Launcher notices

Vojo Launcher derives from Amethyst and PojavLauncher. Credit and copyright notices belonging to their contributors and to third-party components must be preserved. The original root `LICENSE` is unchanged; no component is relicensed by this notice.

Not affiliated with Mojang or Microsoft.

Source for Vojo's covered changes: https://github.com/verseplayzxd-droid/vojo-launcher

The new UI uses AndroidX Compose/Material 3, Activity, Lifecycle and DataStore; Kotlin and its Compose compiler; kotlinx.coroutines; Dagger/Hilt; and KSP. These projects publish Apache-2.0 terms. Their component notices and any transitive dependency terms must also be retained; the bundled Apache text does not replace component-specific notices.

The root LGPL text and the GPL text it incorporates are packaged verbatim with the app. Existing ANGLE and OFL notices remain intact. See `UNRESOLVED.md` for inherited components that are not yet cleared for release.

## Added dependency inventory

| Project | Version used in M2 | Published terms and project |
| --- | --- | --- |
| Compose UI, Foundation, Runtime, tooling | 1.8.3 through BOM 2025.07.00 | Apache-2.0; https://android.googlesource.com/platform/frameworks/support/ |
| Compose Material 3 | 1.3.2 | Apache-2.0; https://android.googlesource.com/platform/frameworks/support/ |
| Activity Compose | 1.9.3 | Apache-2.0; AndroidX, as above |
| Lifecycle Compose | 2.8.7 | Apache-2.0; AndroidX, as above |
| DataStore Preferences | 1.1.7 | Apache-2.0; AndroidX, as above |
| kotlinx.coroutines | 1.9.0 | Apache-2.0; https://github.com/Kotlin/kotlinx.coroutines |
| Dagger/Hilt | 2.59.2 | Apache-2.0; https://github.com/google/dagger |
| Kotlin / Compose compiler (build tool) | 2.2.10 | Apache-2.0; https://github.com/JetBrains/kotlin |
| Kotlin standard library (transitive runtime) | 2.2.20 requested by Hilt | Apache-2.0; https://github.com/JetBrains/kotlin |
| KSP (build tool) | 2.3.6 | Apache-2.0; https://github.com/google/ksp |

This direct-dependency inventory is not a complete transitive binary audit. Existing component-specific notices are retained, and unresolved inherited binaries are listed separately. The original Vojo ribbon and smooth landscape drawings use no game textures or fonts. The new interface uses the Android system sans-serif font.
