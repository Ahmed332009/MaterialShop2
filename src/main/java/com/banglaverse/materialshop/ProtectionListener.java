package com.banglaverse.materialshop;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

/** Keeps shop containers safe and opens shops from containers and shop villagers. */
public class ProtectionListener implements Listener {
    private final MaterialShop pl;

    public ProtectionListener(MaterialShop pl) { this.pl = pl; }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND) return;
        Block b = e.getClickedBlock();
        if (b == null) return;
        Shop s = pl.shops().shopAt(b);
        if (s == null) return;
        Player p = e.getPlayer();
        // owner and bypass staff open the container normally to restock
        if (p.getUniqueId().equals(s.owner) || p.hasPermission("materialshop.admin.bypass")) return;
        e.setCancelled(true);
        pl.gui().openShop(p, s, 0);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Shop s = pl.shops().shopAt(e.getBlock());
        if (s == null) return;
        Player p = e.getPlayer();
        if (p.hasPermission("materialshop.admin.bypass")) {
            pl.shops().remove(s);
            return;
        }
        e.setCancelled(true);
        pl.send(p, "break-denied");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent e) {
        e.blockList().removeIf(b -> pl.shops().shopAt(b) != null);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        e.blockList().removeIf(b -> pl.shops().shopAt(b) != null);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(InventoryMoveItemEvent e) {
        if (isShop(e.getSource()) || isShop(e.getDestination())) e.setCancelled(true);
    }

    private boolean isShop(Inventory inv) {
        InventoryHolder h = inv.getHolder();
        if (h instanceof DoubleChest dc) h = dc.getLeftSide();
        if (h instanceof BlockState bs) return pl.shops().shopAt(bs.getBlock()) != null;
        return false;
    }

    /** A chest placed next to a shop chest would merge into a double chest and bypass protection. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Block b = e.getBlockPlaced();
        Material m = b.getType();
        if (m != Material.CHEST && m != Material.TRAPPED_CHEST) return;
        BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        for (BlockFace f : faces) {
            Block n = b.getRelative(f);
            if (n.getType() == m && pl.shops().shopAt(n) != null) {
                e.setCancelled(true);
                pl.send(e.getPlayer(), "adjacent-denied");
                return;
            }
        }
    }

    // ---------- shop villagers ----------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onNpc(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Entity en = e.getRightClicked();
        String id = en.getPersistentDataContainer().get(pl.npcKey, PersistentDataType.STRING);
        if (id == null) return;
        e.setCancelled(true);
        try {
            Shop s = null;
            UUID u = UUID.fromString(id);
            for (Shop x : pl.shops().all()) if (x.id.equals(u)) s = x;
            if (s == null) { pl.send(e.getPlayer(), "err-gone"); return; }
            pl.gui().openShop(e.getPlayer(), s, 0);
        } catch (IllegalArgumentException ex) {
            pl.send(e.getPlayer(), "err-gone");
        }
    }
}
