package com.guildplus.guild;

import org.bukkit.Location;

import java.util.*;

public class Guild {

    public enum Rank {
        LEADER, OFFICER, MEMBER;

        public String getDisplayName() {
            return switch (this) {
                case LEADER  -> "§6[Leader]";
                case OFFICER -> "§e[Officer]";
                case MEMBER  -> "§7[Member]";
            };
        }
    }

    public enum Relation {
        ALLY, ENEMY, NEUTRAL
    }

    private final String id;
    private String name;
    private String tag;
    private final UUID leaderUuid;
    private final Map<UUID, Rank> members = new LinkedHashMap<>();
    private Location home;
    private boolean pvpEnabled;

    // ally/enemy: guildId -> Relation
    private final Map<String, Relation> relations = new HashMap<>();

    // ── 新規フィールド ──────────────────────────────────────────────────────

    /** ギルドバンク残高 */
    private double bankBalance = 0.0;

    /** ギルド説明 */
    private String description = "";

    /** MOTD (Message of the Day) */
    private String motd = "";

    /** ギルド経験値 */
    private long exp = 0L;

    /** ギルドレベル */
    private int level = 1;

    // ── コンストラクタ ────────────────────────────────────────────────────

    public Guild(String id, String name, String tag, UUID leaderUuid, boolean pvpEnabled) {
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.leaderUuid = leaderUuid;
        this.pvpEnabled = pvpEnabled;
        this.members.put(leaderUuid, Rank.LEADER);
    }

    // ── Getters / Setters ──────────────────────────────────────────────────

    public String getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    public UUID getLeaderUuid() { return leaderUuid; }

    public Map<UUID, Rank> getMembers() { return members; }

    public boolean isMember(UUID uuid) { return members.containsKey(uuid); }

    public Rank getRank(UUID uuid) { return members.getOrDefault(uuid, null); }

    public void addMember(UUID uuid) { members.put(uuid, Rank.MEMBER); }

    public void removeMember(UUID uuid) { members.remove(uuid); }

    public void setRank(UUID uuid, Rank rank) { members.put(uuid, rank); }

    public Location getHome() { return home; }
    public void setHome(Location home) { this.home = home; }

    public boolean isPvpEnabled() { return pvpEnabled; }
    public void setPvpEnabled(boolean pvpEnabled) { this.pvpEnabled = pvpEnabled; }

    public Map<String, Relation> getRelations() { return relations; }

    public Relation getRelation(String guildId) {
        return relations.getOrDefault(guildId, Relation.NEUTRAL);
    }

    public void setRelation(String guildId, Relation relation) {
        if (relation == Relation.NEUTRAL) {
            relations.remove(guildId);
        } else {
            relations.put(guildId, relation);
        }
    }

    public boolean isAlly(String guildId) { return getRelation(guildId) == Relation.ALLY; }
    public boolean isEnemy(String guildId) { return getRelation(guildId) == Relation.ENEMY; }

    public int getMemberCount() { return members.size(); }

    public List<UUID> getMemberList() { return new ArrayList<>(members.keySet()); }

    // ── ギルドバンク ───────────────────────────────────────────────────────

    public double getBankBalance() { return bankBalance; }
    public void setBankBalance(double bankBalance) { this.bankBalance = Math.max(0, bankBalance); }

    public void depositBank(double amount) { this.bankBalance += amount; }

    public boolean withdrawBank(double amount) {
        if (this.bankBalance < amount) return false;
        this.bankBalance -= amount;
        return true;
    }

    // ── 説明 / MOTD ────────────────────────────────────────────────────────

    public String getDescription() { return description != null ? description : ""; }
    public void setDescription(String description) { this.description = description != null ? description : ""; }

    public String getMotd() { return motd != null ? motd : ""; }
    public void setMotd(String motd) { this.motd = motd != null ? motd : ""; }

    // ── EXP / レベル ───────────────────────────────────────────────────────

    public long getExp() { return exp; }
    public void setExp(long exp) { this.exp = Math.max(0, exp); }

    public void addExp(long amount) {
        this.exp += amount;
        recalculateLevel();
    }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = Math.max(1, level); }

    /**
     * レベル計算: level = floor(sqrt(exp / 100)) + 1 (上限50)
     */
    public void recalculateLevel() {
        int newLevel = (int) Math.floor(Math.sqrt(exp / 100.0)) + 1;
        this.level = Math.min(newLevel, 50);
    }

    /** 次のレベルに必要な合計EXP */
    public long getExpForNextLevel() {
        int nextLevel = level + 1;
        if (nextLevel > 50) return -1L; // max level
        return (long) Math.pow(nextLevel - 1, 2) * 100L;
    }
}
