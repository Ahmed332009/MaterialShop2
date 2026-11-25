package com.banglaverse.materialshop;

import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One thing for sale. item.getAmount() is the number of items per purchase unit.
 * Market listings also have a seller, a stock of purchase units held in escrow by the plugin, and a creation time.
 */
public class Listing {
    public final int id;
    public final ItemStack item;
    public final List<ItemStack> payments;
    public UUID seller;     // null for shop listings (chest and server shops)
    public int units;       // escrowed purchase units, market listings only
    public long created;    // epoch millis, market listings only

    public Listing(int id, ItemStack item, List<ItemStack> payments) {
        this.id = id;
        this.item = item.clone();
        this.payments = new ArrayList<>();
        for (ItemStack p : payments) this.payments.add(p.clone());
    }

    public Listing(int id, ItemStack item, List<ItemStack> payments, UUID seller, int units, long created) {
        this(id, item, payments);
        this.seller = seller;
        this.units = units;
        this.created = created;
    }
}
