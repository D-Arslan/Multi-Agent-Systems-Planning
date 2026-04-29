package part2;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 * Answers REQUEST messages from the MobileBuyerAgent with its product offer.
 *
 * Agent arguments (set by PlatformLauncher):
 *   args[0] = product name
 *   args[1] = price
 *   args[2] = quality (/10)
 *   args[3] = delivery days
 */
public class SellerAgent extends Agent {

    private Product offer;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        offer = new Product(
                (String) args[0],
                Double.parseDouble((String) args[1]),
                Double.parseDouble((String) args[2]),
                Integer.parseInt  ((String) args[3]));

        System.out.println("[" + getLocalName() + "] Ready — offer: " + offer);
        addBehaviour(new AnswerBehaviour());
    }

    private class AnswerBehaviour extends CyclicBehaviour {

        private static final MessageTemplate MT =
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST);

        @Override
        public void action() {
            ACLMessage request = myAgent.receive(MT);
            if (request == null) { block(); return; }

            System.out.println("[" + getLocalName() + "] Request received from "
                    + request.getSender().getLocalName() + " — sending offer");

            ACLMessage reply = request.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            // Encode offer as "name;price;quality;deliveryDays"
            reply.setContent(offer.getName() + ";" + offer.getPrice()
                    + ";" + offer.getQuality() + ";" + offer.getDeliveryDays());
            send(reply);
        }
    }
}
