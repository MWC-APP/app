package ch.inf.usi.mindbricks.ui.nav.shop;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.game.TileAsset;
import ch.inf.usi.mindbricks.game.TileBitmapLoader;

public class ShopItemAdapter extends RecyclerView.Adapter<ShopItemAdapter.ShopItemViewHolder> {

    private final List<TileAsset> items;
    private final OnItemBuyClickListener buyClickListener;
    private final TileBitmapLoader bitmapLoader;

    public ShopItemAdapter(List<TileAsset> items, TileBitmapLoader loader, OnItemBuyClickListener listener) {
        this.items = items;
        this.buyClickListener = listener;
        this.bitmapLoader = loader;
    }

    @NonNull
    @Override
    public ShopItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shop_item, parent, false);
        return new ShopItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShopItemViewHolder holder, int position) {
        TileAsset currentItem = items.get(position);
        holder.itemName.setText(currentItem.displayName());
        holder.itemPrice.setText(String.valueOf(currentItem.price()));

        Bitmap preview = bitmapLoader.getPreview(currentItem);
        if (preview != null) holder.itemImage.setImageBitmap(preview);
        else holder.itemImage.setImageResource(android.R.drawable.ic_menu_report_image);

        holder.itemView.setOnClickListener(v -> {
            if (buyClickListener != null) {
                buyClickListener.onItemBuyClick(currentItem);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public interface OnItemBuyClickListener {
        void onItemBuyClick(TileAsset item);
    }

    public static class ShopItemViewHolder extends RecyclerView.ViewHolder {
        public ImageView itemImage;
        public TextView itemName;
        public TextView itemPrice;

        public ShopItemViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.item_image);
            itemName = itemView.findViewById(R.id.item_name);
            itemPrice = itemView.findViewById(R.id.item_price);
        }
    }
}
