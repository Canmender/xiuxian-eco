package com.xiuxian.eco;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class EcoPAPIExpansion extends PlaceholderExpansion {

    private final XiuXianEco plugin;

    public EcoPAPIExpansion(XiuXianEco plugin) {
        this.plugin = plugin;
    }

    @Override public String getIdentifier() { return "xiueco"; }
    @Override public String getAuthor() { return "XiuXianEco"; }
    @Override public String getVersion() { return "1.0"; }
    @Override public boolean persist() { return true; }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) return "0";
        double balance = plugin.getEcoManager().getBalance(player, identifier);
        if (balance == (long) balance) {
            return String.format("%,d", (long) balance);
        }
        return String.format("%,.2f", balance);
    }
}
