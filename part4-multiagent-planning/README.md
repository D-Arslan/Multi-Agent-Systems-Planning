# Partie 4 — Planification Multi-Agents Centralisée

## Scénario
Implémentation du chapitre 3 MAS : **Centralized planning for distributed plans**.
- 1 `CentralPlannerAgent` reçoit le problème global, génère les sous-plans
- N `ExecutorAgent` reçoivent chacun leur sous-plan et l'exécutent
- Coordination via messages ACL

## Classes
| Fichier | Rôle |
|---------|------|
| `PlanAction.java` | Représentation d'une action (nom, préconditions, effets) |
| `CentralPlannerAgent.java` | Décompose le problème, distribue les sous-plans |
| `ExecutorAgent.java` | Reçoit et exécute son sous-plan, rapporte l'état |
| `PlanningLauncher.java` | Lance tous les agents |

## Lancement
```bash
java -cp ../../lib/jade.jar:out jade.Boot -gui -agents "launcher:part4.PlanningLauncher"
```
