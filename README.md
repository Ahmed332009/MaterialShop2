# MaterialShop

Barter marketplace for Paper. Players trade materials for materials. No money, no Vault, no economy plugin. Companion to MaterialClaim.

## What it does
- **Market (main feature):** /shop opens a central GUI market. Players list items for sale from a menu, with no chest needed. Stock is held in escrow by the plugin, so listings work while the seller is offline.
- **Server shops:** staff create shops with infinite stock. Payment items are consumed.
- **Chest shops (optional):** players can turn a chest or barrel into a physical shop. Turn off with `chest-shops.enabled: false`.
- Chest-inventory GUIs only, so it works for Bedrock players through Geyser. Tap only.
- Each listing accepts 1 to 4 payment options. Quantity buttons 1, 8, 64, Max and Custom (chat prompt).
- Payments go to the seller's collect box. Exact item matching including names, enchantments and custom data.
- Atomic trades with rollback, transaction log, protected chest shops.

## Install
The plugin targets the Paper 1.21.11 API with Java 21, the same stack as its companion plugin MaterialClaim.

Build with `mvn package`, or take `MaterialShop-0.2.0.jar` from the GitHub Actions artifact
(`MaterialShop-jar`) or from the release page, and put it in `plugins/`.

Status: the Actions build is green, the jar is verified structurally, and a CI smoke test boots a real
Paper 1.21.11 server where the plugin loads, enables, registers its commands and writes its data files.
No player has clicked through the market yet - run the checklist at the bottom before trusting it.

## Commands
| Command | Use |
|---|---|
| `/shop` | Main menu (aliases `/ms`, `/materialshop`) |
| `/shop market` | Open the market |
| `/shop list` | Your market listings |
| `/shop collect` | Collect box |
| `/shop chest create [name]` | Make the chest or barrel you look at a shop |
| `/shopadmin create <name>` | Create a server shop |
| `/shopadmin edit <name>` | Edit a shop in the menu |
| `/shopadmin remove <name>` | Remove a shop |
| `/shopadmin npc <name>` | Place a shop villager |
| `/shopadmin list`, `reload`, `log` | Utilities |

## Permissions
`materialshop.use`, `materialshop.create`, `materialshop.collect` (default true), `materialshop.limit.<n>` (chest shop limit), `materialshop.admin`, `materialshop.admin.bypass` (default op).

## Making a market listing
1. /shop, then My listings, then New listing.
2. Put the item in the top slot. Its stack size is the amount per purchase. Put payment items in the side slots (up to 4 options).
3. Use the unit buttons to choose how many purchase units to stock. The top slot item counts as the first unit, extra units come from your inventory.
4. Confirm. Add stock or cancel later from My listings. Cancelling sends the stock to your collect box.

## Known limits in 0.2.0
- Compiles and loads on a Paper 1.21.11 server, but no player-driven test has been run yet.
- No price editing: cancel and relist instead.
- Market tabs: Blocks, Tools and weapons, Armor, Food, Redstone, Farming, Ores and ingots, Misc and custom. Named items go to Misc. Override per material in config.yml.
- No MaterialClaim or WorldGuard API hook. Chest shop creation fires a test BlockBreakEvent, which land-claim plugins normally cancel.
- Items sitting in the Add listing menu are returned on close, but would be lost in a hard crash at that moment.

## Manual test checklist
Normal trade, two payment options, custom quantity (including typing `cancel`), Max, full inventory,
offline seller, buying from your own listing, two buyers in a row, restart persistence, named and enchanted
item, backpack item, add stock, cancel listing returns stock, expiry, collect box full, category tabs, sort,
Bedrock tap-only use, chest shop protection against hopper, explosion and adjacent chest, chest shops
disabled in config, a market listing with more than one unit (check the escrow arithmetic both ways),
dragging items into the add-listing slots, and clicking Confirm twice in a row.
