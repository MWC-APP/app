package ch.inf.usi.mindbricks.game;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.LruCache;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class to load tile bitmaps from png assets and tileset sheets (for ground/water tiles).
 */
public class TileBitmapLoader {

    /**
     * Standard tile size in pixels.
     */
    public static final int TILE_SIZE = 16;

    /**
     * Standard preview size for tile bitmaps.
     */
    private static final int PREVIEW_SIZE = 128;

    /**
     * Asset manager to load files from assets.
     */
    private final AssetManager assetManager;

    /**
     * In-memory cache to store loaded tileset sheet bitmaps.
     */
    private final Map<String, Bitmap> sheetCache = new HashMap<>();

    /**
     * In-memory cache for scaled bitmaps to avoid redundant loading and scaling.
     */
    private final LruCache<String, Bitmap> sizedCache;

    public TileBitmapLoader(Context context) {
        this.assetManager = context.getAssets();
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024); // in KB
        int cacheSize = maxMemory / 32; // reserve 1/32 of available memory to store assets
        this.sizedCache = new LruCache<>(cacheSize);
    }

    /**
     * Loads and scales the bitmap for the given asset to a standard preview size.
     * <p>
     * NOTE: this is needed for displaying larger previews in the UI.
     */
    @Nullable
    public Bitmap getPreview(TileAsset asset) {
        return getBitmap(asset, PREVIEW_SIZE, PREVIEW_SIZE);
    }

    /**
     * Loads and scales the bitmap for the given asset to the specified width and height.
     * Uses an in-memory cache to avoid reloading and rescaling frequently used assets.
     */
    @Nullable
    public Bitmap getBitmap(TileAsset asset, int width, int height) {
        if (asset == null) return null;

        // get from cache if available
        String cacheKey = asset.id() + "_" + width + "x" + height;
        Bitmap cached = sizedCache.get(cacheKey);
        if (cached != null) return cached;

        // load raw bitmap
        Bitmap raw = asset.isTilesetTile()
                ? loadFromTileset(asset) // from tileset
                : loadFromAsset(asset.assetPath()); // from individual image file
        if (raw == null) return null;

        // scale to desired size
        Bitmap scaled = Bitmap.createScaledBitmap(raw, width, height, true);
        sizedCache.put(cacheKey, scaled);

        // cleanup raw bitmap if different from scaled
        // NOTE: needed as bitmaps might be huge = better to free memory immediately
        if (raw != scaled) raw.recycle();

        // return scaled bitmap
        return scaled;
    }

    /**
     * Loads a tile bitmap from a tileset based on the tile index.
     */
    @Nullable
    private Bitmap loadFromTileset(TileAsset asset) {
        // calculate tile position in the tileset sheet
        Integer tileIndex = asset.tileIndex();
        if (tileIndex == null) return null;

        // load entire sheet as a big bitmap
        Bitmap sheet = loadSheet(asset.assetPath());
        if (sheet == null) return null;

        // calculate tile coordinates
        int cols = sheet.getWidth() / TILE_SIZE;
        int x = (tileIndex % cols) * TILE_SIZE;
        int y = (tileIndex / cols) * TILE_SIZE;

        // if out of bounds, return null
        if (x + TILE_SIZE > sheet.getWidth() || y + TILE_SIZE > sheet.getHeight()) return null;

        // otherwise, split sheet and return the tile bitmap
        return Bitmap.createBitmap(sheet, x, y, TILE_SIZE, TILE_SIZE);
    }

    /**
     * Loads a bitmap directly from an asset file.
     */
    @Nullable
    private Bitmap loadFromAsset(String assetPath) {
        try (InputStream is = assetManager.open(assetPath)) {
            return BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Loads and caches a tileset sheet bitmap from assets.
     */
    @Nullable
    private Bitmap loadSheet(String assetPath) {
        // if already cached, return it
        if (sheetCache.containsKey(assetPath)) return sheetCache.get(assetPath);

        // otherwise, load from assets
        try (InputStream is = assetManager.open(assetPath)) {
            // read + create bitmap from file handle
            Bitmap sheet = BitmapFactory.decodeStream(is);

            // cache and return
            sheetCache.put(assetPath, sheet);
            return sheet;
        } catch (IOException e) {
            Log.e("TileBitmapLoader", "Failed to load tileset: " + assetPath, e);
            return null;
        }
    }
}
