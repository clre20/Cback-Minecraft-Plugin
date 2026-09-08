package clre20.cback;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CbackCommand implements CommandExecutor, TabCompleter {

    private final Cback plugin;
    private final BackLocationManager manager;

    public CbackCommand(Cback plugin, BackLocationManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("back")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(manager.translateColorWithPrefix(plugin.getConfig().getString("messages.only-players", "&c此指令只能由玩家執行！")));
                return true;
            }
            Player player = (Player) sender;
            if (!player.hasPermission("cback.use")) {
                player.sendMessage(manager.translateColorWithPrefix(plugin.getConfig().getString("messages.no-permission", "&c你沒有權限使用此指令！")));
                return true;
            }
            manager.teleportBack(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("cback.admin")) {
                sender.sendMessage(manager.translateColorWithPrefix(plugin.getConfig().getString("messages.no-permission", "&c你沒有權限使用此指令！")));
                return true;
            }
            manager.reload();
            sender.sendMessage(manager.translateColorWithPrefix(plugin.getConfig().getString("messages.reload-success", "&a設定檔與資料已重新載入！")));
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(manager.translateColor(plugin.getConfig().getString("messages.help-header", "&8&m===================&r &6&lCback &8&m===================")));
        if (sender.hasPermission("cback.use")) {
            sender.sendMessage(manager.translateColor(plugin.getConfig().getString("messages.help-back", "&e/back &7- 快速傳送回上一次的位置 (傳送或死亡點)")));
            sender.sendMessage(manager.translateColor(plugin.getConfig().getString("messages.help-cback", "&e/cback &7- 傳送回上一次的位置")));
            sender.sendMessage(manager.translateColor(plugin.getConfig().getString("messages.help-eback", "&e/eback &7- 開啟傳送歷史選單 (支援雙端 UI)")));
        }
        if (sender.hasPermission("cback.admin")) {
            sender.sendMessage(manager.translateColor(plugin.getConfig().getString("messages.help-reload", "&e/cback reload &7- 重新載入設定檔與歷史資料")));
        }
        sender.sendMessage(manager.translateColor(plugin.getConfig().getString("messages.help-footer", "&8&m=============================================")));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if (sender.hasPermission("cback.use")) {
                completions.add("back");
                completions.add("help");
            }
            if (sender.hasPermission("cback.admin")) {
                completions.add("reload");
            }

            // Filter by prefix
            List<String> result = new ArrayList<>();
            for (String s : completions) {
                if (s.toLowerCase().startsWith(args[0].toLowerCase())) {
                    result.add(s);
                }
            }
            return result;
        }
        return Collections.emptyList();
    }
}
