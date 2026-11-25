# MaterialShop 0.1.0

Barter shops for Paper: players trade **materials for materials**. No money, no Vault, no economy plugin.
Companion plugin to MaterialClaim.

## Install

1. Drop `MaterialShop-0.1.0.jar` into `plugins/`.
2. Restart the server. Built and tested against **Paper 1.21.11 with Java 21** (`api-version: '1.21'`), the
   same stack as MaterialClaim.
3. `/shopadmin create <name>` makes a server shop with unlimited stock; look at a chest or barrel and run
   `/shop create [name]` to make a player shop. Stock for player shops lives in that container.

## What you get

- Admin shops with unlimited stock, plus player shops backed by a chest or barrel.
- Each listing sells one exact item (name, enchantments, custom data and NBT preserved) for 1-4 payment
  options; the buyer picks which payment to use.
- Chest-only menus, so Geyser/Bedrock players can use them: tap to buy, page through listings, quantity
  1 / 8 / 64 / Max / Custom (custom is typed in chat, with `cancel`).
- Payments from player shops go to the owner's collect box, so sales work while the owner is offline.
- Atomic trades with rollback, a transaction log, and shop containers protected from other players,
  hoppers, explosions and adjacent double-chest merging.
- `/shopadmin npc <name>` places a villager that opens a shop; `/shopadmin list`, `reload`, `log`.
- Config: shop limits, payment black/whitelist, value table with a fairness warning, log size.

## Verified for this build

- GitHub Actions compiles it against the Paper 1.21.11 API and verifies the jar (plugin.yml inside,
  compiled classes inside, no unexpanded `${...}`).
- A CI smoke test boots a real Paper 1.21.11 server with this jar: `Loading server plugin MaterialShop
  v0.1.0`, `MaterialShop enabled: 0 shops loaded.`, `Done (27.747s)`, `/shopadmin create` + `/shopadmin
  list` work from the console, `/shop` answers "Only players can use this.", `shops.yml` is written on
  shutdown, and there is no exception in the log.

## Not verified yet

- Nobody has clicked through the menus on a live server. Trades, the collect box, Bedrock taps,
  permissions and the protection listeners still need the manual checklist in `README.md`.
- No MaterialClaim/WorldGuard API integration yet: shop creation asks claim plugins by firing a
  `BlockBreakEvent`, which they normally cancel.

## Fixed since the 0.1.0 source drop

- Dragging items into the add-listing slots works (it was blocked in every menu).
- A second click on "Confirm listing" can no longer create a second listing from the same template items.
- The custom quantity prompt accepts `cancel` instead of reporting a parse error.
- The collect box returns every leftover stack when your inventory is full, and tells you so.
