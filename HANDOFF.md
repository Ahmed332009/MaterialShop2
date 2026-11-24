# MaterialShop handoff (version 0.1.0)

## State
All source files are written: main class, ShopManager (YAML storage), Trade logic, Gui (all menus), GuiListener, ProtectionListener, ShopCommand, config.yml, messages.yml, plugin.yml, pom.xml, GitHub Actions workflow, README, CHANGELOG, LICENSE.
NOT compiled against the Paper API yet. Only a syntax check was done. Expect a few compile errors.

## Next steps, in order
1. Open the MaterialClaim pom and copy its Java version and Paper API version into this pom.xml properties java.version and paper.version. Update plugin.yml api-version and the workflow java-version to match.
2. Push to a new public GitHub repo named MaterialShop. Run the Build workflow. Read the log. Fix every compile error. Repeat until green.
3. Download the jar artifact. Do not call the plugin working until it is tested on the server.
4. Run the manual test checklist in README.md and fix bugs.
5. Optional later: MaterialClaim territory integration, WorldGuard flag check, paid shop signs.

## Continuation prompt (paste into the GitHub agent or another Claude account)
You are continuing the Minecraft Paper plugin MaterialShop (barter shop, no money). The repo source is complete but never compiled. Do this: (1) copy Java and Paper API versions from the MaterialClaim repo into pom.xml, plugin.yml and the workflow; (2) run the GitHub Actions build and fix all compile errors without changing behaviour; (3) re-read src/main/java/com/banglaverse/materialshop/Trade.java and Gui.java for logic bugs such as item duplication or lost items; (4) update CHANGELOG.md and HANDOFF.md with what you changed; (5) report the build status, the jar location and anything untested. If your limit is close, commit what you have, update HANDOFF.md and give me a new continuation prompt plus all files.
