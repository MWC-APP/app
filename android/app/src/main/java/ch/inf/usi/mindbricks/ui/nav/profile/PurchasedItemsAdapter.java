package ch.inf.usi.mindbricks.ui.nav.profile;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ch.inf.usi.mindbricks.databinding.ItemPurchasedAssetBinding;

public class PurchasedItemsAdapter extends RecyclerView.Adapter<PurchasedItemsAdapter.ViewHolder> {

    private final List<PurchasedItem> items;

    public PurchasedItemsAdapter(List<PurchasedItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemPurchasedAssetBinding binding =
                ItemPurchasedAssetBinding.inflate(inflater, parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PurchasedItem currentItem = items.get(position);
        holder.binding.itemName.setText(currentItem.name());
        holder.binding.itemImage.setImageResource(currentItem.imageResId());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemPurchasedAssetBinding binding;

        ViewHolder(ItemPurchasedAssetBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
