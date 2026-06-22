package com.guildplus;

import com.guildplus.ally.AllyManager;
import com.guildplus.chat.GuildChatManager;
import com.guildplus.chest.GuildChestManager;
import com.guildplus.commands.GcCommand;
import com.guildplus.commands.GuildCommand;
import com.guildplus.guild.AllyRequestManager;
import com.guildplus.guild.GuildManager;
import com.guildplus.guild.InviteManager;
import com.guildplus.listeners.ChatListener;
import com.guildplus.listeners.ChestListener;
import com.guildplus.listeners.GuildExpListener;
import com.guildplus.listeners.GuildLoginListener;
import com.guildplus.listeners.GuildMenuListener;
import com.guildplus.listeners.PvpListener;
import com.guildplus.placeholder.GuildPlaceholder;
import com.guildplus.utils.ColorUtils;
import com.guildplus.utils.VaultHook;
import org.bukkit.plugin.java.JavaPlugin;

public class GuildPlusPlugin extends JavaPlugin {

    private GuildManager guildManager;
    private InviteManager inviteManager;
    private AllyRequestManager allyRequestManager;
    private GuildChatManager chatManager;
    private GuildChestManager chestManager;
    private AllyManager allyManager;
    private VaultHook vaultHook;
    private ChestListener chestListener;

    @Override
    public void onEnable() {
        // Config
        saveDefaultConfig();

        // Managers
        guildManager        = new GuildManager(this);
        inviteManager       = new InviteManager(this);
        allyRequestManager  = new AllyRequestManager(this);
        chatManager         = new GuildChatManager(this, guildManager);
        chestManager        = new GuildChestManager(this);
        allyManager         = new AllyManager(guildManager, allyRequestManager);

        // Vault
        vaultHook = new VaultHook();
        if (vaultHook.setup()) {
            getLogger().info("Vault との連携に成功しました。");
        } else {
            getLogger().warning("Vault が見つかりません。経済機能は無効です。");
        }

        // Listeners
        chestListener = new ChestListener(chestManager, guildManager);

        getServer().getPluginManager().registerEvents(
                new ChatListener(this, guildManager, chatManager), this);
        getServer().getPluginManager().registerEvents(
                new PvpListener(guildManager), this);
        getServer().getPluginManager().registerEvents(chestListener, this);

        // 新規リスナー
        getServer().getPluginManager().registerEvents(
                new GuildExpListener(this, guildManager), this);
        getServer().getPluginManager().registerEvents(
                new GuildLoginListener(this, guildManager), this);
        getServer().getPluginManager().registerEvents(
                new GuildMenuListener(this, guildManager, chatManager, chestManager, chestListener), this);

        // Commands
        GuildCommand guildCommand = new GuildCommand(
                this, guildManager, inviteManager, chatManager,
                chestManager, allyManager, vaultHook, chestListener);
        var guildCmd = getCommand("guild");
        if (guildCmd != null) {
            guildCmd.setExecutor(guildCommand);
            guildCmd.setTabCompleter(guildCommand);
        }

        GcCommand gcCommand = new GcCommand(chatManager, guildManager);
        var gcCmd = getCommand("gc");
        if (gcCmd != null) {
            gcCmd.setExecutor(gcCommand);
        }

        // PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new GuildPlaceholder(this, guildManager, chatManager).register();
            getLogger().info("PlaceholderAPI との連携に成功しました。");
        } else {
            getLogger().warning("PlaceholderAPI が見つかりません。プレースホルダーは無効です。");
        }

        getLogger().info(ColorUtils.colorize("&aGuildPlus が有効化されました！ バージョン: "
                + getDescription().getVersion()));
    }

    @Override
    public void onDisable() {
        if (guildManager != null) guildManager.save();
        getLogger().info("GuildPlus が無効化されました。");
    }

    // ── Getters ─────────────────────────────────────────────────────────────

    public GuildManager getGuildManager()         { return guildManager; }
    public InviteManager getInviteManager()       { return inviteManager; }
    public GuildChatManager getChatManager()      { return chatManager; }
    public GuildChestManager getChestManager()    { return chestManager; }
    public AllyManager getAllyManager()           { return allyManager; }
    public VaultHook getVaultHook()               { return vaultHook; }
}
