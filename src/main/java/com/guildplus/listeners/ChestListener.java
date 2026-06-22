package com.guildplus.listeners;

import com.guildplus.chest.GuildChestManager;
import com.guildplus.guild.Guild;
import com.guildplus.guild.GuildManager;
import com.guildplus.utils.ColorUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChestListener implements Listener {

    private final GuildChestManager chestManager;
    private final GuildManager guildManager;
    // player -> guildId (チェストを開いているプレイヤー)
    private final Map<UUID, String> openChests = new HashMap<>();

    public ChestListener(GuildChestManager chestManager, GuildManager guildManager) {
        this.chestManager = chestManager;
        this.guildManager = guildManager;
    }

    public void registerOpenChest(UUID playerUuid, String guildId) {
        openChests.put(playerUuid, guildId);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String guildId = openChests.get(player.getUniqueId());
        if (guildId == null) return;

        Guild guild = guildManager.getGuildById(guildId);
        if (guild == null) return;

        Inventory chestInv = chestManager.getChest(guildId, guild.getName());
        // ギルドチェストのスロットをクリックした場合のみ検証
        if (event.getClickedInventory() == null) return;
        if (!event.getClickedInventory().equals(chestInv)) return;

        int slot = event.getSlot();
        var cursor = event.getCursor();
        var current = event.getCurrentItem();

        // カーソルにアイテムがある場合 → 配置しようとしている
        if (cursor != null && cursor.getType() != org.bukkit.Material.AIR) {
            if (chestManager.isRestricted(slot, cursor)) {
                event.setCancelled(true);
                player.sendMessage(ColorUtils.colorize("&cこのスロットには置けないアイテムです。"));
                return;
            }
        }
        // SHIFTクリックで移動してくる場合は複雑なので、一律に現在のアイテムを確認
        if (event.isShiftClick() && current != null && current.getType() != org.bukkit.Material.AIR) {
            // 移動先スロットを特定するのは難しいため、制限アイテムのShiftClickを禁止
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        String guildId = openChests.remove(player.getUniqueId());
        if (guildId == null) return;

        Guild guild = guildManager.getGuildById(guildId);
        if (guild == null) return;

        Inventory chestInv = chestManager.getChest(guildId, guild.getName());
        chestManager.saveChest(guildId, chestInv);
    }
}
