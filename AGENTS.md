\# AGENTS.md — LoraxCustomTrees



\## Project Purpose



This repository is a fork of `FrankTCA/LoraxCustomTrees`, a Minecraft server plugin that introduces custom trees through world-generation logic.



The original project uses EasyTrees for tree content while Lorax handles generation and integration with the Minecraft server.



The primary objective is to port LoraxCustomTrees to native Minecraft Forge while preserving the original Spigot/Paper implementation and build system.



Both platforms must share a common codebase, provide equivalent world-generation behavior, and maintain Minecraft-version parity.



\*\*Minecraft 1.12.2 support is a mandatory development target for both platforms.\*\*



The Forge edition must operate as a native server-side mod without Bukkit, Paper, or Spigot dependencies.



\## Core Requirements



1\. Preserve the original Spigot/Paper plugin.

2\. Add a native Forge server implementation.

3\. Share platform-independent tree generation and placement logic.

4\. Preserve the project's existing tree assets and generation behavior.

5\. Retain the EasyTrees dependency or an appropriately adapted representation of its content.

6\. Support equivalent configuration and behavior on both platforms.

7\. Maintain Minecraft-version parity wherever technically feasible.

8\. Add Minecraft 1.12.2 support to both platforms.

9\. Produce separate installable plugin and mod artifacts.

10\. Avoid creating two independently maintained implementations of the project.



\## Architecture



Separate shared tree-generation rules from the underlying server APIs.



Suggested conceptual layout:



```text

common/

&#x20; Tree definitions

&#x20; Placement rules

&#x20; Biome-selection rules

&#x20; Configuration models

&#x20; Generation parameters



paper/

&#x20; Bukkit/Paper adapters

&#x20; World-generation integration

&#x20; Plugin initialization

&#x20; Plugin configuration



forge/

&#x20; Forge world-generation integration

&#x20; Mod initialization

&#x20; Forge configuration

&#x20; Server lifecycle integration



assets/

&#x20; Shared tree assets and templates



build.gradle

settings.gradle

```



This is a conceptual layout, not a mandatory restructuring instruction.



Inspect the existing project before selecting the final structure.



\### Shared Logic



Shared code should determine:



\- Which trees may generate.

\- Where they may generate.

\- Biome eligibility.

\- Generation probability and distribution.

\- Placement constraints.

\- Configuration behavior.

\- Selection of tree structures and variants.



Shared code must not depend directly on Bukkit, Paper, Spigot, Forge, or version-specific Minecraft classes.



\### Platform Adapters



Each implementation must handle its own:



\- Server initialization.

\- World-generation hooks.

\- Biome and world access.

\- Block placement.

\- Configuration integration.

\- World/chunk lifecycle events.

\- Structure or template loading.



Do not emulate Bukkit inside Forge.



Use native Forge APIs for Forge-specific world generation.



Keep platform adapters thin, but do not force fundamentally different world-generation systems through an unsuitable abstraction.



\## EasyTrees Integration



The upstream repository contains an EasyTrees Git submodule.



Preserve this relationship unless investigation demonstrates a better architecture is necessary.



Before modifying its integration:



1\. Inspect the submodule and its license.

2\. Identify the tree assets and formats used.

3\. Determine how the existing Paper implementation loads them.

4\. Determine whether the same assets can be used by Forge.

5\. Establish how tree assets must be converted for Minecraft 1.12.2.

6\. Preserve tree appearance and placement characteristics wherever feasible.



Prefer sharing one canonical set of tree assets.



Where Minecraft versions require incompatible structure formats, create version-specific converted outputs.



Do not maintain hand-edited copies of every tree unless unavoidable.



Do not silently omit trees that fail to convert.



Do not modify or redistribute third-party assets without checking the relevant license terms.



\## World-Generation Requirements



The project must preserve the original generation experience.



Important properties include:



\- Tree selection.

\- Biome compatibility.

\- Generation frequency.

\- Tree placement.

\- Terrain suitability.

\- Tree structure integrity.

\- Configuration options.

\- World-generation performance.



Do not substitute ordinary vanilla trees for custom trees merely to establish Forge compatibility.



Do not implement Forge support through Bukkit compatibility layers.



Do not require the mod on Minecraft clients unless a specific feature genuinely requires client-side content and that requirement has been explicitly approved.



\### World Safety



World generation must avoid:



\- Uncontrolled chunk loading.

\- Recursive world-generation loops.

\- Repeated generation in the same location.

\- Unnecessary chunk regeneration.

\- Destruction of existing player structures.

\- Excessive main-thread workloads.

\- Non-deterministic behavior where deterministic generation is expected.

\- Corruption caused by partially placed structures.



Respect chunk boundaries, placement constraints, and the target version's world-generation lifecycle.



Do not automatically retrofit existing chunks unless that behavior is explicitly requested.



\## Minecraft 1.12.2 Support



Minecraft 1.12.2 is a required compatibility target.



This version predates major changes to Minecraft's world-generation, biome, block-state, and structure systems.



Consequently, a modern Forge integration cannot simply be compiled against Forge 1.12.2.



Implement a proper legacy adapter.



Investigate:



\- Forge 1.12.2 world-generation registration.

\- Legacy biome identifiers and biome-dictionary behavior.

\- Pre-flattening block IDs and metadata.

\- Legacy structure and schematic formats.

\- Block palette conversion.

\- Chunk generation and population ordering.

\- World height and placement limits.

\- Java 8 and legacy ForgeGradle requirements.



Use the appropriate Forge 1.12.2 APIs and build tooling.



For the Paper/Spigot edition, implement a corresponding Minecraft 1.12.2 compatibility target.



Do not assume that current Paper APIs are available in older Spigot versions.



Avoid silently converting unsupported blocks to unrelated materials.



Where a modern tree contains blocks unavailable in Minecraft 1.12.2, document the limitation and implement an intentional, visually appropriate compatibility mapping only when justified.



Preserve tree identity and appearance as closely as possible.



\## Version Parity



Maintain a documented version support matrix containing:



\- Minecraft version.

\- Spigot/Paper compatibility.

\- Forge compatibility.

\- Java requirements.

\- Build status.

\- Runtime test status.

\- Tree asset compatibility.

\- Known generation differences.



For versions claimed as fully supported, both platform distributions must build and pass available runtime validation.



Minecraft 1.12.2 must appear in the matrix as a required target.



Do not claim a Minecraft version is fully supported simply because the Java source compiles.



Use separate version-specific adapters when Minecraft APIs differ significantly.



Do not introduce NeoForge as a replacement for the required Forge platform.



Do not assume all versions can share one universal mod JAR.



\## Build System



Preserve the existing Gradle-based Paper build system.



The upstream documented build command is:



```bash

./gradlew shadowJar

```



This build path must remain available or receive a clearly documented, functionally equivalent replacement that preserves the original plugin distribution.



Requirements:



\- Paper and Forge distributions must build independently.

\- The EasyTrees submodule must be handled correctly.

\- Shared tree assets must be packaged appropriately.

\- Each distribution must contain the resources it requires.

\- Different Minecraft versions may use separate build configurations.

\- Legacy Forge builds must use compatible Java and Gradle tooling.

\- Avoid incompatible toolchain upgrades.

\- Do not accidentally package Bukkit APIs into Forge artifacts.

\- Do not package Forge APIs into Paper artifacts.



When required, maintain isolated Gradle builds for legacy Minecraft versions while sharing source code and assets.



Do not force incompatible legacy and modern toolchains into a single build configuration.



\## CI/CD



Preserve existing Forgejo CI and release conventions.



The long-term CI objective is to validate all supported Minecraft/platform combinations.



One Linux runner is sufficient; jobs may execute sequentially.



Use appropriate isolated Java environments per target.



Expected build artifacts should clearly identify:



\- LoraxCustomTrees.

\- Platform.

\- Minecraft version.

\- Project version or Git commit.



All published distributable artifacts must follow the established signing and verification conventions:



\- SHA-256 checksum manifest.

\- Detached GPG signatures for each distributable JAR.

\- Detached GPG signature for the checksum manifest.



Never expose signing credentials to untrusted pull requests.



Do not publish artifacts for targets that fail compilation or packaging validation.



\## Testing and Validation



Test both platform distributions against their intended dedicated servers.



At minimum, verify:



1\. Plugin/mod startup.

2\. Configuration loading.

3\. Tree asset loading.

4\. Biome selection.

5\. Tree generation frequency.

6\. Tree placement.

7\. Chunk boundary behavior.

8\. Generation reproducibility.

9\. Multiplayer server compatibility.

10\. Dedicated-server operation without a client mod.



Additional tests should cover:



\- Fresh world generation.

\- Existing worlds.

\- Multiple world seeds.

\- Different biome types.

\- Chunk generation at long distances.

\- Structures crossing chunk boundaries.

\- Unsupported or missing block mappings.

\- Potential compatibility problems with other world-generation modifications.



Do not claim runtime compatibility based solely on successful compilation.



Record which Minecraft versions and platforms were actually tested.



\## Agent Working Rules



\- Inspect the original implementation before refactoring.

\- Preserve the original Paper distribution.

\- Keep shared logic centralized.

\- Preserve existing tree content and behavior.

\- Treat EasyTrees as a distinct asset dependency.

\- Avoid duplicating tree definitions unnecessarily.

\- Use native Forge APIs.

\- Do not introduce mandatory client-side requirements.

\- Do not modify world-generation semantics without justification.

\- Do not silently remove tree variants or biome support.

\- Do not replace legacy support with modern-only APIs.

\- Do not modernize dependencies unless necessary.

\- Do not modify unrelated repository files.

\- Do not change release automation without a demonstrated need.

\- Inspect licensing terms before redistributing modified code or tree assets.



After significant changes, report which platform and Minecraft-version combinations compile, which have passed runtime tests, and which remain incomplete.



\## Development Priority



1\. Establish a clean baseline of the original Paper build.

2\. Inspect existing world-generation logic and EasyTrees assets.

3\. Identify code and assets suitable for sharing.

4\. Introduce shared logic with platform-specific adapters.

5\. Preserve and validate the original Paper implementation.

6\. Implement native Forge support for Minecraft 1.12.2.

7\. Implement Paper/Spigot support for Minecraft 1.12.2.

8\. Validate tree structure conversion and legacy block mappings.

9\. Extend Forge support to the remaining supported Minecraft versions.

10\. Establish a complete platform/version compatibility matrix.

11\. Integrate builds, tests, and release artifacts into Forgejo CI.



Do not declare the Forge port complete until native Forge world generation is working and the required Minecraft 1.12.2 compatibility targets have been validated.



\*\*Long-term objective:\*\* LoraxCustomTrees remains one maintained project with shared tree definitions and generation rules, producing native Spigot/Paper and Forge server distributions with equivalent gameplay behavior and aligned Minecraft-version support.



