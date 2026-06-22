package com.guildplus.commands;

import com.guildplus.GuildPlusPlugin;
import com.guildplus.ally.AllyManager;
import com.guildplus.chat.GuildChatManager;
import com.guildplus.chest.GuildChestManager;
import com.guildplus.guild.Guild;
import com.guildplus.guild.GuildManager;
import com.guildplus.guild.InviteManager;
import com.guildplus.listeners.ChestListener;
import com.guildplus.utils.ColorUtils;
import com.guildplus.utils.VaultHook;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class GuildCommand implements CommandExecutor, TabCompleter {

    private final GuildPlusPlugin plugin;
    private final GuildManager guildManager;
    private final InviteManager inviteManager;
    private final GuildChatManager chatManager;
    private final GuildChestManager chestManager;
    private final AllyManager allyManager;
    private final VaultHook vaultHook;
    private final ChestListener chestListener;

    public GuildCommand(GuildPlusPlugin plugin,
                        GuildManager guildManager,
                        InviteManager inviteManager,
                        GuildChatManager chatManager,
                        GuildChestManager chestManager,
                        AllyManager allyManager,
                        VaultHook vaultHook,
                        ChestListener chestListener) {
        this.plugin = plugin;
        this.guildManager = guildManager;
        this.inviteManager = inviteManager;
        this.chatManager = chatManager;
        this.chestManager = chestManager;
        this.allyManager = allyManager;
        this.vaultHook = vaultHook;
        this.chestListener = chestListener;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("プレイヤーのみ使用可能です。");
            return true;
        }

        // 引数なし → GUIメニュー
        if (args.length == 0) {
            openGuildMenu(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "help"     -> sendHelp(player);
            case "create"   -> handleCreate(player, args);
            case "delete"   -> handleDelete(player);
            case "invite"   -> handleInvite(player, args);
            case "join"     -> handleJoin(player);
            case "leave"    -> handleLeave(player);
            case "kick"     -> handleKick(player, args);
            case "promote"  -> handlePromote(player, args);
            case "demote"   -> handleDemote(player, args);
            case "home"     -> handleHome(player);
            case "sethome"  -> handleSetHome(player);
            case "chat"     -> handleChat(player, args);
            case "chest"    -> handleChest(player);
            case "pvp"      -> handlePvp(player, args);
            case "ally"     -> handleAlly(player, args);
            case "enemy"    -> handleEnemy(player, args);
            case "info"     -> handleInfo(player, args);
            case "list"     -> handleList(player);
            // ── 新規コマンド ──
            case "rename"   -> handleRename(player, args);
            case "tag"      -> handleTag(player, args);
            case "members"  -> handleMembers(player, args);
            case "top"      -> handleTop(player);
            case "bank"     -> handleBank(player);
            case "deposit"  -> handleDeposit(player, args);
            case "withdraw" -> handleWithdraw(player, args);
            case "desc"     -> handleDesc(player, args);
            case "motd"     -> handleMotd(player, args);
            case "level"    -> handleLevel(player);
            case "menu"     -> openGuildMenu(player);
            default         -> player.sendMessage(ColorUtils.colorize("&c不明なコマンドです。/guild help を確認してください。"));
        }
        return true;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 既存コマンド
    // ═══════════════════════════════════════════════════════════════════════

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ColorUtils.colorize("&c使用法: /guild create <ギルド名> [タグ]"));
            return;
        }
        if (guildManager.isInGuild(player.getUniqueId())) {
            player.sendMessage(ColorUtils.colorize("&cすでにギルドに所属しています。"));
            return;
        }

        String name = args[1];
        String tag  = args.length >= 3 ? args[2] : name.substring(0, Math.min(name.length(), 4));

        if (name.length() > 20) {
            player.sendMessage(ColorUtils.colorize("&cギルド名は20文字以内にしてください。"));
            return;
        }
        if (tag.length() > 6) {
            player.sendMessage(ColorUtils.colorize("&cタグは6文字以内にしてください。"));
            return;
        }

        if (guildManager.getGuildByName(name) != null) {
            player.sendMessage(ColorUtils.colorize("&cそのギルド名はすでに使用されています。"));
            return;
        }
        if (guildManager.getGuildByTag(tag) != null) {
            player.sendMessage(ColorUtils.colorize("&cそのタグはすでに使用されています。"));
            return;
        }

        double cost = plugin.getConfig().getDouble("guild.creation-cost", 1000.0);
        if (plugin.getConfig().getBoolean("economy.enabled", true) && vaultHook.isEnabled()) {
            if (!vaultHook.has(player, cost)) {
                player.sendMessage(ColorUtils.colorize("&cギルド作成には &e" + cost + " &c必要です。"));
                return;
            }
            vaultHook.withdraw(player, cost);
        }

        Guild guild = guildManager.createGuild(name, tag, player.getUniqueId());
        player.sendMessage(ColorUtils.colorize("&aギルド &6" + name + " &a[&6" + tag + "&a] を作成しました！"));
    }

    private void handleDelete(Player player) {
        Guild guild = guildManager.getGuildByPlayer(player.getUniqueId());
        if (guild == null) { notInGuild(player); return; }
        if (guild.getRank(player.getUniqueId()) != Guild.Rank.LEADER) {
            player.sendMessage(ColorUtils.colorize("&cリーダーのみギルドを削除できます。"));
            return;
        }
        broadcastToGuild(guild, ColorUtils.colorize("&cギルド &6" + guild.getName() + " &cが解散されました。"));
        chestManager.deleteChest(guild.getId());
        guildManager.deleteGuild(guild);
    }

    private void handleInvite(Player player, String[] args) {
        Guild guild = guildManager.getGuildByPlayer(player.getUniqueId());
        if (guild == null) { notInGuild(player); return; }
        if (guild.getRank(player.getUniqueId()) == Guild.Rank.MEMBER) {
            player.sendMessage(ColorUtils.colorize("&cOfficer以上のみ招待できます。"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ColorUtils.colorize("&c使用法: /guild invite <プレイヤー名>"));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(ColorUtils.colorize("&cプレイヤーが見つかりません。"));
            return;
        }
        if (guildManager.isInGuild(target.getUniqueId())) {
            player.sendMessage(ColorUtils.colorize("&cそのプレイヤーはすでにギルドに所属しています。"));
            return;
        }
        if (inviteManager.hasInvite(target.getUniqueId())) {
            player.sendMessage(ColorUtils.colorize("&cそのプレイヤーにはすでに招待を送っています。"));
            return;
        }
        inviteManager.addInvite(target.getUniqueId(), guild.getId());
        player.sendMessage(ColorUtils.colorize("&a" + target.getName() + " を招待しました。"));
        target.sendMessage(ColorUtils.colorize("&6" + player.getName() + " &aからギルド &6" + guild.getName()
                + " &aへの招待が届きました。/guild join で参加できます。(60秒間有効)"));
    }

    private void handleJoin(Player player) {
        if (guildManager.isInGuild(player.getUniqueId())) {
            player.sendMessage(ColorUtils.colorize("&cすでにギルドに所属しています。"));
            return;
        }
        if (!inviteManager.hasInvite(player.getUniqueId())) {
            player.sendMessage(ColorUtils.colorize("&c有効な招待がありません。"));
            return;
        }
        String guildId = inviteManager.getInviteGuildId(player.getUniqueId());
        Guild guild = guildManager.getGuildById(guildId);
        if (guild == null) {
            player.sendMessage(ColorUtils.colorize("&c招待されたギルドが見つかりません。"));
            inviteManager.removeInvite(player.getUniqueId());
            return;
        }
        inviteManager.removeInvite(player.getUniqueId());
        guildManager.addMember(guild, player.getUniqueId());
        player.sendMessage(ColorUtils.colorize("&aギルド &6" + guild.getName() + " &aに参加しました！"));
        // MOTDを表示
        if (!guild.getMotd().isEmpty()) {
            player.sendMessage(ColorUtils.colorize("&6[MOTD] &f" + guild.getMotd()));
        }
        broadcastToGuild(guild, ColorUtils.colorize("&a" + player.getName() + " &aがギルドに参加しました！"));
    }

    private void handleLeave(Player player) {
        Guild guild = guildManager.getGuildByPlayer(player.getUniqueId());
        if (guild == null) { notInGuild(player); return; }
        if (guild.getRank(player.getUniqueId()) == Guild.Rank.LEADER) {
            player.sendMessage(ColorUtils.colorize("&cリーダーはギルドを脱退できません。/guild delete でギルドを解散してください。"));
            return;
        }
        guildManager.removeMember(guild, player.getUniqueId());
        player.sendMessage(ColorUtils.colorize("&7ギルド &6" + guild.getName() + " &7を脱退しました。"));
        broadcastToGuild(guild, ColorUtils.colorize("&7" + player.getName() + " がギルドを脱退しました。"));
    }

    private void handleKick(Player player, String[] args) {
        Guild guild = guildManager.getGuildByPlayer(player.getUniqueId());
        if (guild == null) { notInGuild(player); return; }
        Guild.Rank myRank = guild.getRank(player.getUniqueId());
        if (myRank == Guild.Rank.MEMBER) {
            player.sendMessage(ColorUtils.colorize("&c権限がありません。"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ColorUtils.colorize("&c使用法: /guild kick <プレイヤー名>"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        UUID targetUuid = target != null ? target.getUniqueId() : resolveOfflineUuid(args[1]);
        if (targetUuid == null || !guild.isMember(targetUuid)) {
            player.sendMessage(ColorUtils.colorize("&cそのプレイヤーはギルドメンバーではありません。"));
            return;
        }
        if (guild.getRank(targetUuid) == Guild.Rank.LEADER) {
            player.sendMessage(ColorUtils.colorize("&cリーダーをキックすることはできません。"));
            return;
        }
        // Officer は Officer をキックできない
        if (myRank == Guild.Rank.OFFICER && guild.getRank(targetUuid) == Guild.Rank.OFFICER) {
            player.sendMessage(ColorUtils.colorize("&cOfficer 同士はキックできません。"));
            return;
        }
        guildManager.removeMember(guild, targetUuid);
        player.sendMessage(ColorUtils.colorize("&a" + args[1] + " をキックしました。"));
        if (target != null) target.sendMessage(ColorUtils.colorize("&cギルドからキックされました。"));
        broadcastToGuild(guild, ColorUtils.colorize("&c" + args[1] + " がギルドからキックされました。"));
    }

    private void handlePromote(Player player, String[] args) {
        Guild guild = guildManager.getGuildByPlayer(player.getUniqueId());
        if (guild == null) { notInGuild(player); return; }
        if (guild.getRank(player.getUniqueId()) != Guild.Rank.LEADER) {
            player.sendMessage(ColorUtils.colorize("&cリーダーのみ昇格できます。"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ColorUtils.colorize("&c使用法: /guild promote <プレイヤー名>"));
            return;
        }
        UUID targetUuid = resolveGuildMemberUuid(guild, args[1]);
        if (targetUuid == null) {
            player.sendMessage(ColorUtils.colorize("&cそのプレイヤーはギルドメンバーではありません。"));
            return;
        }
        Guild.Rank current = guild.getRank(targetUuid);
        if (current == Guild.Rank.LEADER || current == Guild.Rank.OFFICER) {
            player.sendMessage(ColorUtils.colorize("&cこれ以上昇格できません。"));
            return;
        }
        guild.setRank(targetUuid, Guild.Rank.OFFICER);
        guildManager.save();
        player.sendMessage(ColorUtils.colorize("&a" + args[1] + " をOfficerに昇格しました。"));
        Player target = Bukkit.getPlayer(targetUuid);
        if (target != null) target.sendMessage(ColorUtils.colorize("&aOfficerに昇格しました！"));
    }

    private void handleDemote(Player player, String[] args) {
        Guild guild = guildManager.getGuildByPlayer(player.getUniqueId());
        if (guild == null) { notInGuild(player); return; }
        if (guild.getRank(player.getUniqueId()) != Guild.Rank.LEADER) {
            player.sendMessage(ColorUtils.colorize("&cリーダーのみ降格できます。"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ColorUtils.colorize("&c使用法: /guild demote <プレイヤー名>"));
            return;
        }
        UUID targetUuid = resolveGuildMemberUuid(guild, args[1]);
        if (targetUuid == null) {
            player.sendMessage(ColorUtils.colorize("&cそのプレイヤーはギルドメンバーではありません。"));
            return;
        }
        Guild.Rank current = guild.getRank(targetUuid);
        if (current == Guild.Rank.LEADER || current == Guild.Rank.MEMBER) {
            player.sendMessage(ColorUtils.colorize("&cこれ以上降格できません。"));
            return;
        }
        guild.setRank(targetUuid, Guild.Rank.MEMBER);
        guildManager.save();
        player.sendMessage(ColorUtils.colorize("&a" + args[1] + " をMemberに降格しました。"));
        Player target = Bukkit.getPlayer(targetUuid);
        if (target != null) target.sendMessage(ColorUtils.colorize("&7Memberに降格されました。"));
    }

    private void handleHome(Player player) {
        Guild guild = guildManager.getGuildByPlayer(player.getUniqueId());
        if (guild == null) { notInGuild(player); return; }
        if (guild.getHome() == null) {
            player.sendMessage(ColorUtils.colorize("&cギルドホームが設定されていません。"));
            return;
        }
        player.teleport(guild.getHome());
        player.sendMessage(ColorUtils.colorize("&aギルドホームにテレポートしました。"));
    }

    private void handleSetHome(Player player) {
        Guild guild = guildManager.getGuildByPlayer(player.getUniqueId());
        if (guild == null) { notInGuild(player); return; }
        if (guild.getRank(player.getUniqueId()) != Guild.Rank.LEADER) {
            player.sendMessage(ColorUtils.colorize("&cリーダーのみホームを設定できます。"));
            return;
        }
        guild.setHome(player.getLocation());
        guildManager.save();
        player.sendMessage(ColorUtils.colorize("&aギルドホームを設定しました。"));
    }

    private void handleChat(Player player, String[] args) {
        Guild guild = guildManager.getGuildByPlayer(player.getUniqueId());
        if (guild == null) { notInGuild(player); return; }
        if (args.length == 1) {
            chatManager.toggleGuildChatMode(player.getUniqueId());
            boolean mode = chatManager.isGuildChatMode(player.getUniqueId());
            player.sendMessage(ColorUtils.colorize(mode
                    ? "&aギルドチャットモードをONにしました。"
                    : "&7ギルドチャットモードをOFFにしました。"));
        } else {
            String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            chatManager.sendGuildChat(player, message);
        }
    }

    private void handleChest(Player player) {
        Guild guild = guildManager.getGuildByPlayer(player.getUniqueId());
        if (guild == null) { notInGuild(player); return; }
        if (!plugin.getConfig().getBoolean("chest.enabled", true)) {
            player.sendMessage(ColorUtils.colorize("&cギルドチェストは無効です。"));
            return;
        }
        var inv = chestManager.getChest(guild.getId(), guild.getName());
        chestListener.registerOpenChest(player.getUniqueId(), guild.getId());
        player.openInventory(inv);
    }

    private void handlePvp(Player player, String[] args) {
        Guild guild = guildManager.getGuildByPlayer(player.getUniqueId());
        if (guild == null) { notInGuild(player); return; }
        if (guild.getRank(player.getUniqueId()) != Guild.Rank.LEADER) {
            player.sendMessage(ColorUtils.colorize("&cリーダーのみPVP設定を変更できます。"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ColorUtils.colorize("&c使用法: /guild pvp <true|false>"));
            return;
        }
        boolean enabled = Boolean.parseBoolean(args[1]);
        guild.setPvpEnabled(enabled);
        guildManager.save();
        player.sendMessage(ColorUtils.colorize(enabled
                ? "&aギルド内PVPを有効にしました。"
                : "&7ギルド内PVPを無効にしました。"));
    }

    private void handleAlly(Player player, String[] args) {
        Guild guild = guildManager.getGuildByPlayer(player.getUniqueId());
        if (guild == null) { notInGuild(player); return; }
        if (guild.getRank(player.getUniqueId()) == Guild.Rank.MEMBER) {
            player.sendMessage(ColorUtils.colorize("&c権限がありません。"));
            return;
        }
        if (args.length < 3) {
            player.sendMessage(ColorUtils.colorize("&c使用法: /guild ally <request|accept|remove> <ギルド名>"));
            return;
        }
        String action = args[1].toLowerCase();
        Guild target = guildManager.getGuildByName(args[2]);
        if (target == null) {
            player.sendMessage(ColorUtils.colorize("&cギルドが見つかりません: " + args[2]));
            return;
        }
        if (target.getId().equals(guild.getId())) {
            player.sendMessage(ColorUtils.colorize("&c自分のギルドに申請できません。"));
            return;
        }
        allyManager.handleAlly(player, guild, target, action);
    }

    private void handleEnemy(Player player, String[] args) {
        Guild guild = guildManager.getGuildByPlayer(player.getUniqueId());
        if (guild == null) { notInGuild(player); return; }
        if (guild.getRank(player.getUniqueId()) == Guild.Rank.MEMBER) {
            player.sendMessage(ColorUtils.colorize("&c権限がありません。"));
            return;
        }
        if (args.length < 3) {
            player.sendMessage(ColorUtils.colorize("&c使用法: /guild enemy <set|remove> <ギルド名>"));
            return;
        }
        String action = args[1].toLowerCase();
        Guild target = guildManager.getGuildByName(args[2]);
        if (target == null) {
            player.sendMessage(ColorUtils.colorize("&cギルドが見つかりません: " + args[2]));
            return;
        }
        if (target.getId().equals(guild.getId())) {
            player.sendMessage(ColorUtils.colorize("&c自分のギルドに設定できません。"));
            return;
        }
        allyManager.handleEnemy(player, guild, target, action);
    }

    private void handleInfo(Player player, String[] args) {
        Guild guild;
        if (args.length >= 2) {
            guild = guildManager.getGuildByName(args[1]);
            if (guild == null) {
                player.sendMessage(ColorUtils.colorize("&cギルドが見つかりません: " + args[1]));
                return;
            }
        } else {
            guild = guildManager.getGuildByPlayer(player.getUniqueId());
            if (guild == null) { notInGuild(player); return; }
        }
        long online = guild.getMemberList().stream()
                .filter(uuid -> Bukkit.getPlayer(uuid) != null)
                .count();

        player.sendMessage(ColorUtils.colorize("&6&l━━━━━ ギルド情報 ━━━━━"));
        player.sendMessage(ColorUtils.colorize("&e名前: &f" + guild.getName() + " &7[" + guild.getTag() + "]"));
        player.sendMessage(ColorUtils.colorize("&eレベル: &f" + guild.getLevel() + " &7(EXP: " + guild.getExp() + ")"));
        if (!guild.getDescription().isEmpty()) {
            player.sendMessage(ColorUtils.colorize("&e説明: &f" + guild.getDescription()));
        }
        player.sendMessage(ColorUtils.colorize("&eメンバー: &f" + guild.getMemberCount()
                + " &7(" + online + " オンライン)"));
        player.sendMessage(ColorUtils.colorize("&ePVP: &f" + (guild.isPvpEnabled() ? "&a有効" : "&c無効")));
        String home = guild.getHome() != null && guild.getHome().getWorld() != null
                ? guild.getHome().getWorld().getName() + " ("
                    + (int) guild.getHome().getX() + ", "
                    + (int) guild.getHome().getY() + ", "
                    + (int) guild.getHome().getZ() + ")"
                : "未設定";
        player.sendMessage(ColorUtils.colorize("&eホーム: &f" + home));

        // バンク残高（メンバーのみに表示）
        if (guild.isMember(player.getUniqueId())) {
            player.sendMessage(ColorUtils.colorize("&eバンク: &a" + String.format("%.2f", guild.getBankBalance())));
        }

        // 同盟
        List<String> allies = guild.getRelations().entrySet().stream()
                .filter(e -> e.getValue() == Guild.Relation.ALLY)
                .map(e -> {
                    Guild g = guildManager.getGuildById(e.getKey());
                    return g != null ? g.getName() : e.getKey();
                }).toList();
        if (!allies.isEmpty()) player.sendMessage(ColorUtils.colorize("&e同盟: &a" + String.join(", ", allies)));

        // 敵対
        List<String> enemies = guild.getRelations().entrySet().stream()
                .filter(e -> e.getValue() == Guild.Relation.ENEMY)
                .map(e -> {
                    Guild g = guildManager.getGuildById(e.getKey());
                    return g != null ? g.getName() : e.getKey();
                }).toList();
        if (!enemies.isEmpty()) player.sendMessage(ColorUtils.colorize("&e敵対: &c" + String.join(", ", enemies)));
        player.sendMessage(ColorUtils.colorize("&6&l━━━━━━━━━━━━━━━━━━━"));
    }

    private void handleList(Player player) {
        var guilds = guildManager.getAllGuilds();
        player.sendMessage(ColorUtils.colorize("&6===== ギルド一覧 (" + guilds.size() + ") ====="));
        for (Guild g : guilds) {
            long online = g.getMemberList().stream()
                    .filter(uuid -> Bukkit.getPlayer(uuid) != null)
                    .count();
            player.sendMessage(ColorUtils.colorize("&e" + g.getName() + " &7[" + g.getTag() + "] &fLv." + g.getLevel()
                    + " " + g.getMemberCount() + "人 (&a" + online + " オンライン&f)"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 新規コマンド
    // ═══════════════════════════════════════════════════════════════════════

    // ── /guild rename <新しい名前> ─────────────────────────────────────────

    private void handleRename(Player player, String[] args) {
        Guild guild = guildManager.getGuildByPlayer(player.getUniqueId());
        if (guild == null) { notInGuild(player); return; }
        if (guild.getRank(player.getUniqueId()) != Guild.Rank.LEADER) {
            player.sendMessage(ColorUtils.colorize("&cリーダーのみギルド名を変更できます。"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ColorUtils.colorize("&c使用法: /guild rename <新しい名前>"));
            return;
        }
        String newName = args[1];
        if (newName.length() > 20) {
            player.sendMessage(ColorUtils.colorize("&cギルド名は20文字以内にしてください。"));
            return;
        }
        Guild existing = guildManager.getGuildByName(newName);
        if (existing != null && !existing.getId().equals(guild.getId())) {
            player.sendMessage(ColorUtils.colorize("&cそのギルド名はすでに使用されています。"));
            return;
        }

        // リネームコスト（config から取得、デフォルト500）
        double cost = plugin.getConfig().getDouble("guild.rename-cost", 500.0);
        if (plugin.getConfig().getBoolean("economy.enabled", true) && vaultHook.isEnabled()) {
            if (!vaultHook.has(player, cost)) {
                player.sendMessage(ColorUtils.colorize("&cギルド名変更には &e" + cost + " &c必要です。"));
                return;
            }
            vaultHook.withdraw(player, cost);
        }

        String oldName = guild.getName();
        guild.setName(newName);
        guildManager.save();
        broadcastToGuild(guild, ColorUtils.colorize("&6ギルド名が &e" + oldName + " &6→ &e" + newName + " &6に変更されました。"));
    }

    // ── /guild tag <新しいタグ> ───────────────────────────────────────────

    private void handleTag(Player player, String[] args) {
        Guild guild = guildManager.getGuildByPlayer(player.getUniqueId());
        if (guild == null) { notInGuild(player); return; }
        if (guild.getRank(player.getUniqueId()) != Guild.Rank.LEADER) {
            player.sendMessage(ColorUtils.colorize("&cリーダーのみタグを変更できます。"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ColorUtils.colorize("&c使用法: /guild tag <新しいタグ>"));
            return;
        }
        String newTag = args[1];
        if (newTag.length() > 6) {
            player.sendMessage(ColorUtils.colorize("&cタグは6文字以内にしてください。"));
            return;
        }
        Guild existing = guildManager.getGuildByTag(newTag);
        if (existing != null && !existing.getId().equals(guild.getId())) {
            player.sendMessage(ColorUtils.colorize("&cそのタグはすでに使用されています。"));
            return;
        }

        double cost = plugin.getConfig().getDouble("guild.tag-cost", 200.0);
        if (plugin.getConfig().getBoolean("economy.enabled", true) && vaultHook.isEnabled()) {
            if (!vaultHook.has(player, cost)) {
                player.sendMessage(ColorUtils.colorize("&cタグ変更には &e" + cost + " &c必要です。"));
                return;
            }
            vaultHook.withdraw(player, cost);
        }

        String oldTag = guild.getTag();
        guild.setTag(newTag);
        guildManager.save();
        broadcastToGuild(guild, ColorUtils.colorize("&6ギルドタグが &e[" + oldTag + "] &6→ &e[" + newTag + "] &6に変更されました。"));
    }

    // ── /guild members [ページ] ───────────────────────────────────────────

    private void handleMembers(Player player, String[] args) {
        Guild guild = guildManager.getGuildByPlayer(player.getUniqueId());
        if (guild == null) { notInGuild(player); return; }

        int page = 1;
        if (args.length >= 2) {
            try { page = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) {}
        }
        int perPage = 10;

        // ランク順ソート: LEADER → OFFICER → MEMBER
        List<Map.Entry<UUID, Guild.Rank>> sorted = guild.getMembers().entrySet().stream()
                .sorted(Comparator.comparingInt(e -> e.getValue().ordinal()))
                .toList();

        int total = sorted.size();
        int maxPage = Math.max(1, (int) Math.ceil((double) total / perPage));
        page = Math.min(Math.max(page, 1), maxPage);

        int start = (page - 1) * perPage;
        int end   = Math.min(start + perPage, total);

        player.sendMessage(ColorUtils.colorize("&6===== &e" + guild.getName() + " &6メンバー ("
                + page + "/" + maxPage + ") ====="));

        for (int i = start; i < end; i++) {
            Map.Entry<UUID, Guild.Rank> entry = sorted.get(i);
            var offline = Bukkit.getOfflinePlayer(entry.getKey());
            String name = offline.getName() != null ? offline.getName() : entry.getKey().toString().substring(0, 8);
            boolean online = Bukkit.getPlayer(entry.getKey()) != null;
            String status = online ? "&a●" : "&7○";
            String rankDisplay = entry.getValue().getDisplayName();
            player.sendMessage(ColorUtils.colorize(status + " " + rankDisplay + " &f" + name));
        }
        if (maxPage > 1) {
            player.sendMessage(ColorUtils.colorize("&7/guild members <ページ番号> で次のページを表示"));
        }
    }

    // ── /guild top ────────────────────────────────────────────────────────

    private void handleTop(Player player) {
        List<Guild> top = guildManager.getGuildsSortedByMembers();
        int limit = Math.min(top.size(), 10);
        player.sendMessage(ColorUtils.colorize("&6===== ギルドランキング TOP" + limit + " ====="));
        for (int i = 0; i < limit; i++) {
            Guild g = top.get(i);
            String medal = switch (i) {
                case 0 -> "&6#1";
                case 1 -> "&7#2";
                case 2 -> "&c#3";
                default -> "&f#" + (i + 1);
            };
            player.sendMessage(ColorUtils.colorize(medal + " &e" + g.getName()
                    + " &7[" + g.getTag() + "] &fLv." + g.getLevel()
                    + " &7- &f" + g.getMemberCount() + "人"));
        }
    }

    // ── /guild bank ───────────────────────────────────────────────────────

    private void handleBank(Player player) {
        Guild guild = guildManager.getGuildByPlayer(player.getUniqueId());
        if (guild == null) { notInGuild(player); return; }
        player.sendMessage(ColorUtils.colorize("&6===== ギルドバンク ====="));
        player.sendMessage(ColorUtils.colorize("&e残高: &a" + String.format("%.2f", guild.getBankBalance())));
        player.sendMessage(ColorUtils.colorize("&7/guild deposit <金額>  - 入金"));
        player.sendMessage(ColorUtils.colorize("&7/guild withdraw <金額> - 出金"));
    }

    // ── /guild deposit <金額> ─────────────────────────────────────────────

    private void handleDeposit(Player player, String[] args) {
        Guild guild = guildManager.getGuildByPlayer(player.getUniqueId());
        if (guild == null) { notInGuild(player); return; }
        if (!vaultHook.isEnabled()) {
            player.sendMessage(ColorUtils.colorize("&c経済プラグイン (Vault) が有効ではありません。"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ColorUtils.colorize("&c使用法: /guild deposit <金額>"));
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtils.colorize("&c有効な金額を入力してください。"));
            return;
        }
        if (amount <= 0) {
            player.sendMessage(ColorUtils.colorize("&c0より大きい金額を入力してください。"));
            return;
        }
        if (!vaultHook.has(player, amount)) {
            player.sendMessage(ColorUtils.colorize("&c残高が不足しています。所持金: &e"
                    + String.format("%.2f", vaultHook.getBalance(player))));
            return;
        }
        vaultHook.withdraw(player, amount);
        guildManager.depositToBank(guild, amount);
        player.sendMessage(ColorUtils.colorize("&aギルドバンクに &e" + String.format("%.2f", amount)
                + " &a入金しました。残高: &e" + String.format("%.2f", guild.getBankBalance())));
        broadcastToGuild(guild, ColorUtils.colorize("&7[バンク] " + player.getName() + " が &e"
                + String.format("%.2f", amount) + " &7を入金しました。"));
    }

    // ── /guild withdraw <金額> ────────────────────────────────────────────

    private void handleWithdraw(Player player, String[] args) {
        Guild guild = guildManager.getGuildByPlayer(player.getUniqueId());
        if (guild == null) { notInGuild(player); return; }
        if (guild.getRank(player.getUniqueId()) == Guild.Rank.MEMBER) {
            player.sendMessage(ColorUtils.colorize("&cOfficer以上のみ出金できます。"));
            return;
        }
        if (!vaultHook.isEnabled()) {
            player.sendMessage(ColorUtils.colorize("&c経済プラグイン (Vault) が有効ではありません。"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ColorUtils.colorize("&c使用法: /guild withdraw <金額>"));
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtils.colorize("&c有効な金額を入力してください。"));
            return;
        }
        if (amount <= 0) {
            player.sendMessage(ColorUtils.colorize("&c0より大きい金額を入力してください。"));
            return;
        }
        if (!guildManager.withdrawFromBank(guild, amount)) {
            player.sendMessage(ColorUtils.colorize("&cギルドバンクの残高が不足しています。残高: &e"
                    + String.format("%.2f", guild.getBankBalance())));
            return;
        }
        // Vault に付与
        vaultHook.deposit(player, amount);

        player.sendMessage(ColorUtils.colorize("&aギルドバンクから &e" + String.format("%.2f", amount)
                + " &a出金しました。残高: &e" + String.format("%.2f", guild.getBankBalance())));
        broadcastToGuild(guild, ColorUtils.colorize("&7[バンク] " + player.getName() + " が &e"
                + String.format("%.2f", amount) + " &7を出金しました。"));
    }

    // ── /guild desc [説明文] ──────────────────────────────────────────────

    private void handleDesc(Player player, String[] args) {
        Guild guild = guildManager.getGuildByPlayer(player.getUniqueId());
        if (guild == null) { notInGuild(player); return; }
        if (guild.getRank(player.getUniqueId()) == Guild.Rank.MEMBER) {
            player.sendMessage(ColorUtils.colorize("&cOfficer以上のみ説明を変更できます。"));
            return;
        }
        if (args.length < 2) {
            // 現在の説明を表示
            String current = guild.getDescription().isEmpty() ? "&7未設定" : guild.getDescription();
            player.sendMessage(ColorUtils.colorize("&e現在の説明: &f" + current));
            player.sendMessage(ColorUtils.colorize("&7/guild desc <説明文> で変更できます。クリアするには /guild desc -clear"));
            return;
        }
        if (args[1].equalsIgnoreCase("-clear")) {
            guild.setDescription("");
            guildManager.save();
            player.sendMessage(ColorUtils.colorize("&aギルドの説明をクリアしました。"));
            return;
        }
        String desc = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        if (desc.length() > 100) {
            player.sendMessage(ColorUtils.colorize("&c説明は100文字以内にしてください。"));
            return;
        }
        guild.setDescription(desc);
        guildManager.save();
        player.sendMessage(ColorUtils.colorize("&aギルドの説明を設定しました: &f" + desc));
    }

    // ── /guild motd [MOTD文] ──────────────────────────────────────────────

    private void handleMotd(Player player, String[] args) {
        Guild guild = guildManager.getGuildByPlayer(player.getUniqueId());
        if (guild == null) { notInGuild(player); return; }
        if (guild.getRank(player.getUniqueId()) == Guild.Rank.MEMBER) {
            player.sendMessage(ColorUtils.colorize("&cOfficer以上のみMOTDを変更できます。"));
            return;
        }
        if (args.length < 2) {
            String current = guild.getMotd().isEmpty() ? "&7未設定" : guild.getMotd();
            player.sendMessage(ColorUtils.colorize("&e現在のMOTD: &f" + current));
            player.sendMessage(ColorUtils.colorize("&7/guild motd <メッセージ> で変更。クリアは /guild motd -clear"));
            return;
        }
        if (args[1].equalsIgnoreCase("-clear")) {
            guild.setMotd("");
            guildManager.save();
            player.sendMessage(ColorUtils.colorize("&aMOTDをクリアしました。"));
            return;
        }
        String motd = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        if (motd.length() > 100) {
            player.sendMessage(ColorUtils.colorize("&cMOTDは100文字以内にしてください。"));
            return;
        }
        guild.setMotd(motd);
        guildManager.save();
        player.sendMessage(ColorUtils.colorize("&aMOTDを設定しました: &f" + motd));
        broadcastToGuild(guild, ColorUtils.colorize("&6[MOTD] &f" + motd));
    }

    // ── /guild level ──────────────────────────────────────────────────────

    private void handleLevel(Player player) {
        Guild guild = guildManager.getGuildByPlayer(player.getUniqueId());
        if (guild == null) { notInGuild(player); return; }

        player.sendMessage(ColorUtils.colorize("&6===== ギルドレベル ====="));
        player.sendMessage(ColorUtils.colorize("&eギルド: &f" + guild.getName()));
        player.sendMessage(ColorUtils.colorize("&eレベル: &a" + guild.getLevel()));
        player.sendMessage(ColorUtils.colorize("&e現在のEXP: &f" + guild.getExp()));

        long nextExp = guild.getExpForNextLevel();
        if (nextExp < 0) {
            player.sendMessage(ColorUtils.colorize("&6最大レベル(50)に達しています！"));
        } else {
            long needed = nextExp - guild.getExp();
            player.sendMessage(ColorUtils.colorize("&e次のレベルまで: &f" + Math.max(0, needed) + " EXP"));

            // プログレスバー
            int barLength = 20;
            long prevExp = guild.getLevel() <= 1 ? 0L : (long) Math.pow(guild.getLevel() - 2, 2) * 100L;
            long levelExp = nextExp - prevExp;
            long currentInLevel = guild.getExp() - prevExp;
            double ratio = levelExp > 0 ? (double) currentInLevel / levelExp : 0;
            int filled = (int) (ratio * barLength);
            String bar = "&a" + "█".repeat(Math.max(0, filled))
                    + "&7" + "░".repeat(Math.max(0, barLength - filled));
            player.sendMessage(ColorUtils.colorize("&e進捗: [" + bar + "&e] &f"
                    + String.format("%.1f", ratio * 100) + "%"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GUIメニュー (/guild または /guild menu)
    // ═══════════════════════════════════════════════════════════════════════

    private void openGuildMenu(Player player) {
        Guild guild = guildManager.getGuildByPlayer(player.getUniqueId());

        if (guild == null) {
            // 非所属プレイヤー向け: ギルド作成・参加メニュー
            openNoGuildMenu(player);
        } else {
            openGuildMainMenu(player, guild);
        }
    }

    private void openNoGuildMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27,
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                        .legacySection().deserialize("§6GuildPlus - ギルドなし"));

        // 作成ボタン
        ItemStack createItem = createGuiItem(Material.EMERALD, "§a§lギルドを作成",
                List.of("§7/guild create <名前> [タグ]",
                        "§7コスト: §e" + plugin.getConfig().getDouble("guild.creation-cost", 1000.0)));
        inv.setItem(11, createItem);

        // 参加ボタン
        ItemStack joinItem = createGuiItem(Material.OAK_DOOR, "§b§lギルドに参加",
                List.of("§7招待を受け取った後、§e/guild join §7で参加"));
        inv.setItem(13, joinItem);

        // ランキング
        ItemStack topItem = createGuiItem(Material.GOLD_INGOT, "§e§lランキング",
                List.of("§7/guild top §7で確認"));
        inv.setItem(15, topItem);

        // 装飾
        ItemStack glass = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, glass);
        }

        player.openInventory(inv);
    }

    private void openGuildMainMenu(Player player, Guild guild) {
        Inventory inv = Bukkit.createInventory(null, 54,
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                        .legacySection().deserialize("§6" + guild.getName() + " §7[§e" + guild.getTag() + "§7]"));

        Guild.Rank rank = guild.getRank(player.getUniqueId());
        boolean isLeaderOrOfficer = rank == Guild.Rank.LEADER || rank == Guild.Rank.OFFICER;
        boolean isLeader = rank == Guild.Rank.LEADER;

        // ギルド情報
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§eレベル: §f" + guild.getLevel() + " §7(EXP: " + guild.getExp() + ")");
        infoLore.add("§eメンバー: §f" + guild.getMemberCount() + "人");
        infoLore.add("§ePVP: §f" + (guild.isPvpEnabled() ? "§a有効" : "§c無効"));
        if (!guild.getDescription().isEmpty()) {
            infoLore.add("§e説明: §f" + guild.getDescription());
        }
        infoLore.add("");
        infoLore.add("§7クリックで /guild info");
        inv.setItem(4, createGuiItem(Material.BOOK, "§6§lギルド情報", infoLore));

        // ホーム
        inv.setItem(19, createGuiItem(Material.BED, "§a§lギルドホーム",
                List.of("§7クリックでホームへテレポート", "§e/guild home")));

        // メンバー一覧
        inv.setItem(21, createGuiItem(Material.PLAYER_HEAD, "§b§lメンバー一覧",
                List.of("§7メンバーを確認", "§e/guild members")));

        // チェスト
        inv.setItem(23, createGuiItem(Material.CHEST, "§6§lギルドチェスト",
                List.of("§7共有チェストを開く", "§e/guild chest")));

        // バンク
        inv.setItem(25, createGuiItem(Material.GOLD_NUGGET, "§e§lギルドバンク",
                List.of("§e残高: §f" + String.format("%.2f", guild.getBankBalance()),
                        "§e/guild bank")));

        // チャット
        boolean chatMode = chatManager.isGuildChatMode(player.getUniqueId());
        inv.setItem(28, createGuiItem(Material.WRITABLE_BOOK, "§d§lギルドチャット",
                List.of("§7モード: " + (chatMode ? "§aON" : "§cOFF"),
                        "§7クリックで切替", "§e/guild chat")));

        // ランキング
        inv.setItem(30, createGuiItem(Material.GOLD_INGOT, "§e§lランキング",
                List.of("§7メンバー数ランキング", "§e/guild top")));

        // Officer以上: PVP設定
        if (isLeaderOrOfficer) {
            inv.setItem(32, createGuiItem(Material.DIAMOND_SWORD, "§c§lPVP設定",
                    List.of("§7現在: " + (guild.isPvpEnabled() ? "§a有効" : "§c無効"),
                            "§7クリックで切替")));
        }

        // リーダー専用: 設定
        if (isLeader) {
            inv.setItem(34, createGuiItem(Material.COMMAND_BLOCK, "§4§lギルド設定 (Leader)",
                    List.of("§e/guild rename §7- 名前変更",
                            "§e/guild tag §7- タグ変更",
                            "§e/guild sethome §7- ホーム設定",
                            "§e/guild delete §7- 解散")));
        }

        // レベル
        inv.setItem(40, createGuiItem(Material.EXPERIENCE_BOTTLE, "§a§lレベル情報",
                List.of("§eレベル: §f" + guild.getLevel(),
                        "§eEXP: §f" + guild.getExp(),
                        "§e/guild level で詳細")));

        // 脱退ボタン（リーダー以外）
        if (!isLeader) {
            inv.setItem(49, createGuiItem(Material.BARRIER, "§c§lギルドを脱退",
                    List.of("§cクリックして脱退", "§7/guild leave")));
        }

        // 装飾ガラス
        ItemStack glass = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, glass);
        }

        player.openInventory(inv);
        // GUIクリックイベントはGuildMenuListenerで処理
    }

    // ── Help ──────────────────────────────────────────────────────────────

    private void sendHelp(Player player) {
        player.sendMessage(ColorUtils.colorize("&6&l===== GuildPlus ヘルプ ====="));
        player.sendMessage(ColorUtils.colorize("&e/guild &f- GUIメニューを開く"));
        player.sendMessage(ColorUtils.colorize("&e/guild create <名前> [タグ] &f- ギルドを作成"));
        player.sendMessage(ColorUtils.colorize("&e/guild delete &f- ギルドを解散"));
        player.sendMessage(ColorUtils.colorize("&e/guild rename <名前> &f- ギルド名変更 (Leader)"));
        player.sendMessage(ColorUtils.colorize("&e/guild tag <タグ> &f- タグ変更 (Leader)"));
        player.sendMessage(ColorUtils.colorize("&e/guild invite <プレイヤー> &f- プレイヤーを招待"));
        player.sendMessage(ColorUtils.colorize("&e/guild join &f- ギルドに参加"));
        player.sendMessage(ColorUtils.colorize("&e/guild leave &f- ギルドを脱退"));
        player.sendMessage(ColorUtils.colorize("&e/guild kick <プレイヤー> &f- メンバーをキック"));
        player.sendMessage(ColorUtils.colorize("&e/guild promote/demote <プレイヤー> &f- 昇格/降格"));
        player.sendMessage(ColorUtils.colorize("&e/guild members [ページ] &f- メンバー一覧"));
        player.sendMessage(ColorUtils.colorize("&e/guild home / sethome &f- ホームへ/設定"));
        player.sendMessage(ColorUtils.colorize("&e/guild chat [メッセージ] &f- ギルドチャット"));
        player.sendMessage(ColorUtils.colorize("&e/guild chest &f- 共有チェスト"));
        player.sendMessage(ColorUtils.colorize("&e/guild bank / deposit / withdraw &f- バンク管理"));
        player.sendMessage(ColorUtils.colorize("&e/guild desc [説明] &f- ギルド説明"));
        player.sendMessage(ColorUtils.colorize("&e/guild motd [MOTD] &f- MOTD設定"));
        player.sendMessage(ColorUtils.colorize("&e/guild pvp <true|false> &f- ギルド内PVP"));
        player.sendMessage(ColorUtils.colorize("&e/guild ally <request|accept|remove> <ギルド> &f- 同盟"));
        player.sendMessage(ColorUtils.colorize("&e/guild enemy <set|remove> <ギルド> &f- 敵対"));
        player.sendMessage(ColorUtils.colorize("&e/guild info [ギルド名] &f- ギルド情報"));
        player.sendMessage(ColorUtils.colorize("&e/guild list &f- ギルド一覧"));
        player.sendMessage(ColorUtils.colorize("&e/guild top &f- ランキング TOP10"));
        player.sendMessage(ColorUtils.colorize("&e/guild level &f- レベル情報"));
    }

    // ─────────────────────────────────────────────────────────────────────
    // Utility
    // ─────────────────────────────────────────────────────────────────────

    private void notInGuild(Player player) {
        player.sendMessage(ColorUtils.colorize("&cギルドに所属していません。/guild で参加またはギルドを作成できます。"));
    }

    private void broadcastToGuild(Guild guild, String message) {
        for (UUID uuid : guild.getMemberList()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(message);
        }
    }

    private UUID resolveOfflineUuid(String name) {
        var offline = Bukkit.getOfflinePlayerIfCached(name);
        return offline != null ? offline.getUniqueId() : null;
    }

    private UUID resolveGuildMemberUuid(Guild guild, String name) {
        for (UUID uuid : guild.getMemberList()) {
            var p = Bukkit.getOfflinePlayer(uuid);
            if (name.equalsIgnoreCase(p.getName())) return uuid;
        }
        return null;
    }

    private ItemStack createGuiItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                    .legacySection().deserialize(name)
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> loreComponents = lore.stream()
                    .map(l -> net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                            .legacySection().deserialize(l)
                            .decoration(TextDecoration.ITALIC, false))
                    .collect(Collectors.toList());
            meta.lore(loreComponents);
            item.setItemMeta(meta);
        }
        return item;
    }


    // ── Tab Completion ─────────────────────────────────────────────────────

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return filterStart(List.of(
                    "create","delete","rename","tag","invite","join","leave","kick",
                    "promote","demote","home","sethome","chat","chest","pvp",
                    "ally","enemy","info","list","members","top",
                    "bank","deposit","withdraw","desc","motd","level","menu","help"
            ), args[0]);
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "pvp"      -> filterStart(List.of("true", "false"), args[1]);
                case "ally"     -> filterStart(List.of("request", "accept", "remove"), args[1]);
                case "enemy"    -> filterStart(List.of("set", "remove"), args[1]);
                case "invite", "kick", "promote", "demote" ->
                        Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                                .toList();
                case "info" ->
                        guildManager.getAllGuilds().stream()
                                .map(Guild::getName)
                                .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                                .toList();
                default -> null;
            };
        }
        return null;
    }

    private List<String> filterStart(List<String> list, String prefix) {
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .toList();
    }
}
