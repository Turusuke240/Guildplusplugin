package com.guildplus.guild;

import com.guildplus.GuildPlusPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GuildManager {

    private final GuildPlusPlugin plugin;
    private final Map<String, Guild> guilds = new HashMap<>();        // id -> Guild
    private final Map<UUID, String> playerGuildMap = new HashMap<>(); // player -> guild id
    private File dataFile;
    private YamlConfiguration dataConfig;

    public GuildManager(GuildPlusPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    // ── Public API ─────────────────────────────────────────────────────────

    public Guild createGuild(String name, String tag, UUID leaderUuid) {
        String id = UUID.randomUUID().toString();
        boolean defaultPvp = plugin.getConfig().getBoolean("guild.default-pvp", false);
        Guild guild = new Guild(id, name, tag, leaderUuid, defaultPvp);
        guilds.put(id, guild);
        playerGuildMap.put(leaderUuid, id);
        save();
        return guild;
    }

    public void deleteGuild(Guild guild) {
        for (UUID uuid : guild.getMemberList()) {
            playerGuildMap.remove(uuid);
        }
        guilds.remove(guild.getId());
        // 他ギルドの関係から削除
        for (Guild g : guilds.values()) {
            g.getRelations().remove(guild.getId());
        }
        save();
    }

    public boolean addMember(Guild guild, UUID uuid) {
        if (playerGuildMap.containsKey(uuid)) return false;
        guild.addMember(uuid);
        playerGuildMap.put(uuid, guild.getId());
        save();
        return true;
    }

    public void removeMember(Guild guild, UUID uuid) {
        guild.removeMember(uuid);
        playerGuildMap.remove(uuid);
        save();
    }

    public Guild getGuildByPlayer(UUID uuid) {
        String id = playerGuildMap.get(uuid);
        if (id == null) return null;
        return guilds.get(id);
    }

    public Guild getGuildById(String id) {
        return guilds.get(id);
    }

    public Guild getGuildByName(String name) {
        for (Guild g : guilds.values()) {
            if (g.getName().equalsIgnoreCase(name)) return g;
        }
        return null;
    }

    public Guild getGuildByTag(String tag) {
        for (Guild g : guilds.values()) {
            if (g.getTag().equalsIgnoreCase(tag)) return g;
        }
        return null;
    }

    public Collection<Guild> getAllGuilds() {
        return guilds.values();
    }

    public boolean isInGuild(UUID uuid) {
        return playerGuildMap.containsKey(uuid);
    }

    /**
     * メンバー数順でソートされたギルドリストを返す（降順）
     */
    public List<Guild> getGuildsSortedByMembers() {
        return guilds.values().stream()
                .sorted(Comparator.comparingInt(Guild::getMemberCount).reversed())
                .toList();
    }

    /**
     * ギルドバンクへの入金（Vault から引き落として bankBalance に加算）
     * @return true = 成功
     */
    public boolean depositToBank(Guild guild, double amount) {
        guild.depositBank(amount);
        save();
        return true;
    }

    /**
     * ギルドバンクからの出金（bankBalance から引いて Vault に加算）
     * @return true = 成功, false = 残高不足
     */
    public boolean withdrawFromBank(Guild guild, double amount) {
        if (!guild.withdrawBank(amount)) return false;
        save();
        return true;
    }

    // ── Save ───────────────────────────────────────────────────────────────

    public void save() {
        dataConfig.set("guilds", null);
        for (Guild g : guilds.values()) {
            String base = "guilds." + g.getId();
            dataConfig.set(base + ".name", g.getName());
            dataConfig.set(base + ".tag", g.getTag());
            dataConfig.set(base + ".leader", g.getLeaderUuid().toString());
            dataConfig.set(base + ".pvp", g.isPvpEnabled());
            dataConfig.set(base + ".bank", g.getBankBalance());
            dataConfig.set(base + ".description", g.getDescription());
            dataConfig.set(base + ".motd", g.getMotd());
            dataConfig.set(base + ".exp", g.getExp());
            dataConfig.set(base + ".level", g.getLevel());

            // members
            for (Map.Entry<UUID, Guild.Rank> entry : g.getMembers().entrySet()) {
                dataConfig.set(base + ".members." + entry.getKey().toString(), entry.getValue().name());
            }

            // home
            Location home = g.getHome();
            if (home != null && home.getWorld() != null) {
                dataConfig.set(base + ".home.world", home.getWorld().getName());
                dataConfig.set(base + ".home.x", home.getX());
                dataConfig.set(base + ".home.y", home.getY());
                dataConfig.set(base + ".home.z", home.getZ());
                dataConfig.set(base + ".home.yaw", (double) home.getYaw());
                dataConfig.set(base + ".home.pitch", (double) home.getPitch());
            }

            // relations
            for (Map.Entry<String, Guild.Relation> rel : g.getRelations().entrySet()) {
                dataConfig.set(base + ".relations." + rel.getKey(), rel.getValue().name());
            }
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("ギルドデータの保存に失敗しました: " + e.getMessage());
        }
    }

    // ── Load ───────────────────────────────────────────────────────────────

    private void load() {
        dataFile = new File(plugin.getDataFolder(), "guilds.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException ignored) {}
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        ConfigurationSection section = dataConfig.getConfigurationSection("guilds");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            String base = "guilds." + id;
            String name    = dataConfig.getString(base + ".name", "");
            String tag     = dataConfig.getString(base + ".tag", "");
            UUID leader    = UUID.fromString(dataConfig.getString(base + ".leader",
                    UUID.randomUUID().toString()));
            boolean pvp    = dataConfig.getBoolean(base + ".pvp", false);
            double bank    = dataConfig.getDouble(base + ".bank", 0.0);
            String desc    = dataConfig.getString(base + ".description", "");
            String motd    = dataConfig.getString(base + ".motd", "");
            long exp       = dataConfig.getLong(base + ".exp", 0L);
            int level      = dataConfig.getInt(base + ".level", 1);

            Guild guild = new Guild(id, name, tag, leader, pvp);
            guild.setBankBalance(bank);
            guild.setDescription(desc);
            guild.setMotd(motd);
            guild.setExp(exp);
            guild.setLevel(level);
            guild.getMembers().clear();

            // members
            ConfigurationSection membersSec = dataConfig.getConfigurationSection(base + ".members");
            if (membersSec != null) {
                for (String uuidStr : membersSec.getKeys(false)) {
                    UUID uuid = UUID.fromString(uuidStr);
                    String rankStr = dataConfig.getString(base + ".members." + uuidStr, "MEMBER");
                    Guild.Rank rank;
                    try { rank = Guild.Rank.valueOf(rankStr); } catch (Exception e) { rank = Guild.Rank.MEMBER; }
                    guild.getMembers().put(uuid, rank);
                    playerGuildMap.put(uuid, id);
                }
            }

            // home
            String world = dataConfig.getString(base + ".home.world");
            if (world != null && Bukkit.getWorld(world) != null) {
                double x  = dataConfig.getDouble(base + ".home.x");
                double y  = dataConfig.getDouble(base + ".home.y");
                double z  = dataConfig.getDouble(base + ".home.z");
                float yaw   = (float) dataConfig.getDouble(base + ".home.yaw");
                float pitch = (float) dataConfig.getDouble(base + ".home.pitch");
                guild.setHome(new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch));
            }

            // relations
            ConfigurationSection relSec = dataConfig.getConfigurationSection(base + ".relations");
            if (relSec != null) {
                for (String relId : relSec.getKeys(false)) {
                    String relStr = dataConfig.getString(base + ".relations." + relId, "NEUTRAL");
                    Guild.Relation rel;
                    try { rel = Guild.Relation.valueOf(relStr); } catch (Exception e) { rel = Guild.Relation.NEUTRAL; }
                    guild.setRelation(relId, rel);
                }
            }

            guilds.put(id, guild);
        }
    }
}
