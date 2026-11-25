# MaterialShop 0.2.0

Barter **market** for Paper: players trade materials for materials. No money, no Vault, no economy plugin.
Companion plugin to MaterialClaim.

## The main feature is new in 0.2.0

`/shop` now opens a central GUI market. Sellers list from a menu, no chest or block needed, and the plugin
holds the stock in escrow, so listings keep selling while the seller is offline.

- Market with category tabs (Blocks, Tools and weapons, Armor, Food, Redstone, Farming, Ores and ingots,
  Misc and custom), sort by newest or item name, and pagination.
- "My listings" → New listing: put the item in the top slot, its stack size is the amount per purchase,
  pick up to 4 payment options, choose how many purchase units to stock, confirm. The first unit is the
  template you placed, the rest are taken from your inventory.
- Manage a listing later: add 1 / 8 / 64 units from your inventory, or cancel it and get the stock back in
  your collect box. Optional expiry returns stock to the collect box too.
- Server shops (`/shopadmin create <name>`) still exist with unlimited stock.
- Chest shops are now optional: `/shop chest create [name]` turns the chest or barrel you look at into a
  shop, or set `chest-shops.enabled: false` to hide the feature entirely.
- Prices are paid into the seller's collect box; payments and stock are exact ItemStacks, so names,
  enchantments and custom data survive.

## Install

1. Drop `MaterialShop-0.2.0.jar` into `plugins/`.
2. Restart. Built and smoke-tested against **Paper 1.21.11 with Java 21** (`api-version: '1.21'`), the same
   stack as MaterialClaim.
3. `/shop` for the market, `/shopadmin create <name>` for a server shop.
4. Config: `market.max-listings-per-player`, `market.expiry-hours`, `chest-shops.enabled`,
   `payment-blacklist`/`payment-whitelist`, the value table, and `categories.<TAB>` overrides.

Upgrading from 0.1.0: `/shop create` became `/shop chest create`, and `/shop list` now means your market
listings. `shops.yml` and `collect.yml` are read as before.

## Verified for this build

- GitHub Actions compiles it against the Paper 1.21.11 API and verifies the jar (plugin.yml inside,
  compiled classes inside, no unexpanded `${...}` placeholder).
- A CI smoke test boots a real Paper 1.21.11 server with this jar: the plugin loads and enables,
  `/shopadmin create` + `/shopadmin list` work from the console, `/shop` answers a console sender, and the
  YAML data files are written on shutdown, with no exception in the log.

## Not verified yet

- Nobody has clicked through the market on a live server. Buying, escrow arithmetic, add stock, cancel,
  expiry, the purchase screen, Bedrock/Geyser taps, permissions and chest-shop protection still need the
  manual checklist in `README.md`.
- No MaterialClaim/WorldGuard API hook yet: chest shop creation asks claim plugins by firing a test
  `BlockBreakEvent`, which they normally cancel. No price editing and no item-name search yet.

## Fixed while auditing the 0.2.0 source for item loss and duplication

- One set of template items could create two listings (Confirm stayed live); it now latches.
- Market escrow units could be credited without the items leaving the player's inventory, in both the
  "new listing" and the "add stock" paths; both now check that the removal really happened.
- Items can be dragged into the add-listing slots (drags were blocked in every menu).
- The custom quantity prompt accepts `cancel` as its own text promises.
- The collect box returns every leftover stack when your inventory is full, and says so.
- `market.yml` is saved again on shutdown.
