package com.guildplus.listeners;

import com.guildplus.guild.Guild;
import com.guildplus.guild.GuildManager;
import com.guildplus.utils.ColorUtils;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.Bukkit;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * プレイヤーログイン時にギルドMOTDを表示するリスナー。
 */
public class GuildLoginListener implements Listener {

    private final GuildManager guildManager;
    private final Plugin plugin;

    public GuildLoginListener(Plugin plugin, GuildManager guildManager) {
        this.plugin = plugin;
        this.guildManager = guildManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Guild guild = guildManager.getGuildByPlayer(event.getPlayer().getUniqueId());
        if (guild == null) return;
        if (guild.getMotd().isEmpty()) return;

        // 1秒後に送信（ログイン直後のメッセージに埋もれないよう）
        Bukkit.getScheduler().runTaskLater(plugin,
                () -> event.getPlayer().sendMessage(
                        ColorUtils.colorize("&6[" + guild.getName() + " MOTD] &f" + guild.getMotd())
                ),
                20L);
    }
}
