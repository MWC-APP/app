package ch.inf.usi.mindbricks.ui.nav.shop;

public class ShopItem {
    private final String id;

    private final String name;
    private final int price;
    private final int imageResourceId;

    public ShopItem(String id, String name, int price, int imageResourceId) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.imageResourceId = imageResourceId;
    }

    public String getId() {
        return id;
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
