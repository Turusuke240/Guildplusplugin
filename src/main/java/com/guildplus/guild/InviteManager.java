package com.guildplus.guild;

import com.guildplus.GuildPlusPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 招待の有効期限を管理するクラス。
 * inviterUuid -> (inviteeUuid, guildId)
 */
public class InviteManager {

    private final GuildPlusPlugin plugin;
    // inviteeUuid -> guildId
    private final Map<UUID, String> pendingInvites = new HashMap<>();

    public InviteManager(GuildPlusPlugin plugin) {
        this.plugin = plugin;
    }

    public void addInvite(UUID inviteeUuid, String guildId) {
        pendingInvites.put(inviteeUuid, guildId);
        int expireSeconds = plugin.getConfig().getInt("guild.invite-expire-seconds", 60);

        new BukkitRunnable() {
            @Override
            public void run() {
                // まだ同じ招待が存在すれば削除
                pendingInvites.remove(inviteeUuid, guildId);
            }
        }.runTaskLater(plugin, expireSeconds * 20L);
    }

    public boolean hasInvite(UUID inviteeUuid) {
        return pendingInvites.containsKey(inviteeUuid);
    }

    public String getInviteGuildId(UUID inviteeUuid) {
        return pendingInvites.get(inviteeUuid);
    }

    public void removeInvite(UUID inviteeUuid) {
        pendingInvites.remove(inviteeUuid);
    }
}
