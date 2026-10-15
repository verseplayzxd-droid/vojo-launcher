# M2 screen layouts — shown before implementation

Default palette: **Glacier**, selected by the owner. minSdk remains 21. Internal Java/Kotlin packages, Android namespace and JNI function names are not renamed.

## Revised Home

```
Ribbon V / Vojo                         Account
Make room for play.
[ Larger hero, subtle navy gradient, original artwork ]
[ Profile name + version / loader / RAM chips          ]
[                  Play / choose profile              ]
[ Locked / Unlocked 23h 10m status pill                ]
Jump back in: covered or colored profile cards
Quick actions: Profiles / Downloads / Controls
One practical tip
Home | Versions | Mods | Controls | Settings
```

The rendered study uses example data. The production shell must show honest empty states when profiles/worlds are unavailable. Do not fabricate installed profiles, recent worlds, ownership or an active unlock. The unlock display receives state; a backend-issued entitlement is M9 work. Original smooth landscape illustrations replace game screenshots. The abstract ribbon V uses no game blocks, fonts or textures.

## First run

Logo, the ownership explanation "You need your own copy of Java Edition to play", one Continue button, and links to bundled privacy information and open-source licenses. A scrollable column supports large text. Continue is stored in DataStore.

## Versions / Mods / Controls

Each has its own ViewModel, titled empty state, loading/error/offline presentations, and a meaningful navigation action where an earlier prerequisite exists. These are shell destinations; downloads, mod installation and the control editor are replaced in their respective milestones. Local information remains available offline.

## Settings / About

Settings exposes Dark / Light / System, About, privacy and licenses. About shows the actual version, source link, credits, full disclaimer, readable bundled notices and an explicit Unresolved licenses entry. The privacy notice is a draft requiring owner review before release.

## Interaction

Portrait uses bottom navigation. Landscape and widths of at least 600dp use a rail. Content scrolls, chips wrap, and controls have minimum 48dp touch targets. System sans, 22sp semibold titles, 16sp section headings, 14sp body, 12sp captions; 16dp card corners, 12dp button corners and 16dp padding. Text/controls have TalkBack names and textual status indicators.

Motion: short 220ms card/content transitions and normal press feedback, with no autoplay or looping effects. When the system disables animation, content changes are static. Dark and light Compose previews are required for each new screen.

Physical ARM64 installation, actual GPU/game performance, touch behavior, hardware controllers, and on-device TalkBack: **test later**.
