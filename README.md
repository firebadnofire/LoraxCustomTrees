[![Issues][issues-shield]][issues-url]

<br />
<div align="center">
  <a href="https://github.com/FrankTCA/LoraxCustomTrees">
    <img src="logo.png" alt="Logo" width="291" height="100">
  </a>

<h3 align="center">Lorax Custom Trees</h3>

  <p align="center">
    A modern, simplistic custom trees plugin for Paper
    <br />
    <a href="https://www.spigotmc.org/resources/lorax-custom-trees.94584/"><strong>Download plugin »</strong></a>
    <br />
    <br />
    <a href="https://infotoast.org/aka/easytrees">Download the Datapack</a>
    ·
    <a href="https://github.com/FrankTCA/LoraxCustomTrees">Report Bugs</a>
  </p>
</div>

Lorax is a modern, up-to-date, and simple custom trees plugin!

We use [Easy Trees](https://infotoast.org/aka/easytrees) for the actual trees, while this plugin does the generation.

## Contributors
- <a href="https://infotoast.org">Frank from Info Toast</a>, for plugin development
- <a href="https://www.planetminecraft.com/member/coolscreenname/">CoolScreenName</a>, for trees
- <a href="https://www.planetminecraft.com/member/resistance_is_futile/">resistance_is_futile</a>, for developing SkyPlants, inspiration for EasyTrees

## How to Use
1. Download a jar from <a href="https://www.spigotmc.org/resources/lorax-custom-trees.94584/">SpigotMC</a>
2. Place in `plugins` folder.
3. Restart server.

## Installation/Building
Use JDK 25 for the root Gradle build. Run `./gradlew :build :common:check`
(`.\gradlew.bat` on Windows). The existing Paper distribution remains
`build/libs/Lorax-6.2.jar`; `shadowJar` is no longer used. No dependency currently
requires shading. WorldEdit is optional and is not bundled.

Additional, independent distributions (use each directory's `gradlew.bat` on
Windows; the commands below use Bash):

| Target | Build command | Output |
| --- | --- | --- |
| Spigot 1.12.2 | `./gradlew :spigot112:build` (JDK 25 compiler, Java 8 bytecode) | `spigot112/build/libs/LoraxCustomTrees-spigot-1.12.2-6.2.jar` |
| Spigot/Paper 1.20–26.3 target range | `./gradlew :spigotModern:build` (JDK 25 compiler, Java 17 bytecode) | `spigotModern/build/libs/LoraxCustomTrees-spigot-mc1.20-26.3-6.2.jar` |
| Forge 1.12.2 | `cd forge112 && bash ./gradlew build` (JDK 8, isolated Gradle 5.6.4) | `forge112/build/libs/LoraxCustomTrees-forge-1.12.2-6.2.jar` |
| Forge 26.3 | `cd forge26 && bash ./gradlew build` (JDK 25, isolated Gradle 9.7.1) | `forge26/build/libs/LoraxCustomTrees-forge-26.3-6.2.jar` |
| Forge 1.21.10 | `cd forge12110 && bash ./gradlew build` (build JDK 25, Java 21 bytecode) | `forge12110/build/libs/LoraxCustomTrees-forge-1.21.10-6.2.jar` |

| Forge 1.20 | `cd forge120 && bash ./gradlew build` (JDK 17, isolated Gradle 8.1.1) | `forge120/build/libs/LoraxCustomTrees-forge-1.20-6.2.jar` |
| Forge 1.20.6 | `cd forge1206 && bash ./gradlew build` (build JDK 25, Java 21 bytecode) | `forge1206/build/libs/LoraxCustomTrees-forge-1.20.6-6.2.jar` |
| Forge 1.21 | `cd forge121 && bash ./gradlew build` (build JDK 25, Java 21 bytecode) | `forge121/build/libs/LoraxCustomTrees-forge-1.21-6.2.jar` |
| Forge 26.1 | `cd forge261 && bash ./gradlew build` (JDK 25) | `forge261/build/libs/LoraxCustomTrees-forge-26.1-6.2.jar` |
| Forge 26.2 | `cd forge262 && bash ./gradlew build` (JDK 25) | `forge262/build/libs/LoraxCustomTrees-forge-26.2-6.2.jar` |

Install exactly one appropriate distribution: plugins in `plugins/`, native Forge
mods in `mods/`. Forge does not use Bukkit or require a client mod. See
[the compatibility and validation record](docs/COMPATIBILITY.md) for exact tested
server builds and limitations. A compiled JAR is not proof of runtime support.

### Generation and safety

All distributions use `common/` for BO5 parsing, biome/species policy, seeded
candidate selection, rotation and complete-footprint placement checks. The 40
canonical assets remain in `src/main/resources`; version adapters translate them
at startup rather than maintaining hand-edited legacy copies.

Placement is synchronous inside the native generation callback. There are no
deferred queues, retained generation regions, persisted pending trees, forced
chunk loads or recovery/retrofit passes. An automatic tree whose complete
footprint is unavailable, outside height limits or obstructed is rejected before
writing any blocks. This can reduce successful placements near platform context
boundaries; candidate density and selection have not been reduced to enforce
spacing. Explicit operator spawning bypasses terrain/obstruction checks and can
overwrite blocks, but still requires the entire footprint to be loaded.

Candidate selection preserves the original seed, species arrays, 1–4 candidates
per eligible biome and 0–14 local offsets. Candidate determinism and final block
layout determinism are separate results: overlapping trees can leave different
final blocks when neighboring chunks populate in a different order. No spacing
reservations have been introduced. A future overlap-resolution improvement must
preserve density and generation behavior. Jungle structures remain available for
manual spawning; the original automatic biome policy did not select jungle.

`enable-populator`, `select-worlds` and `enabled-worlds` are supported. Bukkit uses
world names; modern Forge uses dimension identifiers such as
`minecraft:overworld`. Forge 1.12.2 uses world names. Modern Paper/Spigot retains
optional WorldEdit export; legacy Spigot and Forge report export as unsupported.

### Repeatable server validation

Test-only probes are separate JARs under each build's `build/test-libs/`:
`integrationProbeJar` (root/Forge) or `:spigot112:integrationProbeJar` /
`:spigotModern:integrationProbeJar`. They check initialization, all 40 assets,
actual fresh-world generation, multiple worlds, boundary placements, all four
rotations and native block representations. They are never included in releases.

On PowerShell 7, prepare and run a disposable Paper test:

```powershell
./tools/runtime/Prepare-Paper.ps1 -Minecraft 26.3 -Directory build/test-paper/server -Plugin build/libs/Lorax-6.2.jar -Probe build/test-libs/LoraxIntegrationProbe-6.2.jar
./tools/runtime/Test-Paper.ps1 -Directory build/test-paper/server -Java /absolute/path/to/jdk25/bin/java
```

The runner uses an ephemeral loopback port, records console/error logs, requires
a successful structured probe result and stops the server. Use a fresh directory
for each initial run. The Paper probe saves a snapshot for an explicit second
run with `-Restart`; this checks existing chunks, simulated player edits and
disabled generation. The initial result is archived separately. To add two
headless vanilla-protocol client joins, first run `npm ci --ignore-scripts` in
`tools/client-probe`, then pass `-ClientVersion <exact-version>` to the runner.
Supported harness versions and actual successful joins are recorded separately
from server-only results. These offline tests bind only to loopback, keep a
whitelist and disable RCON; do not reuse their properties for a public server.
Test preparation accepts Minecraft's EULA for that disposable
server. For Forge, use `Prepare-Forge.ps1` with the exact Minecraft/Forge versions,
mod/probe paths and Java executable, then run `Test-Paper.ps1 -LaunchManifest
server-launch.json -ResultPath lorax-test/result.json` with that Java executable.
The runner records a structured result including clean shutdown; forced
termination is a failed run. Manual Forgejo `mc-test.yml` supports Paper, Spigot and Forge, isolated
build directories, exact artifact paths and optional generation probes. Without
a probe it reports startup checks only. CI execution itself still needs validation
on the Forgejo runner.

### Licensing and publication

`LICENSE.md` is restrictive. Permission to modify or redistribute this project
has **not** been established. The EasyTrees submodule relationship and existing
asset credits are preserved; its licensing/provenance needs resolution before
release. Local technical builds and tests do not establish release authorization.
The existing signed Paper release pipeline remains restricted to the original
root distribution. New platform JARs are not automatically published or advertised
by that pipeline.

[issues-shield]: https://img.shields.io/github/issues/FrankTCA/LoraxCustomTrees.svg?style=for-the-badge
[issues-url]: https://github.com/FrankTCA/LoraxCustomTrees/issues
[license-shield]: https://img.shields.io/github/license/FrankTCA/LoraxCustomTrees.svg?style=for-the-badge
[license-url]: https://github.com/PG85/OpenTerrainGenerator/blob/master/LICENSE.md
[discord-shield]: https://img.shields.io/discord/714199392516702280?style=for-the-badge
[discord-url]: https://discord.gg/2wMPtrZbPF
