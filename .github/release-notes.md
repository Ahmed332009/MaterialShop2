# MaterialShop 0.3.0

Barter **market** for Paper: players trade materials for materials. No money, no Vault, no economy plugin.
Companion plugin to MaterialClaim.

## New in 0.3.0: a server shop catalog that is ready out of the box

`catalog.yml` ships **6 pre-listed server shops with 168 items already priced**, so staff do not have to add
anything by hand:

| Shop | Icon |
|---|---|
| Building Blocks | bricks |
| Farming and Food | wheat |
| Ores and Materials | diamond |
| Tools and Armor | iron pickaxe |
| Redstone and Utility | redstone |
| Mob Drops and Rare | ender pearl |

One line per item, up to four payment options each:

```yaml
- "COBBLESTONE 64 = COPPER_INGOT 4 | IRON_INGOT 2"
```

The player pays with **one** of the listed options and gets the left-hand item. Edit the file and run
`/shopadmin reload`; no restart needed. Bad material names or malformed lines are skipped and logged, and
the rest of the catalog still loads. Catalog shops are read-only in menus, are never written to
`shops.yml`, and `catalog.enabled: false` turns the whole catalog off.

## Also in this build (from 0.2.0)

- **Market** opened with `/shop`: players list items from a menu with no chest, stock held in escrow so
  listings keep selling while the seller is offline. Category tabs, sort by newest or item name, pagination.
- **My listings**: new listing with a stock-unit selector, add 1/8/64 units later, cancel and get the stock
  back in your collect box, optional expiry.
- **Server shops**: unlimited stock, payments consumed; `/shopadmin create|edit|remove|npc|list|reload|log`.
- **Chest shops** are optional (`chest-shops.enabled`), created with `/shop chest create [name]`.
- Collect box for offline sales, transaction log, chest shop protection against players, hoppers,
  explosions and adjacent double chests, fairness warning from the value table.

## Install

1. Drop `MaterialShop-0.3.0.jar` into `plugins/`.
2. Restart. Built and smoke-tested against **Paper 1.21.11 with Java 21** (`api-version: '1.21'`), the same
   stack as MaterialClaim.
3. `/shop` for the market, Server shops for the catalog, `/shopadmin create <name>` for your own shop.
4. Tune `config.yml` (market caps, expiry, chest shops, payment blacklist/whitelist, value table, category
   overrides) and `catalog.yml` (the pre-listed shops).

Upgrading from 0.1.0: `/shop create` became `/shop chest create`, and `/shop list` now means your market
listings. `shops.yml` and `collect.yml` are read as before; `catalog.yml` is new.

## Verified for this build

- GitHub Actions compiles it against the Paper 1.21.11 API and verifies the jar (plugin.yml inside, compiled
  classes inside, no unexpanded `${...}` placeholder).
- A CI smoke test boots a real Paper 1.21.11 server with this jar: the plugin loads and enables,
  **`Catalog loaded: 6 shops, 168 items` with no skipped lines**, `/shopadmin create` + `/shopadmin list`
  work from the console, every `/shop` subcommand answers a console sender, data files are written on
  shutdown, and there is no exception in the log.

## Not verified yet

- Nobody has clicked through the menus on a live server. Buying, escrow arithmetic, add stock, cancel,
  expiry, the purchase screen, Bedrock/Geyser taps, permissions and chest shop protection still need the
  manual checklist in `README.md`.
- No price editing in the menu, no item-name search, no MaterialClaim/WorldGuard API hook, no catalog
  buy-back (the server only sells, it does not buy from players).

## Fixed while auditing the source for item loss and duplication

- One set of template items could create two listings (Confirm stayed live); it now latches.
- Market escrow units could be credited without the items leaving the player's inventory, in both the
  "new listing" and "add stock" paths; both now check that the removal really happened.
- Items can be dragged into the add-listing slots (drags were blocked in every menu).
- The custom quantity prompt accepts `cancel`, as its own text promises.
- The collect box returns every leftover stack when your inventory is full, and says so.
- `market.yml` is saved again on shutdown.
