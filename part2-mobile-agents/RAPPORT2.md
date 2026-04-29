# Rapport Partie 2 — Agents Mobiles + Décision Multi-Critères

---

## 1. Vue d'ensemble

L'objectif de cette partie est de simuler un acheteur intelligent qui **se déplace physiquement** de container en container pour collecter des offres, puis choisit la meilleure selon plusieurs critères.

Deux concepts sont combinés :
- **Mobilité d'agents** : un agent migre entre containers JADE en transportant son état
- **Décision multi-critères** : choix du meilleur produit selon prix, qualité et délai de livraison

**Agents mis en jeu :**

| Agent | Rôle | Nombre |
|-------|------|--------|
| `SellerAgent` | Répond aux requêtes avec son offre produit | 3 (un par container) |
| `MobileBuyerAgent` | Visite chaque seller, collecte les offres, décide | 1 |

**Fichiers sources :**

```
part2-mobile-agents/src/
├── Product.java            — objet sérialisable avec 4 critères
├── DecisionEngine.java     — calcul du score multi-critères (somme pondérée)
├── SellerAgent.java        — répond aux REQUEST avec son offre
├── MobileBuyerAgent.java   — agent mobile, voyage et décide
└── PlatformLauncher.java   — crée 3 containers, 3 sellers, 1 buyer
```

---

## 2. Explication des classes

### 2.1 `Product.java`

Représente une offre produit avec 4 champs utilisés pour la décision.

```java
public class Product implements Serializable {
    private final String name;
    private final double price;        // critère 1 — plus bas = mieux
    private final double quality;      // critère 2 — plus haut = mieux  (/10)
    private final int    deliveryDays; // critère 3 — plus bas = mieux
}
```

**Pourquoi `Serializable` ici aussi ?**
L'agent transporte ses offres collectées dans son état interne lors des migrations. JADE sérialise l'intégralité de l'objet agent (tous ses champs) pour le déplacer d'un container à l'autre. Tout champ non-sérialisable provoquerait une erreur de migration.

---

### 2.2 `DecisionEngine.java`

Classe utilitaire (pas un agent) qui implémente la **méthode de la somme pondérée**.

#### Poids des critères

| Critère | Poids | Sens |
|---------|-------|------|
| Prix | 0.5 | Plus bas = mieux |
| Qualité | 0.3 | Plus haut = mieux |
| Délai de livraison | 0.2 | Plus bas = mieux |

#### Normalisation min-max

Pour comparer des critères d'unités différentes (€, /10, jours), on ramène chaque valeur entre 0 et 1 :

```
normalise(v) = (v - min) / (max - min)              → pour "plus haut = mieux"
normaliseInverted(v) = 1 - (v - min) / (max - min)  → pour "plus bas = mieux"
```

Si `max == min` (toutes les offres identiques sur ce critère), le score est 1.0 pour tous.

#### Calcul du score

```java
score = 0.5 * normPrix + 0.3 * normQualité + 0.2 * normDélai
```

#### Exemple numérique (données du test)

| Produit | Prix | Qualité | Délai |
|---------|------|---------|-------|
| Laptop A | 1200€ | 7/10 | 3j |
| Laptop B | 900€  | 5/10 | 7j |
| Laptop C | 1100€ | 9/10 | 2j |

Normalisation prix (inversé — min=900, max=1200) :
- Laptop A : 1 - (1200-900)/(1200-900) = **0.00**
- Laptop B : 1 - (900-900)/(1200-900)  = **1.00**
- Laptop C : 1 - (1100-900)/(1200-900) = **0.33**

Normalisation qualité (min=5, max=9) :
- Laptop A : (7-5)/(9-5) = **0.50**
- Laptop B : (5-5)/(9-5) = **0.00**
- Laptop C : (9-5)/(9-5) = **1.00**

Normalisation délai (inversé — min=2, max=7) :
- Laptop A : 1 - (3-2)/(7-2) = **0.80**
- Laptop B : 1 - (7-2)/(7-2) = **0.00**
- Laptop C : 1 - (2-2)/(7-2) = **1.00**

Scores finaux :
| Produit | Prix×0.5 | Qualité×0.3 | Délai×0.2 | **Score** |
|---------|----------|-------------|-----------|-----------|
| Laptop A | 0.00 | 0.15 | 0.16 | **0.310** |
| Laptop B | 0.50 | 0.00 | 0.00 | **0.500** |
| Laptop C | 0.165 | 0.30 | 0.20 | **0.667** |

→ **Laptop C gagne** malgré un prix intermédiaire, grâce à sa qualité maximale et son délai minimal.

---

### 2.3 `SellerAgent.java`

Agent passif — il attend des `REQUEST` et répond avec son offre.

```java
// Encode l'offre en String CSV pour le transport ACL
reply.setContent(offer.getName() + ";" + offer.getPrice()
        + ";" + offer.getQuality() + ";" + offer.getDeliveryDays());
```

**Pourquoi du texte et pas `setContentObject()` ?**
L'offre est envoyée en réponse à un agent qui vient d'arriver par migration — utiliser du texte simple évite des complications de classpath entre containers. Le buyer décode la String à la réception.

---

### 2.4 `MobileBuyerAgent.java`

C'est le cœur de la partie 2. Il combine **mobilité** et **intelligence décisionnelle**.

#### État persistant (survit aux migrations)

```java
private ArrayList<String>   sellerNames;   // noms des sellers à visiter
private ArrayList<Location> destinations;  // containers à visiter
private ArrayList<Product>  offers;        // offres collectées (grandit à chaque stop)
private int currentIndex;                  // position dans le plan de voyage
```

Ces champs sont tous `Serializable` — c'est obligatoire pour que JADE puisse les transporter lors de la migration.

#### Hooks de migration : `beforeMove()` et `afterMove()`

```java
@Override
protected void beforeMove() {
    // Appelé automatiquement par JADE juste avant la migration
    // Utile pour sauvegarder l'état, fermer des ressources
}

@Override
protected void afterMove() {
    // Appelé automatiquement par JADE juste après l'arrivée
    // On re-ajoute le TravelBehaviour car les behaviours ne migrent pas
    addBehaviour(new TravelBehaviour());
}
```

**Pourquoi re-ajouter le behaviour dans `afterMove()` ?**
Les `Behaviour` ne sont PAS sérialisés lors d'une migration JADE — seul l'état des champs de l'agent migre. Sans `afterMove()`, l'agent arriverait sans comportement et resterait inactif.

#### Séquence d'exécution sur chaque container

Le `TravelBehaviour` est un `OneShotBehaviour` — il s'exécute une fois puis se termine :

```
Sur Main-Container :
  setup() → TravelBehaviour → query Seller1 → doMove(Container2)

Sur Container2 :
  afterMove() → TravelBehaviour → query Seller2 → doMove(Container3)

Sur Container3 :
  afterMove() → TravelBehaviour → query Seller3 → doMove(Main-Container)

Sur Main-Container (retour) :
  afterMove() → TravelBehaviour → currentIndex == size → decide()
```

#### Migration : `doMove(Location)`

```java
ContainerID loc2 = new ContainerID("Container2", null);
doMove(loc2);
```

`doMove()` est une méthode JADE qui déclenche la migration. Le `null` signifie que le container est sur la même plateforme (même JVM) — c'est la **migration inter-container**.

---

### 2.5 `PlatformLauncher.java`

Crée la topologie complète : 1 plateforme JADE, 3 containers, 3 sellers, 1 buyer mobile.

```
Plateforme JADE (port 1100)
├── Main-Container  ← Seller1 (Laptop A) + MobileBuyer (départ)
├── Container2      ← Seller2 (Laptop B)
└── Container3      ← Seller3 (Laptop C)
```

**Pourquoi le port 1100 et pas 1099 ?**
Le port 1099 est le port JADE par défaut — si une autre instance JADE tourne (par ex. la Partie 1), il est déjà occupé. On utilise 1100 pour éviter tout conflit.

**Pourquoi `Thread.sleep(500)` entre le main container et les sous-containers ?**
JADE a besoin que le main container soit complètement initialisé et enregistré avant que les sous-containers essaient de s'y connecter. Sans cette pause, la connexion échoue.

**Pourquoi `Thread.sleep(1000)` avant de lancer le buyer ?**
Les 3 sellers doivent être enregistrés dans leur container respectif avant que le buyer leur envoie son `REQUEST`. Sans cette pause, le premier `REQUEST` (à Seller1) partirait avant que Seller1 soit prêt → timeout.

---

## 3. Plan de migration — Diagramme

```
Main-Container          Container2              Container3
      |                      |                       |
[MobileBuyer démarré]        |                       |
      |                      |                       |
  query Seller1              |                       |
  <-- offre Laptop A         |                       |
      |                      |                       |
  doMove(Container2) ------->|                       |
      |              [arrivée + afterMove()]          |
      |                  query Seller2               |
      |                  <-- offre Laptop B          |
      |                      |                       |
      |              doMove(Container3) ------------>|
      |                      |             [arrivée + afterMove()]
      |                      |                  query Seller3
      |                      |                  <-- offre Laptop C
      |                      |                       |
      |<----- doMove(Main-Container) ----------------|
[arrivée + afterMove()]      |                       |
  DecisionEngine.getBest()   |                       |
  --> BEST OFFER: Laptop C   |                       |
```

---

## 4. Guide de test — Exécution pas à pas

### Prérequis

- Fermer toute GUI JADE ouverte (Partie 1) — le port doit être libre
- Se placer à la racine du projet :
  ```powershell
  cd D:\TP_Tech_Agent\Projet
  ```

### Étape 1 — Compilation

```powershell
javac -encoding UTF-8 -cp "lib\jade.jar" -d "part2-mobile-agents\out" "part2-mobile-agents\src\Product.java" "part2-mobile-agents\src\DecisionEngine.java" "part2-mobile-agents\src\SellerAgent.java" "part2-mobile-agents\src\MobileBuyerAgent.java" "part2-mobile-agents\src\PlatformLauncher.java"
```

> Note : `-encoding UTF-8` est nécessaire sur Windows pour éviter les erreurs d'encodage.

### Étape 2 — Lancement

```powershell
java -cp "lib\jade.jar;part2-mobile-agents\out" part2.PlatformLauncher
```

La GUI JADE s'ouvre sur le port 1100. On y voit les 3 containers avec leurs sellers. Le `MobileBuyer` apparaît d'abord dans `Main-Container`, puis disparaît et réapparaît dans `Container2`, puis `Container3`, puis revient dans `Main-Container`.

### Étape 3 — Sortie console observée

```
[Seller1] Ready — offer: Laptop A [price=1200.0, quality=7.0, delivery=3d]
[Seller2] Ready — offer: Laptop B [price=900.0, quality=5.0, delivery=7d]
[Seller3] Ready — offer: Laptop C [price=1100.0, quality=9.0, delivery=2d]

[MobileBuyer] Started on Main-Container — 3 stops planned
[MobileBuyer] Querying Seller1 on Main-Container...
[Seller1] Request received from MobileBuyer — sending offer
[MobileBuyer] Offer collected: Laptop A [price=1200.0, quality=7.0, delivery=3d]
[MobileBuyer] Migrating to Container2...
[MobileBuyer] Leaving Main-Container
[MobileBuyer] Arrived on Container2
[MobileBuyer] Querying Seller2 on Container2...
[Seller2] Request received from MobileBuyer — sending offer
[MobileBuyer] Offer collected: Laptop B [price=900.0, quality=5.0, delivery=7d]
[MobileBuyer] Migrating to Container3...
[MobileBuyer] Leaving Container2
[MobileBuyer] Arrived on Container3
[MobileBuyer] Querying Seller3 on Container3...
[Seller3] Request received from MobileBuyer — sending offer
[MobileBuyer] Offer collected: Laptop C [price=1100.0, quality=9.0, delivery=2d]
[MobileBuyer] Migrating to Main-Container...
[MobileBuyer] Leaving Container3
[MobileBuyer] Arrived on Main-Container

[MobileBuyer] All stops visited — 3 offer(s) collected.
[MobileBuyer] Running multi-criteria decision...

[DecisionEngine] Laptop A             → price=0.00  quality=0.50  delivery=0.80  SCORE=0.310
[DecisionEngine] Laptop B             → price=1.00  quality=0.00  delivery=0.00  SCORE=0.500
[DecisionEngine] Laptop C             → price=0.33  quality=1.00  delivery=1.00  SCORE=0.667

[MobileBuyer] ==========================================
[MobileBuyer]  BEST OFFER : Laptop C [price=1100.0, quality=9.0, delivery=2d]
[MobileBuyer] ==========================================
```

### Étape 4 — Conformité aux résultats attendus

| Point de vérification | Résultat observé | Conforme |
|-----------------------|-----------------|---------|
| Buyer démarre sur Main-Container | `Started on Main-Container` | ✅ |
| Migration Main-Container → Container2 | `Arrived on Container2` | ✅ |
| Migration Container2 → Container3 | `Arrived on Container3` | ✅ |
| Retour Container3 → Main-Container | `Arrived on Main-Container` | ✅ |
| État préservé après migrations (3 offres) | `3 offer(s) collected` | ✅ |
| Score Laptop A = 0.310 | `SCORE=0.310` | ✅ |
| Score Laptop B = 0.500 | `SCORE=0.500` | ✅ |
| Score Laptop C = 0.667 | `SCORE=0.667` | ✅ |
| Laptop C sélectionné | `BEST OFFER : Laptop C` | ✅ |

### Étape 5 — Scénarios alternatifs

**Modifier les poids de décision** dans `DecisionEngine.java` :
```java
// Priorité absolue au prix
private static final double W_PRICE    = 0.8;
private static final double W_QUALITY  = 0.1;
private static final double W_DELIVERY = 0.1;
```
→ Laptop B (900€) devrait gagner.

**Ajouter un 4ème seller** dans `PlatformLauncher.java` :
```java
// Sur Container2 (aux côtés de Seller2)
AgentController s4 = container2.createNewAgent(
    "Seller4", "part2.SellerAgent",
    new Object[]{ "Laptop D", "1050.0", "8.0", "1" });
s4.start();
```
Et ajouter `"Seller4", loc3` dans les args du buyer.

---

## 5. Points clés pour la présentation

| Concept | Où le voir dans le code |
|---------|------------------------|
| Migration inter-container | `doMove(new ContainerID("Container2", null))` dans `TravelBehaviour` |
| État persistant après migration | Champs `ArrayList` dans `MobileBuyerAgent` — tous `Serializable` |
| Re-initialisation après arrivée | `afterMove()` → `addBehaviour(new TravelBehaviour())` |
| Hooks de migration | `beforeMove()` et `afterMove()` dans `MobileBuyerAgent` |
| Normalisation min-max | `normalise()` et `normaliseInverted()` dans `DecisionEngine` |
| Décision multi-critères | `getBest()` — somme pondérée sur offres normalisées |
| Topologie multi-containers | 3 `AgentContainer` créés en une JVM dans `PlatformLauncher` |
