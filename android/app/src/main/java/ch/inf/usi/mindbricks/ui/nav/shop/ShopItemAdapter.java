package ch.inf.usi.mindbricks.ui.nav.shop;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

import ch.inf.usi.mindbricks.R;

public class ShopItemAdapter extends RecyclerView.Adapter<ShopItemAdapter.ShopItemViewHolder> {

    private final List<ShopItem> items;

    // Constructor to pass in the data
    public ShopItemAdapter(List<ShopItem> items) {
        this.items = items;
    }

    // This method creates a new ViewHolder by inflating the shop_item.xml layout
    @NonNull
    @Override
    public ShopItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.shop_item, parent, false);
        return new ShopItemViewHolder(view);
    }

    // This method binds the data from a ShopItem object to the views in the ViewHolder
    @Override
    public void onBindViewHolder(@NonNull ShopItemViewHolder holder, int position) {
        ShopItem currentItem = items.get(position);
        holder.itemName.setText(currentItem.getName());
        holder.itemPrice.setText(String.valueOf(currentItem.getPrice()));
        holder.itemImage.setImageResource(currentItem.getImageResourceId());
    }

    // Returns the total number of items in the list
    @Override
    public int getItemCount() {
        return items.size();
    }

    // The ViewHolder class holds the references to the views in shop_item.xml
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
