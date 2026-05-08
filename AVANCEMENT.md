# Avancement du projet — Multi-Agent Systems & Planning

## Partie 1 — Négociation / Enchères (JADE)
**Assigné à : D-Arslan**

### À faire
- [ ] `Product.java` — classe sérialisable (nom, prixDepart, prixReserve)
- [ ] `SellerAgent.java` — diffuse CFP, collecte offres, gère les rounds, vérifie prix de réserve
- [ ] `BuyerAgent.java` — reçoit CFP, décide d'enchérir ou d'abandonner selon budget max
- [ ] `AuctionLauncher.java` — lance le seller + 2 buyers minimum
- [ ] Test : enchère avec 2 buyers, vérifier que la vente se conclut si prix > réserve
- [ ] Test : enchère avec tous les buyers qui abandonnent (pas de vente)

### Fait
- [x] Structure du dossier + README

---

## Partie 2 — Agents Mobiles + Décision Multi-Critères (JADE)
**Assigné à : Binôme**

### À faire
- [ ] `Product.java` — classe sérialisable avec critères (prix, qualité, délai)
- [ ] `SellerAgent.java` — répond aux requêtes avec ses offres
- [ ] `DecisionEngine.java` — calcul du score par somme pondérée
- [ ] `MobileBuyerAgent.java` — migration inter-container + logique de décision
- [ ] `PlatformLauncher.java` — lance le main container + containers secondaires
- [ ] Test cas 1 : migration inter-container (même plateforme JADE)
- [ ] Test cas 2 : migration inter-platform (deux plateformes JADE)

### Fait
- [x] Structure du dossier + README

---

## Partie 3 — Planificateur AIMA (Python)
**Assigné à : D-Arslan**

### À faire
*(rien)*

### Fait
- [x] Structure du dossier + README
- [x] Implémentation POP from scratch (`pop_test.py` + notebook)
- [x] Notebook exécuté avec outputs dans `notebook/planning_partial_order_planner.ipynb`
- [x] Figures générées : `plan_socks_shoes.png`, `plan_spare_tire.png`
- [x] Rapport des résultats rédigé dans le README

---

## Partie 4 — Planification Centralisée pour Plans Distribués (JADE)
**Assigné à : Binôme**

### À faire
- [ ] `PlanAction.java` — représentation d'une action (nom, préconditions, effets)
- [ ] `CentralPlannerAgent.java` — reçoit le problème, décompose, distribue les sous-plans
- [ ] `ExecutorAgent.java` — reçoit son sous-plan, exécute, rapporte l'état au planificateur
- [ ] `PlanningLauncher.java` — lance le planificateur + N executors
- [ ] Test : problème exemple du chapitre 3 MAS, vérifier distribution et exécution

### Fait
- [x] Structure du dossier + README

---

## Suivi global

| Partie | Assigné | Statut |
|--------|---------|--------|
| Part 1 — Enchères | D-Arslan | En cours |
| Part 2 — Agents mobiles | Binôme | Non démarré |
| Part 3 — AIMA Python | D-Arslan | **Terminé** |
| Part 4 — Planification centralisée | Binôme | Non démarré |
