# MaterialShop

Barter shop plugin for Paper. Players trade materials for materials. No money, no Vault, no economy plugin. Companion to MaterialClaim.

## Features
- Admin shops (infinite stock, payment is consumed) and player shops (stock lives in a chest or barrel).
- Chest GUI only, so it works for Bedrock players through Geyser. Tap only, no forms, no anvils, no signs.
- Each listing sells one item for 1 to 4 payment options. Multiple options are optional.
- Quantity buttons 1, 8, 64, Max and a Custom quantity button that uses a chat prompt.
- Payments from player shops go to the owner's collect box, so offline sales work.
- Exact item matching including names, enchantments and custom data (backpacks work).
- Atomic trades with rollback, transaction log, protected shop containers, no hopper or explosion access.

## Install
Build with `mvn package` or download the jar from the GitHub Actions artifact. Put the jar in `plugins/`.

## Commands
| Command | Use |
|---|---|
| `/shop` | Open the menu (aliases `/ms`, `/materialshop`) |
| `/shop create [name]` | Look at a chest or barrel and make it a shop |
| `/shop list` | Your shops |
| `/shop collect` | Open your collect box |
| `/shopadmin create <name>` | Create a server shop |
| `/shopadmin edit <name>` | Edit a shop in the menu |
| `/shopadmin remove <name>` | Remove a shop |
| `/shopadmin npc <name>` | Place a shop villager |
| `/shopadmin list`, `reload`, `log` | Utilities |

## Permissions
`materialshop.use`, `materialshop.create`, `materialshop.collect` (default true), `materialshop.limit.<n>`, `materialshop.admin`, `materialshop.admin.bypass` (default op).

## How a player makes a listing
1. Place a barrel or chest, look at it, run `/shop create`.
2. Put stock inside the container.
3. In the manage menu click Add listing. Put the item to sell in the top slot, payment items in the side slots, click Confirm.

## Known limits in 0.1.0
- No MaterialClaim or WorldGuard API integration. Shop creation asks other plugins by firing a BlockBreakEvent, which land-claim plugins normally cancel.
- Items placed in the Add listing menu are returned on close, but would be lost in a hard server crash at that moment.
- Admin shop payment items are consumed.

## Manual test checklist
Normal trade, two payment options, custom quantity, Max, full inventory, offline seller, buying from own shop, two buyers in a row, restart persistence, named and enchanted item, backpack item, Bedrock tap-only use, hopper under shop chest, explosion near shop chest, placing a chest next to a shop chest.
