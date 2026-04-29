package part2;

import java.util.List;

/**
 * Weighted-sum multi-criteria decision engine.
 *
 * Criteria and weights:
 *   price        0.5  (lower is better → inverted normalisation)
 *   quality      0.3  (higher is better)
 *   deliveryDays 0.2  (lower is better → inverted normalisation)
 *
 * Normalisation: min-max on [0, 1].
 * For "lower is better" criteria the normalised value is inverted:
 *   score = 1 - (value - min) / (max - min)
 */
public class DecisionEngine {

    private static final double W_PRICE    = 0.5;
    private static final double W_QUALITY  = 0.3;
    private static final double W_DELIVERY = 0.2;

    public static Product getBest(List<Product> products) {
        if (products == null || products.isEmpty()) return null;
        if (products.size() == 1)                   return products.get(0);

        double minPrice    = products.stream().mapToDouble(Product::getPrice).min().getAsDouble();
        double maxPrice    = products.stream().mapToDouble(Product::getPrice).max().getAsDouble();
        double minQuality  = products.stream().mapToDouble(Product::getQuality).min().getAsDouble();
        double maxQuality  = products.stream().mapToDouble(Product::getQuality).max().getAsDouble();
        double minDelivery = products.stream().mapToDouble(Product::getDeliveryDays).min().getAsDouble();
        double maxDelivery = products.stream().mapToDouble(Product::getDeliveryDays).max().getAsDouble();

        Product best      = null;
        double  bestScore = -1;

        for (Product p : products) {
            double normPrice    = normaliseInverted(p.getPrice(),        minPrice,    maxPrice);
            double normQuality  = normalise        (p.getQuality(),      minQuality,  maxQuality);
            double normDelivery = normaliseInverted(p.getDeliveryDays(), minDelivery, maxDelivery);

            double score = W_PRICE * normPrice + W_QUALITY * normQuality + W_DELIVERY * normDelivery;

            System.out.printf("[DecisionEngine] %-20s → price=%.2f  quality=%.2f  delivery=%.2f  SCORE=%.3f%n",
                    p.getName(), normPrice, normQuality, normDelivery, score);

            if (score > bestScore) {
                bestScore = score;
                best      = p;
            }
        }
        return best;
    }

    /** Higher value = better score. */
    private static double normalise(double value, double min, double max) {
        if (max == min) return 1.0;
        return (value - min) / (max - min);
    }

    /** Lower value = better score. */
    private static double normaliseInverted(double value, double min, double max) {
        if (max == min) return 1.0;
        return 1.0 - (value - min) / (max - min);
    }
}
