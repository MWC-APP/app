package ch.inf.usi.mindbricks.game;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.Map;

import ch.inf.usi.mindbricks.util.PreferencesManager;

/**
 * Tile inventory manager to load, save, add, and consume tiles from the player inventory
 */
public class TileInventoryManager {

    /**
     * Preferences manager to persist inventory state.
     */
    private final PreferencesManager preferencesManager;

    /**
     * Constructor method.
     *
     * @param context Application context
     */
    public TileInventoryManager(Context context) {
        preferencesManager = new PreferencesManager(context);
    }

    /**
     * Load the current inventory map from preferences.
     *
     * @return Map of tile IDs to their respective counts
     */
    @NonNull
    public Map<String, Integer> loadInventory() {
        return preferencesManager.getInventory();
    }

    /**
     * Add the given amount of tiles to the inventory for the specified tile ID.
     *
     * @param tileId Tile identifier
     * @param amount Number of tiles to add
     */
    public void addToInventory(String tileId, int amount) {
        // protect against null pointer exception in getOrSet
        if (tileId == null || tileId.isEmpty()) throw new IllegalArgumentException("tileId cannot be null or empty");

        // get inventory and update count
        Map<String, Integer> current = loadInventory();
        int existing = current.getOrDefault(tileId, 0);

        // update inventory + save inside preferences
        current.put(tileId, existing + amount);

        // persist inventory state in app preferences
        preferencesManager.saveInventory(current);
    }

    /**
     * Save the given inventory map into preferences.
     *
     * @param tileId Tile identifier
     * @return True if the tile was successfully used, false if not enough tiles were available
     */
    public boolean consumeFromInventory(String tileId) {
        if (tileId == null || tileId.isEmpty()) throw new IllegalArgumentException("tileId cannot be null or empty");

        // get inventory and update count
        Map<String, Integer> current = loadInventory();
        int existing = current.getOrDefault(tileId, 0);
        if (existing <= 0) return false; // invalid amount to consume
        if (existing == 1) {
            // if fully consumed = remove from inventory
            current.remove(tileId);
        } else {
            // otherwise just decrement count
            current.put(tileId, existing - 1);
        }

        // persist inventory state in app preferences
        preferencesManager.saveInventory(current);
        return true;
    }
}
