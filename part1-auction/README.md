# Partie 1 — Négociation Multi-Agents (Enchères)

## Scénario
- 1 `SellerAgent` met un produit aux enchères avec un prix de départ et un prix de réserve (secret)
- N `BuyerAgent` (min 2) enchérissent en proposant un prix supérieur au prix courant
- Le vendeur diffuse la meilleure offre à chaque round
- L'enchère s'arrête quand tous les acheteurs abandonnent ou que le temps est écoulé
- Si le prix final dépasse le prix de réserve → vente conclue

## Classes
| Fichier | Rôle |
|---------|------|
| `Product.java` | Objet sérialisable (nom, prix départ, prix réserve) |
| `SellerAgent.java` | Gère les rounds d'enchères, CFP/PROPOSE/ACCEPT |
| `BuyerAgent.java` | Stratégie d'enchère avec budget max |
| `AuctionLauncher.java` | Lance tous les agents |

## Lancement
```bash
java -cp ../../lib/jade.jar:out jade.Boot -gui -agents "launcher:part1.AuctionLauncher"
```
