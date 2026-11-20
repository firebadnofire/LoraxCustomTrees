> Implementation update (2026-11-19): the sections below preserve the original
> pre-port investigation at revision 660f2cb. They are historical findings, not a
> description of the current working tree. Current builds, tests and remaining
> targets are recorded in [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md).
>
> Current code adds a Java-8-compatible common engine, callback-local Paper
> placement, native Spigot/Forge 1.12.2 adapters and version-specific native Forge
> integrations for 1.20, 1.20.6, 1.21, 1.21.10 and 26.1–26.3.
> Queues and delayed region placement have been removed from the active path.
> Forty canonical assets are shared; legacy palettes are converted explicitly.
> The normal JAR build and trusted signed Paper release workflow are preserved.
> License authorization remains unresolved. Candidate selection remains seeded;
> overlapping final layouts can depend on chunk population order.
# LoraxCustomTrees repository dissection

> Snapshot date: 2026-11-19. This document describes the checked-out source at
> `660f2cb2f85389cee468bf23bd5c8fc32082f0d5` on `main`, not the intended
> future architecture described in `AGENTS.md`.

## 1. Repository Snapshot

**LoraxCustomTrees** is a Java Paper/Bukkit server plugin that replaces some
vanilla biome tree population with pre-authored custom tree structures. It
loads 40 bundled BO5-like text assets, selects a species and variant during
new overworld chunk population, and writes their blocks into the server world.
It also provides player commands to place a named structure and, when
WorldEdit is installed, export a selected region to a BO5-like file.

| Item | Current implementation |
| --- | --- |
| Branch / revision | `main` / `660f2cb2f85389cee468bf23bd5c8fc32082f0d5` |
| Language and build | Java; Gradle wrapper 9.8.1; Java toolchain/release 25 |
| Server API | Paper API `26.3.build.+`; plugin descriptor has `api-version: 1.21` |
| Distribution today | One Paper/Bukkit plugin JAR (`Lorax-6.2.jar` expected from the standard `jar` task) |
| Runtime model | Server-side `JavaPlugin`, loaded at server startup |
| Stored state | Bukkit-generated `plugins/Lorax/config.yml`; exported BO5 files in the plugin data directory; in-memory static placement queues |
| Not present | Forge module, common module, Paper-version adapters, automated tests, runtime tests, a version-support matrix, or an initialized EasyTrees checkout |

The README accurately identifies Paper and Easy Trees as the project purpose,
but is stale in an important build detail: it instructs users to run
`shadowJar`, while `build.gradle` does not apply Shadow and current history
explicitly removed it. The checked-out build is a single modern Paper target;
it does **not** implement the Forge or Minecraft 1.12.2 support required as a
long-term objective in `AGENTS.md`.

The repository license (`LICENSE.md`) prohibits redistribution and even
source modification/reading. That conflicts with the repository's fork and
porting objective. This is a legal/governance question, not a technical
license determination: obtain explicit rights before redistributing modified
code or tree assets.

## 2. Quick Start for Contributors

Prepare the submodule before investigating its source or licensing:

```powershell
git submodule update --init --recursive
.\gradlew.bat build --no-daemon
```

On macOS/Linux use `./gradlew build --no-daemon`. The build requires a Java 25
toolchain because `build.gradle` sets both the toolchain and `options.release`
to 25. It resolves compile-only dependencies from Maven Central, Paper,
Sonatype, CodeMC, and EngineHub. `build` is the appropriate verification
command; `jar` creates the ordinary plugin JAR. Do not use `shadowJar` unless
Shadow is deliberately restored.

For local Paper experimentation, `.\gradlew.bat runServer` is configured to
download WorldEdit and start Minecraft `26.3`. It is a networked development
server task, not a substitute for dedicated-server compatibility testing.

Install the built JAR in a Paper server's `plugins/` directory and restart.
The descriptor's `load: STARTUP` means Lorax registers its world-initialization
listener early. Generate a new overworld to exercise automatic population.

Main starting points:

- `src/main/java/org/infotoast/lorax/Lorax.java` — plugin lifecycle.
- `src/main/java/org/infotoast/lorax/Populator.java` — chunk-population hook.
- `src/main/java/org/infotoast/lorax/customobject/CustomObjectReader.java` —
  structure parser.

### Validation performed for this dissection

Read-only checks completed: repository/status/revision inspection, full source
and resource inventory, loader-to-asset comparison, workflow/history review,
`git diff --check`, and `git fsck --no-dangling --no-reflogs`. The loader list
matches all 40 packaged BO5 assets. No Gradle task or server was run: the
available JDK is 21 while the build declares Java 25, and this documentation
task permits only `dissection.md` as a persistent change. Consequently, no
build or runtime success is claimed.

## 3. Repository Map

```text
.
├── AGENTS.md                         # Porting requirements and contributor constraints
├── README.md                         # End-user overview; build command is stale
├── build.gradle / settings.gradle     # One-module Paper build, version 6.2
├── gradle/                            # Gradle 9.8.1 wrapper
├── src/main/java/org/infotoast/lorax/
│   ├── Lorax.java                     # JavaPlugin entry point
│   ├── Populator.java                 # Bukkit BlockPopulator
│   ├── TreeSpawner.java / TreeHolder.java / TreeType.java
│   ├── WorldImproved.java             # Bukkit world facade and seeded randomness
│   ├── command/                       # /lorax subcommands
│   ├── customobject/                  # BO5-like model, parser, placement and export
│   └── util/                          # coordinates, terrain and material helpers
├── src/main/resources/
│   ├── plugin.yml / config.yml        # plugin metadata and default config
│   ├── *.bo5                          # 40 canonical runtime tree assets
│   └── splash.txt
├── EasyTrees/                         # Gitlink at 29a0a931…; empty/uninitialized here
├── .github/workflows/gradle.yml       # GitHub build artifact workflow
└── .forgejo/workflows/main.yml        # Forgejo build, signing, and prerelease publication
```

`build/` and `.gradle/` are ignored generated directories. No source-test tree
exists. The `EasyTrees` directory is a Git submodule declared in `.gitmodules`
as `https://github.com/FrankTCA/EasyTrees-Lorax`; because it is not initialized
in this checkout, its contents and license were not verifiable here. The
runtime assets currently used by Lorax are the tracked files under
`src/main/resources`, not files read from that submodule at runtime.

## 4. System Architecture

The code has three practical layers, but they are not platform-independent:
the tree policy, BO5 engine, and world facade all import Bukkit/Paper types.
There is no current boundary suitable for direct Forge reuse.

```mermaid
flowchart TD
  P[Paper loads plugin.yml] --> L[Lorax.onEnable]
  L --> C[saveDefaultConfig and CustomObjectLoader]
  C --> A[40 resources/*.bo5]
  L --> W[WorldInitEvent listener]
  W -->|enabled config| BP[Populator added to world]
  BP --> TS[TreeSpawner + TreeHolder]
  TS --> OP[ObjectPlacer global registry]
  OP --> PO[PlacedObject checks and direct Bukkit writes]
  L --> CMD[/lorax CentralCommandExecutor]
  CMD --> SP[spawn]
  CMD --> EX[export: WorldEdit optional]
```

| Component | Location and entrypoints | Responsibility and dependencies |
| --- | --- | --- |
| Lifecycle | `Lorax`, `Lorax.WorldListener` | Saves default config, constructs and then redundantly reloads `CustomObjectLoader`, registers `/lorax`, listens for `WorldInitEvent`, and conditionally adds one shared `Populator`. Depends directly on Bukkit/Paper. |
| Generation policy | `Populator`, `TreeSpawner`, `TreeHolder`, `TreeType` | Samples distinct biomes within a chunk, maps selected biome keys to five species, chooses 1–4 candidate locations per matching biome, selects a hard-coded object array, and registers candidates. |
| World/terrain facade | `WorldImproved`, `util/Utility` | Caches world name/seed, supplies deterministic random generators, accesses Bukkit worlds/blocks, and finds a ground surface using a hard-coded modern material list. |
| Structure engine | `customobject/` and `customobject/datatype/` | Loads, parses, represents, validates, rotates, queues, places, and exports BO5-like structures. It depends on Bukkit `Material`, `BlockData`, worlds, and `LimitedRegion`. |
| Commands | `command/` | Dispatches `spawn` and an optional-WorldEdit `export` under `/lorax`; checks Bukkit permissions in code. |
| Assets/config | `src/main/resources` | Bundles `plugin.yml`, default config, splash art, and tree structures into the JAR. |

Patterns actually used are a `JavaPlugin` lifecycle callback, a Bukkit
`BlockPopulator`, a command dispatcher with simple command classes, data
objects for parsed BO5 entries, and static registries/caches. There is no DI
container, persistence layer, service interface, or declared shared-platform
module.

## 5. Startup and Execution Flow

1. Paper reads `plugin.yml` and instantiates `org.infotoast.lorax.Lorax` at
   startup.
2. `Lorax.onEnable()` saves `config.yml` only if absent, initializes static
   `Lorax.plugin` and `Lorax.loader`, parses every hard-coded asset name in
   `CustomObjectLoader.loadObjects()`, registers `/lorax`, registers the world
   listener, and optionally prints `splash.txt`.
3. On each `WorldInitEvent`, the listener adds the single `Populator` when
   `enable-populator` is true and, if `select-worlds` is true, the exact world
   name is in `enabled-worlds`.
4. `Populator.populate()` handles normal-environment chunks. It gathers the
   distinct sampled biomes, invokes `TreeSpawner.doTreePlacement()` for each
   eligible biome, then asks `ObjectPlacer` to register the candidate objects.
5. `ObjectPlacer` indexes every accepted candidate by each chunk its bounding
   box spans. On later population calls it marks the current coordinate loaded;
   once all covered coordinates are marked, it schedules placement after 40
   ticks. `PlacedObject.runChecks()` is repeated, then `placeAll(LimitedRegion)`
   loops items. In the current implementation that overload calls each item's
   **world** placement method, so block writes go through Bukkit world access,
   rather than consistently through the supplied `LimitedRegion`.

Generation is intended to be deterministic for a given world seed and chunk:
`TreeSpawner` and random block selection use `WorldImproved.getHashedRand`.
The Bukkit-provided `Random` argument is not used. Tree selection is uniform
within a species array. Random locations use `nextInt(15)`, so the final local
coordinate (15) is never selected.

## 6. Functional Inventory

### Implemented and source-verified

| Capability | Principal path | Notes |
| --- | --- | --- |
| Automatic trees | `Populator` → `TreeSpawner` → `ObjectPlacer` | Applies only to `NORMAL` worlds whose populator is registered; no retrofit facility is implemented. |
| Biome/species rules | `Populator.doBiome()` | Birch: `birch_forest`, `old_growth_birch_forest`; oak: `forest`, `flower_forest`; spruce: `taiga`, `grove`, `snowy_taiga`; acacia: `savanna`, `savanna_plateau`; dark oak: `dark_forest`. |
| Tree variants | `TreeHolder`, `CustomObjectLoader`, 40 BO5 files | 9 oak, 6 birch, 6 spruce, 7 acacia, 4 dark-oak, and 8 jungle assets load. Jungle has an enum/asset array but no automatic jungle-biome case. |
| BO5-like parsing | `CustomObjectReader` | Supports metadata (`Author`, `Version`, `Description`, `MinHeight`, `MaxHeight`), `B`/`Block`, `RB`/`RandomBlock`, `GC`/`GroundCheck`, and `BC`/`BlockCheck` forms. |
| Constraints | `PlacedObject.runChecks`, check classes | Checks min/max absolute height and cacheable ground/block checks. Explicit `/lorax spawn` intentionally bypasses check enforcement via `CustomObjectCheck.check(center, world)`. |
| Manual spawn | `SpawnCommand` | `/lorax spawn <structure> [rotation]`; player-only, needs `lorax.spawn`; omitted rotation is deterministic. Invalid/missing names can propagate null failures. |
| Structure export | `ExportCommand`, `CustomObjectCreator`, `CustomObjectWriter` | `/lorax export <name>` needs player, `lorax.export`, WorldEdit selection, and WorldEdit present at executor construction. It overwrites `<plugin data folder>/<name>.bo5`. |
| WorldEdit absence | `ExportCommandNoWE` | The export subcommand reports a message when WorldEdit was absent at enable time; generation and spawn do not need it. |

### Documented, planned, and incomplete

- **Documented but stale:** README says `shadowJar`; default config says a
  `/loraxgen` command can generate trees. No `shadowJar` configuration or
  `LoraxGenCommand` exists in the checkout.
- **Planned only:** `AGENTS.md` requires native Forge, an API-neutral common
  layer, Paper/Spigot and Forge Minecraft 1.12.2 support, an asset conversion
  strategy, a support matrix, and cross-platform runtime validation. None is
  implemented in the current source tree.
- **Incomplete/fragile by code inspection:** `PlacedObject.placeByChunk()` is
  commented out; `ObjectPlacer.queueNext` and `LOCK` are unused; `okMats` and
  `notOkMats` in `Populator` are constructed but unused. These are facts, not
  proof of a currently user-visible failure.

## 7. Dependencies and Toolchains

| Dependency/tool | Scope | Constraint / use |
| --- | --- | --- |
| Java 25 | Required to compile current build | `build.gradle` requires toolchain and bytecode release 25. The inspected host has Java 21, so it cannot directly satisfy this declaration. |
| Gradle 9.8.1 | Required wrapper | `gradle/wrapper/gradle-wrapper.properties`; no lockfile or dependency verification metadata exists. |
| Paper API `26.3.build.+` | `compileOnly` | Dynamic version selector: reproducibility can drift as the repository publishes newer builds. |
| WorldEdit Core/Bukkit 7.4.6 | `compileOnly`, optional runtime integration | Needed only for active export. Core excludes `org.yaml`; Bukkit adapter is also compiled against. |
| run-paper 3.1.0 | Gradle plugin | Provides `runServer`, configured with WorldEdit download and Minecraft `26.3`. |
| EasyTrees submodule | Source/asset provenance, not current build input | Gitlink is pinned, but current runtime packages copied BO5 files and build files do not consume the submodule. Its license must be inspected after initialization. |

The project has no external database, HTTP service, environment-variable
configuration, client mod, or network protocol of its own. Repository access
is needed only for Gradle dependency/plugin resolution and `runServer` plugin
download.

## 8. Build and Packaging

`settings.gradle` defines one root project named `Lorax`; `build.gradle` applies
the standard Java and run-paper plugins. `processResources` substitutes only
the project version into `plugin.yml`. Standard Java packaging places compiled
classes and every `src/main/resources` asset in the JAR; no shading, relocation,
or custom artifact naming is configured.

```powershell
# Compile, run any configured tests (none currently exist), and package
.\gradlew.bat build --no-daemon

# Package the ordinary plugin artifact only
.\gradlew.bat jar --no-daemon

# Development Paper server; downloads its configured dependency
.\gradlew.bat runServer
```

Expected output is `build/libs/Lorax-6.2.jar` under the conventional Java
plugin naming. Treat that name as configuration-derived, not validated by a
successful local build in this review. Compile-only Paper and WorldEdit APIs
are not packaged. No Forge artifact or Minecraft-version build variant exists.

## 9. Configuration and State

`config.yml` is copied to the plugin data directory once through
`saveDefaultConfig()`. It defines:

| Key | Effect |
| --- | --- |
| `enable-splash-text` | Prints bundled splash art to the server console on enable. |
| `enable-populator` | Enables adding the block populator during world initialization. |
| `select-worlds` / `enabled-worlds` | When enabled, exact allow-list of world names. Default is `world`; otherwise all `NORMAL` worlds receive the populator. |
| `messages.*` | Player-facing command messages. `no-worledit` is a typo in code, while config defines `no-worldedit`; the no-WorldEdit export path can receive `null` and pass it to `colorize`. |
| `config-version` | Informational in current code; no migration or replacement logic reads it. |

Assets are resource streams, not administrator-editable files. `CustomObjectWriter`
exports new BO5 files to the plugin data folder, but `CustomObjectLoader` only
loads the hard-coded bundled resources, so exported files are not automatically
available to `/lorax spawn` without a code change/reload mechanism.

Process-lifetime state is in static collections in `WorldImproved` and
`ObjectPlacer`. It is neither persisted nor cleared on disable. In particular,
`ObjectPlacer` keys data and loaded coordinates only by `(chunkX, chunkZ)`, not
by world, and retains candidates/indexes after placement. Any redesign must
make world identity, eviction, lifecycle cleanup, and thread/lifetime semantics
explicit before changing population behavior.

## 10. Tests and Quality Assurance

There are no tracked Java test classes, test fixtures, test source sets, or
test-specific Gradle dependencies. `build` therefore provides compilation and
packaging validation, not meaningful behavioral coverage. The GitHub workflow
uses `jar`, which does not execute tests.

Minimum manual validation after a generation or asset change:

1. Build with Java 25 and inspect the JAR for `plugin.yml`, `config.yml`, and
   all BO5 resources.
2. Start the intended dedicated Paper server without WorldEdit; verify enable,
   configuration loading, and `/lorax` behavior. Repeat with the pinned
   WorldEdit version for export.
3. Create fresh worlds across each listed biome and multiple seeds; check tree
   selection, placement, terrain checks, edges, and overlapping chunks.
4. Restart/reload-cycle a server and generate more chunks to observe retained
   static state behavior. Test multiple normal worlds because indexes are not
   world scoped.
5. Test `/lorax spawn`, invalid input, permissions, and export overwrite
   behavior in a disposable server directory.

For any future Forge or 1.12.2 work, the runtime matrix demanded by `AGENTS.md`
must be created and passed; current compilation cannot demonstrate it.

## 11. CI/CD and Releases

Two independent workflow systems are present:

| Workflow | Trigger and runner | Build/release behavior |
| --- | --- | --- |
| `.github/workflows/gradle.yml` | Push/PR to `main`, GitHub Ubuntu, Zulu JDK 21 | Runs `jar` and uploads `build/libs` as `Lorax`. It is inconsistent with the current Java-25 build declaration unless a compatible toolchain is otherwise provisioned. |
| `.forgejo/workflows/main.yml` | Push/PR to `main`, Ubuntu runner in `node:24-bookworm` | Selects Java 25 for this repository, runs `./gradlew build --no-daemon`, and uploads non-source/non-javadoc plugin JARs. |

The Forgejo `release` job runs only for pushes to `main` after build. It
downloads artifacts into `dist`, creates `SHA256SUMS`, makes detached armored
GPG signatures for each JAR and manifest, verifies signatures/checksums, then
publishes a uniquely tagged (`build-<12 SHA>`) prerelease through Forgejo's
HTTPS API. Required secret names are `CI_KEY_B64`, `CI_KEY_PASSPHRASE`, and the
provided `github.token`; `RELEASE_AUDIENCE` is an optional variable that enables
OIDC token acquisition. The workflow deliberately does not check out repository
scripts in the signing job, reducing signing-secret exposure.

No deployment environment or stable-release process is defined. Release
artifacts do not identify Minecraft/platform in their filename, contrary to the
future requirement in `AGENTS.md`.

## 12. Development Conventions

- Source uses one package root, direct Bukkit static access, mutable
  `ArrayList`/`HashMap`, static globals, and conventional PascalCase classes.
  Match this style only for a narrowly scoped maintenance fix; do not mistake it
  for an intended Forge-ready abstraction.
- BO5 parsing is literal and order-independent per line. Preserve syntax and
  parser/writer compatibility when changing assets. The reader accepts the
  misspelled `RotateRandomy:` while the writer emits `RotateRandomly:`; this
  mismatch means writer output does not control rotation as intended.
- Keep resource name, loader entry in `CustomObjectLoader`, and `TreeHolder`
  array synchronized. The loader/resource correspondence was verified at this
  revision.
- Keep compile-only APIs out of the distribution. Do not add runtime Bukkit or
  WorldEdit classes to an eventual Forge JAR.
- Existing exceptions are often printed or rethrown as unchecked exceptions;
  parser failures can return `null`. New work should make failures explicit and
  test them, while preserving server-safe behavior.
- Generated `build/`, `.gradle/`, IDE files, archives, and local
  `gradle.properties` are ignored and must not be committed. Do not hand-edit
  generated JARs.

## 13. Where to Make Changes

| Goal | Begin with | Coordinated review |
| --- | --- | --- |
| Change enable/config behavior | `src/main/resources/config.yml`, `Lorax.java` | Message keys, world-init timing, backward-compatible config migration. |
| Change biome frequency/species | `Populator.java`, `TreeSpawner.java`, `TreeHolder.java` | Determinism, biome identifiers, all asset arrays, cross-chunk placement. |
| Add/change a tree asset | `src/main/resources/*.bo5` | `CustomObjectLoader.java`, relevant `TreeHolder` array, parser compatibility, asset licensing, manual generation tests. |
| Extend BO5 syntax | `CustomObjectReader`, `datatype/`, `CustomObjectWriter` | Regexes, serialization, rotations, checks, malformed-input behavior, asset migration. |
| Fix placement/terrain behavior | `ObjectPlacer`, `PlacedObject`, `WorldImproved`, `Utility` | LimitedRegion versus direct writes, world identity, chunk boundaries, lifecycle cleanup, performance. |
| Change commands/export | `command/`, `plugin.yml`, `config.yml` | Argument validation, permissions declared in descriptor, optional WorldEdit behavior and path safety. |
| Change build or release | `build.gradle`, wrapper, both workflow files | Java version alignment, dynamic dependency risk, artifact signing, no untrusted access to release secrets. |
| Add Forge/legacy support | First define actual common data/policy seams | Do not put Forge calls in current Bukkit engine or emulate Bukkit in Forge; establish separate version adapters and runtime matrix as required by `AGENTS.md`. |

## 14. Known Issues, Constraints, and Technical Debt

### Verified from the checkout

- Current code is modern Paper/Bukkit only. It cannot meet native Forge or
  Minecraft 1.12.2 claims without new adapters, asset/block mapping, and legacy
  build tooling.
- The active structure layer is Bukkit-coupled (`Material`, `BlockData`,
  `Bukkit`, `LimitedRegion`, `World`), so it is not reusable common logic yet.
- `ObjectPlacer` retains global, unbounded object/index/loaded-chunk state and
  has no world key or disable cleanup. This creates confirmed lifecycle and
  cross-world correctness risks.
- `PlacedObject.placeAll(LimitedRegion)` uses direct-world item placement;
  `CustomObjectBlock.place(..., LimitedRegion)` also writes the world before
  the region. This undermines the expected constrained chunk-generation API
  path and must be investigated before any performance or safety claim.
- `getHeightAt(WorldImproved, x, z)` begins at y=256 in normal worlds despite
  asset defaults allowing max height 318; modern height ranges need deliberate
  validation.
- The generation policy contains a jungle type/assets but no jungle biome
  mapping. The assets load yet are not automatically selected.
- `/lorax` with no arguments sends help and then indexes `args[0]`; export does
  not validate `args[1]`. Both are source-visible argument-handling hazards.
- The config's no-WorldEdit message key and code key differ (`no-worldedit`
  versus `no-worledit`).
- Tree rotation changes coordinate positions but does not rotate directional
  `BlockData`; comments/history indicate prior fence-rotation concerns, so
  directional asset behavior needs server validation.
- The dynamic Paper API selector and GitHub's JDK 21 create reproducibility/CI
  risk. README `shadowJar` instruction is obsolete.

### Reasonable inferences requiring runtime confirmation

Static placement state may grow with long exploration and may interfere between
multiple normal worlds with identical chunk coordinates. Delayed placement
receives a `LimitedRegion` from the population callback and then waits 40 ticks;
whether this is always valid on supported Paper versions must be verified before
changing behavior. No runtime test in this review establishes either outcome.

## 15. Important Historical Context

The BO5 interpreter and placement model were introduced in commit `006021c`
(May 2025). The hard-coded tree policy and species holder were introduced in
`be149ff` shortly afterward. Commit `6aba22e` added object settings and
rotation-related work; the current reader/writer spelling mismatch should be
treated as a compatibility-sensitive legacy detail. Commit `2755078` replaced
the old command system and deleted `LoraxGenCommand`; this explains the stale
config comment. Commit `6b10b70` made WorldEdit absence selectable at command
executor construction. More recent history moved to current Paper/Gradle
settings (`4974eaa`, `9e395fa`) and removed Shadow-related workflow use
(`93eb937`).

On 2026-11-19, repository-local work added `AGENTS.md` and replaced an earlier
Forgejo workflow with the current signed development-release process. That
document is authoritative for future direction, but its Forge/1.12.2 design is
not historical evidence that those capabilities exist today.

## 16. Contributor Preflight Checklist

- [ ] Confirm branch, clean working tree, Java 25, Gradle wrapper, and
  initialized EasyTrees submodule.
- [ ] Read `AGENTS.md`; if substantial work is proposed, check for `VISION.md`
  (none exists at this revision).
- [ ] Verify license/asset rights before copying, converting, or redistributing
  assets.
- [ ] Identify whether the change touches generation policy, Bukkit integration,
  BO5 representation, assets, commands, or release automation.
- [ ] Trace all affected source-to-resource and loader-to-holder relationships.
- [ ] Build with `build --no-daemon`; do not claim a target works until the
  intended dedicated server has been exercised.
- [ ] For generation changes, test fresh chunks, multiple biomes/seeds, chunk
  borders, multiple worlds, restart behavior, and no-WorldEdit behavior.
- [ ] Update configuration/docs/CI/version matrix only when the corresponding
  behavior and tests actually support the claim.

## 17. Open Questions

1. What license governs the pinned EasyTrees submodule and the 40 copied tree
   assets, and does it permit the planned Forge/legacy conversion and release?
2. Which exact Paper/Minecraft release does `26.3` represent, and are the
   `plugin.yml` API floor and Java-25 requirement intentional and compatible?
3. Has the current Paper JAR ever started and populated terrain successfully
   with the direct-world writes/delayed `LimitedRegion` use?
4. Should generation intentionally include jungle biomes, and what frequency
   and non-overlap semantics are desired when a chunk contains multiple eligible
   biomes?
5. What is the intended lifetime and world scoping of `ObjectPlacer` state?
6. Is `config-version` intended to drive migration, and should exported objects
   become loadable custom assets?
7. Which supported Paper/Spigot and Forge versions are contractual, and which
   runtime test environments will certify the mandatory Minecraft 1.12.2
   compatibility target?

