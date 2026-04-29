package part1;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 * Participates in the auction driven by SellerAgent.
 *
 * Strategy: bid (current price + increment) as long as the next bid
 * stays within the budget. Refuse otherwise.
 *
 * Agent arguments (set by AuctionLauncher):
 *   args[0] = max budget  (double)
 *   args[1] = bid increment  (double)
 */
public class BuyerAgent extends Agent {

    private double maxBudget;
    private double increment;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        maxBudget = Double.parseDouble((String) args[0]);
        increment = Double.parseDouble((String) args[1]);

        System.out.println("[" + getLocalName() + "] Ready"
                + " — budget: " + maxBudget + " €"
                + ", increment: " + increment + " €");

        addBehaviour(new BidBehaviour());
    }

    // -------------------------------------------------------------------------

    private class BidBehaviour extends CyclicBehaviour {

        private static final MessageTemplate MT = MessageTemplate.or(
                MessageTemplate.or(
                        MessageTemplate.MatchPerformative(ACLMessage.CFP),
                        MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL)),
                MessageTemplate.or(
                        MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL),
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM)));

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(MT);
            if (msg == null) { block(); return; }

            switch (msg.getPerformative()) {
                case ACLMessage.CFP             -> handleCfp(msg);
                case ACLMessage.ACCEPT_PROPOSAL -> handleAccept(msg);
                case ACLMessage.REJECT_PROPOSAL -> handleReject(msg);
                case ACLMessage.INFORM          -> handleInform(msg);
            }
        }

        private void handleCfp(ACLMessage cfp) {
            double currentPrice = Double.parseDouble(cfp.getContent());
            double myBid        = currentPrice + increment;

            if (myBid <= maxBudget) {
                System.out.println("[" + getLocalName() + "] Bidding "
                        + myBid + " € (current: " + currentPrice + " €)");

                ACLMessage propose = cfp.createReply();
                propose.setPerformative(ACLMessage.PROPOSE);
                propose.setContent(String.valueOf(myBid));
                send(propose);

            } else {
                System.out.println("[" + getLocalName() + "] Budget exceeded — refusing "
                        + "(next bid would be " + myBid + " €, budget: " + maxBudget + " €)");

                ACLMessage refuse = cfp.createReply();
                refuse.setPerformative(ACLMessage.REFUSE);
                send(refuse);
            }
        }

        private void handleAccept(ACLMessage msg) {
            System.out.println("[" + getLocalName() + "] WON the auction at "
                    + msg.getContent() + " €");
        }

        private void handleReject(ACLMessage msg) {
            System.out.println("[" + getLocalName() + "] Lost. " + msg.getContent());
        }

        private void handleInform(ACLMessage msg) {
            if ("NO_SALE".equals(msg.getContent())) {
                System.out.println("[" + getLocalName() + "] Auction ended with no sale.");
            }
        }
    }
}
