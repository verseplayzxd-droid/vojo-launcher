# Vojo Launcher

[![Vojo debug build](https://github.com/verseplayzxd-droid/vojo-launcher/actions/workflows/vojo-debug.yml/badge.svg)](https://github.com/verseplayzxd-droid/vojo-launcher/actions/workflows/vojo-debug.yml)

An Android Java Edition launcher project derived from Amethyst and PojavLauncher. **Not affiliated with Mojang or Microsoft.** You need your own copy of Java Edition to play. Game files must be obtained from their authorized sources at runtime; they are not included here.

Milestone 2 introduced the Vojo brand and Glacier Compose shell. Milestone 3 adds Microsoft browser sign-in with PKCE, Java entitlement/profile checks, encrypted token storage and saved account selection. Supply your own approved client ID using the [Azure registration guide](docs/auth/AZURE-REGISTRATION.md); the [Mojang review draft](docs/auth/MOJANG-REVIEW-DRAFT.md) is ready to customize. An unconfigured build cannot sign in. Legacy borrowed-ID, plaintext and offline/demo auth paths are disabled. Profiles/downloads are M4; game launch integration is M5. Unlock status remains Locked until the M9 entitlement backend is implemented.

See the [M2 report and device checklist](docs/milestones/M2.md), [screen layouts](docs/design/M2-layouts.md), and [rendered Glacier Home study](docs/design/previews/glacier-home-refined.png). The study uses example data; the app displays honest empty states. Dark and light Compose previews are included in the UI source.

## Build

Use JDK 21 for Gradle and make a JDK 8 toolchain available for MioLibPatcher. The repository declares Gradle 9.6.1, AGP 9.3.1, compile SDK 37, target SDK 34 and minSdk 21. Built-in Kotlin and its Compose compiler are 2.2.10; KSP is 2.3.6 and Hilt is 2.59.2. No Kotlin source-set compatibility override is required.

Initialize the two pinned modules, then build:

```sh
git -c url.https://github.com/.insteadOf=git@github.com: submodule update --init --recursive MioLibPatcher androidnsbypass
./gradlew :app_pojavlauncher:assembleDebug -Dorg.gradle.java.installations.paths=/path/to/jdk8
```

Use `gradlew.bat` on Windows. Native tools require a workspace path without spaces; the [M2 report](docs/milestones/M2.md) contains the verified Windows alias and low-memory build command. Required SDK packages are listed in [CI](.github/workflows/vojo-debug.yml). The APK is `app_pojavlauncher/build/outputs/apk/debug/app_pojavlauncher-debug.apk`, application ID `io.github.verseplayzxd_droid.vojo.debug`. Internal packages and JNI names remain unchanged.

CI builds and verifies a debug APK directly from source, without a release signing secret or downloading opaque runtime artifacts from another workflow. No Android Java runtime is bundled in this M2 build; gameplay acceptance is pending M5. Install/startup on an ARM64 phone: **test later**.

## License and credits

The original [LGPL license](LICENSE) and all existing notices remain in place. Covered changes are available in this repository; no component is relicensed. The incorporated [GPL text](licenses/GPL-3.0.txt) and [Apache text](licenses/Apache-2.0.txt) are also bundled. Read [notices](docs/licenses/NOTICE.md) and the **[Unresolved licenses](docs/licenses/UNRESOLVED.md)** inventory before release. The current development APK has not been cleared for public distribution. The [privacy notice](docs/licenses/PRIVACY-DRAFT.txt) is a draft requiring owner review.

Thanks to Amethyst, PojavLauncher, Boardwalk and all contributors and dependency authors. The complete inherited [README and credits](docs/upstream/README.md) and [original workflow](docs/upstream/android-ci.yml) are retained for provenance; their feature and release claims describe upstream, not this Vojo milestone.
