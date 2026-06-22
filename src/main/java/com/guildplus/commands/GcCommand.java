package com.guildplus.commands;

import com.guildplus.chat.GuildChatManager;
import com.guildplus.guild.GuildManager;
import com.guildplus.utils.ColorUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class GcCommand implements CommandExecutor {

    private final GuildChatManager chatManager;
    private final GuildManager guildManager;

    public GcCommand(GuildChatManager chatManager, GuildManager guildManager) {
        this.chatManager = chatManager;
        this.guildManager = guildManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("プレイヤーのみ使用可能です。");
            return true;
        }
        if (guildManager.getGuildByPlayer(player.getUniqueId()) == null) {
            player.sendMessage(ColorUtils.colorize("&cギルドに所属していません。"));
            return true;
        }
        if (args.length == 0) {
            // トグル
            chatManager.toggleGuildChatMode(player.getUniqueId());
            boolean mode = chatManager.isGuildChatMode(player.getUniqueId());
            player.sendMessage(ColorUtils.colorize(mode
                    ? "&aギルドチャットモードをONにしました。"
                    : "&7ギルドチャットモードをOFFにしました。"));
            return true;
        }
        // 引数があればそのまま送信
        String message = String.join(" ", args);
        chatManager.sendGuildChat(player, message);
        return true;
    }
}
