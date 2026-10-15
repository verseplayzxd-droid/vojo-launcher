# Unresolved licenses — review before release

This is an unresolved inventory, not permission to redistribute the affected components. Existing headers and notices remain in place. Do not assign a guessed license, remove an upstream notice, or call the whole binary dependency tree LGPL.

| Component | Evidence in this checkout | Required review |
| --- | --- | --- |
| MioLibPatcher | Pinned submodule `9a1f807f2b8e7abfd921a3ed5a69e23ed6540b0c` has no root license file. | Obtain the author's explicit applicable license and notices for the shipped support JAR. |
| Boardwalk-derived code | Upstream README lists ambiguous/unknown licensing. | Identify inherited source files and applicable file-level licenses. |
| LWJGLX | Upstream README labels the license unknown. | Match the bundled compatibility code to its source revision and license. |
| Prebuilt renderer/audio/LWJGL archives | Bundled AARs and native libraries predate Vojo; top-level README is not a complete notice/source inventory. | Match binary versions to source and retain all applicable notices and source/relinking obligations. |
| OpenAL / oboe / PFFFT notices | README references license asset files that are absent from this pinned checkout's license directory. | Recover the correct notices from the exact bundled sources; do not infer terms from the filenames. |
| Other inherited JARs and native binaries | A complete transitive notice audit has not been performed. | Inventory the final APK, including nested assets and native components, before public distribution. |

Original README credits, the root LGPL-3.0 text, ANGLE notice, and font OFL notice are retained. This list must be reviewed and resolved or affected components removed before release. The M2 debug APK is a development artifact, not a license-cleared release.
