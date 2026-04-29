package part2;

import jade.core.AID;
import jade.core.Agent;
import jade.core.Location;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Mobile buyer that travels across containers collecting offers,
 * then runs a multi-criteria decision to pick the best product.
 *
 * Args layout (set by PlatformLauncher), repeated per stop:
 *   args[0] = seller1 local name   args[1] = Location to migrate to after querying seller1
 *   args[2] = seller2 local name   args[3] = Location to migrate to after querying seller2
 *   args[n] = sellerN local name   args[n+1] = Location to migrate to after querying sellerN
 *
 * Execution on each container:
 *   1. afterMove() fires → adds TravelBehaviour
 *   2. TravelBehaviour queries the seller at currentIndex, collects offer
 *   3. Migrates to destinations[currentIndex], increments currentIndex
 *   4. When currentIndex == destinations.size() → no more migration → decide
 */
public class MobileBuyerAgent extends Agent {

    // Serializable state — survives migrations
    private ArrayList<String>   sellerNames  = new ArrayList<>();
    private ArrayList<Location> destinations = new ArrayList<>();
    private ArrayList<Product>  offers       = new ArrayList<>();
    private int currentIndex = 0;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        for (int i = 0; i < args.length - 1; i += 2) {
            sellerNames .add((String)   args[i]);
            destinations.add((Location) args[i + 1]);
        }
        System.out.println("[MobileBuyer] Started on " + here().getName()
                + " — " + destinations.size() + " stops planned");
        addBehaviour(new TravelBehaviour());
    }

    @Override
    protected void beforeMove() {
        System.out.println("[MobileBuyer] Leaving " + here().getName());
    }

    @Override
    protected void afterMove() {
        System.out.println("[MobileBuyer] Arrived on " + here().getName());
        addBehaviour(new TravelBehaviour());
    }

    // -------------------------------------------------------------------------

    private class TravelBehaviour extends OneShotBehaviour {

        @Override
        public void action() {
            if (currentIndex < sellerNames.size()) {
                queryCurrentSeller();
            } else {
                decide();
            }
        }

        private void queryCurrentSeller() {
            String   seller = sellerNames .get(currentIndex);
            Location next   = destinations.get(currentIndex);

            System.out.println("[MobileBuyer] Querying " + seller
                    + " on " + here().getName() + "...");

            ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
            request.addReceiver(new AID(seller, AID.ISLOCALNAME));
            request.setContent("QUERY");
            send(request);

            ACLMessage reply = blockingReceive(
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM), 5000);

            if (reply != null) {
                String[] parts = reply.getContent().split(";");
                Product p = new Product(parts[0],
                        Double.parseDouble(parts[1]),
                        Double.parseDouble(parts[2]),
                        Integer.parseInt  (parts[3]));
                offers.add(p);
                System.out.println("[MobileBuyer] Offer collected: " + p);
            } else {
                System.out.println("[MobileBuyer] Timeout — no reply from " + seller);
            }

            currentIndex++;
            System.out.println("[MobileBuyer] Migrating to " + next.getName() + "...");
            doMove(next);
        }

        private void decide() {
            System.out.println();
            System.out.println("[MobileBuyer] All stops visited — " + offers.size()
                    + " offer(s) collected.");
            System.out.println("[MobileBuyer] Running multi-criteria decision...");
            System.out.println();

            Product best = DecisionEngine.getBest(offers);

            System.out.println();
            System.out.println("[MobileBuyer] ==========================================");
            if (best != null) {
                System.out.println("[MobileBuyer]  BEST OFFER : " + best);
            } else {
                System.out.println("[MobileBuyer]  No offers collected.");
            }
            System.out.println("[MobileBuyer] ==========================================");
        }
    }
}
