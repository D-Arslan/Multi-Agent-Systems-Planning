package part1;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs an English auction for one product.
 *
 * Protocol per round:
 *   1. Broadcast CFP with current price to all active buyers.
 *   2. Collect PROPOSE / REFUSE replies (with timeout).
 *   3. Keep the highest bid; remove buyers who refused.
 *   4. Repeat until no active buyer remains or the timeout fires with no bids.
 *   5. If the final price >= reserve price  →  ACCEPT_PROPOSAL to the winner,
 *                                              REJECT_PROPOSAL to the rest.
 *      Otherwise                           →  INFORM everyone: no sale.
 *
 * Agent arguments (set by AuctionLauncher):
 *   args[0] = product name
 *   args[1] = starting price
 *   args[2] = reserve price
 *   args[3..n] = local names of buyer agents
 */
public class SellerAgent extends Agent {

    private static final long ROUND_TIMEOUT_MS = 5000;

    private Product      product;
    private List<AID>    activeBuyers;
    private double       currentPrice;
    private AID          currentWinner;

    @Override
    protected void setup() {
        Object[] args = getArguments();

        product      = new Product((String) args[0],
                                   Double.parseDouble((String) args[1]),
                                   Double.parseDouble((String) args[2]));
        currentPrice = product.getStartingPrice();
        activeBuyers = new ArrayList<>();

        for (int i = 3; i < args.length; i++) {
            activeBuyers.add(new AID((String) args[i], AID.ISLOCALNAME));
        }

        System.out.println("[Seller] Auction started — " + product
                + " | reserve: " + product.getReservePrice() + " €"
                + " | buyers: " + activeBuyers.size());

        addBehaviour(new AuctionBehaviour());
    }

    // -------------------------------------------------------------------------

    private class AuctionBehaviour extends Behaviour {

        private enum State { ANNOUNCE, COLLECT, CLOSE }

        private State  state       = State.ANNOUNCE;
        private long   roundStart;
        private int    proposesExpected;
        private int    proposesReceived;
        private boolean done       = false;

        @Override
        public void action() {
            switch (state) {
                case ANNOUNCE -> announce();
                case COLLECT  -> collect();
                case CLOSE    -> close();
            }
        }

        private void announce() {
            if (activeBuyers.isEmpty()) {
                state = State.CLOSE;
                return;
            }

            System.out.println("[Seller] --- Round — current price: " + currentPrice + " €"
                    + " — active buyers: " + activeBuyers.size());

            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            cfp.setContent(String.valueOf(currentPrice));
            activeBuyers.forEach(cfp::addReceiver);
            send(cfp);

            roundStart        = System.currentTimeMillis();
            proposesExpected  = activeBuyers.size();
            proposesReceived  = 0;
            currentWinner     = null;
            state             = State.COLLECT;
        }

        private void collect() {
            MessageTemplate mt = MessageTemplate.or(
                    MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
                    MessageTemplate.MatchPerformative(ACLMessage.REFUSE));

            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {
                if (msg.getPerformative() == ACLMessage.PROPOSE) {
                    double offer = Double.parseDouble(msg.getContent());
                    System.out.println("[Seller] Received offer " + offer
                            + " € from " + msg.getSender().getLocalName());

                    if (offer > currentPrice) {
                        currentPrice  = offer;
                        currentWinner = msg.getSender();
                    }
                    proposesReceived++;

                } else {
                    System.out.println("[Seller] "
                            + msg.getSender().getLocalName() + " refused.");
                    activeBuyers.remove(msg.getSender());
                    proposesExpected--;
                }

                if (proposesReceived >= proposesExpected) {
                    advanceRound();
                }

            } else if (System.currentTimeMillis() - roundStart > ROUND_TIMEOUT_MS) {
                System.out.println("[Seller] Round timeout.");
                advanceRound();

            } else {
                block(100);
            }
        }

        private void advanceRound() {
            if (currentWinner == null) {
                // Nobody bid this round — all active buyers refused
                state = State.CLOSE;
            } else if (currentPrice >= product.getReservePrice()) {
                // Reserve price reached — no need to continue
                state = State.CLOSE;
            } else {
                System.out.println("[Seller] Best offer this round: "
                        + currentPrice + " € by "
                        + currentWinner.getLocalName());
                state = State.ANNOUNCE;
            }
        }

        private void close() {
            System.out.println("[Seller] === Auction closed — final price: "
                    + currentPrice + " €");

            if (currentWinner != null && currentPrice >= product.getReservePrice()) {
                System.out.println("[Seller] SALE CONFIRMED — winner: "
                        + currentWinner.getLocalName()
                        + " at " + currentPrice + " €");

                ACLMessage accept = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                accept.addReceiver(currentWinner);
                accept.setContent(String.valueOf(currentPrice));
                send(accept);

                activeBuyers.stream()
                        .filter(b -> !b.equals(currentWinner))
                        .forEach(b -> {
                            ACLMessage reject = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                            reject.addReceiver(b);
                            reject.setContent("Winner: " + currentWinner.getLocalName()
                                    + " at " + currentPrice + " €");
                            send(reject);
                        });

            } else {
                System.out.println("[Seller] SALE CANCELLED — reserve price not reached.");

                ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
                inform.setContent("NO_SALE");
                activeBuyers.forEach(inform::addReceiver);
                if (currentWinner != null) inform.addReceiver(currentWinner);
                send(inform);
            }

            done = true;
        }

        @Override
        public boolean done() { return done; }
    }
}
