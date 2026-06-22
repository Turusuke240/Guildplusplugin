package com.guildplus.listeners;

import com.guildplus.guild.Guild;
import com.guildplus.guild.GuildManager;
import com.guildplus.utils.ColorUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class PvpListener implements Listener {

    private final GuildManager guildManager;

    public PvpListener(GuildManager guildManager) {
        this.guildManager = guildManager;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        Guild victimGuild = guildManager.getGuildByPlayer(victim.getUniqueId());
        Guild attackerGuild = guildManager.getGuildByPlayer(attacker.getUniqueId());

        // 同じギルドメンバー同士
        if (victimGuild != null && victimGuild.equals(attackerGuild)) {
            if (!victimGuild.isPvpEnabled()) {
                event.setCancelled(true);
                attacker.sendMessage(ColorUtils.colorize("&cギルド内PVPは無効です。/guild pvp true で有効化できます。"));
            }
        }
    }
}
