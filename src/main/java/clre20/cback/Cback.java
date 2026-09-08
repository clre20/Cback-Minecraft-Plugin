package clre20.cback;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public final class Cback extends JavaPlugin {

    private BackLocationManager manager;

    @Override
    public void onEnable() {
        // Register configuration serializable class first
        ConfigurationSerialization.registerClass(SavedLocation.class);

        // Merge missing config keys from default config while preserving player's custom values
        updateConfig();

        // Initialize location manager
        this.manager = new BackLocationManager(this);

        // Register event listeners
        getServer().getPluginManager().registerEvents(new TeleportListener(this, manager), this);

        // Register commands
        CbackCommand cbackCommand = new CbackCommand(this, manager);
        if (getCommand("cback") != null) {
            getCommand("cback").setExecutor(cbackCommand);
            getCommand("cback").setTabCompleter(cbackCommand);
        }

        BackCommand backCommand = new BackCommand(this, manager);
        if (getCommand("back") != null) {
            getCommand("back").setExecutor(backCommand);
            getCommand("back").setTabCompleter(backCommand);
        }

        EbackCommand ebackCommand = new EbackCommand(this, manager);
        if (getCommand("eback") != null) {
            getCommand("eback").setExecutor(ebackCommand);
            getCommand("eback").setTabCompleter(ebackCommand);
        }

        manager.log("&a插件已成功載入！支援 Geyser 與 Multiverse-Core 安全傳送與歷史 UI。");
    }

    /**
     * Checks the user's config.yml file on disk and merges any missing default configuration
     * keys from the jar, preserving user customized values.
     */
    private void updateConfig() {
        // Create default config.yml on disk if it doesn't exist
        saveDefaultConfig();

        File configFile = new File(getDataFolder(), "config.yml");
        YamlConfiguration userConfig = YamlConfiguration.loadConfiguration(configFile);

        InputStream defStream = getResource("config.yml");
        if (defStream == null) {
            return;
        }

        YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defStream, StandardCharsets.UTF_8)
        );

        boolean updated = false;
        for (String key : defConfig.getKeys(true)) {
            // Check if user config lacks this specific key
            if (!userConfig.contains(key)) {
                // If it is a section, skip it to let children keys be set individually
                if (defConfig.isConfigurationSection(key)) {
                    continue;
                }
                userConfig.set(key, defConfig.get(key));
                updated = true;
            }
        }

        if (updated) {
            try {
                userConfig.save(configFile);
                getLogger().info("已自動補齊 config.yml 中缺失的設定項目，並保留您現有的客製化內容。");
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "無法儲存更新後的 config.yml", e);
            }
            // Reload updated configuration into memory
            reloadConfig();
        }
    }

    @Override
    public void onDisable() {
        // Save all cached data to disk
        if (manager != null) {
            manager.cancelAllCountdowns();
            manager.saveOnlinePlayers();
            manager.log("&c插件已卸載，所有在線玩家的歷史資料已安全保存。");
        }
    }

    public BackLocationManager getManager() {
        return manager;
    }
}
