package com.guildplus.ally;

import com.guildplus.guild.AllyRequestManager;
import com.guildplus.guild.Guild;
import com.guildplus.guild.GuildManager;
import com.guildplus.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class AllyManager {

    private final GuildManager guildManager;
    private final AllyRequestManager requestManager;

    public AllyManager(GuildManager guildManager, AllyRequestManager requestManager) {
        this.guildManager = guildManager;
        this.requestManager = requestManager;
    }

    public void handleAlly(Player player, Guild guild, Guild target, String action) {
        switch (action) {
            case "request" -> {
                if (guild.isAlly(target.getId())) {
                    player.sendMessage(ColorUtils.colorize("&cすでに同盟しています。"));
                    return;
                }
                // 相手からの申請があれば自動承認
                if (requestManager.hasRequest(guild.getId(), target.getId())) {
                    guild.setRelation(target.getId(), Guild.Relation.ALLY);
                    target.setRelation(guild.getId(), Guild.Relation.ALLY);
                    guildManager.save();
                    broadcastToGuild(guild, ColorUtils.colorize("&a" + target.getName() + " と同盟を結びました！"));
                    broadcastToGuild(target, ColorUtils.colorize("&a" + guild.getName() + " と同盟を結びました！"));
                    requestManager.removeRequest(target.getId());
                    return;
                }
                requestManager.addRequest(guild.getId(), target.getId());
                player.sendMessage(ColorUtils.colorize("&a" + target.getName() + " に同盟申請を送りました。"));
                // 相手Leaderに通知
                notifyGuildLeader(target, ColorUtils.colorize("&6" + guild.getName() + " から同盟申請が届きました。/guild ally request " + guild.getName() + " で承認できます。"));
            }
            case "accept" -> {
                if (!requestManager.hasRequest(target.getId(), guild.getId())) {
                    player.sendMessage(ColorUtils.colorize("&c" + target.getName() + " からの申請がありません。"));
                    return;
                }
                guild.setRelation(target.getId(), Guild.Relation.ALLY);
                target.setRelation(guild.getId(), Guild.Relation.ALLY);
                guildManager.save();
                requestManager.removeRequest(target.getId());
                broadcastToGuild(guild, ColorUtils.colorize("&a" + target.getName() + " と同盟を結びました！"));
                broadcastToGuild(target, ColorUtils.colorize("&a" + guild.getName() + " と同盟を結びました！"));
            }
            case "remove" -> {
                if (!guild.isAlly(target.getId())) {
                    player.sendMessage(ColorUtils.colorize("&cそのギルドと同盟していません。"));
                    return;
                }
                guild.setRelation(target.getId(), Guild.Relation.NEUTRAL);
                target.setRelation(guild.getId(), Guild.Relation.NEUTRAL);
                guildManager.save();
                player.sendMessage(ColorUtils.colorize("&7" + target.getName() + " との同盟を解除しました。"));
                broadcastToGuild(target, ColorUtils.colorize("&7" + guild.getName() + " との同盟が解除されました。"));
            }
            default -> player.sendMessage(ColorUtils.colorize("&c使用法: /guild ally <request|accept|remove> <ギルド名>"));
        }
    }

    public void handleEnemy(Player player, Guild guild, Guild target, String action) {
        switch (action) {
            case "set" -> {
                if (guild.isEnemy(target.getId())) {
                    player.sendMessage(ColorUtils.colorize("&cすでに敵対設定済みです。"));
                    return;
                }
                // 同盟解除
                if (guild.isAlly(target.getId())) {
                    target.setRelation(guild.getId(), Guild.Relation.NEUTRAL);
                }
                guild.setRelation(target.getId(), Guild.Relation.ENEMY);
                guildManager.save();
                player.sendMessage(ColorUtils.colorize("&c" + target.getName() + " を敵対に設定しました。"));
                broadcastToGuild(target, ColorUtils.colorize("&c" + guild.getName() + " があなたのギルドを敵対に設定しました。"));
            }
            case "remove" -> {
                if (!guild.isEnemy(target.getId())) {
                    player.sendMessage(ColorUtils.colorize("&cそのギルドと敵対していません。"));
                    return;
                }
                guild.setRelation(target.getId(), Guild.Relation.NEUTRAL);
                guildManager.save();
                player.sendMessage(ColorUtils.colorize("&7" + target.getName() + " との敵対を解除しました。"));
            }
            default -> player.sendMessage(ColorUtils.colorize("&c使用法: /guild enemy <set|remove> <ギルド名>"));
        }
    }

    private void broadcastToGuild(Guild guild, String message) {
        for (UUID uuid : guild.getMemberList()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(message);
        }
    }

    private void notifyGuildLeader(Guild guild, String message) {
        Player leader = Bukkit.getPlayer(guild.getLeaderUuid());
        if (leader != null) leader.sendMessage(message);
    }
}
