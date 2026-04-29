# Résultats Attendus — Base de Comparaison

> Ce document définit exactement ce qu'on doit observer à l'exécution de chaque partie.
> Il servira de référence lors du bilan final : on compare ce qu'on obtient à ce qui est attendu ici.

---

## Partie 1 — Négociation / Enchères

### Scénario de test de référence
- **Produit** : "Laptop"
- **Prix de départ** : 1000 €
- **Prix de réserve** : 1300 € (secret, connu seulement du seller)
- **Buyer1** : budget max = 1500 €, incrément = 50 €
- **Buyer2** : budget max = 1200 €, incrément = 50 €
- **Buyer3** : budget max = 900 € (sous le prix de départ — doit refuser immédiatement)

---

### Résultat attendu — Cas 1 : Vente conclue

**Déroulement attendu des rounds :**

| Round | Prix diffusé | Buyer1 | Buyer2 | Buyer3 |
|-------|-------------|--------|--------|--------|
| 1 | 1000 € | PROPOSE 1050 € | PROPOSE 1050 € | REFUSE (budget < prix) |
| 2 | 1050 € | PROPOSE 1100 € | PROPOSE 1100 € | — (déjà sorti) |
| 3 | 1100 € | PROPOSE 1150 € | PROPOSE 1150 € | — |
| 4 | 1150 € | PROPOSE 1200 € | PROPOSE 1200 € | — |
| 5 | 1200 € | PROPOSE 1250 € | REFUSE (budget atteint) | — |
| 6 | 1250 € | PROPOSE 1300 € | — | — |
| 7 | 1300 € | REFUSE (ou dernier) | — | — |

**Sorties console attendues :**
```
[SellerAgent] Produit mis aux enchères : Laptop — Prix de départ : 1000.0 €
[SellerAgent] Round 1 — Prix courant : 1000.0 € — Envoi CFP à 3 acheteurs
[Buyer3] Budget insuffisant (900.0 < 1000.0) — REFUSE
[Buyer1] Offre : 1050.0 €
[Buyer2] Offre : 1050.0 €
[SellerAgent] Meilleure offre du round : 1050.0 € par Buyer1 (ex aequo — premier reçu)
...
[SellerAgent] Round 5 — Prix courant : 1200.0 €
[Buyer2] Budget max atteint — REFUSE
[Buyer1] Offre : 1250.0 €
[SellerAgent] Plus qu'un acheteur actif.
...
[SellerAgent] Enchère terminée — Dernier prix : 1300.0 €
[SellerAgent] Prix de réserve (1300.0 €) atteint — VENTE CONCLUE
[SellerAgent] Gagnant : Buyer1 — Prix final : 1300.0 €
[Buyer1] Félicitations ! Vous avez remporté Laptop pour 1300.0 €
[Buyer2] Enchère perdue. Gagnant : Buyer1 à 1300.0 €
```

**Points de vérification :**
- [ ] Buyer3 refuse dès le round 1 (budget < prix départ)
- [ ] Buyer2 abandonne quand le prix dépasse son budget max
- [ ] Le seller envoie le CFP uniquement aux buyers encore actifs
- [ ] Le prix final (1300 €) est >= prix de réserve (1300 €) → vente conclue
- [ ] Un seul gagnant désigné (Buyer1)
- [ ] Buyer2 et Buyer3 reçoivent REJECT_PROPOSAL

---

### Résultat attendu — Cas 2 : Vente non conclue (prix de réserve non atteint)

**Configuration :** Prix de réserve = 1800 € (très élevé), budgets Buyer1=1200€, Buyer2=1100€

**Déroulement attendu :**
- Les buyers enchérissent jusqu'à leurs budgets respectifs
- Buyer2 abandonne en premier, Buyer1 abandonne ensuite
- Dernier prix atteint : ~1200 € < 1800 € (prix de réserve)

**Sorties console attendues :**
```
[SellerAgent] Enchère terminée — Dernier prix : 1200.0 €
[SellerAgent] Prix de réserve (1800.0 €) non atteint — VENTE ANNULÉE
[SellerAgent] Aucun acheteur n'a remporté le produit.
[Buyer1] Enchère terminée sans vente.
[Buyer2] Enchère terminée sans vente.
```

**Points de vérification :**
- [ ] Le seller annonce explicitement que la vente est annulée
- [ ] Aucun ACCEPT_PROPOSAL n'est envoyé
- [ ] Les buyers sont informés de la fin sans gagnant

---

### Résultat attendu — Cas 3 : Timeout

**Configuration :** Délai max par round = 3 secondes, les buyers répondent lentement (simulé)

**Sorties console attendues :**
```
[SellerAgent] Round X — Timeout atteint — Clôture de l'enchère
[SellerAgent] Dernier prix valide retenu : XXX €
```

**Points de vérification :**
- [ ] Le seller ne se bloque pas indéfiniment en attente de réponses
- [ ] L'enchère se clôture proprement après le timeout

---

## Partie 2 — Agents Mobiles + Décision Multi-Critères

### Scénario de test de référence
**3 sellers avec les offres suivantes :**

| Seller | Produit | Prix (€) | Qualité (/10) | Délai (jours) |
|--------|---------|----------|---------------|---------------|
| Seller1 | Laptop A | 1200 | 7 | 3 |
| Seller2 | Laptop B | 900 | 5 | 7 |
| Seller3 | Laptop C | 1100 | 9 | 2 |

**Poids des critères :** Prix=0.5, Qualité=0.3, Délai=0.2

**Calcul du score attendu (normalisation min-max) :**

Prix normalisé (inversé — plus bas = mieux) :
- Seller1 : (1200-900)/(1200-900) → 0.0 (le plus cher)
- Seller2 : (1200-900)/(1200-900) → 1.0 (le moins cher)
- Seller3 : (1200-1100)/(1200-900) → 0.33

Qualité normalisée :
- Seller1 : (7-5)/(9-5) → 0.5
- Seller2 : (5-5)/(9-5) → 0.0
- Seller3 : (9-5)/(9-5) → 1.0

Délai normalisé (inversé — plus court = mieux) :
- Seller1 : (7-3)/(7-2) → 0.8... → inversé : 0.2
- Seller2 : (7-7)/(7-2) → 0.0 → inversé : 1.0 (non, c'est le plus long)

**Recalcul correct (inversé : score = 1 - normalisé) :**
- Seller1 délai : 1 - (3-2)/(7-2) = 1 - 0.2 = 0.8
- Seller2 délai : 1 - (7-2)/(7-2) = 1 - 1.0 = 0.0
- Seller3 délai : 1 - (2-2)/(7-2) = 1 - 0.0 = 1.0

**Scores finaux :**
| Seller | Prix×0.5 | Qualité×0.3 | Délai×0.2 | **Score total** |
|--------|----------|-------------|-----------|-----------------|
| Seller1 | 0.0×0.5=0.00 | 0.5×0.3=0.15 | 0.8×0.2=0.16 | **0.31** |
| Seller2 | 1.0×0.5=0.50 | 0.0×0.3=0.00 | 0.0×0.2=0.00 | **0.50** |
| Seller3 | 0.33×0.5=0.165 | 1.0×0.3=0.30 | 1.0×0.2=0.20 | **0.665** |

**→ Seller3 doit être sélectionné (score 0.665)**

---

### Résultat attendu — Cas 1 : Migration inter-container

**Sorties console attendues :**
```
[MobileBuyerAgent] Démarrage sur Container1
[MobileBuyerAgent] Visite de Seller1 sur Container1...
[MobileBuyerAgent] Offre reçue — Prix: 1200€, Qualité: 7, Délai: 3j
[MobileBuyerAgent] Migration vers Container2...
[MobileBuyerAgent] Arrivée sur Container2
[MobileBuyerAgent] Visite de Seller2 sur Container2...
[MobileBuyerAgent] Offre reçue — Prix: 900€, Qualité: 5, Délai: 7j
[MobileBuyerAgent] Visite de Seller3 sur Container2...
[MobileBuyerAgent] Offre reçue — Prix: 1100€, Qualité: 9, Délai: 2j
[MobileBuyerAgent] Retour sur Container1 pour calcul final...
[MobileBuyerAgent] === RÉSULTAT DÉCISION MULTI-CRITÈRES ===
[MobileBuyerAgent] Seller1 — Score : 0.31
[MobileBuyerAgent] Seller2 — Score : 0.50
[MobileBuyerAgent] Seller3 — Score : 0.665
[MobileBuyerAgent] MEILLEURE OFFRE : Seller3 — Laptop C — 1100€ (score: 0.665)
```

**Points de vérification :**
- [ ] L'agent migre bien entre les deux containers (visible dans la GUI JADE)
- [ ] Les offres collectées avant migration sont conservées après migration (état préservé)
- [ ] Seller3 est bien sélectionné comme meilleure offre
- [ ] Le score calculé correspond aux valeurs ci-dessus (±0.01)
- [ ] L'agent retourne sur le container d'origine après décision

---

### Résultat attendu — Cas 2 : Migration inter-platform

**Sorties console attendues :**
```
[MobileBuyerAgent] Migration inter-platform vers PlatformeB:1100/JADE...
[MobileBuyerAgent] Arrivée sur PlatformeB
...
[MobileBuyerAgent] Retour sur PlatformeA:1099/JADE
[MobileBuyerAgent] MEILLEURE OFFRE : Seller3 — Laptop C — 1100€
```

**Points de vérification :**
- [ ] La migration inter-platform réussit (pas d'exception `jade.core.mobility`)
- [ ] Les deux plateformes JADE sont bien démarrées et communiquent via MTP
- [ ] Le résultat final est identique au cas inter-container (même données → même décision)

---

## Partie 3 — Planificateur AIMA (Partial Order Planner)

### Résultat attendu — Exécution du notebook

**Toutes les cellules s'exécutent sans erreur.**

**Problème des chaussettes et chaussures — Plan partiel attendu :**
```
Actions dans le plan :
- Start
- EnfilerChaussetteDroite
- EnfilerChaussetteGauche
- EnfilerChaussureDroite   [précond : ChaussetteDroite-Enfilée]
- EnfilerChaussureGauche   [précond : ChaussetteGauche-Enfilée]
- Finish

Contraintes d'ordre (liens causaux) :
Start < EnfilerChaussetteDroite < EnfilerChaussureDroite < Finish
Start < EnfilerChaussetteGauche < EnfilerChaussureGauche < Finish

Contraintes NON imposées (ordre libre) :
EnfilerChaussetteDroite et EnfilerChaussetteGauche → pas d'ordre entre eux
EnfilerChaussureDroite et EnfilerChaussureGauche → pas d'ordre entre eux
```

**Ce que le notebook doit afficher :**
- Le plan sous forme de liste d'actions avec leurs liens causaux
- Un graphe visualisant l'ordre partiel (nœuds = actions, arêtes = contraintes d'ordre)
- Confirmation : "Plan found" ou équivalent

**Points de vérification :**
- [ ] Toutes les cellules s'exécutent sans `Exception` ni `Error`
- [ ] Le plan trouvé contient exactement 4 actions (hors Start/Finish)
- [ ] Les 2 liens causaux obligatoires sont présents (chaussette avant chaussure, pour chaque pied)
- [ ] Aucun ordre n'est imposé entre pied droit et pied gauche
- [ ] La visualisation graphique s'affiche correctement

---

## Partie 4 — Planification Centralisée pour Plans Distribués

### Scénario de test de référence
**Problème** : Construction d'une maison (inspiré chapitre 3 MAS)

**Actions globales :**
| Action | Préconditions | Effets | Assigné à |
|--------|---------------|--------|-----------|
| `poserFondations` | ∅ | `fondations_ok` | ExecutorA1 |
| `construireMurs` | `fondations_ok` | `murs_ok` | ExecutorA2 |
| `installerPlomberie` | `fondations_ok` | `plomberie_ok` | ExecutorA2 |
| `poserToit` | `murs_ok` | `toit_ok` | ExecutorA3 |
| `finitionsInternes` | `murs_ok`, `plomberie_ok` | `finitions_ok` | ExecutorA3 |

---

### Résultat attendu — Déroulement

**Sorties console attendues :**
```
[CentralPlannerAgent] Problème reçu — 5 actions à distribuer entre 3 executors
[CentralPlannerAgent] Distribution :
  → ExecutorA1 : [poserFondations]
  → ExecutorA2 : [construireMurs, installerPlomberie]
  → ExecutorA3 : [poserToit, finitionsInternes]
[CentralPlannerAgent] Plans distribués. Surveillance en cours...

[ExecutorA1] Sous-plan reçu : [poserFondations]
[ExecutorA1] Exécution de poserFondations... OK
[ExecutorA1] DONE:poserFondations — rapport envoyé au planificateur

[CentralPlannerAgent] DONE:poserFondations reçu — État global mis à jour : {fondations_ok}
[CentralPlannerAgent] Préconditions satisfaites pour ExecutorA2 — Notification envoyée

[ExecutorA2] Préconditions satisfaites — démarrage de construireMurs
[ExecutorA2] Exécution de construireMurs... OK
[ExecutorA2] DONE:construireMurs
[ExecutorA2] Exécution de installerPlomberie... OK
[ExecutorA2] DONE:installerPlomberie

[CentralPlannerAgent] État global mis à jour : {fondations_ok, murs_ok, plomberie_ok}
[CentralPlannerAgent] Préconditions satisfaites pour ExecutorA3 — Notification envoyée

[ExecutorA3] Exécution de poserToit... OK
[ExecutorA3] Exécution de finitionsInternes... OK
[ExecutorA3] DONE:finitionsInternes

[CentralPlannerAgent] Toutes les actions complétées — Plan global RÉUSSI
[CentralPlannerAgent] État final : {fondations_ok, murs_ok, plomberie_ok, toit_ok, finitions_ok}
```

**Points de vérification :**
- [ ] ExecutorA2 et ExecutorA3 ne démarrent PAS avant d'avoir reçu la notification du planner
- [ ] `construireMurs` et `installerPlomberie` peuvent s'exécuter en parallèle (même agent ici, mais l'ordre interne est libre)
- [ ] `poserToit` n'est exécuté qu'après `murs_ok` dans l'état global
- [ ] `finitionsInternes` n'est exécuté qu'après `murs_ok` ET `plomberie_ok`
- [ ] L'état final contient exactement les 5 effets attendus
- [ ] Le planner détecte la complétion globale et l'annonce

---

### Résultat attendu — Cas d'échec (robustesse)

**Configuration :** ExecutorA1 échoue à `poserFondations`

**Sorties console attendues :**
```
[ExecutorA1] Échec de poserFondations — FAILED:poserFondations
[CentralPlannerAgent] FAILED:poserFondations reçu
[CentralPlannerAgent] Préconditions de ExecutorA2 non satisfiables — Plan BLOQUÉ
[CentralPlannerAgent] Plan global ÉCHOUÉ — Cause : poserFondations non complété
```

**Points de vérification :**
- [ ] ExecutorA2 et ExecutorA3 ne démarrent pas si leurs préconditions ne sont jamais satisfaites
- [ ] Le planner détecte et annonce l'échec global
- [ ] Aucun agent ne reste bloqué indéfiniment (pas de deadlock)

---

## Résumé — Tableau de conformité (à remplir après implémentation)

| Partie | Cas de test | Résultat attendu | Résultat obtenu | Conforme ? |
|--------|-------------|-----------------|-----------------|------------|
| 1 | Vente conclue | Buyer1 gagne à 1300€ | | |
| 1 | Vente annulée | Aucun gagnant | | |
| 1 | Timeout | Clôture propre | | |
| 2 | Inter-container | Seller3 sélectionné (0.665) | | |
| 2 | Inter-platform | Seller3 sélectionné (0.665) | | |
| 3 | Notebook AIMA | Plan partiel correct, graphe affiché | | |
| 4 | Plan distribué | 5 actions complétées dans l'ordre | | |
| 4 | Cas d'échec | Planner détecte et annonce l'échec | | |
