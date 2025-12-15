package ch.inf.usi.mindbricks.game;

/**
 * Represents the placement of a tile in the tile world.
 */
public class TilePlacement {
    /**
     * Tile ID of the placed tile
     */
    private String tileId;

    /**
     * Width of the tile placement (in number of grid cells)
     */
    private int width;

    /**
     * Height of the tile placement (in number of grid cells)
     */
    private int height;

    /**
     * Anchor row of the tile placement (top-left corner)
     */
    private int anchorRow;

    /**
     * Anchor column of the tile placement (top-left corner)
     */
    private int anchorCol;

    // NOTE: Needed by GSON
    @SuppressWarnings("unused")
    public TilePlacement() {}

    /**
     * Constructor method.
     *
     * @param tileId ID of the placed tile
     * @param width Width of the tile placement (in number of grid cells)
     * @param height Height of the tile placement (in number of grid cells)
     * @param anchorRow Anchor row of the tile placement (top-left corner)
     * @param anchorCol Anchor column of the tile placement (top-left corner)
     */
    public TilePlacement(String tileId, int width, int height, int anchorRow, int anchorCol) {
        this.tileId = tileId;
        this.width = width;
        this.height = height;
        this.anchorRow = anchorRow;
        this.anchorCol = anchorCol;
    }

    public String getTileId() { return tileId; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getAnchorRow() { return anchorRow; }
    public int getAnchorCol() { return anchorCol; }
}
