package com.banglaverse.materialshop;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Shop {
    public final UUID id;
    public final UUID owner; // null = admin shop (infinite stock)
    public String name = "Shop";
    public final List<String> blocks = new ArrayList<>(); // container block keys, first = primary
    public final List<Listing> listings = new ArrayList<>();
    public int nextListingId = 1;
    public boolean catalog = false; // built from catalog.yml, never saved to shops.yml, read-only in menus
    public Material icon = null;    // menu icon for catalog shops

    public Shop(UUID id, UUID owner) {
        this.id = id;
        this.owner = owner;
    }

    public boolean isAdmin() {
        return owner == null;
    }

    public Listing find(int listingId) {
        for (Listing l : listings) if (l.id == listingId) return l;
        return null;
    }
}
