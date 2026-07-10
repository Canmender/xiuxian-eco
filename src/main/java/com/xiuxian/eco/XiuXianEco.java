package com.xiuxian.eco;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class XiuXianEco extends JavaPlugin {

    private static XiuXianEco instance;
    private EcoManager ecoManager;

    // Currency definitions loaded from config
    private Map<String, CurrencyDef> currencies = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Load currencies from config
        loadCurrencies();

        // Init economy manager
        ecoManager = new EcoManager(this);

        // Register commands
        getCommand("eco").setExecutor(new EcoCommand(this));

        // Register PAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new EcoPAPIExpansion(this).register();
            getLogger().info("PAPI expansion registered");
        }

        getLogger().info("XiuXianEco enabled: " + currencies.size() + " currencies");
    }

    @Override
    public void onDisable() {
        if (ecoManager != null) ecoManager.saveAll();
    }

    private void loadCurrencies() {
        FileConfiguration cfg = getConfig();
        var section = cfg.getConfigurationSection("currencies");
        if (section == null) return;
        for (String id : section.getKeys(false)) {
            var cs = section.getConfigurationSection(id);
            if (cs == null) continue;
            currencies.put(id, new CurrencyDef(
                id,
                cs.getString("name", id),
                cs.getString("symbol", id),
                cs.getString("icon", "GOLD_INGOT"),
                cs.getDouble("starting-balance", 0),
                cs.getString("color", "&7")
            ));
        }
    }

    public static XiuXianEco getInstance() { return instance; }
    public EcoManager getEcoManager() { return ecoManager; }
    public Map<String, CurrencyDef> getCurrencies() { return currencies; }
    public CurrencyDef getCurrency(String id) { return currencies.get(id); }

    public static class CurrencyDef {
        public final String id, name, symbol, icon, color;
        public final double startingBalance;
        public CurrencyDef(String id, String name, String symbol, String icon, double startingBalance, String color) {
            this.id = id; this.name = name; this.symbol = symbol; this.icon = icon;
            this.startingBalance = startingBalance; this.color = color;
        }
    }
}
