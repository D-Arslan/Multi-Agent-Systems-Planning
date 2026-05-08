# Partie 3 — Planificateur AIMA : Partial Order Planner (POP)

## Objectif

Implémenter et tester le **Partial Order Planner (POP)** décrit au Chapitre 11 de *Artificial Intelligence: A Modern Approach* (Russell & Norvig).

## Fichiers

```
part3-aima-planning/
├── notebook/
│   ├── planning_partial_order_planner.ipynb   ← notebook exécuté (outputs inclus)
│   ├── plan_socks_shoes.png                   ← visualisation exemple 1
│   └── plan_spare_tire.png                    ← visualisation exemple 2
├── pop_test.py                                ← script Python standalone (tests)
└── README.md
```

## Prérequis

```powershell
pip install notebook networkx matplotlib
```

## Lancement

```powershell
cd D:\TP_Tech_Agent\Projet\part3-aima-planning\notebook
jupyter notebook planning_partial_order_planner.ipynb
```

---

## Contenu du notebook

### Algorithme POP implémenté from scratch

Le notebook implémente le POP en Python pur.

**Structures de données :**

| Classe | Rôle |
|--------|------|
| `Step` | Représente une action (préconditions, effets+, effets−) |
| `CausalLink` | Lien `producteur -[condition]→ consommateur` |
| Plan | `dict` avec `steps`, `orderings`, `causal_links` |

**Algorithme :**

```
POP(état_initial, but, actions) :
  plan = {Start, Finish}
  agenda = {(p, Finish) | p ∈ but}

  tant que agenda ≠ ∅ :
    (cond, consommateur) ← agenda.pop()
    fournisseur ← step existant ou nouvelle action qui satisfait cond
    ajouter lien causal : fournisseur -[cond]→ consommateur
    ajouter ordonnancement : fournisseur < consommateur
    pour chaque menace détectée :
      résoudre par promotion (menaçant < fournisseur)
      ou par dégradation  (consommateur < menaçant)
  retourner plan
```

---

## Résultats

### Exemple 1 — Chaussettes et Chaussures

- **État initial** : ∅
- **But** : `RightShoeOn ∧ LeftShoeOn`

**Plan obtenu :**

```
Start
  ├── RightSock ──[RightSockOn]──► RightShoe ──[RightShoeOn]──► Finish
  └── LeftSock  ──[LeftSockOn] ──► LeftShoe  ──[LeftShoeOn] ──► Finish
```

| Niveau | Actions parallèles |
|--------|-------------------|
| 0 | Start |
| 1 | RightSock, LeftSock *(simultanées)* |
| 2 | RightShoe, LeftShoe *(simultanées)* |
| 3 | Finish |

### Exemple 2 — Pneu de secours

- **État initial** : `At(Flat,Axle) ∧ At(Spare,Trunk)`
- **But** : `At(Spare,Axle) ∧ At(Flat,Ground)`

**Plan obtenu :**

```
Start
  ├── Remove(Flat,Axle)   ──────────────────────────────────────► Finish
  └── Remove(Spare,Trunk) ──► PutOn(Spare,Axle) ────────────────► Finish
```

| Niveau | Actions parallèles |
|--------|-------------------|
| 0 | Start |
| 1 | Remove(Flat,Axle), Remove(Spare,Trunk) *(simultanées)* |
| 2 | PutOn(Spare,Axle) |
| 3 | Finish |

---

## Concepts clés démontrés

| Concept | Résultat |
|---------|----------|
| **Liens causaux** | Chaque précondition satisfaite = 1 lien causal tracé |
| **Ordre partiel** | Seules les contraintes nécessaires sont imposées |
| **Parallélisme** | Deux actions sans contrainte d'ordre s'exécutent simultanément |
| **Résolution de menaces** | Promotion ou dégradation pour protéger les liens causaux |
| **Application SMA** | Le plan à ordre partiel est directement distribuable entre agents (cf. Partie 4) |
