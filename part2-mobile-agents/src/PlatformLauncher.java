package part2;

import jade.core.ContainerID;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

/**
 * Launches the multi-container scenario (inter-container migration).
 *
 * Container layout:
 *   Main-Container — Seller1 (Laptop A : price=1200, quality=7, delivery=3)
 *   Container2     — Seller2 (Laptop B : price=900,  quality=5, delivery=7)
 *   Container3     — Seller3 (Laptop C : price=1100, quality=9, delivery=2)
 *
 * MobileBuyerAgent travel plan:
 *   1. Start on Main-Container → query Seller1 locally
 *   2. Migrate to Container2   → query Seller2
 *   3. Migrate to Container3   → query Seller3
 *   4. Return to Main-Container → run DecisionEngine, print best offer
 *
 * Run with:
 *   java -cp "lib\jade.jar;part2-mobile-agents\out" part2.PlatformLauncher
 */
public class PlatformLauncher {

    public static void main(String[] args) throws Exception {
        Runtime rt = Runtime.instance();

        // ── Main container (port 1100 to avoid conflicts with other JADE instances) ──
        Profile mainProfile = new ProfileImpl();
        mainProfile.setParameter(Profile.GUI,        "true");
        mainProfile.setParameter(Profile.LOCAL_PORT, "1100");
        AgentContainer mainContainer = rt.createMainContainer(mainProfile);

        // Wait for the main container to be fully up before creating sub-containers
        Thread.sleep(500);

        // ── Container 2 ─────────────────────────────────────────────────────
        Profile c2Profile = new ProfileImpl();
        c2Profile.setParameter(Profile.MAIN_HOST,      "localhost");
        c2Profile.setParameter(Profile.MAIN_PORT,      "1100");
        c2Profile.setParameter(Profile.CONTAINER_NAME, "Container2");
        AgentContainer container2 = rt.createAgentContainer(c2Profile);

        // ── Container 3 ─────────────────────────────────────────────────────
        Profile c3Profile = new ProfileImpl();
        c3Profile.setParameter(Profile.MAIN_HOST,      "localhost");
        c3Profile.setParameter(Profile.MAIN_PORT,      "1100");
        c3Profile.setParameter(Profile.CONTAINER_NAME, "Container3");
        AgentContainer container3 = rt.createAgentContainer(c3Profile);

        // ── Sellers ─────────────────────────────────────────────────────────
        AgentController s1 = mainContainer.createNewAgent(
                "Seller1", "part2.SellerAgent",
                new Object[]{ "Laptop A", "1200.0", "7.0", "3" });
        s1.start();

        AgentController s2 = container2.createNewAgent(
                "Seller2", "part2.SellerAgent",
                new Object[]{ "Laptop B", "900.0", "5.0", "7" });
        s2.start();

        AgentController s3 = container3.createNewAgent(
                "Seller3", "part2.SellerAgent",
                new Object[]{ "Laptop C", "1100.0", "9.0", "2" });
        s3.start();

        // Give all sellers time to register before the buyer starts
        Thread.sleep(1000);

        // ── Mobile buyer ────────────────────────────────────────────────────
        // Travel plan: query Seller1 here, then migrate to Container2 (Seller2),
        // then Container3 (Seller3), then back to Main-Container.
        // Args layout: sellerName, destinationAfterQuery, sellerName, destination, ...
        // The buyer queries the seller BEFORE migrating to the next destination.
        ContainerID loc2   = new ContainerID("Container2",     null);
        ContainerID loc3   = new ContainerID("Container3",     null);
        ContainerID home   = new ContainerID("Main-Container", null);

        AgentController buyer = mainContainer.createNewAgent(
                "MobileBuyer", "part2.MobileBuyerAgent",
                new Object[]{
                    // seller to query here,  container to go to next
                    "Seller1", loc2,
                    "Seller2", loc3,
                    "Seller3", home
                });
        buyer.start();
    }
}
