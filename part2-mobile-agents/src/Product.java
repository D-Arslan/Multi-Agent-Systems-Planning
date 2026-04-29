package part2;

import java.io.Serializable;

public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final double price;
    private final double quality;   // score /10
    private final int    deliveryDays;

    public Product(String name, double price, double quality, int deliveryDays) {
        this.name         = name;
        this.price        = price;
        this.quality      = quality;
        this.deliveryDays = deliveryDays;
    }

    public String getName()        { return name; }
    public double getPrice()       { return price; }
    public double getQuality()     { return quality; }
    public int    getDeliveryDays(){ return deliveryDays; }

    @Override
    public String toString() {
        return name + " [price=" + price + ", quality=" + quality + ", delivery=" + deliveryDays + "d]";
    }
}
