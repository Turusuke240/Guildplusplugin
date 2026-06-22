package com.guildplus.placeholder;

import com.guildplus.GuildPlusPlugin;
import com.guildplus.chat.GuildChatManager;
import com.guildplus.guild.Guild;
import com.guildplus.guild.GuildManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI 拡張。
 *
 * 利用可能なプレースホルダー:
 *   %guildplus_name%       ギルド名
 *   %guildplus_tag%        ギルドタグ
 *   %guildplus_rank%       ランク名 (LEADER/OFFICER/MEMBER)
 *   %guildplus_rank_display% ランク表示名 (色付き)
 *   %guildplus_members%    メンバー数
 *   %guildplus_online%     オンライン人数
 *   %guildplus_level%      ギルドレベル
 *   %guildplus_exp%        ギルドEXP
 *   %guildplus_bank%       バンク残高
 *   %guildplus_desc%       ギルド説明
 *   %guildplus_motd%       MOTD
 *   %guildplus_chat_mode%  ギルドチャットモード (true/false)
 *   %guildplus_pvp%        PVP状態 (有効/無効)
 *   %guildplus_home%       ホーム座標文字列
 */
public class GuildPlaceholder extends PlaceholderExpansion {

    private final GuildPlusPlugin plugin;
    private final GuildManager guildManager;
    private final GuildChatManager chatManager;

    public GuildPlaceholder(GuildPlusPlugin plugin, GuildManager guildManager, GuildChatManager chatManager) {
        this.plugin = plugin;
        this.guildManager = guildManager;
        this.chatManager = chatManager;
    }

    @Override public @NotNull String getIdentifier() { return "guildplus"; }
    @Override public @NotNull String getAuthor()     { return "GuildPlus"; }
    @Override public @NotNull String getVersion()    { return plugin.getDescription().getVersion(); }
    @Override public boolean persist()               { return true; }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        Guild guild = guildManager.getGuildByPlayer(player.getUniqueId());

        return switch (params.toLowerCase()) {
            case "name"  -> guild != null ? guild.getName() : "";
            case "tag"   -> guild != null ? guild.getTag()  : "";
            case "rank"  -> {
                if (guild == null) yield "";
                Guild.Rank rank = guild.getRank(player.getUniqueId());
                yield rank != null ? rank.name() : "";
            }
            case "rank_display" -> {
                if (guild == null) yield "";
                Guild.Rank rank = guild.getRank(player.getUniqueId());
                yield rank != null ? rank.getDisplayName() : "";
            }
            case "members" -> guild != null ? String.valueOf(guild.getMemberCount()) : "0";
            case "online"  -> {
                if (guild == null) yield "0";
                long online = guild.getMemberList().stream()
                        .filter(uuid -> Bukkit.getPlayer(uuid) != null)
                        .count();
                yield String.valueOf(online);
            }
            case "level"  -> guild != null ? String.valueOf(guild.getLevel()) : "0";
            case "exp"    -> guild != null ? String.valueOf(guild.getExp())   : "0";
            case "bank"   -> guild != null ? String.format("%.2f", guild.getBankBalance()) : "0.00";
            case "desc"   -> guild != null ? guild.getDescription() : "";
            case "motd"   -> guild != null ? guild.getMotd() : "";
            case "pvp"    -> guild != null ? (guild.isPvpEnabled() ? "有効" : "無効") : "";
            case "home"   -> {
                if (guild == null || guild.getHome() == null || guild.getHome().getWorld() == null)
                    yield "未設定";
                var h = guild.getHome();
                yield h.getWorld().getName() + " ("
                        + (int) h.getX() + "," + (int) h.getY() + "," + (int) h.getZ() + ")";
            }
            case "chat_mode" -> {
                if (player.getPlayer() == null) yield "false";
                yield String.valueOf(chatManager.isGuildChatMode(player.getUniqueId()));
            }
            default -> null;
        };
    }
}
