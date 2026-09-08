package clre20.cback;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.List;
import java.util.UUID;

public class TeleportListener implements Listener {

    private final Cback plugin;
    private final BackLocationManager manager;

    public TeleportListener(Cback plugin, BackLocationManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Location from = event.getFrom();

        // If this teleport was initiated by the /back command or UI, we force-record
        // the previous location and bypass distance/cooldown checks.
        if (manager.isBypassing(uuid)) {
            manager.recordLocation(uuid, from, false, true);
            return;
        }

        // Validate cause
        String causeName = event.getCause().name();
        List<String> allowedCauses = plugin.getConfig().getStringList("allowed-causes");

        if (allowedCauses.contains(causeName)) {
            manager.recordLocation(uuid, from, false, false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.getConfig().getBoolean("record-death", true)) {
            return;
        }

        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();
        Location deathLoc = player.getLocation();

        // Force record death location as death point (bypassing normal movement/cooldown checks)
        manager.recordLocation(uuid, deathLoc, true, true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        // Load location list from disk into cache
        manager.loadPlayer(uuid);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        // Cancel active countdowns to prevent leaks
        manager.cancelCountdown(uuid, false);
        // Unload from memory to prevent leaks
        manager.unloadPlayer(uuid);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof EbackInventoryHolder) {
            event.setCancelled(true); // Cancel all interactions to prevent stealing items

            if (!(event.getWhoClicked() instanceof Player)) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            int slot = event.getRawSlot();

            // We display up to 10 locations in slots 0 to 9
            if (slot >= 0 && slot < 10) {
                List<SavedLocation> locations = manager.getLocations(player.getUniqueId());
                if (slot < locations.size()) {
                    player.closeInventory();
                    manager.teleportTo(player, slot);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (manager.hasActiveCountdown(uuid)) {
            // Cancel teleport if player changes block coordinate (allows looking around)
            Location from = event.getFrom();
            Location to = event.getTo();
            if (from.getBlockX() != to.getBlockX() ||
                    from.getBlockY() != to.getBlockY() ||
                    from.getBlockZ() != to.getBlockZ()) {

                if (plugin.getConfig().getBoolean("cancel-teleport-on-move", true)) {
                    manager.cancelCountdown(uuid, true);
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            UUID uuid = player.getUniqueId();

            if (manager.hasActiveCountdown(uuid)) {
                if (plugin.getConfig().getBoolean("cancel-teleport-on-damage", true)) {
                    manager.cancelCountdown(uuid, true);
                }
            }
        }
    }
}
