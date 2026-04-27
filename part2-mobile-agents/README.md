# Partie 2 — Agents Mobiles + Décision Multi-Critères

## Scénario
- 1 `MobileBuyerAgent` visite plusieurs `SellerAgent` pour trouver le meilleur produit
- Critères de décision : prix, qualité, délai de livraison (somme pondérée)
- Deux cas de migration :
  - **Inter-container** : même plateforme JADE, containers différents
  - **Inter-platform** : deux plateformes JADE distinctes

## Classes
| Fichier | Rôle |
|---------|------|
| `MobileBuyerAgent.java` | Agent mobile, visite les vendeurs, décide via multi-critères |
| `SellerAgent.java` | Répond aux requêtes avec ses offres produits |
| `DecisionEngine.java` | Calcul de score multi-critères (somme pondérée) |
| `PlatformLauncher.java` | Lance les deux containers/plateformes |

## Lancement
```bash
# Container principal
java -cp ../../lib/jade.jar:out jade.Boot -gui -port 1099 -agents "launcher:part2.PlatformLauncher"
# Container secondaire (inter-platform)
java -cp ../../lib/jade.jar:out jade.Boot -container -host localhost -port 1099
```
