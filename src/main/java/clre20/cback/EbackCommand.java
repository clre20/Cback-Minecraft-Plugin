package clre20.cback;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class EbackCommand implements CommandExecutor, TabCompleter {

    private final Cback plugin;
    private final BackLocationManager manager;

    public EbackCommand(Cback plugin, BackLocationManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(manager.translateColorWithPrefix(plugin.getConfig().getString("messages.only-players", "此指令只能由玩家執行！")));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("cback.use")) {
            player.sendMessage(manager.translateColorWithPrefix(plugin.getConfig().getString("messages.no-permission", "&c你沒有權限使用此指令！")));
            return true;
        }

        List<SavedLocation> locations = manager.getLocations(player.getUniqueId());
        if (locations.isEmpty()) {
            player.sendMessage(manager.translateColorWithPrefix(plugin.getConfig().getString("messages.no-location", "&c找不到你上一次的傳送位置！")));
            return true;
        }

        // Open UI
        if (Bukkit.getPluginManager().isPluginEnabled("floodgate") &&
                org.geysermc.floodgate.api.FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
            // Bedrock Dedicated UI
            BedrockFormHandler.openEbackForm(player, locations, manager);
        } else {
            // Java Chest UI
            JavaGuiHandler.openEbackGui(player, locations, manager);
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
