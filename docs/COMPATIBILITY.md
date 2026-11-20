# Compatibility and validation

Status date: 2026-11-20. These are local dedicated-server integration results,
not release authorization. The complete advertised target matrix remains in
scope; exact targets lacking server evidence remain explicitly unvalidated. Passing one patch does
not validate every patch in a version family.

## Builds and server results

`P` = root `:build`, `M` = `:spigotModern:build`, `S` = `:spigot112:build`,
`F12` = `(cd forge112 && bash ./gradlew build)`, `F26` = `(cd forge26 && bash ./gradlew build)`.
Root builds require JDK 25; F12 requires JDK 8. Output paths are in README.

| Platform | Minecraft | Runtime Java | Build | Compiles | Startup / 40 assets | Fresh generation + placement | Controlled shutdown | Human gameplay |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Paper | 26.3 build 256 | 25 | P | Yes | Passed | Passed | Passed | Not tested |
| Paper | 1.20 | 17 | M | Yes | Passed | Passed | Passed | Not tested |
| Paper | 1.20.1 | 17 | M | Yes | Passed | Passed | Passed | Not tested |
| Paper | 1.20.4 | 17 | M | Yes | Passed | Passed | Passed | Not tested |
| Paper | 1.20.5, 1.20.6 | 21 | M | Yes | Passed each | Passed each | Passed | Not tested |
| Paper | 1.21, 1.21.10, 1.21.11 | 21 | M | Yes | Passed each | Passed each | Passed | Not tested |
| Paper | 26.1.1, 26.1.2 | 25 | M | Yes | Passed each | Passed each | Passed | Not tested |
| Paper | 26.2 | 25 | M | Yes | Passed | Passed | Passed | Not tested |
| Spigot | 1.12.2 | 8 | S | Yes | Passed | Passed | Passed | Not tested |
| Spigot | 1.20.1 | 17 | M | Yes | Passed | Passed | Passed | Not tested |
| Spigot | 26.3 | 25 | M | Yes | Passed | Passed | Passed | Not tested |
| Spigot | 1.20.6 | 21 | M | Yes | Passed | Passed | Passed | Not tested |
| Spigot | 1.21.1 | 21 | M | Yes | Passed | Passed | Passed | Not tested |
| Spigot | 26.1.2 | 25 | M | Yes | Passed | Passed | Passed | Not tested |
| Spigot | 26.2 | 25 | M | Yes | Passed | Passed | Passed | Not tested |
| Spigot | 1.21.10 | 21 | M | Yes | Passed | Passed | Passed | Not tested |
| Spigot | 1.20, 1.21, 26.1 | version dependent | M | Shared source compiled | Untested on Spigot | Untested on Spigot | Untested | Not tested |
| Forge | 1.12.2 / 14.23.5.2864 | 8 | F12 | Yes, reobfuscated | Passed | Passed | Passed | Not tested |
| Forge | 26.3 / 66.1.8 | 25 | F26 | Yes | Passed | Passed | Passed | Not tested |
| Forge | 1.21.10 / 60.1.18 | 21 | (cd forge12110 && bash ./gradlew build) (build JDK 25) | Yes | Passed | Passed | Passed | Not tested |
| Forge | 1.20 / 46.0.14 | 17 | (cd forge120 && bash ./gradlew build) | Yes, reobfuscated | Passed | Passed | Passed | Not tested |
| Forge | 1.20.6 / 50.2.15 | 21 | (cd forge1206 && bash ./gradlew build) | Yes | Passed | Passed | Passed | Not tested |
| Forge | 1.21 / 51.0.33 | 21 | (cd forge121 && bash ./gradlew build) | Yes | Passed | Passed | Blocked | Not tested |
| Forge | 26.1 / 62.0.9 | 25 | (cd forge261 && bash ./gradlew build) | Yes | Passed | Passed | Passed | Not tested |
| Forge | 26.2 / 65.1.6 | 25 | (cd forge262 && bash ./gradlew build) | Yes | Passed | Passed | Passed | Not tested |

Paper's download API has no bare `26.1` release; it exposes 26.1.1 and 26.1.2.
Spigot BuildTools `--rev 1.20` produced **1.20.1**, so that test is recorded as
1.20.1. Likewise, `--rev 1.21` produced 1.21.1 and `--rev 26.1` produced
26.1.2. No support is inferred for missing patch versions or NeoForge.

The [machine-readable local evidence](validation-2026-11-19.json) records tested
artifact hashes, result contents and local evidence directories. Original logs
and disposable worlds stay outside tracked sources; hosted CI has not run.

The modern Spigot artifact is now named `LoraxCustomTrees-spigot-mc1.20-26.3-6.2.jar`.
Earlier local runs used the filename `LoraxCustomTrees-spigot-modern-6.2.jar`;
both files have identical SHA-256 contents. The range identifies the intended
target range; only exact matrix entries have server evidence.

## What the tests establish

The dependency-free shared-engine suite passes 135,760 assertions over every
asset, all rotations, legacy mappings, strict parsing, obstruction preflight,
context boundaries and world isolation. Legacy bytecode targets Java 8.

Dedicated-server probes load all 40 assets and exercise actual native generation
hooks in disposable fresh worlds. They validate all 40 manual structures in all
four rotations, including cross-chunk footprints and native block data. Legacy
probes also inspect skull/flower-pot tile entities. Modern probes separately
report seeded candidate determinism and observed final-layout reproducibility.
Candidate checks pass; final layouts often differ when population order changes.
An observed equal layout is not a guarantee of final-layout determinism.

Forge 26.3 tests use a larger fresh-world sample because native vanilla decoration
can obstruct custom candidates and a small sample may contain no distinctive
custom decoration. This changes test coverage only, not candidate generation.

Two simultaneous unmodified-protocol clients joined Spigot 1.12.2 and 1.21.10, Forge 1.12.2,
Forge 1.20, Forge 1.21.10 and Forge 26.1 and remained connected for five seconds. These headless protocol
checks establish basic multiplayer handshakes without Forge client mods; they do
not establish rendered client gameplay. The pinned protocol harness does not yet
support 26.2/26.3. Forge 1.20.6 and 1.21 client validation remains incomplete
because the protocol decoder reported recipe/component-payload errors. A Forge
1.20.6 control with no Lorax or probe reproduces the decoder errors; these are
not evidence that Lorax requires a client mod. A rendered vanilla-client test is
still needed on these versions. Forge 1.20 hit a native shared-random-source race
during simultaneous handshakes; sequential handshakes with both players connected
together pass. The failed stress attempt remains recorded.

Forge 1.21 / 51.0.33 passes startup, asset and generation assertions, but leaves a
non-daemon scheduler alive after controlled shutdown. A control server with both
Lorax and the probe removed reproduces the same hang. This is an external runtime
blocker; forced termination is recorded as failure, never a successful shutdown.
On Windows, Forge 26.1/26.3 also log upstream native Netty availability errors
through their debug appender; generation and clean shutdown nevertheless pass.

All exact Paper entries in the matrix, plus Spigot 1.20.1, 1.21.1, 1.21.10,
26.1.2 and 26.2, additionally pass native generation in forest, birch forest,
taiga, savanna and dark forest, and generation 65,536 blocks from origin with
negative Z. Paper 26.3 and Spigot 1.21.1 also pass a real save/restart cycle.
Restart preserves the sampled existing layout and simulated diamond/glass player
edits. A newly created world with generation disabled stays undecorated. Other
adapters have not yet passed this broader suite.

This is automated server integration validation. Human gameplay acceptance,
WorldEdit export round trips, broader existing-world/player-building scenarios
and combinations with third-party world-generation mods remain unvalidated.
These are not fully gameplay-validated releases.

## Intentional platform differences

- Native generation order and writable neighborhoods differ. Trees are placed
  only when the full footprint is available and unobstructed; no deferred replay
  or forced chunk loading occurs. Selection remains 1–4 candidates per eligible
  biome, with original species distribution and seed inputs.
- Rotation is now a real quarter turn, including directional block states and
  check coordinates. The original implementation mirrored coordinates.
- Jungle remains manual-only, preserving the original automatic policy.
- Forge uses native configuration and commands; WorldEdit export is unavailable
  on Forge and legacy Spigot and returns an explicit message.
- Manual operator placement intentionally bypasses obstruction/terrain checks.
- Unexpected platform write errors abort and surface immediately. Complete
  preflight prevents expected boundary/mapping/obstruction partial writes; there
  is no crash-atomic transaction spanning Minecraft chunk saves.

## Legacy palette

`common/LegacyBlocks.java` performs repeatable conversion at load/placement time
from the canonical BO5 states. Every bundled state and rotation maps explicitly;
unknown materials/properties fail initialization rather than becoming air.

Logs/wood preserve species, orientation and bark. Leaves preserve species and
persistence; modern `distance` is represented by legacy decay flags (the server
may update CHECK_DECAY). Legacy fence connections, gate `in_wall` and vine `up`
are derived by the old engine rather than independent persisted states.
Waterlogged blocks and powered heads fail explicitly. Unpowered modern wall
heads map to legacy player-skull tiles; potted allium maps to the flower-pot tile
with RED_ROSE metadata 2. Buttons, torches, cocoa, slabs, melon and colored
terracotta use explicit block IDs/metadata. Structural supports are written before
attached decorations so legacy physics does not discard a torch prematurely.
The modern pre-powered-head API mapping omits only `powered=false`; it rejects
unsupported powered states.

## Test environment and artifact identity

Windows runs on the Btrfs-backed scratch drive intermittently failed with native
file-sharing errors while reading JARs, creating world directories or opening
logs. Fresh NTFS runs passed; the failing directories/logs are preserved. The
remaining exact final-artifact checks used NTFS. No security settings or volume
configuration were changed. Spigot 26.2 also exposed a removed deprecated
string-based game-rule API in the probe. The probe now uses the typed API and
fails its result when world-initialization listeners fail; the clean rerun is
the counted result.

The evidence includes both archive SHA-256 and a normalized payload SHA-256 over
entry names and uncompressed contents. Legacy reobfuscation can change ZIP
metadata while keeping every packaged file byte-identical. Payload comparison
links those runs explicitly; it does not replace archive checksums or release
signatures. Historical runs using earlier payloads remain marked separately from
final-artifact validation.

## Licensing and release gates

Root `LICENSE.md` is restrictive; no supported evidence confirms modification or
redistribution permission. The pinned EasyTrees gitlink and credits are retained.
Its upstream tree listing did not expose a license file during inspection; this
is unresolved provenance, not permission. Technical validation and authorization
to publish are distinct. No new distributions have been published.

The existing trusted Paper signing/release job still receives only root
`build/libs` JARs. Its Gradle task is explicitly scoped to `:build :common:check`
to avoid accidentally building/publishing newly introduced subprojects. Existing
SHA-256 and detached GPG signing behavior is preserved. Additional distributions
require explicit release gating after validation and license resolution.

