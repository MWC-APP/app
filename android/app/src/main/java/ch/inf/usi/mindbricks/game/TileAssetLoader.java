package ch.inf.usi.mindbricks.game;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Tile asset loader to load available tile assets from the assets folder.
 */
public class TileAssetLoader {

    /**
     * Standard tile size in pixels.
     */
    private static final int TILE_SIZE = 16;

    /**
     * Prices for terrain tiles.
     */
    private static final int PRICE_TERRAIN = 2;

    /**
     * Prices for water tiles.
     */
    private static final int PRICE_WATER = 2;

    /**
     * Prices for road tiles.
     */
    private static final int PRICE_ROAD = 3;

    /**
     * Prices for building tiles.
     */
    private static final int PRICE_BUILDING = 20;

    /**
     * Prices for decoration / resource tiles.
     */
    private static final int PRICE_DECORATION = 8;

    /**
     * Asset manager to load files from assets.
     */
    private final AssetManager assetManager;

    /**
     * Constructor method.
     * @param context Application context.
     */
    public TileAssetLoader(Context context) {
        this.assetManager = context.getAssets();
    }

    /**
     * Loads all available tile assets from the assets folder.
     * @return List of available tile assets.
     */
    public List<TileAsset> loadAvailableAssets() {
        List<TileAsset> assets = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();

        // Terrain tile sets (tiled textures)
        assets.addAll(loadTilesetsFromDir("tiles/terrain/tiles", seenIds));

        // Buildings
        assets.addAll(loadImagesRecursively("tiles/building", TileType.BUILDING, PRICE_BUILDING, seenIds));

        // Decorations / resources (non-tileset images)
        assets.addAll(loadImagesRecursively("tiles/terrain/extra/decorations", TileType.DECORATION, PRICE_DECORATION, seenIds));
        assets.addAll(loadImagesRecursively("tiles/terrain/extra/resources", TileType.DECORATION, PRICE_DECORATION, seenIds));

        // return the collected assets
        return assets;
    }

    /**
     * Loads tilesets from the specified directory.
     * @param dir Directory path in assets.
     * @param seenIds Set of already seen asset IDs to avoid duplicates.
     * @return List of loaded tile assets.
     */
    private List<TileAsset> loadTilesetsFromDir(String dir, Set<String> seenIds) {
        List<TileAsset> result = new ArrayList<>();
        try {
            String[] files = assetManager.list(dir);
            if (files == null) return result;
            for (String file : files) {
                // only process PNG files
                if (!file.toLowerCase(Locale.US).endsWith(".png")) continue;

                String assetPath = dir + "/" + file;
                String lower = file.toLowerCase(Locale.US);

                // detect tile type based on filename
                TileType type;
                if (lower.contains("road")) {
                    type = TileType.ROAD;
                } else if (lower.contains("water")) {
                    type = TileType.WATER;
                } else {
                    type = TileType.TERRAIN;
                }

                // expand the tileset into individual tiles
                result.addAll(expandTileset(assetPath, type, seenIds));
            }
        } catch (IOException ignored) {
        }
        return result;
    }

    /**
     * Expands a tileset image into individual tile assets.
     * @param assetPath Path to the tileset image in assets.
     * @param type Type of tiles in the tileset.
     * @param seenIds Set of already seen asset IDs to avoid duplicates.
     * @return List of tile assets extracted from the tileset.
     */
    private List<TileAsset> expandTileset(String assetPath, TileType type, Set<String> seenIds) {
        List<TileAsset> result = new ArrayList<>();
        // open image stream
        try (InputStream is = assetManager.open(assetPath)) {
            // decode the full tileset image
            Bitmap sheet = BitmapFactory.decodeStream(is);
            if (sheet == null) return result;

            // compute number of tiles in each dimension
            int cols = sheet.getWidth() / TILE_SIZE;
            int rows = sheet.getHeight() / TILE_SIZE;

            // determine price based on tile type
            int price;
            if (type == TileType.ROAD) price = PRICE_ROAD;
            else if (type == TileType.WATER) price = PRICE_WATER;
            else price = PRICE_TERRAIN;

            // extract individual tiles
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    // extract tile bitmap
                    Bitmap tile = Bitmap.createBitmap(sheet, c * TILE_SIZE, r * TILE_SIZE, TILE_SIZE, TILE_SIZE);

                    // NOTE: skip fully transparent tiles (tilesets have padding)
                    if (isTransparent(tile)) {
                        tile.recycle(); // deallocate bitmap
                        continue;
                    }

                    // generate unique tile ID
                    int index = r * cols + c;
                    String id = assetPath + "#" + index;

                    // skip duplicates
                    if (seenIds.contains(id)) {
                        tile.recycle();
                        continue;
                    }

                    // add tile asset to the result list
                    seenIds.add(id);
                    String name = friendlyName(assetPath) + " " + (index + 1);
                    result.add(new TileAsset(id, name, assetPath, index, type, price));

                    // cleanup
                    tile.recycle();
                }
            }

            // deallocate tileset bitmap
            sheet.recycle();
        } catch (IOException ignored) {
        }

        // prefer bottom-left ground tiles for terrain type
        return result;
    }

    /**
     * Recursively loads images from the specified directory (simple tree traversal algoritm).
     *
     * @param dir The directory to load images from.
     * @param type The type of tiles being loaded.
     * @param price The price of the tiles being loaded.
     * @param seenIds Set of already seen asset IDs to avoid duplicates.
     * @return List of loaded tile assets.
     */
    private List<TileAsset> loadImagesRecursively(String dir, TileType type, int price, Set<String> seenIds) {
        List<TileAsset> result = new ArrayList<>();
        try {
            // list entries in the directory
            String[] entries = assetManager.list(dir);
            if (entries == null) return result;

            // process each entry
            for (String entry : entries) {
                String fullPath = dir + "/" + entry;

                // if png file -> process as tile image
                if (entry.toLowerCase(Locale.US).endsWith(".png")) {
                    String id = String.valueOf(fullPath.hashCode());
                    if (seenIds.contains(id)) continue;

                    // add tile asset to the result list
                    seenIds.add(id);
                    result.add(new TileAsset(id, friendlyName(fullPath), fullPath, null, type, price));
                }
                // if directory -> scan it recursively
                else {
                    result.addAll(loadImagesRecursively(fullPath, type, price, seenIds));
                }
            }
        } catch (IOException e) {
            Log.e("TileAssetLoader", "Error loading images from " + dir, e);
        }
        return result;
    }

    /**
     * Checks if a bitmap is fully transparent (detect if padding tile).
     * @param bitmap The bitmap to check.
     * @return True if the bitmap is fully transparent, false otherwise.
     */
    private boolean isTransparent(Bitmap bitmap) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int[] pixels = new int[w * h];

        // get all pixels
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h);

        // check alpha channel
        for (int color : pixels) {
            if (Color.alpha(color) != 0) return false; // found non-transparent pixel
        }

        // all pixels are transparent
        return true;
    }

    /**
     * Generates a friendly name for a tile asset based on its path.
     *
     * @param path The asset path.
     * @return Friendly name.
     */
    private String friendlyName(String path) {
        int lastSlash = path.lastIndexOf('/');
        String name = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
        name = name.replace(".png", "").replace('_', ' ').replace('-', ' ');
        if (name.startsWith("Tileset ")) {
            name = name.replace("Tileset ", "");
        }
        return name.trim();
    }
}
