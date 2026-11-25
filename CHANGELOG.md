# Changelog

## 0.2.0 — GUI market (compiles; loads on a real Paper server, trades not yet played)

The main feature changed from chest shops to a central GUI market with escrow, so 0.2.0 is not a drop-in
upgrade of 0.1.0: `/shop` now opens the market, and chest shops became an optional extra
(`chest-shops.enabled`).

### Source as shipped in the 0.2.0 archive
- Central GUI market opened with `/shop`: no chest or block needed, stock held in escrow by the plugin.
- Market features: category tabs, sort by newest or item name, pagination, add stock, cancel listing,
  optional expiry, per-player listing cap.
- Chest shops are optional now, created with `/shop chest create [name]`.
- Server shops (admin, infinite stock) kept, with `/shopadmin create|edit|remove|npc|list|reload|log`.
- Collect box, transaction log, container protection and the fairness warning kept.

### Build and toolchain (handoff step 1)
- Paper API `1.21.11-R0.1-SNAPSHOT` and Java 21, copied from the MaterialClaim build so both plugins run
  on the same server. `plugin.yml` `api-version` is `'1.21'`, as in MaterialClaim; the workflow already
  used Java 21.
- Maven compiler/resources/jar plugins pinned so `release 21` is honoured; `plugin.yml`
  `version: ${project.version}` is filtered to `0.2.0` at package time.
- GitHub Actions `Build`: compile, jar sanity check (plugin.yml inside, classes inside, no unexpanded
  placeholder), artifact upload, release with the jar attached on a `v*` tag.
- GitHub Actions `Smoke test`: downloads Paper 1.21.11, installs the jar, boots a flat world, runs console
  commands and asserts on the log and on the YAML files the plugin writes.

### Fixed while auditing for item loss and duplication (handoff step 3)
- Add listing: one set of template items could create two listings, because the Confirm button stayed
  live until the menu closed. `confirmed` now latches in both the market and the shop path.
- Add listing (market): the escrowed units are only credited once every unit is really taken from the
  player; if the extra units cannot be removed the listing is refused instead of being created empty-handed.
- Add stock: the units were credited even when the inventory removal failed (it is checked first, but the
  result was ignored). The result is now checked.
- Add listing: dragging was cancelled by the generic handler, so items could not be dragged into the
  sell/payment slots (only clicked in). Drags are now allowed when every touched slot is a free slot.
- Custom quantity: the prompt says "type cancel", but `cancel` produced a parse error. It now cancels
  quietly and reopens the purchase screen.
- Collect box: only the last leftover stack was put back when the player's inventory was full. Every
  leftover stack is returned now, with the new `collect-no-room` message.
- Shutdown also saves `market.yml`, so a market mutation made in the last tick before shutdown is not lost.

### Audited and left alone
- `Trade.execute` is unchanged in shape: market stock is a unit counter, so a market trade decrements units
  and a chest shop trade must remove the items from the live container; both roll the buyer's inventory
  back from a snapshot on any failure. Payment is only stored in the seller's box after the goods are out.
- Listing creation and cancellation move exactly `units * item.getAmount()` items in both directions.
- `expire` returns units to the seller's collect box and refuses to expire when the box is full, so it
  retries later instead of voiding the stock.

## 0.1.0 — chest shops (superseded)
- First draft with chest shops as the main feature.
