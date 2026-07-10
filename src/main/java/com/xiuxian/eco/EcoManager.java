package com.xiuxian.eco;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EcoManager {

    private final XiuXianEco plugin;
    private final Map<UUID, Map<String, Double>> balances = new HashMap<>();
    private final File dataFolder;

    public EcoManager(XiuXianEco plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) dataFolder.mkdirs();
    }

    public double getBalance(Player player, String currency) {
        return getBalance(player.getUniqueId(), currency);
    }

    public double getBalance(UUID uuid, String currency) {
        Map<String, Double> playerBal = balances.computeIfAbsent(uuid, k -> loadPlayer(k));
        return playerBal.getOrDefault(currency, 0.0);
    }

    public boolean addBalance(Player player, String currency, double amount) {
        return addBalance(player.getUniqueId(), currency, amount);
    }

    public boolean addBalance(UUID uuid, String currency, double amount) {
        if (amount <= 0) return false;
        Map<String, Double> playerBal = balances.computeIfAbsent(uuid, k -> loadPlayer(k));
        playerBal.merge(currency, amount, Double::sum);
        savePlayer(uuid, playerBal);
        return true;
    }

    public boolean removeBalance(Player player, String currency, double amount) {
        return removeBalance(player.getUniqueId(), currency, amount);
    }

    public boolean removeBalance(UUID uuid, String currency, double amount) {
        if (amount <= 0) return false;
        Map<String, Double> playerBal = balances.computeIfAbsent(uuid, k -> loadPlayer(k));
        double current = playerBal.getOrDefault(currency, 0.0);
        if (current < amount) return false;
        playerBal.put(currency, current - amount);
        savePlayer(uuid, playerBal);
        return true;
    }

    public boolean setBalance(Player player, String currency, double amount) {
        return setBalance(player.getUniqueId(), currency, amount);
    }

    public boolean setBalance(UUID uuid, String currency, double amount) {
        if (amount < 0) return false;
        Map<String, Double> playerBal = balances.computeIfAbsent(uuid, k -> loadPlayer(k));
        playerBal.put(currency, amount);
        savePlayer(uuid, playerBal);
        return true;
    }

    public boolean has(Player player, String currency, double amount) {
        return getBalance(player, currency) >= amount;
    }

    public boolean transfer(Player from, Player to, String currency, double amount) {
        if (!removeBalance(from, currency, amount)) return false;
        addBalance(to, currency, amount);
        return true;
    }

    public void initPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        if (!balances.containsKey(uuid)) {
            Map<String, Double> bal = new HashMap<>();
            for (XiuXianEco.CurrencyDef def : plugin.getCurrencies().values()) {
                bal.put(def.id, def.startingBalance);
            }
            balances.put(uuid, bal);
            savePlayer(uuid, bal);
        }
    }

    private Map<String, Double> loadPlayer(UUID uuid) {
        Map<String, Double> bal = new HashMap<>();
        File file = new File(dataFolder, uuid.toString() + ".yml");
        if (file.exists()) {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            for (String key : cfg.getKeys(false)) {
                bal.put(key, cfg.getDouble(key, 0));
            }
        } else {
            for (XiuXianEco.CurrencyDef def : plugin.getCurrencies().values()) {
                bal.put(def.id, def.startingBalance);
            }
        }
        return bal;
    }

    private void savePlayer(UUID uuid, Map<String, Double> bal) {
        File file = new File(dataFolder, uuid.toString() + ".yml");
        FileConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<String, Double> e : bal.entrySet()) {
            cfg.set(e.getKey(), e.getValue());
        }
        try {
            cfg.save(file);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save data for " + uuid);
        }
    }

    public void saveAll() {
        for (Map.Entry<UUID, Map<String, Double>> e : balances.entrySet()) {
            savePlayer(e.getKey(), e.getValue());
        }
    }
}
