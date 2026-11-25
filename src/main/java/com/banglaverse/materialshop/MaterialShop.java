package com.banglaverse.materialshop;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class MaterialShop extends JavaPlugin {
    private ShopManager shops;
    private Gui gui;
    private YamlConfiguration messages;
    private final Map<UUID, Consumer<String>> prompts = new ConcurrentHashMap<>();
    public NamespacedKey npcKey;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadMessages();
        npcKey = new NamespacedKey(this, "npc_shop");
        shops = new ShopManager(this);
        shops.load();
        gui = new Gui(this);
        Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ProtectionListener(this), this);
        ShopCommand cmd = new ShopCommand(this);
        getCommand("shop").setExecutor(cmd);
        getCommand("shop").setTabCompleter(cmd);
        getCommand("shopadmin").setExecutor(cmd);
        getCommand("shopadmin").setTabCompleter(cmd);
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            long hours = getConfig().getLong("market.expiry-hours", 0);
            if (hours > 0) shops.expire(hours);
        }, 200L, 6000L);
        getLogger().info("MaterialShop enabled: " + shops.all().size() + " shops loaded.");
    }

    @Override
    public void onDisable() {
        // closing menus returns any items players had placed in the setup menu
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getOpenInventory().getTopInventory().getHolder(false) instanceof Gui.Menu) p.closeInventory();
        }
        if (shops != null) {
            shops.save();
            shops.saveBoxes();
            shops.saveMarket();
        }
    }

    public ShopManager shops() { return shops; }
    public Gui gui() { return gui; }
    public YamlConfiguration messages() { return messages; }

    public void loadMessages() {
        File f = new File(getDataFolder(), "messages.yml");
        if (!f.exists()) saveResource("messages.yml", false);
        messages = YamlConfiguration.loadConfiguration(f);
        InputStream in = getResource("messages.yml");
        if (in != null) {
            messages.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8)));
        }
    }

    public String raw(String key, String... kv) {
        String s = messages.getString(key, "&cMissing message: " + key);
        s = s.replace("{prefix}", messages.getString("prefix", ""));
        for (int i = 0; i + 1 < kv.length; i += 2) s = s.replace("{" + kv[i] + "}", kv[i + 1]);
        return s;
    }

    public void send(CommandSender to, String key, String... kv) {
        to.sendMessage(Text.c(raw(key, kv)));
    }

    public boolean paymentAllowed(Material m) {
        if (m.isAir() || !m.isItem()) return false;
        if (m.name().endsWith("_SPAWN_EGG")) return false;
        if (getConfig().getStringList("payment-blacklist").contains(m.name())) return false;
        List<String> wl = getConfig().getStringList("payment-whitelist");
        return wl.isEmpty() || wl.contains(m.name());
    }

    public double value(Material m) {
        return getConfig().getDouble("values." + m.name(), 0);
    }

    public int shopLimit(Player p) {
        int best = getConfig().getInt("default-shop-limit", 5);
        for (int i = 1; i <= 100; i++) if (p.hasPermission("materialshop.limit." + i)) best = Math.max(best, i);
        return best;
    }

    public boolean chestShopsEnabled() {
        return getConfig().getBoolean("chest-shops.enabled", true);
    }

    public int boxCapacity() {
        return Math.max(1, Math.min(45, getConfig().getInt("collect-capacity", 45)));
    }

    /** Ask the player to type something in chat. The callback runs on the main thread. */
    public void prompt(Player p, String key, Consumer<String> cb) {
        Bukkit.getScheduler().runTask(this, () -> {
            p.closeInventory();
            send(p, key);
            UUID id = p.getUniqueId();
            prompts.put(id, cb);
            long ticks = 20L * getConfig().getInt("prompt-timeout-seconds", 30);
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (prompts.get(id) == cb) {
                    prompts.remove(id);
                    send(p, "prompt-timeout");
                }
            }, ticks);
        });
    }

    public Consumer<String> takePrompt(UUID id) {
        return prompts.remove(id);
    }

    public synchronized void log(String line) {
        try {
            File dir = new File(getDataFolder(), "logs");
            dir.mkdirs();
            File f = new File(dir, "transactions.log");
            if (f.length() > getConfig().getLong("log-max-bytes", 2_000_000L)) {
                f.renameTo(new File(dir, "transactions-" + System.currentTimeMillis() + ".log"));
            }
            Files.writeString(f.toPath(), Instant.now() + " " + line + "\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            getLogger().warning("Could not write transaction log: " + ex.getMessage());
        }
    }
}
