# Changelog

## 0.3.0 — pre-listed server shop catalog (compiles; catalog loads on a real Paper server)

### Source as shipped in the 0.3.0 archive
- `catalog.yml` ships 6 pre-listed server shops with 168 items already priced: Building Blocks, Farming and
  Food, Ores and Materials, Tools and Armor, Redstone and Utility, Mob Drops and Rare.
- One line per item, `"ITEM amount = PAYMENT amount | PAYMENT amount"`, up to 4 payment options per line.
- The catalog is built on start and rebuilt by `/shopadmin reload`; catalog shops are read-only in menus
  (`/shopadmin edit` and `remove` answer `catalog-readonly`), are never written to `shops.yml`, and are
  skipped by the menu-manage permission check. `catalog.enabled: false` turns the whole catalog off.
- Everything from 0.2.0 is kept: GUI market with escrow, optional chest shops, collect box, transaction log,
  container protection, fairness warning.

### Build and toolchain (handoff step 1)
- Paper API `1.21.11-R0.1-SNAPSHOT` and Java 21, copied from the MaterialClaim build so both plugins run on
  the same server. `plugin.yml` `api-version` is `'1.21'`, as in MaterialClaim; the workflow already used
  Java 21.
- Maven compiler/resources/jar plugins pinned so `release 21` is honoured; `plugin.yml`
  `version: ${project.version}` is filtered to `0.3.0` at package time.
- Actions `Build`: compile, jar sanity check, artifact, release on `v*` tags. Actions `Smoke test`: boots a
  real Paper 1.21.11 server, runs console commands, and now also asserts the catalog: the log must contain
  `Catalog loaded: 6 shops, 168 items` and no `catalog.yml: skipped bad line`.

### Catalog audit
- `parseCatalogLine` / `parseStack` were checked against all 168 shipped lines: every line has exactly one
  `=`, between 1 and 4 payment options, a material name of the form `[A-Z0-9_]+` and integer amounts, so
  the catalog parses to 6 shops and 168 listings. Amounts above the stack size are clamped.
- Bad material names or malformed lines are skipped and logged one by one; they never abort the load, and
  they never create a half-priced listing.
- Catalog shops are admin shops (`owner == null`), so they have unlimited stock, charge nothing to the
  server, and cannot be edited or removed through the menus (`canManage` returns false).
- Rebuilding the catalog on reload replaces the shops under stable UUIDs
  (`nameUUIDFromBytes("materialshop-catalog:" + key)`), so a GUI opened before the reload says "gone"
  instead of acting on a stale shop.

### Fixed while auditing for item loss and duplication (handoff step 3, carried over from 0.2.0)
- Add listing: one set of template items could create two listings, because Confirm stayed live until the
  menu closed. `confirmed` now latches in both the market and the shop path.
- Add listing (market): escrow units were credited even if the extra units could not be removed from the
  player; the removal result is now checked.
- Add stock: same ignored-result issue, now checked.
- Add listing: dragging was cancelled by the generic handler, so items could not be dragged into the
  sell/payment slots (only clicked in). Drags are allowed when every touched slot is a free slot.
- Custom quantity: the prompt says "type cancel", but `cancel` produced a parse error; it now cancels
  quietly and reopens the purchase screen.
- Collect box: only the last leftover stack was put back when the player's inventory was full. Every
  leftover stack is returned now, with the new `collect-no-room` message.
- Shutdown also saves `market.yml`.

## 0.2.0 — GUI market (superseded by 0.3.0)
- Central GUI market opened with `/shop`, escrow stock, category tabs, sort, pagination, add stock, cancel
  listing, optional expiry; chest shops became an optional extra.

## 0.1.0 — chest shops (superseded)
- First draft with chest shops as the main feature.
