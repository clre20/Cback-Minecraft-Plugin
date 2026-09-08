package clre20.cback;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class JavaGuiHandler {

    public static void openEbackGui(Player player, List<SavedLocation> locations, BackLocationManager manager) {
        FileConfiguration config = manager.getPlugin().getConfig();
        String title = config.getString("messages.gui-title", "&6&l傳送歷史選單 &7(Eback)");
        
        Inventory inv = Bukkit.createInventory(new EbackInventoryHolder(), 18, manager.translateColor(title));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        String deathNameFormat = config.getString("messages.gui-death-name", "&c&l第 %d 筆 - 死亡點");
        String teleportNameFormat = config.getString("messages.gui-teleport-name", "&a&l第 %d 筆 - 傳送點");
        String typeDeathText = config.getString("messages.gui-type-death", "&c&l死亡點");
        String typeTeleportText = config.getString("messages.gui-type-teleport", "&a&l傳送點");

        List<String> loreTemplate = config.getStringList("messages.gui-lore");
        if (loreTemplate.isEmpty()) {
            loreTemplate = new ArrayList<>();
            loreTemplate.add("&8&m---------------------------------");
            loreTemplate.add("&7類型: {type}");
            loreTemplate.add("&7世界: &f{world}");
            loreTemplate.add("&7座標: &eX:{x} &7| &eY:{y} &7| &eZ:{z}");
            loreTemplate.add("&7時間: &b{time}");
            loreTemplate.add("&8&m---------------------------------");
            loreTemplate.add("&e點擊以傳送至此位置");
        }

        // Fill slots 0 to 9 with the history locations
        for (int i = 0; i < locations.size(); i++) {
            SavedLocation loc = locations.get(i);
            ItemStack item = new ItemStack(loc.isDeath() ? Material.SKELETON_SKULL : Material.COMPASS);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String typeName = loc.isDeath() ? typeDeathText : typeTeleportText;
                String formatName = loc.isDeath() ? deathNameFormat : teleportNameFormat;
                
                meta.displayName(manager.translateColor(String.format(formatName, i + 1)));

                List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                String worldName = loc.getLocation().getWorld() != null ? loc.getLocation().getWorld().getName() : "未知世界";
                String xStr = String.format("%.1f", loc.getLocation().getX());
                String yStr = String.format("%.1f", loc.getLocation().getY());
                String zStr = String.format("%.1f", loc.getLocation().getZ());
                String timeStr = sdf.format(new Date(loc.getTimestamp()));

                for (String line : loreTemplate) {
                    String formattedLine = line
                            .replace("{type}", typeName)
                            .replace("{world}", worldName)
                            .replace("{x}", xStr)
                            .replace("{y}", yStr)
                            .replace("{z}", zStr)
                            .replace("{time}", timeStr);
                    lore.add(manager.translateColor(formattedLine));
                }

                meta.lore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(i, item);
        }

        // Fill the rest with black glass panes as background
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.displayName(manager.translateColor(" "));
            filler.setItemMeta(fillerMeta);
        }
        for (int i = locations.size(); i < 18; i++) {
            inv.setItem(i, filler);
        }

        player.openInventory(inv);
    }
}
