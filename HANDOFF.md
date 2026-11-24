# MaterialShop handoff (version 0.1.0)

## State

The 0.1.0 source is complete **and now compiles**. The GitHub Actions `Build` workflow runs
`mvn -B -ntp clean package` against the Paper 1.21.11 API with Java 21 on every push and is green, and
it verifies the jar (plugin.yml present, classes present, no unexpanded `${...}`) before uploading it.

Toolchain now matches MaterialClaim (handoff step 1, done):

| Thing | MaterialClaim | MaterialShop |
|---|---|---|
| Java | 21 (toolchain) | 21 (`java.version`, `release`) |
| Paper API | `1.21.11-R0.1-SNAPSHOT` | `1.21.11-R0.1-SNAPSHOT` (`paper.version`) |
| plugin.yml api-version | `'1.21'` | `'1.21'` |
| CI Java | 21 | 21 |

Nothing was tested on a live Paper server. See "Not done / untested" below.

## What changed this session

1. Imported the 0.1.0 source archive unchanged and pointed `pom.xml` at Paper 1.21.11 + Java 21, with the
   compiler, resources and jar plugins pinned.
2. Added a real CI pipeline: build, jar sanity check, artifact upload, and a `v*`-tag release step that
   attaches the jar. First run on branch `arena/7eff1135-materialshop2` was green; the jar is
   `target/MaterialShop-0.1.0.jar` (about 57 KB) and the artifact is `MaterialShop-jar`.
3. Re-read `Trade.java`, `Gui.java`, `GuiListener.java` and fixed five behaviour bugs (drag into the
   add-listing menu, double-confirm duplication, custom-quantity `cancel`, collect-box leftovers, and the
   missing `collect-no-room` message). Details in `CHANGELOG.md`.
4. Audited the trade path for duplication and item loss; the sequence in `Trade.execute` is sound (see
   the changelog note) and was left unchanged.

## Not done / untested

- No live server test at all: trades, menus, Bedrock/Geyser taps, permissions, persistence and the
  protection listeners are **unverified in game**.
- No automated tests. The README checklist is the acceptance test.
- The jar has not been opened on a server; it has only been verified structurally in CI.
- No MaterialClaim or WorldGuard API integration. Shop creation still asks other plugins by firing a
  `BlockBreakEvent`, which claim plugins normally cancel.
- Transaction log rotation is by size only; there is no daily rotation.
- Item placeholders in `messages.yml` are plain text replacement, not MiniMessage.

## Exact next steps, in order

1. Download the jar from the Actions artifact `MaterialShop-jar` (or the release once a `v0.1.0` tag is
   pushed) and drop it in `plugins/` on a Paper 1.21.11 (26.3) test server with Java 21.
2. Run `/shopadmin create test`, add listings through the menu, then work through the README manual test
   checklist in order. Watch the console and `plugins/MaterialShop/logs/transactions.log`.
3. Fix anything that breaks in the file listed below, commit to this branch, and let Actions rebuild.
   Keep the build green; do not change behaviour without adding it to `CHANGELOG.md`.
4. When the checklist is green, take screenshots, bump `pom.xml` to 0.1.1, push a `v0.1.1` tag and the
   workflow publishes the release with the jar attached.

## Which file does what

| File | Responsibility |
|---|---|
| `MaterialShop.java` | plugin bootstrap, config/messages, chat prompt map, transaction log writer |
| `ShopManager.java` | shops + collect boxes in memory, `shops.yml` / `collect.yml` persistence |
| `Shop.java`, `Listing.java` | data model; a listing owns clones of the item and the payment options |
| `Trade.java` | counting, room checks, the atomic trade and its rollback |
| `Gui.java` | every chest menu: lists, shop, purchase, manage, add listing, collect box |
| `GuiListener.java` | click/drag/close/chat routing for those menus |
| `ProtectionListener.java` | container protection, explosions, hoppers, adjacent chests, shop villagers |
| `ShopCommand.java` | `/shop` and `/shopadmin` plus tab completion |
| `Text.java` | colour codes, item names, icon helper |
| `src/main/resources/config.yml` | limits, blacklist/whitelist, value table, log size |
| `src/main/resources/messages.yml` | every player-facing string; add a key here for any new message |
| `.github/workflows/build.yml` | build, jar verification, artifact, release on `v*` |

## Continuation prompt (paste into the next agent)

You are continuing the Minecraft Paper plugin MaterialShop (barter shop, no money), branch
`arena/7eff1135-materialshop2` in the `Ahmed332009/MaterialShop2` repo. Read `HANDOFF.md`, `README.md`
and `CHANGELOG.md` first. The source compiles: the GitHub Actions `Build` workflow is green on Paper
1.21.11 / Java 21 and uploads `MaterialShop-0.1.0.jar`. Nothing has been tested in game. Do this:
(1) read the README manual test checklist and the protection code, and look for anything that would
duplicate, lose or leak items that the static audit missed; (2) run the checklist on a real Paper server
if you have one, fix what breaks, commit to this branch only and keep the build green;
(3) extend `CHANGELOG.md`, `HANDOFF.md` and the README checklist with what you found; (4) optionally add
the MaterialClaim/WorldGuard territory hooks listed as next steps; (5) report build status, jar path and
what is still untested. Do not switch branches, do not add Vault or an economy plugin, and do not
overwrite the separate `Ahmed332009/MaterialShop` repo.
