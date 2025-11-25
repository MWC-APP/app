package ch.inf.usi.mindbricks.ui.nav.shop;

public class ShopItem {

    private final String name;
    private final int price;
    private final int imageResourceId; // Using a drawable resource for the image

    public ShopItem(String name, int price, int imageResourceId) {
        this.name = name;
        this.price = price;
        this.imageResourceId = imageResourceId;
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    public int getImageResourceId() {
        return imageResourceId;
    }
}
