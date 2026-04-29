package part1;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

/**
 * Bootstraps the auction:
 *   - 1 SellerAgent  (product: Laptop, start: 1000, reserve: 1300)
 *   - 3 BuyerAgents  (budgets: 1500 / 1200 / 900, increment: 50 each)
 *
 * Buyers are started first so they are registered in the DF before
 * the seller broadcasts the first CFP.
 */
public class AuctionLauncher {

    private static final String PRODUCT_NAME   = "Laptop";
    private static final String STARTING_PRICE = "1000";
    private static final String RESERVE_PRICE  = "1300";

    private static final String INCREMENT = "50";

    private static final String[][] BUYERS = {
        { "Buyer1", "1500", INCREMENT },
        { "Buyer2", "1200", INCREMENT },
        { "Buyer3", "900",  INCREMENT },
    };

    public static void main(String[] args) throws Exception {
        Runtime rt = Runtime.instance();

        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.GUI, "true");
        AgentContainer container = rt.createMainContainer(profile);

        // Start buyers first
        for (String[] buyer : BUYERS) {
            String name   = buyer[0];
            String budget = buyer[1];
            String incr   = buyer[2];
            AgentController ac = container.createNewAgent(
                    name, "part1.BuyerAgent",
                    new Object[]{ budget, incr });
            ac.start();
        }

        // Small pause — let buyers register before the first CFP goes out
        Thread.sleep(500);

        // Build seller argument list: name, startPrice, reservePrice, buyer1, buyer2, ...
        Object[] sellerArgs = new Object[3 + BUYERS.length];
        sellerArgs[0] = PRODUCT_NAME;
        sellerArgs[1] = STARTING_PRICE;
        sellerArgs[2] = RESERVE_PRICE;
        for (int i = 0; i < BUYERS.length; i++) {
            sellerArgs[3 + i] = BUYERS[i][0];
        }

        AgentController seller = container.createNewAgent(
                "Seller", "part1.SellerAgent", sellerArgs);
        seller.start();
    }
}
