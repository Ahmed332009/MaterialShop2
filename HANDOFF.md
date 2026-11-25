# MaterialShop handoff (version 0.2.0)

## State

The 0.2.0 source (GUI market + escrow, optional chest shops) is complete **and compiles**. The GitHub
Actions `Build` workflow runs `mvn -B -ntp clean package` against the Paper 1.21.11 API with Java 21 on
every push, verifies the jar, uploads it and can publish a release; the `Smoke test` workflow boots a real
Paper 1.21.11 server with the jar and drives a few console commands.

Toolchain matches MaterialClaim (handoff step 1, done):

| Thing | MaterialClaim | MaterialShop |
|---|---|---|
| Java | 21 (toolchain) | 21 (`java.version`, `release`) |
| Paper API | `1.21.11-R0.1-SNAPSHOT` | `1.21.11-R0.1-SNAPSHOT` (`paper.version`) |
| plugin.yml api-version | `'1.21'` | `'1.21'` |
| CI Java | 21 | 21 |

No player has used this build on a server yet - see "Not done / untested".

## What changed this session

1. Imported the 0.2.0 archive unchanged, then applied handoff step 1: Paper 1.21.11 + Java 21, pinned
   compiler/resources/jar plugins so `release 21` and resources filtering are explicit.
2. Kept the CI pipeline: build, jar verification, artifact, release on `v*` tags, plus the real-server
   smoke test.
3. Re-read `Trade.java`, `Gui.java` (`SetupMenu`, `ListingManageMenu`, `CollectMenu`) and `ShopManager.java`
   (`market`, `expire`) for item loss and duplication, and fixed what it turned up. Details in
   `CHANGELOG.md`; the short version:
   - a second click on Confirm could create two listings from one set of template items;
   - market escrow units could be credited without the items actually leaving the player's inventory;
   - the same ignored-result issue in "add stock";
   - drags could not reach the add-listing slots;
   - `cancel` was documented but rejected in the quantity prompt;
   - the collect box could swallow leftover stacks when the inventory was full;
   - `market.yml` was not saved again on shutdown.
4. The trade engine itself needed no change: market stock is a unit counter, chest shops still take items
   out of the live container, and every failure path restores the buyer's inventory snapshot.

## Not done / untested

- No player-driven test: the market menus, buying, escrow, add stock, cancel, expiry, the collect box,
  Geyser/Bedrock taps, permissions and protection listeners are **unverified in game**. Only the console
  path is covered by the smoke test.
- No automated unit tests. The README checklist is the acceptance test.
- No price editing (cancel and relist), no item-name search, no MaterialClaim or WorldGuard API hook.
- Expiry only runs every 5 minutes and silently skips listings when the seller's collect box is full.
- Market listings have no "last seen" notification for offline sellers beyond the collect box.

## Exact next steps, in order

1. Download `MaterialShop-0.2.0.jar` from the release or the `MaterialShop-jar` artifact and put it in
   `plugins/` on a Paper 1.21.11 (26.3) server with Java 21.
2. Create a server shop with `/shopadmin create test`, add a listing, then work through the README manual
   test checklist: market listing with `units > 1`, buy it, add stock, cancel a listing, expiry, collect
   box full, chest shop protection, chest shops disabled.
3. Fix what breaks in the file listed below, commit to this branch, let Actions rebuild, and add a line to
   `CHANGELOG.md` for every fix. Keep the build green.
4. When the checklist is green, take screenshots, bump `pom.xml`, push a `v*` tag and the workflow
   publishes the release with the jar.

## Which file does what

| File | Responsibility |
|---|---|
| `MaterialShop.java` | plugin bootstrap, config/messages, chat prompt map, transaction log writer, expiry timer |
| `ShopManager.java` | shops, chest shop index, collect boxes, market listings and their three YAML files |
| `Shop.java`, `Listing.java` | data model; a listing owns clones of the item and the payment options, plus market seller/units/created |
| `Category.java` | market tab mapping (config override, named items, name heuristic) |
| `Trade.java` | counting, room checks, the atomic trade and its rollback, for market and shop listings |
| `Gui.java` | every chest menu: main, market, my listings, listing manage, add listing, purchase, shop, manage, list, collect |
| `GuiListener.java` | click/drag/close/chat routing for those menus |
| `ProtectionListener.java` | chest shop protection, explosions, hoppers, adjacent chests, shop villagers |
| `ShopCommand.java` | `/shop` and `/shopadmin` plus tab completion |
| `Text.java` | colour codes, item names, icon helper |
| `src/main/resources/config.yml` | limits, market caps and expiry, blacklist/whitelist, value table, category overrides |
| `src/main/resources/messages.yml` | every player-facing string; add a key here for any new message |
| `.github/workflows/build.yml` | build, jar verification, artifact, release on `v*` |
| `.github/workflows/smoke.yml` | boots a real Paper server with the jar and asserts the log |
| `.github/release-notes.md` | body of the GitHub Release published on a `v*` tag |

## Continuation prompt (paste into the next agent)

You are continuing the Minecraft Paper plugin MaterialShop (GUI barter market, no money), branch
`arena/7eff1135-materialshop2` in the `Ahmed332009/MaterialShop2` repo. Read `HANDOFF.md`, `README.md` and
`CHANGELOG.md` first. The 0.2.0 source compiles against Paper 1.21.11 / Java 21, the `Build` workflow is
green and a smoke test boots a real Paper server where the plugin loads, registers its commands and
persists data. Nothing has been clicked by a player yet. Do this: (1) run the README checklist on a real
server, starting with a market listing that has more than one unit, buying it, adding stock and cancelling;
(2) fix what breaks, keeping the escrow arithmetic exact (`units * item.getAmount()` in and out);
(3) re-check `Trade.execute`, `Gui.SetupMenu`, `Gui.ListingManageMenu` and `ShopManager.expire` for item
loss or duplication after each fix; (4) keep the Actions build green and update `CHANGELOG.md` and
`HANDOFF.md`; (5) report build status, jar path and what is still untested. Do not switch branches, do not
add Vault or an economy plugin, and do not touch the separate `Ahmed332009/MaterialShop` repo.
