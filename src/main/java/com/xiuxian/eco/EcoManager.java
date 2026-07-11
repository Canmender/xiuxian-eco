package com.xiuxian.eco;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class EcoManager {

    private final XiuXianEco plugin;
    private final Map<UUID, Map<String, Double>> balances = new HashMap<>();
    private final Set<UUID> dirtyPlayers = new HashSet<>();
    private final File dataFolder;
    private int saveTaskId = -1;

    public EcoManager(XiuXianEco plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) dataFolder.mkdirs();
        startAutoSave();
    }

    // ========== auto save every 30s ==========
    private void startAutoSave() {
        saveTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::flushDirty, 600L, 600L).getTaskId();
    }

    public void flushDirty() {
        Set<UUID> toSave;
        synchronized (dirtyPlayers) {
            if (dirtyPlayers.isEmpty()) return;
            toSave = new HashSet<>(dirtyPlayers);
            dirtyPlayers.clear();
        }
        for (UUID uuid : toSave) {
            Map<String, Double> bal = balances.get(uuid);
            if (bal != null) savePlayerToFile(uuid, bal);
        }
    }

    private void markDirty(UUID uuid) {
        synchronized (dirtyPlayers) { dirtyPlayers.add(uuid); }
    }

    // ========== balance ops (memory only) ==========
    public double getBalance(Player player, String currency) {
        return getBalance(player.getUniqueId(), currency);
    }

    public double getBalance(UUID uuid, String currency) {
        return balances.computeIfAbsent(uuid, this::loadPlayer).getOrDefault(currency, 0.0);
    }

    public boolean addBalance(Player player, String currency, double amount) {
        return addBalance(player.getUniqueId(), currency, amount);
    }

    public boolean addBalance(UUID uuid, String currency, double amount) {
        if (amount <= 0) return false;
        balances.computeIfAbsent(uuid, this::loadPlayer).merge(currency, amount, Double::sum);
        markDirty(uuid);
        return true;
    }

    public boolean removeBalance(Player player, String currency, double amount) {
        return removeBalance(player.getUniqueId(), currency, amount);
    }

    public boolean removeBalance(UUID uuid, String currency, double amount) {
        if (amount <= 0) return false;
        Map<String, Double> bal = balances.computeIfAbsent(uuid, this::loadPlayer);
        double current = bal.getOrDefault(currency, 0.0);
        if (current < amount) return false;
        bal.put(currency, current - amount);
        markDirty(uuid);
        return true;
    }

    public boolean setBalance(Player player, String currency, double amount) {
        return setBalance(player.getUniqueId(), currency, amount);
    }

    public boolean setBalance(UUID uuid, String currency, double amount) {
        if (amount < 0) return false;
        balances.computeIfAbsent(uuid, this::loadPlayer).put(currency, amount);
        markDirty(uuid);
        return true;
    }

    public boolean has(Player player, String currency, double amount) {
        return getBalance(player, currency) >= amount;
    }

    // ========== atomic transfer (single memory op) ==========
    public boolean transfer(Player from, Player to, String currency, double amount) {
        UUID fromUuid = from.getUniqueId();
        UUID toUuid = to.getUniqueId();
        Map<String, Double> fromBal = balances.computeIfAbsent(fromUuid, this::loadPlayer);
        Map<String, Double> toBal = balances.computeIfAbsent(toUuid, this::loadPlayer);

        double fromCurrent = fromBal.getOrDefault(currency, 0.0);
        if (fromCurrent < amount) return false;

        fromBal.put(currency, fromCurrent - amount);
        toBal.merge(currency, amount, Double::sum);
        markDirty(fromUuid);
        markDirty(toUuid);
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
            markDirty(uuid);
        }
    }

    // ========== file IO ==========
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

    private void savePlayerToFile(UUID uuid, Map<String, Double> bal) {
        File file = new File(dataFolder, uuid.toString() + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();
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
        if (saveTaskId != -1) {
            Bukkit.getScheduler().cancelTask(saveTaskId);
            saveTaskId = -1;
        }
        for (Map.Entry<UUID, Map<String, Double>> e : balances.entrySet()) {
            savePlayerToFile(e.getKey(), e.getValue());
        }
        synchronized (dirtyPlayers) { dirtyPlayers.clear(); }
    }
}
