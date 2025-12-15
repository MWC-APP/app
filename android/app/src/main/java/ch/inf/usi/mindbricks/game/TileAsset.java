package ch.inf.usi.mindbricks.game;

import androidx.annotation.Nullable;

public record TileAsset(String id, String displayName, String assetPath,
                        @Nullable Integer tileIndex, TileType type, int price) {

    public boolean isTilesetTile() {
        return tileIndex != null;
    }

    /**
     * Get the size (in grid cells) occupied by the tile.
     *
     * @return Array with two elements: [height, width]
     */
    public int[] getSize() {
        // Only buildings can be multi-cell
        if (type() != TileType.BUILDING) {
            return new int[]{1, 1};
        }

        // Determine building size based on display name or asset path
        String identifier = (displayName() + " " + id()).toLowerCase();

        // Castle: 3x4
        if (identifier.contains("castle")) {
            return new int[]{3, 4};
        }

        // Archery and Monastery: 3x2
        if (identifier.contains("archery") || identifier.contains("monastery")) {
            return new int[]{3, 2};
        }

        // Houses and Tower: 2x2
        if (identifier.contains("house") || identifier.contains("tower")) {
            return new int[]{2, 2};
        }

        // Barracks: 2x2
        if (identifier.contains("barracks")) {
            return new int[]{2, 2};
        }

        // Default for unknown buildings: 1x1
        return new int[]{1, 1};
    }
}
