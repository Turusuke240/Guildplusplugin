package com.guildplus.listeners;

import com.guildplus.GuildPlusPlugin;
import com.guildplus.chat.GuildChatManager;
import com.guildplus.chest.GuildChestManager;
import com.guildplus.guild.Guild;
import com.guildplus.guild.GuildManager;
import com.guildplus.utils.ColorUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryClickEvent;
import java.util.UUID;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.inventory.ItemStack;

/**
 * GUIメニュー（/guild）のクリックイベントを処理するリスナー。
 */
public class GuildMenuListener implements Listener {

    private final GuildPlusPlugin plugin;
    private final GuildManager guildManager;
    private final GuildChatManager chatManager;
    private final GuildChestManager chestManager;
    private final ChestListener chestListener;

    public GuildMenuListener(GuildPlusPlugin plugin,
                             GuildManager guildManager,
                             GuildChatManager chatManager,
                             GuildChestManager chestManager,
                             ChestListener chestListener) {
        this.plugin = plugin;
        this.guildManager = guildManager;
        this.chatManager = chatManager;
        this.chestManager = chestManager;
        this.chestListener = chestListener;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title()).replaceAll("§[0-9a-fk-or]", "").trim();

        // GuildPlusのGUIかどうか判定
        boolean isNoGuildMenu = title.equals("GuildPlus - ギルドなし");
        boolean isMainMenu = title.contains("[") && title.contains("]") && !isNoGuildMenu;

        if (!isMainMenu && !isNoGuildMenu) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;
        if (!clicked.hasItemMeta() || clicked.getItemMeta() == null) return;

        String itemName = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName()).replaceAll("§[0-9a-fk-or]", "").trim();

        Guild guild = guildManager.getGuildByPlayer(player.getUniqueId());

        if (isNoGuildMenu) {
            handleNoGuildMenuClick(player, itemName);
            return;
        }

        if (guild == null) return;

        switch (itemName.trim()) {
            case "ギルド情報" -> {
                player.closeInventory();
                player.performCommand("guild info");
            }
            case "ギルドホーム" -> {
                player.closeInventory();
                player.performCommand("guild home");
            }
            case "メンバー一覧" -> {
                player.closeInventory();
                player.performCommand("guild members");
            }
            case "ギルドチェスト" -> {
                player.closeInventory();
                player.performCommand("guild chest");
            }
            case "ギルドバンク" -> {
                player.closeInventory();
                player.performCommand("guild bank");
            }
            case "ギルドチャット" -> {
                player.closeInventory();
                boolean newMode = !chatManager.isGuildChatMode(player.getUniqueId());
                chatManager.toggleGuildChatMode(player.getUniqueId());
                player.sendMessage(ColorUtils.colorize(newMode
                        ? "&aギルドチャットモードをONにしました。"
                        : "&7ギルドチャットモードをOFFにしました。"));
            }
            case "ランキング" -> {
                player.closeInventory();
                player.performCommand("guild top");
            }
            case "PVP設定" -> {
                player.closeInventory();
                boolean newPvp = !guild.isPvpEnabled();
                guild.setPvpEnabled(newPvp);
                guildManager.save();
                player.sendMessage(ColorUtils.colorize(newPvp
                        ? "&aギルド内PVPを有効にしました。"
                        : "&7ギルド内PVPを無効にしました。"));
            }
            case "ギルド設定 (Leader)" -> {
                player.closeInventory();
                player.sendMessage(ColorUtils.colorize("&6===== ギルド設定コマンド ====="));
                player.sendMessage(ColorUtils.colorize("&e/guild rename <新名前> &f- 名前変更"));
                player.sendMessage(ColorUtils.colorize("&e/guild tag <新タグ> &f- タグ変更"));
                player.sendMessage(ColorUtils.colorize("&e/guild sethome &f- ホーム設定"));
                player.sendMessage(ColorUtils.colorize("&e/guild desc <説明> &f- 説明変更"));
                player.sendMessage(ColorUtils.colorize("&e/guild motd <MOTD> &f- MOTD変更"));
                player.sendMessage(ColorUtils.colorize("&e/guild delete &f- §cギルド解散"));
            }
            case "レベル情報" -> {
                player.closeInventory();
                player.performCommand("guild level");
            }
            case "ギルドを脱退" -> {
                player.closeInventory();
                if (guild.getRank(player.getUniqueId()) != Guild.Rank.LEADER) {
                    guildManager.removeMember(guild, player.getUniqueId());
                    player.sendMessage(ColorUtils.colorize("&7ギルド &6" + guild.getName() + " &7を脱退しました。"));
                    for (UUID uuid : guild.getMemberList()) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) p.sendMessage(ColorUtils.colorize("&7" + player.getName() + " がギルドを脱退しました。"));
                    }
                } else {
                    player.sendMessage(ColorUtils.colorize("&cリーダーは脱退できません。/guild delete で解散してください。"));
                }
            }
        }
    }

    private void handleNoGuildMenuClick(Player player, String itemName) {
        switch (itemName.trim()) {
            case "ギルドを作成" -> {
                player.closeInventory();
                player.sendMessage(ColorUtils.colorize("&e/guild create <ギルド名> [タグ] &fでギルドを作成できます。"));
            }
            case "ギルドに参加" -> {
                player.closeInventory();
                player.performCommand("guild join");
            }
            case "ランキング" -> {
                player.closeInventory();
                player.performCommand("guild top");
            }
        }
    }
}
