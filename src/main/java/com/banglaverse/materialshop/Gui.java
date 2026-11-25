package com.banglaverse.materialshop;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;

/** All chest menus. Every menu is an InventoryHolder so GuiListener can recognise it. */
public class Gui {
    public enum Kind { ADMIN, PLAYER, MINE }

    private static final int PAGE = 45;
    private static final int MARKET_PAGE = 36;
    private static final int SELL_SLOT = 4;
    private static final int[] PAY_SLOTS = {11, 12, 14, 15};

    final MaterialShop pl;

    public Gui(MaterialShop pl) { this.pl = pl; }

    public abstract static class Menu implements InventoryHolder {
        protected Inventory inv;

        @Override
        public Inventory getInventory() { return inv; }

        public abstract void click(Player p, int slot);
    }

    // ---------- helpers ----------

    private void fill(Inventory inv) {
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, Text.filler());
    }

    private void nav(Inventory inv, int page, int total) {
        for (int i = 45; i < 54; i++) inv.setItem(i, Text.filler());
        if (page > 0) inv.setItem(45, Text.icon(Material.ARROW, "&ePrevious page"));
        if ((page + 1) * PAGE < total) inv.setItem(53, Text.icon(Material.ARROW, "&eNext page"));
        inv.setItem(49, Text.icon(Material.BARRIER, "&cBack"));
    }

    private int clampPage(int page, int total) {
        int pages = Math.max(1, (total + PAGE - 1) / PAGE);
        return Math.min(Math.max(0, page), pages - 1);
    }

    private void open(Player p, Supplier<Menu> sup) {
        Bukkit.getScheduler().runTask(pl, () -> {
            if (p.isOnline()) p.openInventory(sup.get().getInventory());
        });
    }

    private void reopen(Player p, Menu m) {
        if (p.isOnline()) p.openInventory(m.getInventory());
    }

    private boolean canManage(Player p, Shop s) {
        return pl.shops().exists(s) && !s.catalog && ((s.owner != null && s.owner.equals(p.getUniqueId()))
                || p.hasPermission("materialshop.admin"));
    }

    private String ownerName(Shop s) {
        if (s.isAdmin()) return "Server";
        String n = Bukkit.getOfflinePlayer(s.owner).getName();
        return n == null ? "Unknown" : n;
    }

    private ItemStack display(Listing l, boolean manage, Shop shop, Inventory stock) {
        ItemStack it = l.item.clone();
        ItemMeta m = it.getItemMeta();
        List<Component> lore = m.hasLore() ? new ArrayList<>(m.lore()) : new ArrayList<>();
        lore.add(Text.c(" "));
        lore.add(Text.c("&7Per purchase: &f" + l.item.getAmount() + "x"));
        lore.add(Text.c("&7Price" + (l.payments.size() > 1 ? " (choose one)" : "") + ":"));
        for (ItemStack pay : l.payments) lore.add(Text.c("&e" + pay.getAmount() + "x &f" + Text.itemName(pay)));
        if (shop.isAdmin()) lore.add(Text.c("&7Stock: &aunlimited"));
        else if (stock == null) lore.add(Text.c("&cShop container missing"));
        else lore.add(Text.c("&7In stock: &f" + (Trade.count(stock, l.item) / Math.max(1, l.item.getAmount()))));
        lore.add(Text.c(manage ? "&cClick to remove this listing" : "&aClick to buy"));
        m.lore(lore);
        it.setItemMeta(m);
        return it;
    }

    private boolean alive(Shop shop, Listing l) {
        return shop == null ? pl.shops().marketGet(l.id) == l : (pl.shops().exists(shop) && shop.find(l.id) == l);
    }

    private String sellerName(UUID u) {
        String n = Bukkit.getOfflinePlayer(u).getName();
        return n == null ? "Unknown" : n;
    }

    private ItemStack marketDisplay(Listing l, String last) {
        ItemStack it = l.item.clone();
        ItemMeta m = it.getItemMeta();
        List<Component> lore = m.hasLore() ? new ArrayList<>(m.lore()) : new ArrayList<>();
        lore.add(Text.c(" "));
        lore.add(Text.c("&7Per purchase: &f" + l.item.getAmount() + "x"));
        lore.add(Text.c("&7Price" + (l.payments.size() > 1 ? " (choose one)" : "") + ":"));
        for (ItemStack pay : l.payments) lore.add(Text.c("&e" + pay.getAmount() + "x &f" + Text.itemName(pay)));
        lore.add(Text.c("&7Seller: &f" + sellerName(l.seller)));
        lore.add(Text.c("&7Available: &f" + l.units));
        if (last != null) lore.add(Text.c(last));
        m.lore(lore);
        it.setItemMeta(m);
        return it;
    }

    // ---------- public open methods ----------

    public void openMain(Player p) { open(p, () -> new MainMenu()); }
    public void openList(Player p, Kind k, int page) { open(p, () -> new ListMenu(p, k, page)); }
    public void openShop(Player p, Shop s, int page) { open(p, () -> new ShopMenu(s, page)); }
    public void openManage(Player p, Shop s, int page) { open(p, () -> new ManageMenu(s, page)); }
    public void openPurchase(Player p, Shop s, Listing l) { open(p, () -> new PurchaseMenu(s, l)); }
    public void openSetup(Player p, Shop s, int page) { open(p, () -> new SetupMenu(s, page)); }
    public void openCollect(Player p) { open(p, () -> new CollectMenu(p)); }
    public void openMarket(Player p, Category c, int sort, int page) { open(p, () -> new MarketMenu(c, sort, page)); }
    public void openMyListings(Player p, int page) { open(p, () -> new MyListingsMenu(p, page)); }
    public void openListingManage(Player p, Listing l) { open(p, () -> new ListingManageMenu(l)); }

    // ---------- main menu ----------

    class MainMenu extends Menu {
        MainMenu() {
            inv = Bukkit.createInventory(this, 27, Text.c("&8MaterialShop"));
            fill(inv);
            inv.setItem(10, Text.icon(Material.EMERALD, "&aMarket", "&7Browse items players are selling"));
            inv.setItem(12, Text.icon(Material.ENDER_CHEST, "&bServer shops", "&7Unlimited stock"));
            inv.setItem(14, Text.icon(Material.WRITABLE_BOOK, "&eMy listings", "&7Sell items and manage your listings"));
            inv.setItem(16, Text.icon(Material.HOPPER, "&6Collect box", "&7Payments and returned items"));
            if (pl.chestShopsEnabled()) {
                inv.setItem(21, Text.icon(Material.BARREL, "&fChest shops", "&7Physical shops run by players"));
                inv.setItem(23, Text.icon(Material.CHEST, "&fMy chest shops", "&7Manage your chest shops"));
            }
        }

        @Override
        public void click(Player p, int slot) {
            if (slot == 10) openMarket(p, Category.ALL, 0, 0);
            else if (slot == 12) openList(p, Kind.ADMIN, 0);
            else if (slot == 14) openMyListings(p, 0);
            else if (slot == 16) openCollect(p);
            else if (slot == 21 && pl.chestShopsEnabled()) openList(p, Kind.PLAYER, 0);
            else if (slot == 23 && pl.chestShopsEnabled()) openList(p, Kind.MINE, 0);
        }
    }

    // ---------- shop lists ----------

    private String listTitle(Kind k) {
        return switch (k) {
            case ADMIN -> "Server shops";
            case PLAYER -> "Chest shops";
            case MINE -> "My chest shops";
        };
    }

    class ListMenu extends Menu {
        final Kind kind;
        final int page;
        final List<Shop> list = new ArrayList<>();

        ListMenu(Player p, Kind kind, int page) {
            this.kind = kind;
            for (Shop s : pl.shops().all()) {
                boolean ok = switch (kind) {
                    case ADMIN -> s.isAdmin();
                    case PLAYER -> !s.isAdmin();
                    case MINE -> !s.isAdmin() && s.owner.equals(p.getUniqueId());
                };
                if (ok) list.add(s);
            }
            this.page = clampPage(page, list.size());
            inv = Bukkit.createInventory(this, 54, Text.c("&8" + listTitle(kind)));
            nav(inv, this.page, list.size());
            for (int i = 0; i < PAGE; i++) {
                int idx = this.page * PAGE + i;
                if (idx >= list.size()) break;
                Shop s = list.get(idx);
                inv.setItem(i, Text.icon(s.icon != null ? s.icon : (s.isAdmin() ? Material.ENDER_CHEST : Material.BARREL), "&f" + s.name,
                        "&7Owner: &f" + ownerName(s), "&7Listings: &f" + s.listings.size(),
                        kind == Kind.MINE ? "&eClick to manage" : "&aClick to open"));
            }
        }

        @Override
        public void click(Player p, int slot) {
            if (slot == 49) { openMain(p); return; }
            if (slot == 45 && page > 0) { openList(p, kind, page - 1); return; }
            if (slot == 53 && (page + 1) * PAGE < list.size()) { openList(p, kind, page + 1); return; }
            if (slot < PAGE) {
                int idx = page * PAGE + slot;
                if (idx < list.size()) {
                    Shop s = list.get(idx);
                    if (kind == Kind.MINE) openManage(p, s, 0);
                    else openShop(p, s, 0);
                }
            }
        }
    }

    // ---------- browsing a shop ----------

    class ShopMenu extends Menu {
        final Shop shop;
        final int page;

        ShopMenu(Shop shop, int page) {
            this.shop = shop;
            this.page = clampPage(page, shop.listings.size());
            inv = Bukkit.createInventory(this, 54, Text.c("&8" + shop.name));
            nav(inv, this.page, shop.listings.size());
            Inventory stock = shop.isAdmin() ? null : pl.shops().stockInv(shop);
            for (int i = 0; i < PAGE; i++) {
                int idx = this.page * PAGE + i;
                if (idx >= shop.listings.size()) break;
                inv.setItem(i, display(shop.listings.get(idx), false, shop, stock));
            }
        }

        @Override
        public void click(Player p, int slot) {
            if (!pl.shops().exists(shop)) { pl.send(p, "err-gone"); openMain(p); return; }
            if (slot == 49) { openMain(p); return; }
            if (slot == 45 && page > 0) { openShop(p, shop, page - 1); return; }
            if (slot == 53 && (page + 1) * PAGE < shop.listings.size()) { openShop(p, shop, page + 1); return; }
            if (slot < PAGE) {
                int idx = page * PAGE + slot;
                if (idx < shop.listings.size()) openPurchase(p, shop, shop.listings.get(idx));
            }
        }
    }

    // ---------- purchase ----------

    class PurchaseMenu extends Menu {
        final Shop shop;
        final Listing l;
        int payIdx = 0;
        int qty = 1;

        PurchaseMenu(Shop shop, Listing l) {
            this.shop = shop;
            this.l = l;
            inv = Bukkit.createInventory(this, 36, Text.c("&8Buy: " + Text.itemName(l.item)));
            render();
        }

        void render() {
            fill(inv);
            ItemStack show = l.item.clone();
            ItemMeta sm = show.getItemMeta();
            List<Component> slore = sm.hasLore() ? new ArrayList<>(sm.lore()) : new ArrayList<>();
            slore.add(Text.c("&7Per purchase: &f" + l.item.getAmount() + "x"));
            if (shop == null) {
                slore.add(Text.c("&7Seller: &f" + sellerName(l.seller)));
                slore.add(Text.c("&7Available: &f" + l.units));
            }
            sm.lore(slore);
            show.setItemMeta(sm);
            inv.setItem(4, show);

            for (int i = 0; i < 4; i++) {
                if (i < l.payments.size()) {
                    ItemStack pay = l.payments.get(i).clone();
                    ItemMeta pm = pay.getItemMeta();
                    List<Component> plore = pm.hasLore() ? new ArrayList<>(pm.lore()) : new ArrayList<>();
                    plore.add(Text.c(i == payIdx ? "&aSelected payment" : "&7Click to select this payment"));
                    pm.lore(plore);
                    pm.setEnchantmentGlintOverride(i == payIdx);
                    pay.setItemMeta(pm);
                    inv.setItem(10 + i, pay);
                } else {
                    inv.setItem(10 + i, Text.icon(Material.BLACK_STAINED_GLASS_PANE, " "));
                }
            }
            inv.setItem(19, Text.icon(Material.PAPER, "&fQuantity x1"));
            inv.setItem(20, Text.icon(Material.PAPER, "&fQuantity x8"));
            inv.setItem(21, Text.icon(Material.PAPER, "&fQuantity x64"));
            inv.setItem(22, Text.icon(Material.GOLD_NUGGET, "&bMax", "&7As many as you can afford and the shop has"));
            inv.setItem(23, Text.icon(Material.WRITABLE_BOOK, "&dCustom quantity", "&7Type a number in chat"));
            ItemStack pay = l.payments.get(Math.min(payIdx, l.payments.size() - 1));
            inv.setItem(25, Text.icon(Material.NAME_TAG, "&fQuantity: &e" + qty,
                    "&7Each purchase gives " + l.item.getAmount() + "x"));
            inv.setItem(27, Text.icon(Material.BARRIER, "&cBack"));
            inv.setItem(31, Text.icon(Material.LIME_CONCRETE, "&aConfirm trade",
                    "&7You give: &e" + (pay.getAmount() * qty) + "x " + Text.itemName(pay),
                    "&7You get: &a" + (l.item.getAmount() * qty) + "x " + Text.itemName(l.item)));
        }

        @Override
        public void click(Player p, int slot) {
            if (!alive(shop, l)) {
                pl.send(p, "err-gone");
                openMain(p);
                return;
            }
            if (slot >= 10 && slot <= 13 && slot - 10 < l.payments.size()) { payIdx = slot - 10; render(); }
            else if (slot == 19) { qty = 1; render(); }
            else if (slot == 20) { qty = 8; render(); }
            else if (slot == 21) { qty = 64; render(); }
            else if (slot == 22) { qty = Math.max(1, Trade.maxQty(pl, p, shop, l, payIdx)); render(); }
            else if (slot == 23) {
                pl.prompt(p, "prompt-qty", text -> {
                    if (!text.trim().equalsIgnoreCase("cancel")) {
                        try {
                            int q = Integer.parseInt(text.trim());
                            if (q < 1) throw new NumberFormatException();
                            qty = Math.min(q, pl.getConfig().getInt("max-quantity", 2304));
                        } catch (NumberFormatException ex) {
                            pl.send(p, "err-bad-qty");
                        }
                    }
                    render();
                    reopen(p, this);
                });
            } else if (slot == 27) {
                if (shop == null) openMarket(p, Category.ALL, 0, 0);
                else openShop(p, shop, 0);
            }
            else if (slot == 31) {
                String err = Trade.execute(pl, p, shop, l, payIdx, qty);
                if (err != null) pl.send(p, "err-" + err);
                render();
            }
        }
    }

    // ---------- manage (owner / admin) ----------

    class ManageMenu extends Menu {
        final Shop shop;
        final int page;
        boolean confirmClose = false;

        ManageMenu(Shop shop, int page) {
            this.shop = shop;
            this.page = clampPage(page, shop.listings.size());
            inv = Bukkit.createInventory(this, 54, Text.c("&8Manage: " + shop.name));
            nav(inv, this.page, shop.listings.size());
            Inventory stock = shop.isAdmin() ? null : pl.shops().stockInv(shop);
            for (int i = 0; i < PAGE; i++) {
                int idx = this.page * PAGE + i;
                if (idx >= shop.listings.size()) break;
                inv.setItem(i, display(shop.listings.get(idx), true, shop, stock));
            }
            inv.setItem(46, Text.icon(Material.NAME_TAG, "&eRename shop"));
            inv.setItem(47, Text.icon(Material.TNT, "&cClose shop", "&7Removes the shop. Stock stays in the container."));
            inv.setItem(51, Text.icon(Material.EMERALD, "&aAdd listing", "&7Set an item to sell and its price"));
        }

        @Override
        public void click(Player p, int slot) {
            if (!canManage(p, shop)) { p.closeInventory(); return; }
            if (slot == 49) {
                if (shop.isAdmin()) openMain(p); else openList(p, Kind.MINE, 0);
            } else if (slot == 45 && page > 0) { openManage(p, shop, page - 1); }
            else if (slot == 53 && (page + 1) * PAGE < shop.listings.size()) { openManage(p, shop, page + 1); }
            else if (slot == 51) { openSetup(p, shop, page); }
            else if (slot == 46) {
                pl.prompt(p, "prompt-rename", text -> {
                    String t = text.trim();
                    if (!t.isEmpty() && !t.equalsIgnoreCase("cancel") && pl.shops().exists(shop)) {
                        shop.name = t.length() > 24 ? t.substring(0, 24) : t;
                        pl.shops().save();
                        pl.send(p, "renamed", "name", shop.name);
                    }
                    openManage(p, shop, page);
                });
            } else if (slot == 47) {
                if (!confirmClose) {
                    confirmClose = true;
                    inv.setItem(47, Text.icon(Material.TNT, "&4Click again to close the shop"));
                    pl.send(p, "close-confirm");
                } else {
                    pl.shops().remove(shop);
                    pl.send(p, "shop-closed");
                    openMain(p);
                }
            } else if (slot < PAGE) {
                int idx = page * PAGE + slot;
                if (idx < shop.listings.size()) {
                    shop.listings.remove(idx);
                    pl.shops().save();
                    pl.send(p, "listing-removed");
                    openManage(p, shop, page);
                }
            }
        }
    }

    // ---------- market browsing ----------

    class MarketMenu extends Menu {
        final Category cat;
        final int sort; // 0 = newest, 1 = item name
        final int page;
        final List<Listing> list = new ArrayList<>();

        MarketMenu(Category cat, int sort, int page) {
            this.cat = cat;
            this.sort = sort;
            for (Listing l : pl.shops().market()) {
                if (cat == Category.ALL || Category.of(pl, l.item) == cat) list.add(l);
            }
            if (sort == 1) list.sort(Comparator.comparing((Listing x) -> Text.itemName(x.item).toLowerCase(Locale.ROOT)));
            else list.sort(Comparator.comparingLong((Listing x) -> x.created).reversed());
            int pages = Math.max(1, (list.size() + MARKET_PAGE - 1) / MARKET_PAGE);
            this.page = Math.min(Math.max(0, page), pages - 1);
            inv = Bukkit.createInventory(this, 54, Text.c("&8Market: " + cat.label));
            Category[] cs = Category.values();
            for (int i = 0; i < cs.length; i++) {
                ItemStack ic = Text.icon(cs[i].icon, (cs[i] == cat ? "&a" : "&f") + cs[i].label);
                if (cs[i] == cat) {
                    ItemMeta meta = ic.getItemMeta();
                    meta.setEnchantmentGlintOverride(true);
                    ic.setItemMeta(meta);
                }
                inv.setItem(i, ic);
            }
            for (int i = 45; i < 54; i++) inv.setItem(i, Text.filler());
            if (this.page > 0) inv.setItem(45, Text.icon(Material.ARROW, "&ePrevious page"));
            if ((this.page + 1) * MARKET_PAGE < list.size()) inv.setItem(53, Text.icon(Material.ARROW, "&eNext page"));
            inv.setItem(47, Text.icon(Material.COMPARATOR, "&eSort: " + (sort == 1 ? "Item name" : "Newest"), "&7Click to change"));
            inv.setItem(49, Text.icon(Material.BARRIER, "&cBack"));
            for (int i = 0; i < MARKET_PAGE; i++) {
                int idx = this.page * MARKET_PAGE + i;
                if (idx >= list.size()) break;
                inv.setItem(9 + i, marketDisplay(list.get(idx), "&aClick to buy"));
            }
        }

        @Override
        public void click(Player p, int slot) {
            if (slot < 9) { openMarket(p, Category.values()[slot], sort, 0); return; }
            if (slot == 45 && page > 0) { openMarket(p, cat, sort, page - 1); return; }
            if (slot == 47) { openMarket(p, cat, 1 - sort, 0); return; }
            if (slot == 49) { openMain(p); return; }
            if (slot == 53 && (page + 1) * MARKET_PAGE < list.size()) { openMarket(p, cat, sort, page + 1); return; }
            if (slot >= 9 && slot < 45) {
                int idx = page * MARKET_PAGE + (slot - 9);
                if (idx < list.size()) openPurchase(p, null, list.get(idx));
            }
        }
    }

    // ---------- my market listings ----------

    class MyListingsMenu extends Menu {
        final int page;
        final List<Listing> list;

        MyListingsMenu(Player p, int page) {
            list = pl.shops().marketOf(p.getUniqueId());
            this.page = clampPage(page, list.size());
            inv = Bukkit.createInventory(this, 54, Text.c("&8My listings"));
            nav(inv, this.page, list.size());
            for (int i = 0; i < PAGE; i++) {
                int idx = this.page * PAGE + i;
                if (idx >= list.size()) break;
                inv.setItem(i, marketDisplay(list.get(idx), "&eClick to manage"));
            }
            inv.setItem(51, Text.icon(Material.EMERALD, "&aNew listing", "&7Sell items on the market"));
        }

        @Override
        public void click(Player p, int slot) {
            if (slot == 49) { openMain(p); return; }
            if (slot == 45 && page > 0) { openMyListings(p, page - 1); return; }
            if (slot == 53 && (page + 1) * PAGE < list.size()) { openMyListings(p, page + 1); return; }
            if (slot == 51) { openSetup(p, null, 0); return; }
            if (slot < PAGE) {
                int idx = page * PAGE + slot;
                if (idx < list.size()) openListingManage(p, list.get(idx));
            }
        }
    }

    class ListingManageMenu extends Menu {
        final Listing l;

        ListingManageMenu(Listing l) {
            this.l = l;
            inv = Bukkit.createInventory(this, 27, Text.c("&8Manage listing"));
            render();
        }

        void render() {
            fill(inv);
            inv.setItem(4, marketDisplay(l, null));
            inv.setItem(10, Text.icon(Material.LIME_STAINED_GLASS_PANE, "&aAdd 1 unit", "&7Takes the items from your inventory"));
            inv.setItem(11, Text.icon(Material.LIME_STAINED_GLASS_PANE, "&aAdd 8 units", "&7Takes the items from your inventory"));
            inv.setItem(12, Text.icon(Material.LIME_STAINED_GLASS_PANE, "&aAdd 64 units", "&7Takes the items from your inventory"));
            inv.setItem(16, Text.icon(Material.TNT, "&cCancel listing", "&7Stock goes to your collect box"));
            inv.setItem(22, Text.icon(Material.BARRIER, "&cBack"));
        }

        private boolean mayManage(Player p) {
            return l.seller.equals(p.getUniqueId()) || p.hasPermission("materialshop.admin");
        }

        @Override
        public void click(Player p, int slot) {
            if (pl.shops().marketGet(l.id) != l || !mayManage(p)) {
                pl.send(p, "err-gone");
                openMyListings(p, 0);
                return;
            }
            if (slot == 22) { openMyListings(p, 0); return; }
            if (slot == 10) addUnits(p, 1);
            else if (slot == 11) addUnits(p, 8);
            else if (slot == 12) addUnits(p, 64);
            else if (slot == 16) cancel(p);
        }

        private void addUnits(Player p, int n) {
            int need = l.item.getAmount() * n;
            if (l.units + n > pl.getConfig().getInt("max-quantity", 2304)) { pl.send(p, "err-bad-qty"); return; }
            if (Trade.count(p.getInventory(), l.item) < need) { pl.send(p, "setup-no-stock"); return; }
            if (!Trade.remove(p.getInventory(), l.item, need)) { pl.send(p, "setup-no-stock"); return; }
            l.units += n;
            pl.shops().saveMarket();
            pl.send(p, "stock-added");
            render();
        }

        private void cancel(Player p) {
            int total = l.units * l.item.getAmount();
            if (!pl.shops().canStore(l.seller, l.item, total)) { pl.send(p, "cancel-box-full"); return; }
            pl.shops().store(l.seller, l.item, total);
            pl.shops().saveBoxes();
            pl.shops().removeMarket(l);
            pl.send(p, "listing-cancelled");
            openMyListings(p, 0);
        }
    }

    // ---------- add listing (market listing when shop == null, otherwise a chest or server shop listing) ----------

    class SetupMenu extends Menu {
        final Shop shop;
        final int page;
        int units = 1;
        boolean confirmed = false;

        SetupMenu(Shop shop, int page) {
            this.shop = shop;
            this.page = page;
            inv = Bukkit.createInventory(this, 36, Text.c("&8Add listing"));
            render();
        }

        boolean isSlot(int s) {
            if (s == SELL_SLOT) return true;
            for (int x : PAY_SLOTS) if (x == s) return true;
            return false;
        }

        void render() {
            for (int i = 0; i < inv.getSize(); i++) if (!isSlot(i)) inv.setItem(i, Text.filler());
            inv.setItem(0, Text.icon(Material.BOOK, "&eHow it works",
                    "&7Top middle slot: the item you sell.",
                    "&7Its stack size is the amount per purchase.",
                    shop == null ? "&7It counts as the first unit. More units come from your inventory."
                            : "&7Stock lives in the shop container.",
                    "&7Side slots: payment options, up to 4.",
                    "&7Your items are returned when you leave."));
            inv.setItem(13, Text.icon(Material.PAPER, "&7Payment options", "&7Put payment items in the", "&7slots on both sides"));
            inv.setItem(27, Text.icon(Material.RED_CONCRETE, "&cCancel"));
            inv.setItem(33, Text.icon(Material.LIME_CONCRETE, "&aConfirm listing"));
            if (shop == null) {
                inv.setItem(28, Text.icon(Material.RED_STAINED_GLASS_PANE, "&c-8 units"));
                inv.setItem(29, Text.icon(Material.RED_STAINED_GLASS_PANE, "&c-1 unit"));
                inv.setItem(30, Text.icon(Material.CHEST, "&fStock units: &e" + units, "&7Purchase units to put on the market"));
                inv.setItem(31, Text.icon(Material.LIME_STAINED_GLASS_PANE, "&a+1 unit"));
                inv.setItem(32, Text.icon(Material.LIME_STAINED_GLASS_PANE, "&a+8 units"));
            }
        }

        @Override
        public void click(Player p, int slot) { /* handled in handle() */ }

        void handle(Player p, InventoryClickEvent e) {
            boolean inTop = e.getRawSlot() >= 0 && e.getRawSlot() < inv.getSize();
            if (inTop) {
                int s = e.getRawSlot();
                if (isSlot(s)) return; // free slots: normal item movement
                e.setCancelled(true);
                int cap = pl.getConfig().getInt("max-quantity", 2304);
                if (s == 33) confirm(p);
                else if (s == 27) back(p);
                else if (shop == null && s == 28) { units = Math.max(1, units - 8); render(); }
                else if (shop == null && s == 29) { units = Math.max(1, units - 1); render(); }
                else if (shop == null && s == 31) { units = Math.min(cap, units + 1); render(); }
                else if (shop == null && s == 32) { units = Math.min(cap, units + 8); render(); }
            } else if (e.isShiftClick() || e.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
                e.setCancelled(true);
            }
        }

        private void back(Player p) {
            Bukkit.getScheduler().runTask(pl, () -> {
                p.closeInventory(); // returns the items from the free slots
                if (shop == null) openMyListings(p, 0);
                else openManage(p, shop, page);
            });
        }

        private void confirm(Player p) {
            if (confirmed) return;
            if (shop != null && !canManage(p, shop)) return;
            ItemStack sell = inv.getItem(SELL_SLOT);
            if (sell == null || sell.getType().isAir()) { pl.send(p, "setup-no-item"); return; }
            List<ItemStack> pays = new ArrayList<>();
            for (int s : PAY_SLOTS) {
                ItemStack it = inv.getItem(s);
                if (it != null && !it.getType().isAir()) pays.add(it.clone());
            }
            if (pays.isEmpty()) { pl.send(p, "setup-no-price"); return; }
            if (shop == null || !shop.isAdmin()) {
                for (ItemStack pay : pays) {
                    if (!pl.paymentAllowed(pay.getType())) {
                        pl.send(p, "setup-blocked", "item", Text.pretty(pay.getType()));
                        return;
                    }
                }
            }
            if (shop == null) {
                if (pl.shops().countMarket(p.getUniqueId()) >= pl.getConfig().getInt("market.max-listings-per-player", 20)) {
                    pl.send(p, "listing-limit");
                    return;
                }
                int extra = sell.getAmount() * (units - 1);
                if (extra > 0 && Trade.count(p.getInventory(), sell) < extra) { pl.send(p, "setup-no-stock"); return; }
                // the escrow only exists once every unit is really in the plugin's hands
                if (extra > 0 && !Trade.remove(p.getInventory(), sell, extra)) { pl.send(p, "setup-no-stock"); return; }
                confirmed = true;
                pl.shops().addMarket(p.getUniqueId(), sell, pays, units);
                inv.setItem(SELL_SLOT, null); // consumed: it is now the first unit in escrow
            } else {
                if (shop.listings.size() >= pl.getConfig().getInt("max-listings", 100)) { pl.send(p, "setup-too-many"); return; }
                confirmed = true;
                shop.listings.add(new Listing(shop.nextListingId++, sell, pays));
                pl.shops().save();
            }

            double ratio = pl.getConfig().getDouble("fairness-warning-ratio", 3.0);
            double vs = pl.value(sell.getType()) * sell.getAmount();
            for (ItemStack pay : pays) {
                double vp = pl.value(pay.getType()) * pay.getAmount();
                if (ratio > 0 && vs > 0 && vp > 0 && (vp / vs > ratio || vs / vp > ratio)) {
                    pl.send(p, "fair-warn");
                    break;
                }
            }
            pl.send(p, "listing-added");
            back(p);
        }

        void giveBack(Player p) {
            List<Integer> slots = new ArrayList<>();
            slots.add(SELL_SLOT);
            for (int s : PAY_SLOTS) slots.add(s);
            for (int s : slots) {
                ItemStack it = inv.getItem(s);
                if (it == null || it.getType().isAir()) continue;
                inv.setItem(s, null);
                for (ItemStack left : p.getInventory().addItem(it).values()) {
                    p.getWorld().dropItemNaturally(p.getLocation(), left);
                }
            }
        }
    }

    // ---------- collect box ----------

    class CollectMenu extends Menu {
        final List<ItemStack> box;

        CollectMenu(Player p) {
            box = pl.shops().box(p.getUniqueId());
            inv = Bukkit.createInventory(this, 54, Text.c("&8Collect box"));
            render();
        }

        void render() {
            inv.clear();
            for (int i = 45; i < 54; i++) inv.setItem(i, Text.filler());
            inv.setItem(49, Text.icon(Material.BARRIER, "&cBack"));
            for (int i = 0; i < Math.min(PAGE, box.size()); i++) inv.setItem(i, box.get(i).clone());
        }

        @Override
        public void click(Player p, int slot) {
            if (slot == 49) { openMain(p); return; }
            if (slot >= PAGE || slot >= box.size()) return;
            ItemStack it = box.get(slot).clone();
            box.remove(slot);
            List<ItemStack> leftover = new ArrayList<>(p.getInventory().addItem(it).values());
            if (!leftover.isEmpty()) {
                box.addAll(Math.min(slot, box.size()), leftover);
                pl.send(p, "collect-no-room");
            } else {
                pl.send(p, "collected");
            }
            pl.shops().saveBoxes();
            render();
        }
    }
}
