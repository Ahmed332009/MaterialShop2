package com.banglaverse.materialshop;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Trade logic. Everything runs on the main thread, so a trade is validated and applied in one step. */
public final class Trade {
    private Trade() {}

    public static int count(Inventory inv, ItemStack t) {
        int n = 0;
        for (ItemStack s : inv.getStorageContents()) if (s != null && s.isSimilar(t)) n += s.getAmount();
        return n;
    }

    public static ItemStack[] snapshot(Inventory inv) {
        ItemStack[] c = inv.getStorageContents();
        ItemStack[] r = new ItemStack[c.length];
        for (int i = 0; i < c.length; i++) r[i] = c[i] == null ? null : c[i].clone();
        return r;
    }

    public static boolean remove(Inventory inv, ItemStack t, int amount) {
        if (count(inv, t) < amount) return false;
        ItemStack[] c = snapshot(inv);
        int left = amount;
        for (int i = 0; i < c.length && left > 0; i++) {
            ItemStack s = c[i];
            if (s != null && s.isSimilar(t)) {
                int take = Math.min(left, s.getAmount());
                if (take == s.getAmount()) c[i] = null;
                else s.setAmount(s.getAmount() - take);
                left -= take;
            }
        }
        inv.setStorageContents(c);
        return true;
    }

    public static boolean fits(Inventory inv, ItemStack t, int total) {
        int max = t.getMaxStackSize();
        int free = 0;
        for (ItemStack s : inv.getStorageContents()) {
            if (s == null || s.getType().isAir()) free += max;
            else if (s.isSimilar(t)) free += Math.max(0, max - s.getAmount());
        }
        return free >= total;
    }

    public static List<ItemStack> stacks(ItemStack t, int total) {
        List<ItemStack> out = new ArrayList<>();
        int max = t.getMaxStackSize();
        while (total > 0) {
            ItemStack n = t.clone();
            int a = Math.min(max, total);
            n.setAmount(a);
            out.add(n);
            total -= a;
        }
        return out;
    }

    public static int maxQty(MaterialShop pl, Player buyer, Shop shop, Listing l, int payIdx) {
        int cap = pl.getConfig().getInt("max-quantity", 2304);
        ItemStack pay = l.payments.get(payIdx);
        int byPay = count(buyer.getInventory(), pay) / Math.max(1, pay.getAmount());
        int byStock = cap;
        if (shop == null) {
            byStock = l.units;
        } else if (!shop.isAdmin()) {
            Inventory stock = pl.shops().stockInv(shop);
            byStock = stock == null ? 0 : count(stock, l.item) / Math.max(1, l.item.getAmount());
        }
        return Math.max(0, Math.min(cap, Math.min(byPay, byStock)));
    }

    /**
     * Runs one trade. shop == null means a market listing (stock in escrow).
     * Returns null on success, otherwise a message key suffix (used as "err-" + result).
     */
    public static String execute(MaterialShop pl, Player buyer, Shop shop, Listing l, int payIdx, int qty) {
        boolean market = shop == null;
        if (market) {
            if (pl.shops().marketGet(l.id) != l) return "gone";
        } else if (!pl.shops().exists(shop) || shop.find(l.id) != l) {
            return "gone";
        }
        if (payIdx < 0 || payIdx >= l.payments.size()) return "gone";
        if (qty < 1 || qty > pl.getConfig().getInt("max-quantity", 2304)) return "bad-qty";
        UUID seller = market ? l.seller : (shop.isAdmin() ? null : shop.owner);
        if (seller != null && seller.equals(buyer.getUniqueId())) return "own-shop";

        ItemStack pay = l.payments.get(payIdx);
        int payTotal = pay.getAmount() * qty;
        int itemTotal = l.item.getAmount() * qty;

        Inventory stock = null;
        if (market) {
            if (l.units < qty) return "no-stock";
        } else if (!shop.isAdmin()) {
            stock = pl.shops().stockInv(shop);
            if (stock == null) return "broken";
            if (count(stock, l.item) < itemTotal) return "no-stock";
        }
        if (seller != null && !pl.shops().canStore(seller, pay, payTotal)) return "box-full";

        Inventory bi = buyer.getInventory();
        if (count(bi, pay) < payTotal) return "no-payment";

        ItemStack[] snap = snapshot(bi);
        if (!remove(bi, pay, payTotal)) return "no-payment";
        if (!fits(bi, l.item, itemTotal)) {
            bi.setStorageContents(snap);
            return "no-room";
        }
        Map<Integer, ItemStack> left = bi.addItem(stacks(l.item, itemTotal).toArray(new ItemStack[0]));
        if (!left.isEmpty()) {
            bi.setStorageContents(snap);
            return "no-room";
        }
        if (stock != null && !remove(stock, l.item, itemTotal)) {
            bi.setStorageContents(snap);
            return "no-stock";
        }
        if (market) {
            l.units -= qty;
            if (l.units <= 0) pl.shops().removeMarket(l);
            else pl.shops().saveMarket();
        }
        if (seller != null) {
            pl.shops().store(seller, pay, payTotal);
            pl.shops().saveBoxes();
        }

        String itemName = Text.itemName(l.item);
        String payName = Text.itemName(pay);
        String where = market ? "market listing=" + l.id : "shop=" + shop.id + " \"" + shop.name + "\"";
        pl.log(buyer.getName() + " (" + buyer.getUniqueId() + ") bought " + itemTotal + "x " + itemName
                + " for " + payTotal + "x " + payName + " from " + (seller == null ? "ADMIN" : seller.toString())
                + " " + where);
        pl.send(buyer, "trade-success", "qty", String.valueOf(itemTotal), "item", itemName,
                "pay", payTotal + "x " + payName);
        if (seller != null) {
            Player o = Bukkit.getPlayer(seller);
            if (o != null) {
                pl.send(o, "trade-sold", "buyer", buyer.getName(), "qty", String.valueOf(itemTotal),
                        "item", itemName, "shop", market ? "Market" : shop.name);
            }
        }
        return null;
    }
}
