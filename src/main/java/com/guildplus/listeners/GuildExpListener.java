package com.guildplus.listeners;

import com.guildplus.GuildPlusPlugin;
import com.guildplus.guild.Guild;
import com.guildplus.guild.GuildManager;
import com.guildplus.utils.ColorUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.Bukkit;
import java.util.UUID;

/**
 * Mob 討伐でギルドEXPを付与するリスナー。
 * 付与量は config.yml の guild.exp-per-kill で設定可能。
 */
public class GuildExpListener implements Listener {

    private final GuildPlusPlugin plugin;
    private final GuildManager guildManager;

    public GuildExpListener(GuildPlusPlugin plugin, GuildManager guildManager) {
        this.plugin = plugin;
        this.guildManager = guildManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();

        // プレイヤー同士は除外（プレイヤーキルは別途対応可能）
        if (entity instanceof Player) return;
        if (!(entity instanceof LivingEntity)) return;

        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        Guild guild = guildManager.getGuildByPlayer(killer.getUniqueId());
        if (guild == null) return;

        if (!plugin.getConfig().getBoolean("guild.exp-enabled", true)) return;

        // エンティティタイプに応じてEXP量を変える（config で個別設定も可能）
        long expBase = plugin.getConfig().getLong("guild.exp-per-kill", 5L);
        String entityTypeName = entity.getType().name().toLowerCase();
        long bonus = plugin.getConfig().getLong("guild.exp-bonus." + entityTypeName, 0L);
        long expGain = expBase + bonus;

        if (expGain <= 0) return;

        int oldLevel = guild.getLevel();
        guild.addExp(expGain);
        int newLevel = guild.getLevel();

        guildManager.save();

        // レベルアップ通知
        if (newLevel > oldLevel) {
            String msg = ColorUtils.colorize("&6&l[GuildPlus] &eギルド &6" + guild.getName()
                    + " &eがレベル &6" + newLevel + " &eに上がりました！ 🎉");
            for (UUID uuid : guild.getMemberList()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) p.sendMessage(msg);
            }
        }
    }
}
