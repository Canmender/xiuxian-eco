package com.xiuxian.eco;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EcoCommand implements CommandExecutor {
    private static final char COLOR = '&';

    private final XiuXianEco plugin;

    public EcoCommand(XiuXianEco plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        EcoManager eco = plugin.getEcoManager();

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "bal":
            case "balance":
                return handleBalance(sender, args);
            case "give":
                return handleGive(sender, args);
            case "take":
                return handleTake(sender, args);
            case "set":
                return handleSet(sender, args);
            case "pay":
                return handlePay(sender, args);
            case "reload":
                if (!sender.hasPermission("xiueco.admin")) {
                    sender.sendMessage(msg("&cNo permission."));
                    return true;
                }
                plugin.reloadConfig();
                plugin.getLogger().info("Config reloaded");
                sender.sendMessage(msg("&aConfig reloaded."));
                return true;
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleBalance(CommandSender sender, String[] args) {
        Player target = args.length >= 2 ? Bukkit.getPlayer(args[1]) : (sender instanceof Player ? (Player) sender : null);
        if (target == null) {
            sender.sendMessage(msg("&cPlayer not found."));
            return true;
        }

        sender.sendMessage(msg("&6&l=== " + target.getName() + " " + "===."));
        for (var entry : plugin.getCurrencies().entrySet()) {
            XiuXianEco.CurrencyDef def = entry.getValue();
            double bal = plugin.getEcoManager().getBalance(target, def.id);
            sender.sendMessage(msg(def.color + def.name + ": &f" + formatBal(bal)));
        }
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("xiueco.admin")) {
            sender.sendMessage(msg("&cNo permission."));
            return true;
        }
        if (args.length < 4) {
            sender.sendMessage(msg("&cUsage: /eco give <player> <currency> <amount>"));
            return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(msg("&cPlayer not found."));
            return true;
        }
        String currency = args[2].toLowerCase();
        if (plugin.getCurrency(currency) == null) {
            sender.sendMessage(msg("&cUnknown currency: " + currency));
            return true;
        }
        double amount;
        try { amount = Double.parseDouble(args[3]); } catch (NumberFormatException e) {
            sender.sendMessage(msg("&cInvalid amount."));
            return true;
        }
        plugin.getEcoManager().addBalance(target, currency, amount);
        XiuXianEco.CurrencyDef def = plugin.getCurrency(currency);
        sender.sendMessage(msg("&a+" + formatBal(amount) + " " + def.name + " to " + target.getName()));
        target.sendMessage(msg(def.color + "+" + formatBal(amount) + " " + def.name));
        return true;
    }

    private boolean handleTake(CommandSender sender, String[] args) {
        if (!sender.hasPermission("xiueco.admin")) {
            sender.sendMessage(msg("&cNo permission."));
            return true;
        }
        if (args.length < 4) {
            sender.sendMessage(msg("&cUsage: /eco take <player> <currency> <amount>"));
            return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(msg("&cPlayer not found."));
            return true;
        }
        String currency = args[2].toLowerCase();
        if (plugin.getCurrency(currency) == null) {
            sender.sendMessage(msg("&cUnknown currency: " + currency));
            return true;
        }
        double amount;
        try { amount = Double.parseDouble(args[3]); } catch (NumberFormatException e) {
            sender.sendMessage(msg("&cInvalid amount."));
            return true;
        }
        if (!plugin.getEcoManager().removeBalance(target, currency, amount)) {
            sender.sendMessage(msg("&cInsufficient balance."));
            return true;
        }
        XiuXianEco.CurrencyDef def = plugin.getCurrency(currency);
        sender.sendMessage(msg("&c-" + formatBal(amount) + " " + def.name + " from " + target.getName()));
        target.sendMessage(msg("&c-" + formatBal(amount) + " " + def.name));
        return true;
    }

    private boolean handleSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("xiueco.admin")) {
            sender.sendMessage(msg("&cNo permission."));
            return true;
        }
        if (args.length < 4) {
            sender.sendMessage(msg("&cUsage: /eco set <player> <currency> <amount>"));
            return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(msg("&cPlayer not found."));
            return true;
        }
        String currency = args[2].toLowerCase();
        if (plugin.getCurrency(currency) == null) {
            sender.sendMessage(msg("&cUnknown currency: " + currency));
            return true;
        }
        double amount;
        try { amount = Double.parseDouble(args[3]); } catch (NumberFormatException e) {
            sender.sendMessage(msg("&cInvalid amount."));
            return true;
        }
        plugin.getEcoManager().setBalance(target, currency, amount);
        XiuXianEco.CurrencyDef def = plugin.getCurrency(currency);
        sender.sendMessage(msg("&aSet " + target.getName() + " " + def.name + " = " + formatBal(amount)));
        return true;
    }

    private boolean handlePay(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(msg("&cPlayers only."));
            return true;
        }
        if (args.length < 4) {
            sender.sendMessage(msg("&cUsage: /eco pay <player> <currency> <amount>"));
            return true;
        }
        Player from = (Player) sender;
        Player to = Bukkit.getPlayer(args[1]);
        if (to == null) {
            sender.sendMessage(msg("&cPlayer not found."));
            return true;
        }
        String currency = args[2].toLowerCase();
        if (plugin.getCurrency(currency) == null) {
            sender.sendMessage(msg("&cUnknown currency: " + currency));
            return true;
        }
        double amount;
        try { amount = Double.parseDouble(args[3]); } catch (NumberFormatException e) {
            sender.sendMessage(msg("&cInvalid amount."));
            return true;
        }
        if (!plugin.getEcoManager().transfer(from, to, currency, amount)) {
            sender.sendMessage(msg("&cInsufficient balance."));
            return true;
        }
        XiuXianEco.CurrencyDef def = plugin.getCurrency(currency);
        sender.sendMessage(msg("&c-" + formatBal(amount) + " " + def.name + " -> " + to.getName()));
        to.sendMessage(msg(def.color + "+" + formatBal(amount) + " " + def.name + " <- " + from.getName()));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(msg("&6&l=== XiuXianEco ==="));
        sender.sendMessage(msg("&e/eco bal [player] &7- Check balance"));
        sender.sendMessage(msg("&e/eco give <player> <currency> <amount>"));
        sender.sendMessage(msg("&e/eco take <player> <currency> <amount>"));
        sender.sendMessage(msg("&e/eco set <player> <currency> <amount>"));
        sender.sendMessage(msg("&e/eco pay <player> <currency> <amount>"));
        sender.sendMessage(msg("&e/eco reload"));
    }

    private String msg(String s) {
        return ChatColor.translateAlternateColorCodes(COLOR, s);
    }

    private String formatBal(double bal) {
        if (bal == (long) bal) return String.format("%,d", (long) bal);
        return String.format("%,.2f", bal);
    }
}
