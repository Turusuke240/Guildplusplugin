package com.guildplus.utils;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Vault 経済連携ユーティリティ。
 * deposit(Player, double) を追加。
 */
public class VaultHook {

    private Economy economy;
    private boolean enabled;

    public boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            enabled = false;
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            enabled = false;
            return false;
        }
        economy = rsp.getProvider();
        enabled = true;
        return true;
    }

    public boolean isEnabled() { return enabled; }

    public double getBalance(Player player) {
        if (!enabled || economy == null) return 0;
        return economy.getBalance(player);
    }

    public boolean withdraw(Player player, double amount) {
        if (!enabled || economy == null) return false;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean deposit(Player player, double amount) {
        if (!enabled || economy == null) return false;
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (!enabled || economy == null) return false;
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    public boolean has(Player player, double amount) {
        if (!enabled || economy == null) return false;
        return economy.has(player, amount);
    }

    /** フォーマット済み金額文字列を返す */
    public String format(double amount) {
        if (!enabled || economy == null) return String.format("%.2f", amount);
        return economy.format(amount);
    }
}
