package com.guildplus.listeners;

import com.guildplus.GuildPlusPlugin;
import com.guildplus.chat.GuildChatManager;
import com.guildplus.guild.Guild;
import com.guildplus.guild.GuildManager;
import com.guildplus.utils.ColorUtils;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {

    private final GuildPlusPlugin plugin;
    private final GuildManager guildManager;
    private final GuildChatManager chatManager;

    public ChatListener(GuildPlusPlugin plugin, GuildManager guildManager, GuildChatManager chatManager) {
        this.plugin = plugin;
        this.guildManager = guildManager;
        this.chatManager = chatManager;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        // ギルドチャットモードの場合はギルドチャットへ
        if (chatManager.isGuildChatMode(player.getUniqueId())) {
            event.setCancelled(true);
            String rawMessage = LegacyComponentSerializer.legacyAmpersand()
                    .serialize(event.message());
            chatManager.sendGuildChat(player, rawMessage);
            return;
        }

        // グローバルチャット: ギルド名Prefixを追加
        Guild guild = guildManager.getGuildByPlayer(player.getUniqueId());
        if (guild != null) {
            String guildNameColor = plugin.getConfig().getString("chat.guild-name-color", "&6");
            String rawPrefix = guildNameColor + "[" + guild.getName() + "] &r";
            // ColorUtils.colorize returns a legacy §-formatted string
            String colorized = ColorUtils.colorize(rawPrefix);
            Component prefix = LegacyComponentSerializer.legacySection().deserialize(colorized);

            event.renderer((source, sourceDisplayName, message, viewer) ->
                    prefix.append(sourceDisplayName).append(Component.text(": ")).append(message)
            );
        }
    }
}
