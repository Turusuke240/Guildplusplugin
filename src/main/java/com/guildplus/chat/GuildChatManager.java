package com.guildplus.chat;

import com.guildplus.GuildPlusPlugin;
import com.guildplus.guild.Guild;
import com.guildplus.guild.GuildManager;
import com.guildplus.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GuildChatManager {

    private final GuildPlusPlugin plugin;
    private final GuildManager guildManager;
    // ギルドチャットモードがONのプレイヤー
    private final Set<UUID> guildChatMode = new HashSet<>();

    public GuildChatManager(GuildPlusPlugin plugin, GuildManager guildManager) {
        this.plugin = plugin;
        this.guildManager = guildManager;
    }

    public boolean isGuildChatMode(UUID uuid) {
        return guildChatMode.contains(uuid);
    }

    public void setGuildChatMode(UUID uuid, boolean enabled) {
        if (enabled) guildChatMode.add(uuid);
        else guildChatMode.remove(uuid);
    }

    public void toggleGuildChatMode(UUID uuid) {
        if (guildChatMode.contains(uuid)) guildChatMode.remove(uuid);
        else guildChatMode.add(uuid);
    }

    /**
     * ギルドチャットメッセージを送信する。
     */
    public void sendGuildChat(Player sender, String message) {
        Guild guild = guildManager.getGuildByPlayer(sender.getUniqueId());
        if (guild == null) {
            sender.sendMessage(ColorUtils.colorize("&cギルドに所属していません。"));
            return;
        }

        String prefix = ColorUtils.colorize(plugin.getConfig().getString("chat.prefix", "&7[&bGuild&7]&r"));
        boolean allowColor = plugin.getConfig().getBoolean("chat.allow-color-codes", true);
        String formattedMessage = allowColor ? ColorUtils.colorize(message) : message;

        String rankDisplay = getRankDisplay(guild.getRank(sender.getUniqueId()));
        String guildNameColor = ColorUtils.colorize(plugin.getConfig().getString("chat.guild-name-color", "&6"));
        String fullMessage = prefix + " " + guildNameColor + guild.getName() + " &r" + rankDisplay + sender.getName() + "&f: " + formattedMessage;
        String colored = ColorUtils.colorize(fullMessage);

        for (UUID memberUuid : guild.getMemberList()) {
            Player member = Bukkit.getPlayer(memberUuid);
            if (member != null && member.isOnline()) {
                member.sendMessage(colored);
            }
        }
    }

    private String getRankDisplay(Guild.Rank rank) {
        if (rank == null) return "";
        return switch (rank) {
            case LEADER -> "&c[Leader] ";
            case OFFICER -> "&6[Officer] ";
            case MEMBER -> "&a[Member] ";
        };
    }
}
