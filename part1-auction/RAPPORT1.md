# Rapport Partie 1 — Négociation Multi-Agents : Protocole d'Enchères

---

## 1. Vue d'ensemble

L'objectif de cette partie est de simuler une **enchère anglaise** dans un Système Multi-Agents JADE.
Un vendeur met un produit aux enchères, plusieurs acheteurs surenchérissent à chaque round jusqu'à ce que le prix de réserve soit atteint ou que tous abandonnent.

**Agents mis en jeu :**

| Agent | Rôle | Nombre |
|-------|------|--------|
| `SellerAgent` | Orchestre l'enchère (CFP, collecte, clôture) | 1 |
| `BuyerAgent` | Enchérit ou abandonne selon son budget | N (min 2) |

**Fichiers sources :**

```
part1-auction/src/
├── Product.java          — objet métier sérialisable
├── SellerAgent.java      — vendeur, maître du protocole
├── BuyerAgent.java       — acheteur, stratégie budget/incrément
└── AuctionLauncher.java  — point d'entrée, crée et lance tous les agents
```

---

## 2. Explication des classes

### 2.1 `Product.java`

Représente le produit mis aux enchères.

```java
public class Product implements Serializable {
    private final String name;
    private final double startingPrice;
    private final double reservePrice;    // secret — connu du seller uniquement
}
```

**Pourquoi `Serializable` ?**
JADE transmet les objets entre agents via des messages ACL. Pour envoyer un objet Java directement (via `setContentObject()`), il doit implémenter `Serializable` — JADE le sérialise en bytes pour le transport.

**Pourquoi `reservePrice` ?**
C'est le prix minimum en dessous duquel le vendeur refuse de vendre. Il est encapsulé dans `SellerAgent` et n'est jamais communiqué aux acheteurs — c'est le principe d'**information asymétrique** dans les SMA.

---

### 2.2 `SellerAgent.java`

C'est l'agent central du protocole. Il orchestre tous les rounds de l'enchère.

#### Initialisation (`setup`)

```java
protected void setup() {
    // Récupère les arguments passés par AuctionLauncher
    product      = new Product(args[0], args[1], args[2]);
    currentPrice = product.getStartingPrice();
    activeBuyers = new ArrayList<>();        // liste des buyers encore en jeu
    // Construit la liste AID des buyers depuis les noms passés en args
    addBehaviour(new AuctionBehaviour());
}
```

Les **AID (Agent Identifier)** sont les adresses uniques des agents dans JADE. On les construit avec `new AID(name, AID.ISLOCALNAME)` pour cibler des agents du même container.

#### Comportement principal : `AuctionBehaviour`

C'est un `Behaviour` avec une **machine à 3 états** :

```
┌─────────┐     ┌─────────┐     ┌───────┐
│ ANNOUNCE│────>│ COLLECT │────>│ CLOSE │
└─────────┘     └────┬────┘     └───────┘
     ^               │
     └───────────────┘
     (si des offres ont été reçues
      ET prix < réserve)
```

**État ANNOUNCE**
- Envoie un message `CFP` (Call For Proposal) à tous les buyers actifs
- Le contenu du message = le prix courant en String
- Lance le timer du round (`roundStart = System.currentTimeMillis()`)

```java
ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
cfp.setContent(String.valueOf(currentPrice));
activeBuyers.forEach(cfp::addReceiver);
send(cfp);
```

**État COLLECT**
- Écoute les réponses `PROPOSE` et `REFUSE` via un `MessageTemplate`
- `PROPOSE` → compare l'offre au prix courant, retient la meilleure
- `REFUSE` → retire le buyer de la liste `activeBuyers`
- Si toutes les réponses sont reçues → `advanceRound()`
- Si timeout (5 secondes) sans réponse complète → `advanceRound()`
- Sinon → `block(100)` (cède le thread pendant 100ms, évite la busy-wait)

```java
MessageTemplate mt = MessageTemplate.or(
    MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
    MessageTemplate.MatchPerformative(ACLMessage.REFUSE));
ACLMessage msg = myAgent.receive(mt);
```

**Pourquoi `MessageTemplate` ?**
Sans filtre, `receive()` prendrait le premier message quelle que soit sa nature. Le `MessageTemplate` garantit qu'on ne traite que les messages attendus dans cet état précis du protocole.

**`advanceRound()` — logique de transition**

```java
private void advanceRound() {
    if (currentWinner == null) {
        // Personne n'a enchéri ce round → tous ont refusé → clôture
        state = State.CLOSE;
    } else if (currentPrice >= product.getReservePrice()) {
        // Prix de réserve atteint → inutile de continuer
        state = State.CLOSE;
    } else {
        // Il y a eu des offres et le prix reste sous le réserve → round suivant
        state = State.ANNOUNCE;
    }
}
```

**État CLOSE**
- Compare le prix final au prix de réserve
- **Vente conclue** : envoie `ACCEPT_PROPOSAL` au gagnant, `REJECT_PROPOSAL` aux autres
- **Vente annulée** : envoie `INFORM "NO_SALE"` à tous
- Passe `done = true` pour terminer le `Behaviour`

---

### 2.3 `BuyerAgent.java`

Agent réactif — il répond aux messages du seller selon une stratégie simple.

#### Initialisation (`setup`)

```java
protected void setup() {
    maxBudget = Double.parseDouble((String) args[0]);  // budget maximum
    increment = Double.parseDouble((String) args[1]);  // montant d'enchère au-dessus du prix courant
    addBehaviour(new BidBehaviour());
}
```

#### Comportement : `BidBehaviour` (CyclicBehaviour)

Un `CyclicBehaviour` tourne indéfiniment — il écoute les messages en permanence via un `MessageTemplate` qui filtre 4 performatifs :

```
CFP             → décider d'enchérir ou refuser
ACCEPT_PROPOSAL → annoncer la victoire
REJECT_PROPOSAL → annoncer la défaite
INFORM          → traiter "NO_SALE"
```

**Stratégie d'enchère (`handleCfp`)**

```java
double myBid = currentPrice + increment;

if (myBid <= maxBudget) {
    // Envoie PROPOSE avec le nouveau prix
} else {
    // Envoie REFUSE — budget dépassé
}
```

Le `cfp.createReply()` génère automatiquement un message avec les bons champs `sender`/`receiver` inversés — pratique JADE standard pour répondre à un message.

---

### 2.4 `AuctionLauncher.java`

Point d'entrée du programme. Configure et lance tous les agents dans un même container JADE.

```java
// 1. Créer le container JADE principal (avec GUI)
AgentContainer container = rt.createMainContainer(profile);

// 2. Démarrer les buyers EN PREMIER
for (String[] buyer : BUYERS) {
    container.createNewAgent(name, "part1.BuyerAgent", new Object[]{ budget, incr });
}

// 3. Attendre que les buyers soient enregistrés
Thread.sleep(500);

// 4. Démarrer le seller avec les noms des buyers en arguments
container.createNewAgent("Seller", "part1.SellerAgent", sellerArgs);
```

**Pourquoi les buyers avant le seller ?**
Le seller envoie des CFP dès qu'il démarre. Si les buyers ne sont pas encore enregistrés dans le container JADE, les messages sont perdus. Le `Thread.sleep(500)` laisse le temps à JADE d'initialiser les agents.

**Paramètres du scénario de test (modifiables dans le launcher) :**

```java
PRODUCT_NAME   = "Laptop"
STARTING_PRICE = "1000"    // prix d'ouverture
RESERVE_PRICE  = "1300"    // prix minimum du vendeur (secret)

BUYERS = {
    { "Buyer1", budget="1500", increment="50" },
    { "Buyer2", budget="1200", increment="50" },
    { "Buyer3", budget="900",  increment="50" },  // sous le prix de départ → refuse immédiatement
}
```

---

## 3. Protocole ACL — Messages échangés

```
Seller                   Buyer1          Buyer2          Buyer3
  |                        |               |               |
  |---CFP("1000.0")------->|               |               |
  |---CFP("1000.0")----------------------->|               |
  |---CFP("1000.0")---------------------------------------->|
  |                        |               |               |
  |<--PROPOSE("1050.0")----|               |               |
  |<--PROPOSE("1050.0")----------------- --|               |
  |<--REFUSE()-------------------------------------- -------|
  |                        |               |               |
  | [retient 1050, retire Buyer3]          |               |
  |                        |               |               |
  | ... rounds 2 à 5 ...   |               |               |
  |                        |               |               |
  |---CFP("1200.0")------->|               |               |
  |---CFP("1200.0")----------------------->|               |
  |                        |               |               |
  |<--PROPOSE("1250.0")----|               |               |
  |<--REFUSE()------------------------ ----|               |
  |                        |               |               |
  | [retire Buyer2]        |               |               |
  |                        |               |               |
  |---CFP("1250.0")------->|               |               |
  |<--PROPOSE("1300.0")----|               |               |
  |                        |               |               |
  | [1300 >= réserve(1300) → CLOSE]        |               |
  |                        |               |               |
  |---ACCEPT_PROPOSAL("1300.0")----------->|               |
  |---REJECT_PROPOSAL("Winner: Buyer1")----------------- ->|
```

---

## 4. Guide de test — Exécution pas à pas

### Prérequis

- Java 8 ou supérieur installé (`java -version`)
- Ouvrir PowerShell et se placer à la **racine du projet** :
  ```powershell
  cd D:\TP_Tech_Agent\Projet
  ```

> **Important :** toutes les commandes s'exécutent depuis `Projet\`. Ne pas lancer depuis `part1-auction\`.

> **Astuce encodage :** lancer `chcp 65001` avant pour afficher les `€` et `—` correctement dans la console.

### Étape 1 — Compilation

```powershell
javac -cp "lib\jade.jar" -d "part1-auction\out" "part1-auction\src\Product.java" "part1-auction\src\BuyerAgent.java" "part1-auction\src\SellerAgent.java" "part1-auction\src\AuctionLauncher.java"
```

> Aucun message affiché = compilation réussie.

### Étape 2 — Lancement

```powershell
java -cp "lib\jade.jar;part1-auction\out" part1.AuctionLauncher
```

La **GUI JADE** s'ouvre. On y voit dans `Main-Container` :
- `Buyer1`, `Buyer2`, `Buyer3` — les agents acheteurs
- `Seller` — l'agent vendeur
- `ams`, `df`, `rma` — agents systèmes JADE (ne pas y toucher)

La GUI peut être laissée ouverte. Le déroulement de l'enchère s'affiche dans la **console PowerShell**.

### Étape 3 — Sortie console observée (scénario par défaut)

Voici la sortie réelle obtenue lors du test :

```
[Buyer3] Ready — budget: 900.0 €, increment: 50.0 €
[Buyer2] Ready — budget: 1200.0 €, increment: 50.0 €
[Buyer1] Ready — budget: 1500.0 €, increment: 50.0 €
[Seller] Auction started — Laptop (starting: 1000.0 €) | reserve: 1300.0 € | buyers: 3

[Seller] --- Round — current price: 1000.0 € — active buyers: 3
[Buyer3] Budget exceeded — refusing (next bid would be 1050.0 €, budget: 900.0 €)
[Buyer1] Bidding 1050.0 € (current: 1000.0 €)
[Buyer2] Bidding 1050.0 € (current: 1000.0 €)
[Seller] Buyer3 refused.
[Seller] Received offer 1050.0 € from Buyer1
[Seller] Received offer 1050.0 € from Buyer2
[Seller] Best offer this round: 1050.0 € by Buyer1

[Seller] --- Round — current price: 1050.0 € — active buyers: 2
[Buyer1] Bidding 1100.0 € (current: 1050.0 €)
[Buyer2] Bidding 1100.0 € (current: 1050.0 €)
[Seller] Received offer 1100.0 € from Buyer1
[Seller] Received offer 1100.0 € from Buyer2
[Seller] Best offer this round: 1100.0 € by Buyer1

[Seller] --- Round — current price: 1100.0 € — active buyers: 2
[Buyer1] Bidding 1150.0 € (current: 1100.0 €)
[Buyer2] Bidding 1150.0 € (current: 1100.0 €)
[Seller] Received offer 1150.0 € from Buyer1
[Seller] Received offer 1150.0 € from Buyer2
[Seller] Best offer this round: 1150.0 € by Buyer1

[Seller] --- Round — current price: 1150.0 € — active buyers: 2
[Buyer1] Bidding 1200.0 € (current: 1150.0 €)
[Buyer2] Bidding 1200.0 € (current: 1150.0 €)
[Seller] Received offer 1200.0 € from Buyer1
[Seller] Received offer 1200.0 € from Buyer2
[Seller] Best offer this round: 1200.0 € by Buyer1

[Seller] --- Round — current price: 1200.0 € — active buyers: 2
[Buyer2] Budget exceeded — refusing (next bid would be 1250.0 €, budget: 1200.0 €)
[Buyer1] Bidding 1250.0 € (current: 1200.0 €)
[Seller] Buyer2 refused.
[Seller] Received offer 1250.0 € from Buyer1
[Seller] Best offer this round: 1250.0 € by Buyer1

[Seller] --- Round — current price: 1250.0 € — active buyers: 1
[Buyer1] Bidding 1300.0 € (current: 1250.0 €)
[Seller] Received offer 1300.0 € from Buyer1

[Seller] === Auction closed — final price: 1300.0 €
[Seller] SALE CONFIRMED — winner: Buyer1 at 1300.0 €
[Buyer1] WON the auction at 1300.0 €
```

### Étape 4 — Conformité aux résultats attendus

| Point de vérification | Résultat observé | Conforme |
|-----------------------|-----------------|---------|
| Buyer3 refuse dès le round 1 (budget 900 < 1050) | `Buyer3 refused` round 1 | ✅ |
| Buyer2 abandonne quand le prix dépasse son budget (1200€) | `Buyer2 refused` au round 1200€ | ✅ |
| Le seller n'envoie le CFP qu'aux buyers encore actifs | `active buyers: 2` puis `1` | ✅ |
| Buyer1 continue seul jusqu'à 1300€ | `Bidding 1300.0€` round 6 | ✅ |
| Clôture dès que prix >= réserve (1300€) | `Auction closed — final price: 1300.0€` | ✅ |
| Vente confirmée, gagnant Buyer1 | `SALE CONFIRMED — winner: Buyer1` | ✅ |
| Buyer1 notifié de sa victoire | `WON the auction at 1300.0€` | ✅ |

### Étape 5 — Tester les autres scénarios

**Cas 2 : Vente annulée (prix de réserve non atteint)**

Dans `AuctionLauncher.java`, modifier puis recompiler :
```java
private static final String RESERVE_PRICE = "1800";
```
Résultat attendu :
```
[Seller] SALE CANCELLED — reserve price not reached.
[Buyer1] Auction ended with no sale.
```

**Cas 3 : Tous les buyers sous le prix de départ**

```java
private static final String[][] BUYERS = {
    { "Buyer1", "800", "50" },
    { "Buyer2", "700", "50" },
};
```
Résultat attendu : tous refusent round 1 → clôture immédiate sans vente.

**Cas 4 : Ajouter un 4ème buyer avec incrément agressif**

```java
private static final String[][] BUYERS = {
    { "Buyer1", "1500", "50"  },
    { "Buyer2", "1200", "50"  },
    { "Buyer3", "900",  "50"  },
    { "Buyer4", "2000", "100" },
};
```
Résultat attendu : Buyer4 remporte l'enchère plus tôt grâce à son incrément de 100€.

---

## 5. Points clés pour la présentation

| Concept | Où le voir dans le code |
|---------|------------------------|
| Machine à états (FSM) | `AuctionBehaviour.State` enum + switch dans `action()` |
| Filtrage de messages | `MessageTemplate.or(PROPOSE, REFUSE)` dans `collect()` |
| Information asymétrique | `reservePrice` dans `Product`, jamais envoyé aux buyers |
| Timeout robuste | `System.currentTimeMillis() - roundStart > ROUND_TIMEOUT_MS` |
| `block()` vs busy-wait | `block(100)` dans `collect()` — cède le thread plutôt que boucler |
| Passage d'arguments aux agents | `getArguments()` dans `setup()` des deux agents |
