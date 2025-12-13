package ch.inf.usi.mindbricks.ui.nav;

import androidx.recyclerview.widget.RecyclerView;

import ch.inf.usi.mindbricks.databinding.ItemPurchasedAssetBinding;
import ch.inf.usi.mindbricks.ui.nav.profile.PurchasedItem;
import ch.inf.usi.mindbricks.ui.nav.shop.ShopItem;

public class ItemViewHolder extends RecyclerView.ViewHolder {

    private final ItemPurchasedAssetBinding binding;

    public ItemViewHolder(ItemPurchasedAssetBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    public void bind(ShopItem item) {
        binding.itemName.setText(item.getName());
        binding.itemImage.setImageResource(item.getDrawableResId());
    }

    public void bind(PurchasedItem item) {
        binding.itemName.setText(item.name());
        binding.itemImage.setImageResource(item.imageResId());
    }
}
