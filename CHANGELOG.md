# Changelog

## 0.1.0 — first build that actually compiles (never tested in game)

Build and toolchain (handoff step 1)
- Targets the Paper 1.21.11 API (`1.21.11-R0.1-SNAPSHOT`) with Java 21, copied from the MaterialClaim
  build so both plugins run on the same server. `plugin.yml` `api-version` is `1.21`, as in MaterialClaim.
- Maven compiler/resources/jar plugins pinned so `release 21` is actually honoured; `plugin.yml`
  `version: ${project.version}` is filtered to `0.1.0` at package time.
- GitHub Actions: Java 21 workflow, first green run on branch `arena/7eff1135-materialshop2`, jar plus
  artifact upload, a jar sanity check (plugin.yml present, classes present, no unexpanded placeholder)
  and a release step for `v*` tags.

Fixed while re-reading `Trade.java` and `Gui.java` (handoff step 3)
- Add-listing menu: the click guard was not the only hole. Dragging was cancelled for *every* menu, so
  items could not be dragged into the sell/payment slots. Drags are now allowed when every touched slot
  is one of those free slots and still blocked everywhere else.
- Add-listing menu: a second click on "Confirm listing" before the menu closed created a second listing
  from the same template items. `confirmed` now latches and the extra listing cannot be created.
- Add-listing menu: the four template slots are given back on close (unchanged) and dropped at the
  player's feet only when their inventory is full, so nothing is destroyed.
- Custom quantity prompt: the prompt text says "type cancel", but `cancel` produced a parse error.
  It now cancels quietly and reopens the purchase screen.
- Collect box: only the last leftover stack was put back when the player's inventory was full; every
  leftover stack is now returned and the player is told the rest stays in the box (`collect-no-room`).

Checked and left alone (`Trade.execute` is atomic, main thread only)
- Payment is counted and removed from a clone snapshot, goods are added, stock is removed from the live
  container, and any failure restores the snapshot before returning an error key. No path returns
  success without stock leaving the container, and no path removes payment without giving goods.
- Buying from your own shop, buying with a stale listing object, a removed shop, a missing container and
  a full collect box all fail before anything moves.

## 0.1.0 — source written, never compiled

- First version: admin shops, player shops, chest GUI, multi-payment listings, custom quantity, collect
  box, transaction log, container protection, shop villagers.
- Source written without a local compile.
