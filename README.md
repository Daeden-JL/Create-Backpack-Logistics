# Create: Backpack Logistics

A NeoForge 1.21.1 addon that bridges **Create** (6.x) logistics networks, **Create Mobile Packages** and
**Sophisticated Backpacks** with new backpack upgrade modules.

Built against Create `6.0.10`, Create Mobile Packages `0.7.6`, Sophisticated Backpacks `3.25.64`,
Sophisticated Core `1.4.59` on NeoForge `21.1.219+`.

## Modules

### Auto-Unpacker Upgrade
Insert into a backpack and it watches for Create packages addressed **to you** (CMP convention: the
package address is your player name, or anything ending in `@YourName`) sitting in your inventory or
inside the backpack, and unpacks their contents straight into the backpack.

- Contents go through the backpack's normal insert pipeline, so filters/void/compacting upgrades apply.
- If not everything fits, it unpacks what it can and leaves the rest in the package.
- The settings tab has an optional **extra address filter** (supports Create's `*` globs) so a backpack
  can also claim packages addressed to e.g. `MyBase*` — this also works while the backpack is placed as a block.
- Toggle it on/off with the standard Sophisticated Backpacks upgrade switch.

### Stock Caller Upgrade (basic and advanced)
Keeps the backpack stocked from a Create logistics network, delivered to you by Create Mobile Packages robo
bees — and includes the Auto-Unpacker's behaviour, so arriving packages are unboxed automatically.

1. Hold the upgrade item and **right-click a Stock Link / Stock Ticker** (any logistically-linked block) to tune it
   to that network. Sneak-right-click in the air to unlink.
2. Put it in a backpack, open its settings tab and set:
   - **Call below** — the lower threshold; dropping under it triggers a call.
   - **Fill to** — the upper threshold; each call orders enough to reach it (the range gap is the call size).
   - The item filter (1 slot on basic, 9 on advanced).
3. Every 5 seconds it checks the range; when below the lower bound, it broadcasts a package request.
   Carried, requests are addressed to your player name and a Bee Port dispatches a robo bee to you;
   either way the upgrade unboxes deliveries straight into the backpack.
4. On the advanced version each of the nine filter slots has its own independent range — press the
   **Slot** selector to switch which slot's thresholds you're editing.

Once stock drops below the lower bound, that slot keeps calling on every check until it actually
reaches the upper bound, even across several checks — so a call that undershoots (limited network
stock, a busy packager, a partial delivery) doesn't strand it partway through the range. The Sender
mirrors this: once above the upper bound it keeps draining until it's back down to the lower bound.

Number selectors: left-click +, right-click −, scroll to adjust; Shift = ×8, Ctrl = ×64.

Anti-spam accounting: items already en route are "promised" for 60s, and items still boxed in packages
addressed to you (in your inventory or the backpack) count as stock — so it won't double-order while a
delivery is in flight.

### Sender Upgrade (basic and advanced)
The outbound counterpart: watches the backpack for filtered items **above a threshold** and ships the
excess to a **named Bee Port**.

1. Link it to a network the same way (right-click a Stock Link / Stock Ticker).
2. In the settings tab set the item filter (1 slot basic / 9 advanced), the range — **Send above** (upper
   threshold that triggers sending) and **Keep** (what remains after a send) — and the **target Bee Port
   address** (matched against each Bee Port's address filter).
3. Every 5 seconds it trims filtered items exceeding the upper bound down to the Keep amount (up to 9
   stacks per batch). Carried, the excess goes to your Create Mobile Packages outbox and a robo bee
   collects it from you; if the outbox is still occupied it waits for the previous batch first (outbox
   items are visible in the Portable Stock Ticker's send menu).

### Placed backpacks (not worn)
Modules stay active when the backpack sits in your inventory without being worn — SB ticks upgrades for
carried backpacks either way. When the backpack is **placed as a block**, the modules switch to working
through Bee Ports instead of robo-bee-to-player delivery:

- **Auto-Unpacker** — unpacks packages matching its extra address filter (as before).
- **Stock Caller** — set its **Deliver to (when placed)** address; calls are addressed there, and a Bee
  Port with a matching address filter placed next to the backpack pushes the delivery into it, where it
  is unpacked automatically.
- **Sender** — boxes the excess into an addressed package left inside the backpack; a Bee Port next to
  the backpack pulls it (ports pull packages from adjacent inventories) and ships it to the target port.

### Contraptions (trains) and Sable ships
- **Sable entities** (Aeronautics ships etc.): blocks on a Sable craft keep ticking like normal blocks and
  CMP's Bee Ports natively support Sable sub-levels, so a placed backpack + adjacent Bee Port works there
  exactly like on the ground — calls arrive at the ship's port, senders dispatch from it.
- **Train/classic contraptions**: the SB Create integration ticks mounted-backpack upgrades while moving.
  The modules look for a **player riding the same contraption** and treat them as the carrier — calls are
  delivered to the rider by robo bee (bees track moving players) and unpacked into the mounted backpack,
  and senders hand their batches to a bee that collects from the rider. Without a rider they fall back to
  address mode; note that a Bee Port mounted on a classic (non-Sable) contraption is inert while moving —
  that's a CMP limitation, so use the rider flow (or Sable) for moving bases.

> **Note:** deliveries in both directions require you to be added to the network via CMP's network settings,
> and the network needs Bee Ports stocked with robo bees. Create Mobile Packages is a required dependency.

## Integrations

- **JEI / REI** — the crafting recipes appear automatically in both; each upgrade additionally has an
  "Information" page describing how to use it (JEI: recipe catalyst info; REI: information display).
- **Ponder** — hold W (Create's ponder key) over any of the upgrades for an animated scene explaining
  the module: Auto-Unpacker, Stock Caller (shared by both tiers) and Sender (shared by both tiers), each
  staged with Bee Ports, tickers and packagers.
- Recipe-book unlock advancements: recipes pop up as you acquire the ingredients of each tier.

## Installation and sidedness

This is a **content mod** (items, upgrades, recipes), so like Create and Sophisticated Backpacks themselves it
**must be installed on the server** (and on clients for the settings GUIs). It cannot run single-sided:
recipes, items and all upgrade logic live on the server; the client side only renders the settings tabs.

It degrades gracefully instead of blocking connections:

- Client has the mod, server doesn't → you can still join; on login you get a chat notice that the server
  is missing the mod (its items/recipes simply won't exist in that world).
- Server has the mod, client doesn't → if the client can join, it receives a chat notice that upgrade
  settings tabs won't be usable.

## Crafting

- **Auto-Unpacker**: upgrade base + 3× cardboard (Create) + hopper
- **Stock Caller**: **Auto-Unpacker** + transmitter (Create) + 2× redstone + cardboard
- **Advanced Stock Caller**: Stock Caller + 3× transmitter + diamond
- **Sender**: upgrade base + transmitter + 2× hopper + cardboard
- **Advanced Sender**: Sender + 3× transmitter + diamond

## Releases and auto-updates

Releases are published automatically: pushing a tag `vX.Y.Z` builds the mod in CI and creates a
GitHub release carrying the jar, a table of required dependencies, and a machine-readable
`dependencies.json` used by the in-game updater.

- **Servers** check the latest GitHub release on startup. If a newer version exists **and** every
  dependency it declares is installed in a matching version, the new jar is downloaded (checksum-
  and content-verified), the old jar replaced, and the update takes effect on the next restart.
  Online operators are notified in chat.
- **Clients** report their mod version when joining a server with this mod. If the server has a
  newer version (running, or freshly downloaded and pending restart), it streams the jar to the
  client, which verifies it and swaps its own copy — taking effect on the next game launch.
- All of it can be turned off in `config/backpack_logistics-common.toml`:
  `checkForUpdates`, `autoDownloadUpdates` (server side), `acceptUpdatesFromServer` (client side).

> Security note: auto-update means executable code is fetched from this repo's releases (server)
> or from the server you join (client). Downloads are SHA-256-verified against the release digest
> or the transfer header, and rejected unless they are a valid jar of this very mod with the
> expected version. If that trade-off isn't right for your setup, disable the config toggles.

Cutting a release: bump `mod_version` in `gradle.properties`, commit, then
`git tag vX.Y.Z && git push origin vX.Y.Z`.

## Building

Requires JDK 21 (Gradle resolves everything else). Dependencies come from public mavens
(createmod.net, Modrinth, blamejared, shedaniel); Ponder/Flywheel/Registrate are extracted at build
time from the Create jar's bundled libraries, so versions always match the targeted Create release
(see `gradle.properties` to bump versions):

```
gradle build
```

Output: `build/libs/create_backpack_logistics-<version>.jar` — drop it into your instance's `mods/`
folder (client **and** server).

To smoke-test on a dev server: place Create, Sophisticated Core, Sophisticated Backpacks and
Create Mobile Packages jars into `run/mods/`, accept `run/eula.txt`, then `gradle runServer`.

## Project layout

- `src/main/java/dev/dae/backpacklogistics/` — mod sources
  - `upgrades/autounpacker/`, `upgrades/stockcaller/`, `upgrades/sender/` — the modules
    (advanced tiers reuse the same classes with 9 filter slots)
  - `client/gui/` — upgrade settings tabs and widgets
  - `client/ponder/` — Ponder plugin and animated scenes
  - `compat/jei/`, `compat/rei/` — recipe viewer information pages
- `src/main/resources/assets/backpack_logistics/ponder/` — generated scene structure templates
- `run/` — dev-server working dir (gitignored; test mods staged in `run/mods/`)
