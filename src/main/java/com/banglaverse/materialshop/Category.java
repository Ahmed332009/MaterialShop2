package com.banglaverse.materialshop;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Market tabs. The slot of each tab in the market menu is its ordinal, so keep exactly 9 values. */
public enum Category {
    ALL("All", Material.COMPASS),
    BLOCKS("Blocks", Material.GRASS_BLOCK),
    TOOLS("Tools and weapons", Material.IRON_PICKAXE),
    ARMOR("Armor", Material.IRON_CHESTPLATE),
    FOOD("Food", Material.BREAD),
    REDSTONE("Redstone", Material.REDSTONE),
    FARMING("Farming", Material.WHEAT_SEEDS),
    ORES("Ores and ingots", Material.DIAMOND),
    MISC("Misc and custom", Material.CHEST);

    public final String label;
    public final Material icon;

    Category(String label, Material icon) {
        this.label = label;
        this.icon = icon;
    }

    /** Config override first (categories.<NAME> lists materials), then named custom items, then a name heuristic. */
    public static Category of(MaterialShop pl, ItemStack it) {
        for (Category c : values()) {
            if (c == ALL) continue;
            if (pl.getConfig().getStringList("categories." + c.name()).contains(it.getType().name())) return c;
        }
        ItemMeta m = it.getItemMeta();
        if (m != null && m.hasDisplayName()) return MISC;
        return of(it.getType());
    }

    static Category of(Material m) {
        String n = m.name();
        if (n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE") || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS")
                || n.equals("SHIELD") || n.equals("ELYTRA")) return ARMOR;
        if (n.endsWith("_SWORD") || n.endsWith("_PICKAXE") || n.endsWith("_AXE") || n.endsWith("_SHOVEL")
                || n.endsWith("_HOE") || n.equals("BOW") || n.equals("CROSSBOW") || n.equals("TRIDENT")
                || n.equals("FISHING_ROD") || n.equals("SHEARS") || n.equals("FLINT_AND_STEEL") || n.equals("MACE")) return TOOLS;
        if (m.isEdible()) return FOOD;
        if (n.contains("REDSTONE") || n.contains("PISTON") || n.contains("REPEATER") || n.contains("COMPARATOR")
                || n.contains("OBSERVER") || n.equals("HOPPER") || n.equals("DROPPER") || n.equals("DISPENSER")
                || n.equals("LEVER") || n.endsWith("_BUTTON") || n.endsWith("_PRESSURE_PLATE") || n.equals("TNT")
                || n.equals("TARGET") || n.contains("DAYLIGHT") || n.endsWith("RAIL") || n.endsWith("_LAMP")) return REDSTONE;
        if (n.contains("SEEDS") || n.equals("WHEAT") || n.endsWith("_SAPLING") || n.equals("BONE_MEAL")
                || n.equals("SUGAR_CANE") || n.contains("PUMPKIN") || n.contains("MELON") || n.equals("COCOA_BEANS")
                || n.equals("NETHER_WART") || n.equals("BAMBOO") || n.equals("KELP") || n.equals("HAY_BLOCK")) return FARMING;
        if (n.endsWith("_INGOT") || n.endsWith("_ORE") || n.startsWith("RAW_") || n.equals("DIAMOND") || n.equals("EMERALD")
                || n.equals("COAL") || n.equals("CHARCOAL") || n.equals("LAPIS_LAZULI") || n.equals("QUARTZ")
                || n.equals("NETHERITE_SCRAP") || n.equals("ANCIENT_DEBRIS") || n.equals("AMETHYST_SHARD")) return ORES;
        if (m.isBlock()) return BLOCKS;
        return MISC;
    }
}
