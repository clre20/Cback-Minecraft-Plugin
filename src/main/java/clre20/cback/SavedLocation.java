package clre20.cback;

import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@SerializableAs("SavedLocation")
public class SavedLocation implements ConfigurationSerializable {

    private final Location location;
    private final long timestamp;
    private final boolean isDeath;

    public SavedLocation(Location location, long timestamp, boolean isDeath) {
        this.location = location;
        this.timestamp = timestamp;
        this.isDeath = isDeath;
    }

    public Location getLocation() {
        return location;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isDeath() {
        return isDeath;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("location", location);
        map.put("timestamp", timestamp);
        map.put("isDeath", isDeath);
        return map;
    }

    public static SavedLocation deserialize(Map<String, Object> map) {
        Location loc = (Location) map.get("location");
        long time = ((Number) map.get("timestamp")).longValue();
        boolean death = (Boolean) map.get("isDeath");
        return new SavedLocation(loc, time, death);
    }
}
