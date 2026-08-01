package com.lifesteal.chunkcollector.commands;

import com.lifesteal.chunkcollector.ChunkCollectorPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CollectorCommand implements CommandExecutor {

    private final ChunkCollectorPlugin plugin;

    public CollectorCommand(ChunkCollectorPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(color("&eUsage: /collector <give|limit|reload>"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give" -> handleGive(sender, args);
            case "limit" -> handleLimit(sender, args);
            case "reload" -> handleReload(sender);
            default -> sender.sendMessage(color("&eUsage: /collector <give|limit|reload>"));
        }
        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("chunkcollector.admin")) {
            sender.sendMessage(color(prefix() + plugin.getConfig().getString("messages.no-permission")));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(color("&eUsage: /collector give <player> [amount]"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(color("&cPemain tidak ditemukan atau sedang offline."));
            return;
        }
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Math.max(1, Integer.parseInt(args[2]));
            } catch (NumberFormatException ignored) {
            }
        }

        ItemStack item = plugin.getItemFactory().createCollectorItem(amount);
        target.getInventory().addItem(item).values()
                .forEach(leftover -> target.getWorld().dropItemNaturally(target.getLocation(), leftover));

        String msg = plugin.getConfig().getString("messages.gave-item", "&aGave item")
                .replace("%amount%", String.valueOf(amount))
                .replace("%player%", target.getName());
        sender.sendMessage(color(prefix() + msg));
        target.sendMessage(color(prefix() + "&aKamu menerima " + amount + "x Chunk Collector!"));
    }

    private void handleLimit(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(color("&cPemain tidak ditemukan."));
                return;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage(color("&cSpesifikasikan nama pemain: /collector limit <player>"));
            return;
        }

        int limit = plugin.getCollectorManager().getLimit(target);
        int owned = plugin.getCollectorManager().getOwnedCount(target.getUniqueId());
        sender.sendMessage(color(prefix() + "&e" + target.getName() + " memiliki " + owned + "/" + limit + " Chunk Collector."));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("chunkcollector.admin")) {
            sender.sendMessage(color(prefix() + plugin.getConfig().getString("messages.no-permission")));
            return;
        }
        plugin.reloadConfig();
        sender.sendMessage(color(prefix() + plugin.getConfig().getString("messages.reload-success")));
    }

    private String prefix() {
        return plugin.getConfig().getString("messages.prefix", "");
    }

    private String color(String s) {
        return s == null ? "" : ChatColor.translateAlternateColorCodes('&', s);
    }
}
