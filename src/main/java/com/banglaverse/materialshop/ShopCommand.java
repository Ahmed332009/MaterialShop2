package com.banglaverse.materialshop;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ShopCommand implements CommandExecutor, TabCompleter {
    private final MaterialShop pl;

    public ShopCommand(MaterialShop pl) { this.pl = pl; }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (cmd.getName().equalsIgnoreCase("shopadmin")) return admin(s, a);
        if (!(s instanceof Player p)) { pl.send(s, "players-only"); return true; }
        if (!p.hasPermission("materialshop.use")) { pl.send(p, "no-permission"); return true; }
        String sub = a.length == 0 ? "menu" : a[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "create" -> create(p, a);
            case "list" -> pl.gui().openList(p, Gui.Kind.MINE, 0);
            case "collect" -> {
                if (p.hasPermission("materialshop.collect")) pl.gui().openCollect(p);
                else pl.send(p, "no-permission");
            }
            case "help" -> {
                for (String line : pl.messages().getStringList("help")) p.sendMessage(Text.c(line));
            }
            default -> pl.gui().openMain(p);
        }
        return true;
    }

    private void create(Player p, String[] a) {
        if (!p.hasPermission("materialshop.create")) { pl.send(p, "no-permission"); return; }
        Block b = p.getTargetBlockExact(5);
        if (b == null || !(b.getType() == Material.CHEST || b.getType() == Material.TRAPPED_CHEST
                || b.getType() == Material.BARREL)) {
            pl.send(p, "not-container");
            return;
        }
        if (!(b.getState() instanceof Container c)) { pl.send(p, "not-container"); return; }
        List<Block> blocks = new ArrayList<>();
        InventoryHolder h = c.getInventory().getHolder();
        if (h instanceof DoubleChest dc) {
            if (dc.getLeftSide() instanceof BlockState l) blocks.add(l.getBlock());
            if (dc.getRightSide() instanceof BlockState r) blocks.add(r.getBlock());
        }
        if (blocks.isEmpty()) blocks.add(b);
        for (Block x : blocks) {
            if (pl.shops().shopAt(x) != null) { pl.send(p, "already-shop"); return; }
        }
        if (!p.hasPermission("materialshop.admin.bypass")) {
            int limit = pl.shopLimit(p);
            if (pl.shops().countOwned(p.getUniqueId()) >= limit) {
                pl.send(p, "shop-limit", "limit", String.valueOf(limit));
                return;
            }
            // ask other plugins (land claims) whether this player may modify the block
            BlockBreakEvent test = new BlockBreakEvent(b, p);
            test.setDropItems(false);
            test.setExpToDrop(0);
            Bukkit.getPluginManager().callEvent(test);
            if (test.isCancelled()) { pl.send(p, "claim-denied"); return; }
        }
        String name = a.length > 1 ? String.join(" ", Arrays.copyOfRange(a, 1, a.length)) : p.getName() + "'s shop";
        if (name.length() > 24) name = name.substring(0, 24);
        Shop shop = pl.shops().createPlayer(p.getUniqueId(), name, blocks);
        pl.send(p, "shop-created", "name", name);
        pl.gui().openManage(p, shop, 0);
    }

    private boolean admin(CommandSender s, String[] a) {
        if (!s.hasPermission("materialshop.admin")) { pl.send(s, "no-permission"); return true; }
        if (a.length == 0) {
            s.sendMessage(Text.c("&6/shopadmin &7create <name> | remove <name> | edit <name> | npc <name> | list | reload | log"));
            return true;
        }
        String sub = a[0].toLowerCase(Locale.ROOT);
        String name = a.length > 1 ? String.join(" ", Arrays.copyOfRange(a, 1, a.length)) : "";
        switch (sub) {
            case "create" -> {
                if (name.isEmpty()) return false;
                if (pl.shops().findByName(name) != null) { pl.send(s, "name-taken"); return true; }
                pl.shops().createAdmin(name);
                pl.send(s, "admin-created", "name", name);
            }
            case "remove" -> {
                Shop shop = pl.shops().findByName(name);
                if (shop == null) { pl.send(s, "admin-not-found"); return true; }
                pl.shops().remove(shop);
                pl.send(s, "admin-removed");
            }
            case "edit" -> {
                if (!(s instanceof Player p)) { pl.send(s, "players-only"); return true; }
                Shop shop = pl.shops().findByName(name);
                if (shop == null) { pl.send(s, "admin-not-found"); return true; }
                pl.gui().openManage(p, shop, 0);
            }
            case "npc" -> {
                if (!(s instanceof Player p)) { pl.send(s, "players-only"); return true; }
                Shop shop = pl.shops().findByName(name);
                if (shop == null) { pl.send(s, "admin-not-found"); return true; }
                p.getWorld().spawn(p.getLocation(), Villager.class, v -> {
                    v.setAI(false);
                    v.setInvulnerable(true);
                    v.setPersistent(true);
                    v.setSilent(true);
                    v.customName(Text.c(shop.name));
                    v.setCustomNameVisible(true);
                    v.getPersistentDataContainer().set(pl.npcKey, PersistentDataType.STRING, shop.id.toString());
                });
                pl.send(s, "npc-spawned");
            }
            case "list" -> {
                for (Shop shop : pl.shops().all()) {
                    s.sendMessage(Text.c("&7- &f" + shop.name + " &8(" + (shop.isAdmin() ? "server" : "player")
                            + ", " + shop.listings.size() + " listings)"));
                }
            }
            case "reload" -> {
                pl.reloadConfig();
                pl.loadMessages();
                pl.send(s, "reload");
            }
            case "log" -> {
                File f = new File(new File(pl.getDataFolder(), "logs"), "transactions.log");
                try {
                    List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
                    for (String line : lines.subList(Math.max(0, lines.size() - 10), lines.size())) {
                        s.sendMessage(Text.c("&8" + line));
                    }
                } catch (IOException ex) {
                    s.sendMessage(Text.c("&7No transactions logged yet."));
                }
            }
            default -> { return false; }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] a) {
        List<String> out = new ArrayList<>();
        if (cmd.getName().equalsIgnoreCase("shopadmin")) {
            if (!s.hasPermission("materialshop.admin")) return out;
            if (a.length == 1) {
                for (String x : List.of("create", "remove", "edit", "npc", "list", "reload", "log")) {
                    if (x.startsWith(a[0].toLowerCase(Locale.ROOT))) out.add(x);
                }
            } else if (a.length >= 2 && List.of("remove", "edit", "npc").contains(a[0].toLowerCase(Locale.ROOT))) {
                String typed = String.join(" ", Arrays.copyOfRange(a, 1, a.length)).toLowerCase(Locale.ROOT);
                for (Shop shop : pl.shops().all()) {
                    if (shop.name.toLowerCase(Locale.ROOT).startsWith(typed) && !shop.name.contains(" ")) out.add(shop.name);
                }
            }
            return out;
        }
        if (a.length == 1) {
            for (String x : List.of("create", "list", "collect", "help")) {
                if (x.startsWith(a[0].toLowerCase(Locale.ROOT))) out.add(x);
            }
        }
        return out;
    }
}
