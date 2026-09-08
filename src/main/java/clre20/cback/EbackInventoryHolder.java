package clre20.cback;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class EbackInventoryHolder implements InventoryHolder {

    @Override
    public @NotNull Inventory getInventory() {
        return null; // Return null as we don't store a backing inventory here
    }
}
