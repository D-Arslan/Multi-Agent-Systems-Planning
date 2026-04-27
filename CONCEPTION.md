# Conception & Compréhension du projet — Multi-Agent Systems & Planning

---

## Concepts fondamentaux (base commune)

### Qu'est-ce qu'un agent ?
Un **agent** est une entité autonome qui :
- **Perçoit** son environnement (via des messages, des capteurs)
- **Décide** d'une action selon ses objectifs et son état interne
- **Agit** sur son environnement (envoie des messages, modifie des données)

### Qu'est-ce qu'un SMA (Système Multi-Agents) ?
Un SMA est un ensemble d'agents qui **coexistent et interagissent** dans un environnement partagé.
Les agents peuvent être :
- **Coopératifs** : travaillent ensemble vers un but commun
- **Compétitifs** : ont des objectifs opposés (ex. les acheteurs dans une enchère)
- **Mixtes** : coopèrent localement, compétitifs globalement

### JADE — Java Agent DEvelopment Framework
JADE est le framework qu'on utilise pour implémenter les SMA en Java.

**Concepts clés JADE :**

| Concept | Description |
|---------|-------------|
| `Agent` | Classe de base à étendre pour créer un agent |
| `Behaviour` | Unité de comportement d'un agent (ce qu'il fait) |
| `ACLMessage` | Message au format standard FIPA (performatif + contenu) |
| `AID` | Agent Identifier — l'adresse unique d'un agent |
| `Platform` | Conteneur d'exécution des agents |
| `Container` | Nœud d'une plateforme JADE |

**Cycle de vie d'un agent JADE :**
```
INITIATED → ACTIVE → SUSPENDED → WAITING → DELETED
```
Un agent démarre dans `setup()`, tourne ses `Behaviour`, et s'arrête dans `takeDown()`.

**Types de Behaviour :**

| Type | Usage |
|------|-------|
| `OneShotBehaviour` | S'exécute une seule fois |
| `CyclicBehaviour` | Boucle infinie (réception de messages) |
| `SimpleBehaviour` | Contrôle manuel via `done()` |
| `SequentialBehaviour` | Enchaîne des sous-comportements |
| `FSMBehaviour` | Machine à états finis |
| `TickerBehaviour` | S'exécute à intervalles réguliers |

**Performatifs ACL (protocoles de communication) :**

| Performatif | Signification |
|-------------|---------------|
| `CFP` | Call For Proposal — appel d'offres |
| `PROPOSE` | Offre en réponse à un CFP |
| `ACCEPT_PROPOSAL` | Acceptation d'une offre |
| `REJECT_PROPOSAL` | Refus d'une offre |
| `REFUSE` | Refus de participer |
| `INFORM` | Transmission d'information |
| `REQUEST` | Demande d'action |

---

## Partie 1 — Négociation Multi-Agents : Protocole d'Enchères

### Concept théorique : Négociation dans les SMA
La **négociation** est un mécanisme par lequel des agents avec des intérêts potentiellement conflictuels arrivent à un accord.

Il existe plusieurs protocoles :
- **Contract Net Protocol (CNP)** : un agent initiateur lance un appel d'offres, les autres soumissionnent
- **Enchères anglaises (English Auction)** : le prix monte, les acheteurs surenchérissent
- **Enchères hollandaises** : le prix descend, le premier acheteur qui accepte remporte
- **Enchères scellées** : chaque acheteur soumet une offre unique secrète

**On implémente ici une enchère anglaise** (prix monte par rounds).

### Protocole détaillé
```
Seller                          Buyer1          Buyer2
  |                               |               |
  |---- CFP (prix courant) ------>|               |
  |---- CFP (prix courant) ---------------------->|
  |                               |               |
  |<--- PROPOSE (offre) ----------|               |
  |<--- PROPOSE (offre) --------------------------|
  |                               |               |
  | [retient la meilleure offre]  |               |
  |                               |               |
  |---- CFP (nouveau prix) ------>|               |
  |---- CFP (nouveau prix) ---------------------->|
  |                               |               |
  |<--- REFUSE (abandon) ---------|               |
  |<--- PROPOSE (offre) --------------------------|
  |                               |               |
  | [tous abandonnés ou timeout]  |               |
  |                               |               |
  | [prix final > prix réserve ?] |               |
  |---- ACCEPT_PROPOSAL (gagnant) -------------->|
  |---- REJECT_PROPOSAL (perdant)|               |
```

### Choix d'implémentation

**`Product.java`**
- Implémente `Serializable` pour pouvoir être envoyé dans un `ACLMessage` via `setContentObject()`
- Contient : `name`, `startingPrice`, `reservePrice`
- `reservePrice` n'est connu que du `SellerAgent` — les buyers ne voient jamais ce champ

**`SellerAgent.java`**
- Utilise un `FSMBehaviour` (machine à états) avec les états :
  - `ANNOUNCE` → diffuse le CFP avec le prix courant
  - `COLLECT` → attend les réponses avec `TickerBehaviour` (timeout)
  - `EVALUATE` → retient la meilleure offre, passe au round suivant ou clôture
  - `CLOSE` → envoie ACCEPT/REJECT, affiche le résultat
- Maintient une liste des `AID` des buyers actifs (ceux qui n'ont pas encore REFUSE)
- Stocke le `currentHighestBid` et le `currentWinner`

**`BuyerAgent.java`**
- Reçoit le prix courant via CFP
- Décide selon sa stratégie :
  - Si `currentPrice < maxBudget` → envoie `PROPOSE` avec `currentPrice + increment`
  - Sinon → envoie `REFUSE`
- Le `maxBudget` est passé en argument de lancement (`agent.getArguments()`)
- L'`increment` peut être fixe ou aléatoire (pour simuler des comportements différents)

**`AuctionLauncher.java`**
- Crée les agents programmatiquement via `ContainerController.createNewAgent()`
- Lance d'abord les buyers (ils doivent être enregistrés avant que le seller envoie les CFP)
- Lance le seller en dernier avec un délai (`Thread.sleep`) si nécessaire

### Concepts clés à retenir pour la présentation
1. **Contract Net Protocol** : notre enchère est une variante du CNP où l'initiateur (seller) répète les rounds
2. **Prix de réserve** : information privée de l'agent — illustre le concept d'**information asymétrique** dans les SMA
3. **Abandon progressif** : les buyers quittent un à un → illustre la **dynamique** d'un groupe d'agents
4. **Timeout** : si aucun buyer ne répond dans le délai → le système ne se bloque pas (robustesse)

---

## Partie 2 — Agents Mobiles + Décision Multi-Critères

### Concept théorique : Agents Mobiles
Un **agent mobile** est un agent capable de **migrer** d'un container/plateforme à un autre en transportant son code et son état.

Avantages :
- Réduit le trafic réseau (l'agent va au données, pas l'inverse)
- Permet l'exécution distribuée sur plusieurs nœuds
- Utile pour les systèmes de collecte d'informations (shopping agents, monitoring)

Dans JADE, la migration se fait via `doMove(Location destination)`.

**Deux types de migration :**

| Type | Description | Complexité |
|------|-------------|------------|
| **Inter-container** | Même plateforme JADE, container différent | Simple — même AMS/DF |
| **Inter-platform** | Plateforme JADE différente (autre JVM/machine) | Complexe — AMS/DF différents, besoin de MTP |

### Concept théorique : Décision Multi-Critères
Un agent ne choisit pas toujours sur un seul critère (ex. le prix le plus bas). La **décision multi-critères** permet d'évaluer des alternatives selon plusieurs dimensions.

**Méthode implémentée : Somme Pondérée (Weighted Sum)**
```
Score(offre) = w1 * critère1_normalisé + w2 * critère2_normalisé + w3 * critère3_normalisé
```

Critères utilisés :

| Critère | Poids | Sens |
|---------|-------|------|
| Prix | 0.5 | Plus bas = mieux (inverser la normalisation) |
| Qualité | 0.3 | Plus haut = mieux |
| Délai de livraison | 0.2 | Plus court = mieux (inverser) |

La **normalisation** ramène chaque critère entre 0 et 1 pour les rendre comparables.

### Choix d'implémentation

**`MobileBuyerAgent.java`**
- Démarre sur le container principal
- Maintient une liste d'`Location` à visiter (les containers des sellers)
- À chaque seller visité :
  1. Envoie un `REQUEST` pour obtenir l'offre
  2. Reçoit un `INFORM` avec les détails (prix, qualité, délai)
  3. Stocke l'offre dans son état interne (`List<Offer>`)
  4. Migre vers le container suivant (`doMove()`)
- Après avoir visité tous les sellers : appelle `DecisionEngine.getBest(offers)`
- Revient sur le container principal pour afficher le résultat

**État transporté lors de la migration :**
- La liste des offres collectées (`ArrayList<Offer>` — doit être `Serializable`)
- L'index du seller courant
- Le container de retour (pour savoir où rentrer)

**`DecisionEngine.java`**
- Classe utilitaire (pas un agent) — appelée par `MobileBuyerAgent`
- `normalize(offers)` : normalise chaque critère sur [0,1]
- `score(offer, weights)` : calcule le score pondéré
- `getBest(offers)` : retourne l'offre avec le meilleur score

**`PlatformLauncher.java` (inter-container)**
- Crée 2 containers dans la même JVM : `Container1` (sellers 1, 2) et `Container2` (seller 3)
- L'acheteur démarre sur `Container1`, migre vers `Container2`, revient sur `Container1`

**Configuration inter-platform :**
- Plateforme A : port 1099 (main), contient seller1 et seller2
- Plateforme B : port 1100 (main), contient seller3
- Le `MTP (Message Transport Protocol)` de JADE gère la communication inter-plateforme

### Concepts clés à retenir pour la présentation
1. **Mobilité forte vs faible** : JADE implémente la mobilité forte (code + état migrent ensemble)
2. **`beforeMove()` / `afterMove()`** : hooks JADE exécutés avant/après migration — utile pour sauvegarder l'état
3. **Serialisation obligatoire** : tout l'état de l'agent doit être `Serializable` pour migrer
4. **Décision multi-critères** : reflète le comportement d'un acheteur rationnel qui ne se limite pas au prix

---

## Partie 3 — Planificateur AIMA (Partial Order Planner)

### Concept théorique : Planification en IA
La **planification** consiste à trouver une séquence d'actions qui mène d'un état initial à un état but.

**Planification par ordre total** : les actions sont totalement ordonnées (A avant B avant C)
**Planification par ordre partiel (POP)** : on n'ordonne les actions que si nécessaire — plus flexible

### Partial Order Planner (POP)
Le POP construit un plan en résolvant des **flaws** (défauts) :

| Type de flaw | Description | Résolution |
|--------------|-------------|------------|
| **Open precondition** | Une précondition n'est satisfaite par aucune action | Ajouter une action qui produit cet effet |
| **Threat** | Une action peut détruire un lien causal établi | Promotion ou dégradation (réordonnancement) |

**Algorithme POP :**
```
1. Plan initial : {Start, Finish}
2. Choisir un flaw (open precondition ou threat)
3. Résoudre le flaw
4. Répéter jusqu'à ce qu'il n'y ait plus de flaws → plan trouvé
   ou jusqu'à ce qu'il soit impossible de résoudre → échec
```

**Exemple classique (chaussettes et chaussures) :**
```
But : ChaussetteD-Enfilée ∧ ChaussureD-Enfilée ∧ ChaussetteG-Enfilée ∧ ChaussureG-Enfilée

Actions disponibles :
- EnfilerChaussette(x) : précond=∅, effet=Chaussette(x)-Enfilée
- EnfilerChaussure(x)  : précond=Chaussette(x)-Enfilée, effet=Chaussure(x)-Enfilée

Plan partiel résultant :
EnfilerChaussette(D) < EnfilerChaussure(D)
EnfilerChaussette(G) < EnfilerChaussure(G)
(pas d'ordre imposé entre D et G)
```

### Ce que fait le notebook AIMA
1. Définit la représentation d'un problème de planification (actions, préconditions, effets)
2. Implémente l'algorithme POP
3. Résout l'exemple chaussettes/chaussures
4. Visualise le plan partiel sous forme de graphe

### Concepts clés à retenir pour la présentation
1. **Liens causaux** : `A -[p]→ B` signifie "A est là pour satisfaire la précondition p de B"
2. **Ordre partiel** : on n'impose un ordre entre deux actions que si c'est strictement nécessaire
3. **Avantage du POP** : génère des plans plus flexibles, parallélisables — essentiel pour les SMA

---

## Partie 4 — Planification Centralisée pour Plans Distribués

### Concept théorique : Planification dans les SMA
Dans un SMA, la planification peut être :

| Approche | Description | Avantages | Inconvénients |
|----------|-------------|-----------|---------------|
| **Centralisée** | Un agent planifie pour tous | Cohérence globale, facile à raisonner | Goulot d'étranglement, point de défaillance unique |
| **Distribuée** | Chaque agent planifie localement | Scalable, robuste | Conflits possibles, coordination difficile |
| **Centralisée pour plans distribués** | Un agent planifie, N agents exécutent | Meilleur des deux mondes | Communication requise |

**On implémente la 3ème approche** (chapitre 3 MAS).

### Architecture détaillée

```
                    ┌─────────────────────────┐
                    │   CentralPlannerAgent    │
                    │                          │
                    │  - Reçoit le problème    │
                    │  - Décompose en sous-    │
                    │    plans (par agent)     │
                    │  - Distribue les plans   │
                    │  - Surveille l'exécution │
                    └───────────┬─────────────┘
                                │ INFORM (sous-plan)
              ┌─────────────────┼──────────────────┐
              ▼                 ▼                   ▼
    ┌──────────────┐  ┌──────────────┐   ┌──────────────┐
    │ ExecutorAgent│  │ ExecutorAgent│   │ ExecutorAgent│
    │      A1      │  │      A2      │   │      A3      │
    │              │  │              │   │              │
    │ - Reçoit son │  │ - Reçoit son │   │ - Reçoit son │
    │   sous-plan  │  │   sous-plan  │   │   sous-plan  │
    │ - Exécute    │  │ - Exécute    │   │ - Exécute    │
    │ - Rapporte   │  │ - Rapporte   │   │ - Rapporte   │
    └──────┬───────┘  └──────┬───────┘   └──────┬───────┘
           │                 │                   │
           └─────────────────┴───────────────────┘
                    INFORM (status: DONE/FAILED)
                             │
                    ┌────────▼────────┐
                    │ CentralPlanner  │
                    │ (collecte les   │
                    │  confirmations) │
                    └─────────────────┘
```

### Exemple concret implémenté
**Problème** : Construire une maison
- Agent A1 : poser les fondations
- Agent A2 : construire les murs (nécessite fondations faites)
- Agent A3 : poser le toit (nécessite murs faits)

**Représentation d'un sous-plan :**
```java
// PlanAction.java
PlanAction a = new PlanAction(
    "poserFondations",                    // nom
    List.of(),                            // préconditions
    List.of("fondations_posées")          // effets
);
```

**Protocole de coordination :**
1. `CentralPlannerAgent` envoie `INFORM` avec la liste des `PlanAction` à chaque executor
2. Chaque `ExecutorAgent` vérifie ses préconditions (demande au planner si satisfaites)
3. Exécute ses actions dans l'ordre
4. Envoie `INFORM "DONE:nomAction"` au planner après chaque action
5. Le planner met à jour l'état global et notifie les agents dont les préconditions sont maintenant satisfaites

### Choix d'implémentation

**`PlanAction.java`**
- Implémente `Serializable`
- Champs : `name`, `preconditions (List<String>)`, `effects (List<String>)`
- Méthode `isPreconditionSatisfied(Set<String> worldState)` : vérifie si le monde courant satisfait les préconditions

**`CentralPlannerAgent.java`**
- Maintient un `Set<String> globalState` (faits vrais dans le monde)
- Maintient un `Map<AID, List<PlanAction>> assignments` (qui fait quoi)
- Comportement en deux phases :
  1. `PlanningBehaviour` : décompose et distribue (OneShotBehaviour)
  2. `MonitorBehaviour` : écoute les DONE/FAILED et met à jour globalState (CyclicBehaviour)
- Notifie les executors bloqués quand leurs préconditions deviennent satisfaites

**`ExecutorAgent.java`**
- Reçoit son sous-plan sous forme de liste sérialisée via `getContentObject()`
- `ExecutePlanBehaviour` : itère sur les actions, vérifie les préconditions, exécute (simule avec un `Thread.sleep` + affichage), rapporte

### Concepts clés à retenir pour la présentation
1. **Décomposition de tâches** : le planner central fait du *task decomposition* — concept fondamental en IA distribuée
2. **Dépendances inter-agents** : A2 dépend de A1 → illustre les **contraintes de précédence** entre agents
3. **État du monde partagé** : le planner maintient une vue globale que les executors n'ont pas — **information centralisée vs distribuée**
4. **Synchronisation** : le planner joue le rôle de **barrière de synchronisation** entre les agents

---

## Comparaison globale des 4 parties

| | Partie 1 | Partie 2 | Partie 3 | Partie 4 |
|--|---------|---------|---------|---------|
| **Type d'agent** | Réactif | Mobile | — (théorie) | Délibératif |
| **Interaction** | Compétitive | Coopérative | — | Coopérative |
| **Protocole** | CNP / Enchères | Migration JADE | POP | Plan distribué |
| **Difficulté** | Moyenne | Élevée | Faible | Élevée |
| **Concept clé** | Négociation | Mobilité | Planification PO | Coordination |

---

## Lexique rapide (pour la présentation)

| Terme | Définition courte |
|-------|------------------|
| **ACL** | Agent Communication Language — langage standard de communication entre agents |
| **FIPA** | Foundation for Intelligent Physical Agents — organisme de standardisation des SMA |
| **AMS** | Agent Management System — annuaire des agents actifs dans JADE |
| **DF** | Directory Facilitator — pages jaunes des services dans JADE |
| **MTP** | Message Transport Protocol — protocole de transport inter-plateformes JADE |
| **CNP** | Contract Net Protocol — protocole d'enchères/appels d'offres |
| **POP** | Partial Order Planner — planificateur à ordre partiel |
| **Flaw** | Défaut dans un plan partiel (précondition ouverte ou menace) |
| **Lien causal** | Relation "action A satisfait précondition p de action B" |
