package clre20.cback;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.SimpleForm;
import org.geysermc.cumulus.response.SimpleFormResponse;
import org.geysermc.cumulus.util.FormImage;
import org.geysermc.floodgate.api.FloodgateApi;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class BedrockFormHandler {

    public static void openEbackForm(Player player, List<SavedLocation> locations, BackLocationManager manager) {
        UUID uuid = player.getUniqueId();
        FileConfiguration config = manager.getPlugin().getConfig();

        String title = config.getString("messages.bedrock-title", "傳送歷史選單 (Eback)");
        String content = config.getString("messages.bedrock-content", "請點擊下方按鈕以傳送到指定的歷史位置：");
        String buttonDeathFormat = config.getString("messages.bedrock-button-death", "[死亡點] 第 %d 筆\n世界: %s | 時間: %s\n座標: X:%s Y:%s Z:%s");
        String buttonTeleportFormat = config.getString("messages.bedrock-button-teleport", "[傳送點] 第 %d 筆\n世界: %s | 時間: %s\n座標: X:%s Y:%s Z:%s");

        SimpleForm.Builder builder = SimpleForm.builder()
                .title(manager.colorizeString(title))
                .content(manager.colorizeString(content));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (int i = 0; i < locations.size(); i++) {
            SavedLocation loc = locations.get(i);
            String worldName = loc.getLocation().getWorld() != null ? loc.getLocation().getWorld().getName() : "未知世界";
            String timeStr = sdf.format(new Date(loc.getTimestamp()));
            String xStr = String.format("%.1f", loc.getLocation().getX());
            String yStr = String.format("%.1f", loc.getLocation().getY());
            String zStr = String.format("%.1f", loc.getLocation().getZ());

            String formatPattern = loc.isDeath() ? buttonDeathFormat : buttonTeleportFormat;
            String buttonText = manager.colorizeString(String.format(
                    formatPattern,
                    i + 1,
                    worldName,
                    timeStr,
                    xStr,
                    yStr,
                    zStr
            ));

            if (loc.isDeath()) {
                builder.button(buttonText, FormImage.Type.PATH, "textures/items/bone.png");
            } else {
                builder.button(buttonText, FormImage.Type.PATH, "textures/items/ender_pearl.png");
            }
        }

        SimpleForm form = builder.build();
        form.setResponseHandler(responseData -> {
            SimpleFormResponse response = form.parseResponse(responseData);
            if (response.isCorrect()) {
                int clickedButtonId = response.getClickedButtonId();
                // Execute on main thread since teleport calls in Spigot must run on the main thread
                Bukkit.getScheduler().runTask(manager.getPlugin(), () -> {
                    manager.teleportTo(player, clickedButtonId);
                });
            }
        });

        FloodgateApi.getInstance().sendForm(uuid, form);
    }
}
