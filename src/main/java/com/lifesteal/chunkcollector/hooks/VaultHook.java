package com.lifesteal.chunkcollector.hooks;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHook {

    private static Economy economy;

    public static boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return true;
    }

    public static boolean isEnabled() {
        return economy != null;
    }

    public static Economy getEconomy() {
        return economy;
    }

    public static void deposit(org.bukkit.entity.Player player, double amount) {
        if (economy != null) {
            economy.depositPlayer(player, amount);
        }
    }
}
