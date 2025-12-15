package ch.inf.usi.mindbricks.game;

import android.content.Context;
import android.util.Log;


import ch.inf.usi.mindbricks.util.PreferencesManager;

/**
 * Repository class for managing the tile world state persistence.
 */
public class TileWorldRepository {

    /**
     * Preferences manager for persisting and retrieving the tile world state.
     */
    private final PreferencesManager preferencesManager;

    /**
     * Constructor method.
     *
     * @param context Application context
     */
    public TileWorldRepository(Context context) {
        preferencesManager = new PreferencesManager(context);
    }

    /**
     * Loads the tile world state from preferences, or creates a new one if none exists.
     *
     * @param defaultRows The default number of rows for the tile world
     * @param defaultCols The default number of columns for the tile world
     * @param defaultBaseTileId The default base tile ID for the tile world
     * @return The loaded or newly created tile world state
     */
    public TileWorldState loadWorldState(int defaultRows, int defaultCols, String defaultBaseTileId) {
        // load saved world state from preferences
        TileWorldState state = preferencesManager.getWorldState();

        // if no save date -> create brand-new world
        if (state == null) {
            // create + configure world state
            return new TileWorldState(defaultRows, defaultCols, defaultBaseTileId);
        }

        // NOTE: validate loaded world state
        // 1) ensure correct base tile
        if (state.getBaseTileId() == null || state.getBaseTileId().isEmpty()) {
            Log.w("TileWorldRepository", "Loaded world state has no base tile ID, setting default.");
            state.setBaseTileId(defaultBaseTileId);
        }
        // 2) ensure we got the expected dimensions
        if (state.getRows() != defaultRows || state.getCols() != defaultCols) {
            Log.w("TileWorldRepository", "Loaded world state has incorrect dimensions, resetting to default.");
            state = new TileWorldState(defaultRows, defaultCols, state.getBaseTileId(), state.getPlacedTiles(), state.getPlacements());
        }

        // return loaded world state
        return state;
    }

    /**
     * Saves the given tile world state to preferences.
     * @param state Tile world state to persist
     */
    public void saveWorld(TileWorldState state) {
        preferencesManager.saveWorldState(state);
    }
}
