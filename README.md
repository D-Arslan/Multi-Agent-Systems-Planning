# Multi-Agent Systems & Planning
**USTHB — M1 SII S2 — Module : Agents Technology — 2025/2026**

## Structure

| Dossier | Contenu |
|---------|---------|
| `part1-auction/` | Négociation multi-agents — protocole d'enchères (1 vendeur, N acheteurs) |
| `part2-mobile-agents/` | Agents mobiles + décision multi-critères (1 acheteur, N vendeurs) |
| `part3-aima-planning/` | Test du planificateur AIMA (Python/Jupyter) |
| `part4-multiagent-planning/` | Planification centralisée pour plans distribués |
| `lib/` | `jade.jar` partagé |

## Prérequis

- Java 8+
- JADE 4.x (`lib/jade.jar`)
- Python 3.x + Jupyter (pour la partie 3)

## Compilation (parties Java)

```bash
javac -cp lib/jade.jar part1-auction/src/*.java -d part1-auction/out/
```

## Lancement JADE

```bash
java -cp lib/jade.jar:part1-auction/out jade.Boot -gui -agents "launcher:part1.AuctionLauncher"
```
