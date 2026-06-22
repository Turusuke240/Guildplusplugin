package com.guildplus.chest;

import com.guildplus.GuildPlusPlugin;
import com.guildplus.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuildChestManager {

    private final GuildPlusPlugin plugin;
    private final File chestFile;
    private final YamlConfiguration chestConfig;
    // guildId -> Inventory (runtime cache)
    private final Map<String, Inventory> chestCache = new HashMap<>();

    // 専用スロット
    private final List<Integer> waterBucketSlots;
    private final List<Integer> arrowSlots;

    @SuppressWarnings("unchecked")
    public GuildChestManager(GuildPlusPlugin plugin) {
        this.plugin = plugin;
        chestFile = new File(plugin.getDataFolder(), "chests.yml");
        if (!chestFile.exists()) {
            try { chestFile.createNewFile(); } catch (IOException ignored) {}
        }
        chestConfig = YamlConfiguration.loadConfiguration(chestFile);
        waterBucketSlots = (List<Integer>) plugin.getConfig().getList("chest.water-bucket-slots", List.of(0,1,2,3,4,5,6));
        arrowSlots = (List<Integer>) plugin.getConfig().getList("chest.arrow-slots", List.of(7,8,9,10));
    }

    public Inventory getChest(String guildId, String guildName) {
        if (chestCache.containsKey(guildId)) {
            return chestCache.get(guildId);
        }
        // YAMLから読み込む
        Inventory inv = Bukkit.createInventory(null, 54,
                ColorUtils.colorize("&6ギルドチェスト: " + guildName));
        loadChest(guildId, inv);
        chestCache.put(guildId, inv);
        return inv;
    }

    /**
     * アイテムが専用スロットのルールに違反しているか検証する。
     * @return true なら投入不可
     */
    public boolean isRestricted(int slot, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;

        // 水バケツ専用スロット
        if (waterBucketSlots.contains(slot)) {
            return item.getType() != Material.WATER_BUCKET && item.getType() != Material.LAVA_BUCKET;
        }
        // 矢専用スロット
        if (arrowSlots.contains(slot)) {
            return item.getType() != Material.ARROW
                    && item.getType() != Material.SPECTRAL_ARROW
                    && item.getType() != Material.TIPPED_ARROW;
        }
        // 専用スロットに水バケツ/溶岩バケツ/矢を置こうとした場合も制限
        if (item.getType() == Material.WATER_BUCKET || item.getType() == Material.LAVA_BUCKET) {
            return !waterBucketSlots.contains(slot);
        }
        if (item.getType() == Material.ARROW || item.getType() == Material.SPECTRAL_ARROW || item.getType() == Material.TIPPED_ARROW) {
            return !arrowSlots.contains(slot);
        }
        return false;
    }

    public void saveChest(String guildId, Inventory inv) {
        chestConfig.set("chests." + guildId, null);
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                chestConfig.set("chests." + guildId + ".slot" + i, item);
            }
        }
        try {
            chestConfig.save(chestFile);
        } catch (IOException e) {
            plugin.getLogger().severe("ギルドチェストの保存に失敗しました: " + e.getMessage());
        }
    }

    private void loadChest(String guildId, Inventory inv) {
        if (!chestConfig.contains("chests." + guildId)) return;
        for (int i = 0; i < 54; i++) {
            ItemStack item = chestConfig.getItemStack("chests." + guildId + ".slot" + i);
            if (item != null) {
                inv.setItem(i, item);
            }
        }
    }

    public void deleteChest(String guildId) {
        chestCache.remove(guildId);
        chestConfig.set("chests." + guildId, null);
        try {
            chestConfig.save(chestFile);
        } catch (IOException ignored) {}
    }
}
