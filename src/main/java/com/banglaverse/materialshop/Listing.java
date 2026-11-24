package com.banglaverse.materialshop;

import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.List;

/** One thing for sale. item.getAmount() is the number of items per purchase unit. */
public class Listing {
    public final int id;
    public final ItemStack item;
    public final List<ItemStack> payments;

    public Listing(int id, ItemStack item, List<ItemStack> payments) {
        this.id = id;
        this.item = item.clone();
        this.payments = new ArrayList<>();
        for (ItemStack p : payments) this.payments.add(p.clone());
    }
}
