package clre20.cback;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

public class BackLocationManager {

    private final Cback plugin;
    private final Map<UUID, List<SavedLocation>> lastLocations = new ConcurrentHashMap<>();
    private final Set<UUID> bypassPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, BukkitTask> activeCountdowns = new ConcurrentHashMap<>();

    public BackLocationManager(Cback plugin) {
        this.plugin = plugin;
        loadOnlinePlayers();
    }

    /**
     * Set bypass status for a player (used during /back teleports).
     */
    public void setBypass(UUID uuid, boolean bypass) {
        if (bypass) {
            bypassPlayers.add(uuid);
        } else {
            bypassPlayers.remove(uuid);
        }
    }

    /**
     * Checks if a player has bypass status enabled.
     */
    public boolean isBypassing(UUID uuid) {
        return bypassPlayers.contains(uuid);
    }

    /**
     * Records the player's last location if they pass cooldown/distance checks.
     */
    public void recordLocation(UUID uuid, Location location, boolean isDeath, boolean force) {
        long now = System.currentTimeMillis();
        List<SavedLocation> list = lastLocations.computeIfAbsent(uuid, k -> new CopyOnWriteArrayList<>());

        if (!force && !isBypassing(uuid)) {
            // Cooldown check using the latest location's timestamp
            long cooldownMs = plugin.getConfig().getLong("save-cooldown-ms", 1000);
            long lastSave = !list.isEmpty() ? list.get(0).getTimestamp() : 0L;
            if (now - lastSave < cooldownMs) {
                return; // Teleport too rapid (e.g. Geyser C04 packet desync / correction)
            }

            // Distance check (same world only) using the latest location
            if (!list.isEmpty()) {
                Location lastLoc = list.get(0).getLocation();
                if (lastLoc != null && lastLoc.getWorld() != null && lastLoc.getWorld().equals(location.getWorld())) {
                    double minDistance = plugin.getConfig().getDouble("min-distance", 3.0);
                    if (lastLoc.distanceSquared(location) < minDistance * minDistance) {
                        return; // Too close, likely a rubberband or small adjustment
                    }
                }
            }
        }

        // Add to the front of the list (index 0 is the most recent)
        list.add(0, new SavedLocation(location, now, isDeath));

        // Limit the list to maximum 10 elements
        while (list.size() > 10) {
            list.remove(list.size() - 1);
        }

        // Instantly save to player's specific uuid.yml file
        savePlayer(uuid);
    }

    /**
     * Teleports the player back to their last location asynchronously (index 0).
     */
    public void teleportBack(Player player) {
        teleportTo(player, 0);
    }

    /**
     * Teleports the player to a specific saved location index, scheduling a countdown if configured.
     */
    public void teleportTo(Player player, int index) {
        UUID uuid = player.getUniqueId();
        List<SavedLocation> list = lastLocations.get(uuid);
        if (list == null || index < 0 || index >= list.size()) {
            player.sendMessage(translateColorWithPrefix(plugin.getConfig().getString("messages.no-location", "&c找不到你上一次的傳送位置！")));
            return;
        }

        if (activeCountdowns.containsKey(uuid)) {
            player.sendMessage(translateColorWithPrefix(plugin.getConfig().getString("messages.teleporting-already", "&c您已在傳送倒數中，請稍候！")));
            return;
        }

        SavedLocation saved = list.get(index);
        Location loc = saved.getLocation();
        if (loc == null) {
            player.sendMessage(translateColorWithPrefix(plugin.getConfig().getString("messages.no-location", "&c找不到該位置的詳細座標！")));
            return;
        }

        int delaySeconds = plugin.getConfig().getInt("teleport-delay-seconds", 5);
        if (delaySeconds <= 0) {
            performTeleport(player, loc);
        } else {
            // Start countdown
            BukkitRunnable runnable = new BukkitRunnable() {
                int remaining = delaySeconds;

                @Override
                public void run() {
                    if (!player.isOnline()) {
                        cancelCountdown(uuid, false);
                        return;
                    }

                    if (remaining <= 0) {
                        activeCountdowns.remove(uuid);
                        cancel();
                        performTeleport(player, loc);
                        return;
                    }

                    player.sendMessage(translateColorWithPrefix(String.format(
                            plugin.getConfig().getString("messages.teleport-countdown", "&a將在 &e%d &a秒後傳送，請勿移動或受到傷害..."),
                            remaining
                    )));
                    remaining--;
                }
            };

            BukkitTask task = runnable.runTaskTimer(plugin, 0L, 20L);
            activeCountdowns.put(uuid, task);
        }
    }

    /**
     * Performs the actual teleportation and displays the Title message.
     */
    private void performTeleport(Player player, Location loc) {
        UUID uuid = player.getUniqueId();
        player.sendMessage(translateColorWithPrefix(plugin.getConfig().getString("messages.teleporting", "&a正在傳送回指定位置...")));

        setBypass(uuid, true);

        // Perform async teleport to load chunk safely
        player.teleportAsync(loc, PlayerTeleportEvent.TeleportCause.PLUGIN).thenAccept(success -> {
            setBypass(uuid, false);
            if (success) {
                // Show arrival Title and Subtitle
                String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "未知世界";
                String xStr = String.format("%.1f", loc.getX());
                String yStr = String.format("%.1f", loc.getY());
                String zStr = String.format("%.1f", loc.getZ());

                String title = plugin.getConfig().getString("messages.arrival-title", "&a&l已成功抵達！");
                String subtitle = String.format(
                        plugin.getConfig().getString("messages.arrival-subtitle", "&7已抵達 世界: &f%s &8| &7座標: &eX:%s Y:%s Z:%s"),
                        worldName, xStr, yStr, zStr
                );

                player.sendTitle(colorizeString(title), colorizeString(subtitle), 10, 40, 10);
            } else {
                player.sendMessage(translateColorWithPrefix(plugin.getConfig().getString("messages.teleport-fail", "&c傳送失敗，可能目標區域未載入或安全檢查未通過。")));
            }
        });
    }

    /**
     * Checks if the player has an active teleport countdown.
     */
    public boolean hasActiveCountdown(UUID uuid) {
        return activeCountdowns.containsKey(uuid);
    }

    /**
     * Cancels a player's active teleport countdown.
     */
    public void cancelCountdown(UUID uuid, boolean sendMessage) {
        BukkitTask task = activeCountdowns.remove(uuid);
        if (task != null) {
            task.cancel();
            if (sendMessage) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.sendMessage(translateColorWithPrefix(plugin.getConfig().getString("messages.teleport-cancelled", "&c您的動作或受到傷害，傳送已取消！")));
                }
            }
        }
    }

    /**
     * Cancels all active teleport countdowns.
     */
    public void cancelAllCountdowns() {
        for (UUID uuid : activeCountdowns.keySet()) {
            cancelCountdown(uuid, false);
        }
    }

    /**
     * Helper to translate legacy ampersand color codes into Adventure Component
     */
    public Component translateColor(String message) {
        if (message == null) {
            return Component.empty();
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }

    /**
     * Translates color codes prepended with the configured prefix.
     */
    public Component translateColorWithPrefix(String message) {
        String prefix = plugin.getConfig().getString("prefix", "&8[&6Cback&8] ");
        return translateColor(prefix + message);
    }

    /**
     * Translates ampersand colors into section sign colors for Bedrock Forms.
     */
    public String colorizeString(String text) {
        if (text == null) {
            return "";
        }
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Logs a message to the console with the configured prefix.
     */
    public void log(String message) {
        String prefix = plugin.getConfig().getString("prefix", "&8[&6Cback&8] ");
        Bukkit.getConsoleSender().sendMessage(translateColor(prefix + message));
    }

    /**
     * Gets the most recent saved location for a player.
     */
    public Location getLastLocation(UUID uuid) {
        List<SavedLocation> list = lastLocations.get(uuid);
        if (list != null && !list.isEmpty()) {
            return list.get(0).getLocation();
        }
        return null;
    }

    /**
     * Gets the full list of saved locations for a player.
     */
    public List<SavedLocation> getLocations(UUID uuid) {
        List<SavedLocation> list = lastLocations.get(uuid);
        if (list != null) {
            return list;
        }
        return Collections.emptyList();
    }

    /**
     * Loads the saved locations list for a specific player from their uuid.yml file inside plugins/Cback/data/.
     */
    public void loadPlayer(UUID uuid) {
        File dataFolder = new File(plugin.getDataFolder(), "data");
        File userFile = new File(dataFolder, uuid.toString() + ".yml");
        if (!userFile.exists()) {
            lastLocations.put(uuid, new CopyOnWriteArrayList<>());
            return;
        }

        FileConfiguration userConfig = YamlConfiguration.loadConfiguration(userFile);
        List<?> rawList = userConfig.getList("locations");
        List<SavedLocation> list = new CopyOnWriteArrayList<>();
        if (rawList != null) {
            for (Object obj : rawList) {
                if (obj instanceof SavedLocation) {
                    list.add((SavedLocation) obj);
                }
            }
        }
        lastLocations.put(uuid, list);
    }

    /**
     * Saves the locations list for a specific player to their uuid.yml file inside plugins/Cback/data/.
     */
    public void savePlayer(UUID uuid) {
        List<SavedLocation> list = lastLocations.get(uuid);
        if (list == null) {
            return;
        }

        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File userFile = new File(dataFolder, uuid.toString() + ".yml");

        // If the player history list is empty, delete the file to save disk space
        if (list.isEmpty()) {
            if (userFile.exists()) {
                userFile.delete();
            }
            return;
        }

        FileConfiguration userConfig = new YamlConfiguration();
        userConfig.set("locations", list);
        try {
            userConfig.save(userFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save userdata for " + uuid, e);
        }
    }

    /**
     * Unloads the player locations from memory (saving it first).
     */
    public void unloadPlayer(UUID uuid) {
        savePlayer(uuid);
        lastLocations.remove(uuid);
        bypassPlayers.remove(uuid);
    }

    /**
     * Loads locations for all currently online players (used on reload/startup).
     */
    public void loadOnlinePlayers() {
        lastLocations.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            loadPlayer(player.getUniqueId());
        }
    }

    /**
     * Saves locations for all currently loaded online players (used on disable/reload).
     */
    public void saveOnlinePlayers() {
        for (UUID uuid : lastLocations.keySet()) {
            savePlayer(uuid);
        }
    }

    /**
     * Reloads configuration and refreshes loaded data.
     */
    public void reload() {
        // Cancel all current teleport tasks to prevent anomalies
        cancelAllCountdowns();

        // Save current progress before reload
        saveOnlinePlayers();
        
        plugin.reloadConfig();
        
        // Reload all online players data
        loadOnlinePlayers();
    }

    public Cback getPlugin() {
        return plugin;
    }
}
