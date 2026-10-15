"""Verify packaged M2 invariants without claiming device/runtime acceptance.

Usage: python scripts/verify_debug_apk.py path/to/app_pojavlauncher-debug.apk
Metadata (application ID, launch activity and minSdk) and signatures are checked
separately with Android's aapt and apksigner tools.
"""
import hashlib
import struct
import sys
from pathlib import Path
from zipfile import ZipFile

root = Path(__file__).resolve().parents[1]
apk = Path(sys.argv[1])
documents = {
    "licenses/vojo/LGPL-3.0.txt": "LICENSE",
    "licenses/vojo/GPL-3.0.txt": "licenses/GPL-3.0.txt",
    "licenses/vojo/Apache-2.0.txt": "licenses/Apache-2.0.txt",
    "licenses/vojo/NOTICE.txt": "docs/licenses/NOTICE.md",
    "licenses/vojo/UNRESOLVED.txt": "docs/licenses/UNRESOLVED.md",
    "licenses/vojo/PRIVACY-DRAFT.txt": "docs/licenses/PRIVACY-DRAFT.txt",
    "licenses/ANGLE_LICENSE": "app_pojavlauncher/src/main/assets/licenses/ANGLE_LICENSE",
    "font/LICENSE_OFL.txt": "app_pojavlauncher/src/main/assets/font/LICENSE_OFL.txt",
}
arm64 = [
    "lib/arm64-v8a/libpojavexec.so",
    "lib/arm64-v8a/libexithook.so",
    "lib/arm64-v8a/libandroidnsbypass.so",
    "assets/components/lwjgl-3.3.3-natives/arm64-v8a/liblwjgl.so",
    "assets/components/lwjgl-3.4.1-natives/arm64-v8a/liblwjgl.so",
]
with ZipFile(apk) as archive:
    for asset, source in documents.items():
        if archive.read("assets/" + asset) != (root / source).read_bytes():
            raise SystemExit(f"Bundled document differs from source: {asset}")
    for library in arm64:
        header = archive.read(library)[:20]
        if header[:6] != b"\x7fELF\x02\x01" or struct.unpack_from("<H", header, 18)[0] != 183:
            raise SystemExit(f"Not a little-endian 64-bit AArch64 ELF: {library}")
    if "res/font/minecraft_ten.ttf" in archive.namelist():
        raise SystemExit("Inherited Minecraft-style font must not ship")
print(f"PASS: {len(documents)} verbatim legal documents; {len(arm64)} AArch64 ELF libraries; excluded inherited pixel font")
print(f"APK bytes: {apk.stat().st_size}")
print(f"SHA-256: {hashlib.sha256(apk.read_bytes()).hexdigest()}")
print("Physical ARM64 installation, startup, graphics and accessibility: test later")
