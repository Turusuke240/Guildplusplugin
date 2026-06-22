package com.guildplus.guild;

import com.guildplus.GuildPlusPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

/**
 * 同盟申請を管理するクラス。
 * requesterGuildId -> targetGuildId
 */
public class AllyRequestManager {

    private final GuildPlusPlugin plugin;
    private final Map<String, String> pendingRequests = new HashMap<>();

    public AllyRequestManager(GuildPlusPlugin plugin) {
        this.plugin = plugin;
    }

    public void addRequest(String fromGuildId, String toGuildId) {
        pendingRequests.put(fromGuildId, toGuildId);
        new BukkitRunnable() {
            @Override
            public void run() {
                pendingRequests.remove(fromGuildId, toGuildId);
            }
        }.runTaskLater(plugin, 60 * 20L);
    }

    /** toGuildId に fromGuildId からのリクエストがあるか */
    public boolean hasRequest(String toGuildId, String fromGuildId) {
        String target = pendingRequests.get(fromGuildId);
        return toGuildId.equals(target);
    }

    public void removeRequest(String fromGuildId) {
        pendingRequests.remove(fromGuildId);
    }
}
