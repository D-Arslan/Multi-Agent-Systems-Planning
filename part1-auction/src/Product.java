package part1;

import java.io.Serializable;

public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final double startingPrice;
    private final double reservePrice;

    public Product(String name, double startingPrice, double reservePrice) {
        this.name         = name;
        this.startingPrice = startingPrice;
        this.reservePrice  = reservePrice;
    }

    public String getName()         { return name; }
    public double getStartingPrice() { return startingPrice; }
    public double getReservePrice()  { return reservePrice; }

    @Override
    public String toString() {
        return name + " (starting: " + startingPrice + " €)";
    }
}
