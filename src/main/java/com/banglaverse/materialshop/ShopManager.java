package com.banglaverse.materialshop;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class ShopManager {
    private final MaterialShop pl;
    private final Map<UUID, Shop> shops = new LinkedHashMap<>();
    private final Map<String, Shop> index = new HashMap<>();
    private final Map<UUID, List<ItemStack>> boxes = new HashMap<>();
    private final File shopsFile;
    private final File boxFile;

    public ShopManager(MaterialShop pl) {
        this.pl = pl;
        pl.getDataFolder().mkdirs();
        this.shopsFile = new File(pl.getDataFolder(), "shops.yml");
        this.boxFile = new File(pl.getDataFolder(), "collect.yml");
    }

    public static String key(Block b) {
        return b.getWorld().getName() + ";" + b.getX() + ";" + b.getY() + ";" + b.getZ();
    }

    public Collection<Shop> all() { return shops.values(); }
    public Shop shopAt(Block b) { return index.get(key(b)); }
    public boolean exists(Shop s) { return s != null && shops.get(s.id) == s; }

    public int countOwned(UUID owner) {
        int n = 0;
        for (Shop s : shops.values()) if (owner.equals(s.owner)) n++;
        return n;
    }

    public Shop findByName(String name) {
        for (Shop s : shops.values()) if (s.name.equalsIgnoreCase(name)) return s;
        return null;
    }

    public Shop createAdmin(String name) {
        Shop s = new Shop(UUID.randomUUID(), null);
        s.name = name;
        shops.put(s.id, s);
        save();
        return s;
    }

    public Shop createPlayer(UUID owner, String name, List<Block> blocks) {
        Shop s = new Shop(UUID.randomUUID(), owner);
        s.name = name;
        for (Block b : blocks) {
            String k = key(b);
            s.blocks.add(k);
            index.put(k, s);
        }
        shops.put(s.id, s);
        save();
        return s;
    }

    public void remove(Shop s) {
        shops.remove(s.id);
        for (String k : s.blocks) index.remove(k);
        save();
    }

    /** Live inventory of the shop container, or null. */
    public Inventory stockInv(Shop s) {
        if (s.blocks.isEmpty()) return null;
        String[] p = s.blocks.get(0).split(";");
        if (p.length < 4) return null;
        World w = Bukkit.getWorld(p[0]);
        if (w == null) return null;
        try {
            Block b = w.getBlockAt(Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
            if (b.getState() instanceof Container c) return c.getInventory();
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    // ---------- collect boxes ----------

    public List<ItemStack> box(UUID u) {
        return boxes.computeIfAbsent(u, k -> new ArrayList<>());
    }

    public boolean canStore(UUID u, ItemStack t, int total) {
        List<ItemStack> box = box(u);
        int max = t.getMaxStackSize();
        int free = 0;
        for (ItemStack s : box) if (s.isSimilar(t)) free += max - s.getAmount();
        free += Math.max(0, pl.boxCapacity() - box.size()) * max;
        return free >= total;
    }

    public void store(UUID u, ItemStack t, int total) {
        List<ItemStack> box = box(u);
        int max = t.getMaxStackSize();
        for (ItemStack s : box) {
            if (total > 0 && s.isSimilar(t)) {
                int add = Math.min(max - s.getAmount(), total);
                if (add > 0) {
                    s.setAmount(s.getAmount() + add);
                    total -= add;
                }
            }
        }
        while (total > 0) {
            ItemStack n = t.clone();
            int a = Math.min(max, total);
            n.setAmount(a);
            box.add(n);
            total -= a;
        }
    }

    // ---------- persistence ----------

    public void load() {
        shops.clear();
        index.clear();
        boxes.clear();
        if (shopsFile.exists()) {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(shopsFile);
            ConfigurationSection root = y.getConfigurationSection("shops");
            if (root != null) {
                for (String k : root.getKeys(false)) {
                    ConfigurationSection c = root.getConfigurationSection(k);
                    if (c == null) continue;
                    try {
                        String o = c.getString("owner");
                        Shop s = new Shop(UUID.fromString(k), (o == null || o.isEmpty()) ? null : UUID.fromString(o));
                        s.name = c.getString("name", "Shop");
                        s.blocks.addAll(c.getStringList("blocks"));
                        s.nextListingId = c.getInt("next-id", 1);
                        ConfigurationSection ls = c.getConfigurationSection("listings");
                        if (ls != null) {
                            for (String lk : ls.getKeys(false)) {
                                ConfigurationSection lc = ls.getConfigurationSection(lk);
                                if (lc == null) continue;
                                ItemStack item = lc.getItemStack("item");
                                List<ItemStack> pays = new ArrayList<>();
                                for (Object o2 : lc.getList("payments", new ArrayList<>())) {
                                    if (o2 instanceof ItemStack is) pays.add(is);
                                }
                                if (item != null && !pays.isEmpty()) {
                                    s.listings.add(new Listing(Integer.parseInt(lk), item, pays));
                                }
                            }
                        }
                        s.listings.sort(Comparator.comparingInt(x -> x.id));
                        shops.put(s.id, s);
                        for (String b : s.blocks) index.put(b, s);
                    } catch (Exception ex) {
                        pl.getLogger().warning("Skipping bad shop entry " + k + ": " + ex);
                    }
                }
            }
        }
        if (boxFile.exists()) {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(boxFile);
            for (String k : y.getKeys(false)) {
                try {
                    UUID u = UUID.fromString(k);
                    List<ItemStack> list = new ArrayList<>();
                    for (Object o : y.getList(k, new ArrayList<>())) if (o instanceof ItemStack is) list.add(is);
                    boxes.put(u, list);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    public synchronized void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Shop s : shops.values()) {
            String p = "shops." + s.id;
            y.set(p + ".owner", s.owner == null ? null : s.owner.toString());
            y.set(p + ".name", s.name);
            y.set(p + ".blocks", s.blocks);
            y.set(p + ".next-id", s.nextListingId);
            for (Listing l : s.listings) {
                String lp = p + ".listings." + l.id;
                y.set(lp + ".item", l.item);
                y.set(lp + ".payments", l.payments);
            }
        }
        writeAtomic(y, shopsFile);
    }

    public synchronized void saveBoxes() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<UUID, List<ItemStack>> e : boxes.entrySet()) {
            if (!e.getValue().isEmpty()) y.set(e.getKey().toString(), e.getValue());
        }
        writeAtomic(y, boxFile);
    }

    private void writeAtomic(YamlConfiguration y, File target) {
        try {
            File tmp = new File(target.getParentFile(), target.getName() + ".tmp");
            y.save(tmp);
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            pl.getLogger().severe("Could not save " + target.getName() + ": " + ex.getMessage());
        }
    }
}
