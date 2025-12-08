package ch.inf.usi.mindbricks.ui.nav.shop;

import androidx.recyclerview.widget.RecyclerView;

import ch.inf.usi.mindbricks.databinding.ItemPurchasedAssetBinding;
import ch.inf.usi.mindbricks.ui.nav.profile.PurchasedItem;

public class ItemViewHolder extends RecyclerView.ViewHolder {

    private final ItemPurchasedAssetBinding binding;

    public ItemViewHolder(ItemPurchasedAssetBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    /**
     * A method to use the views in the ViewHolder with data from a PurchasedItem object.
     *
     * @param item The PurchasedItem object containing the data for this view.
     */
    public void bind(PurchasedItem item) {
        binding.purchasedItemName.setText(item.name());
        binding.purchasedItemImage.setImageResource(item.imageResId());
    }
}
