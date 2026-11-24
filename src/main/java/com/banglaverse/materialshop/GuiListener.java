package com.banglaverse.materialshop;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

import java.util.function.Consumer;

public class GuiListener implements Listener {
    private final MaterialShop pl;

    public GuiListener(MaterialShop pl) { this.pl = pl; }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder(false) instanceof Gui.Menu m)) return;
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (m instanceof Gui.SetupMenu s) {
            s.handle(p, e);
            return;
        }
        // every other menu is read-only: nothing may move in or out
        e.setCancelled(true);
        int raw = e.getRawSlot();
        if (raw >= 0 && raw < top.getSize()) m.click(p, raw);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Gui.Menu) e.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder(false) instanceof Gui.SetupMenu s && e.getPlayer() instanceof Player p) {
            s.giveBack(p);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent e) {
        Consumer<String> cb = pl.takePrompt(e.getPlayer().getUniqueId());
        if (cb == null) return;
        e.setCancelled(true);
        String text = Text.plain(e.message());
        Bukkit.getScheduler().runTask(pl, () -> cb.accept(text));
    }
}
