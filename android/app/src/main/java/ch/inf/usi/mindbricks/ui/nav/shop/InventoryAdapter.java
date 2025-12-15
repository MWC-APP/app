package ch.inf.usi.mindbricks.ui.nav.shop;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.game.TileAsset;
import ch.inf.usi.mindbricks.game.TileBitmapLoader;

/**
 * Adapter for displaying inventory items in a RecyclerView.
 */
public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder> {

    /**
     * Record representing an inventory entry.
     *
     * @param asset the tile asset
     * @param quantity the quantity of a possessed tile
     */
    public record InventoryEntry(TileAsset asset, int quantity) {
    }

    public interface OnInventoryInteractionListener {
        void onTileSelected(TileAsset asset);
        void onTileDragRequested(TileAsset asset, View view);
    }

    /**
     * Bitmap loader for tile previews.
     */
    private final TileBitmapLoader bitmapLoader;

    /**
     * Listener for inventory interactions.
     */
    private final OnInventoryInteractionListener listener;

    /**
     * List of possessed inventory entries.
     */
    private final List<InventoryEntry> entries = new ArrayList<>();

    /**
     * ID of the currently selected tile for highlighting.
     */
    private String selectedTileId;

    /**
     * Constructor method.
     *
     * @param bitmapLoader The bitmap loader for tile previews
     * @param listener The listener for inventory interactions
     */
    public InventoryAdapter(TileBitmapLoader bitmapLoader, OnInventoryInteractionListener listener) {
        this.bitmapLoader = bitmapLoader;
        this.listener = listener;
    }

    /**
     * Updates the inventory entries displayed by the adapter.
     *
     * @param newEntries The new list of inventory entries
     */
    public void submitList(List<InventoryEntry> newEntries) {
        entries.clear();
        if (newEntries != null) entries.addAll(newEntries);
        notifyDataSetChanged();
    }

    /**
     * Sets the selected tile ID for highlighting.
     *
     * @param tileId The ID of the selected tile
     */
    public void setSelectedTileId(String tileId) {
        this.selectedTileId = tileId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public InventoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.inventory_item, parent, false);
        return new InventoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InventoryViewHolder holder, int position) {
        // get the inventory entry
        InventoryEntry entry = entries.get(position);

        // update the view holder with entry data
        holder.itemName.setText(entry.asset.displayName());
        holder.itemQuantity.setText(String.valueOf(entry.quantity));

        // get tile preview bitmap (scaled version of the real asset)
        Bitmap preview = bitmapLoader.getPreview(entry.asset);

        if (preview != null) {
            // set the preview image if available
            holder.itemImage.setImageBitmap(preview);
        } else {
            // show placeholder image
            // NOTE: 404 error image as placeholder
            holder.itemImage.setImageResource(android.R.drawable.ic_menu_report_image);
        }

        // set selection state
        holder.itemView.setSelected(entry.asset.id().equals(selectedTileId));

        // prepare listeners
        // a) select tile on click
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTileSelected(entry.asset);
        });
        // b) initiate drag on long click
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onTileDragRequested(entry.asset, holder.itemView);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    /**
     * View holder for inventory items.
     */
    public static class InventoryViewHolder extends RecyclerView.ViewHolder {
        /**
         * Image view for the tile preview.
         */
        ImageView itemImage;

        /**
         * Item name text view.
         */
        TextView itemName;

        /**
         * Item quantity text view.
         */
        TextView itemQuantity;

        public InventoryViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.inventory_item_image);
            itemName = itemView.findViewById(R.id.inventory_item_name);
            itemQuantity = itemView.findViewById(R.id.inventory_item_quantity);
        }
    }
}
