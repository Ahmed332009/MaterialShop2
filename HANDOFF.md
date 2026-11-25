# MaterialShop handoff (version 0.3.0)

## State
Complete source for the GUI market design: Market, My listings, Server shops, optional chest shops, collect
box, escrow, expiry, transaction log, plus the pre-listed `catalog.yml` server shops (6 shops, 168 items).

**Compiles and loads.** The `Build` workflow runs `mvn -B -ntp clean package` against the Paper 1.21.11 API
with Java 21, verifies the jar, uploads it and publishes a release on `v*` tags. The `Smoke test` workflow
boots a real Paper 1.21.11 server with the jar, runs console commands and asserts that the catalog loads as
`6 shops, 168 items` with no skipped lines. No player has used the menus yet.

Toolchain matches MaterialClaim: Java 21, `paper-api 1.21.11-R0.1-SNAPSHOT`, `api-version: '1.21'`, CI Java 21.

## Done this session

1. Copied the Java and Paper API versions from MaterialClaim into `pom.xml` (1.21.11 / 21); `plugin.yml` and
   the workflow already matched.
2. Built and verified the jar in Actions (first run green), kept the release-on-tag step, and extended the
   real-server smoke test to cover the catalog.
3. Re-read `Trade.java`, `Gui.java` (`SetupMenu`, `ListingManageMenu`, `CollectMenu`) and `ShopManager.java`
   (market storage, `expire`, `loadCatalog`) and fixed what it turned up - see `CHANGELOG.md`. The catalogue
   files: every one of the 168 lines parses, catalog shops are read-only and never saved to `shops.yml`.

## Next steps, in order
1. Download `MaterialShop-0.3.0.jar` from the release or the `MaterialShop-jar` artifact and put it in
   `plugins/` on a Paper 1.21.11 (26.3) server with Java 21.
2. Open `/shop`, browse Server shops, buy one catalog item, then work through the README checklist: market
   listing with more than one unit, add stock, cancel, expiry, collect box full, chest shop protection,
   chest shops disabled, `/shopadmin reload` after editing `catalog.yml`.
3. Fix what breaks in the file listed below, commit to this branch, let Actions rebuild, and add a line to
   `CHANGELOG.md` for every fix. Keep the build green.
4. When the checklist is green, take screenshots, bump `pom.xml`, push a `v*` tag and the workflow publishes
   the release with the jar.

## Files
- Trade.java: trade engine for market and shop listings.
- Gui.java: all menus (MainMenu, MarketMenu, MyListingsMenu, ListingManageMenu, SetupMenu, PurchaseMenu, ShopMenu, ManageMenu, ListMenu, CollectMenu).
- ShopManager.java: storage for shops.yml, market.yml, collect.yml.
- src/main/resources/catalog.yml: pre-listed server shops (6 shops, 168 items). Parsed by
  ShopManager.loadCatalog()/parseCatalogLine()/parseStack(); malformed lines are skipped and logged.
- Category.java: market tab mapping.
- ProtectionListener.java, GuiListener.java, ShopCommand.java, MaterialShop.java, Text.java, Listing.java, Shop.java.

## Not done / untested
- No player-driven test: menus, buying, escrow arithmetic, add stock, cancel, expiry, the collect box,
  Geyser/Bedrock taps, permissions and chest shop protection are unverified in game.
- No automated unit tests; the README checklist is the acceptance test.
- No price editing in the menu (cancel and relist; catalog prices live in `catalog.yml`), no item-name
  search, no MaterialClaim or WorldGuard API hook, no catalog buy-back.

## Continuation prompt
You are continuing the Minecraft Paper plugin MaterialShop (GUI barter market + pre-listed server shop
catalog, no money), branch `arena/7eff1135-materialshop2` in the `Ahmed332009/MaterialShop2` repo. Read
`HANDOFF.md`, `README.md` and `CHANGELOG.md` first. The 0.3.0 source compiles against Paper 1.21.11 / Java
21, `Build` is green, and `Smoke test` boots a real Paper server that loads the catalog (6 shops, 168 items)
without skipped lines. Nothing has been clicked by a player yet. Do this: (1) run the README checklist on a
real server, starting with a catalog purchase and a multi-unit market listing; (2) fix what breaks, keeping
the escrow arithmetic exact and the catalog read-only; (3) re-check `Trade.execute`, `Gui.SetupMenu`,
`Gui.ListingManageMenu` and `ShopManager.expire` after each fix; (4) keep the Actions build green and update
`CHANGELOG.md` and `HANDOFF.md`; (5) report build status, jar path and what is still untested. Do not switch
branches, do not add Vault or an economy plugin, and do not touch the separate `Ahmed332009/MaterialShop` repo.
