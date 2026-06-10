# BE Graphes &mdash; INSA Toulouse 3MIC S2

Copie locale du [dépôt de Mikael CAPELLE](https://gitea.typename.fr/INSA/be-graphes) dans le cadre du Bureau d'Études Graphes de 3ème année (filière MIC, semestre 2).

---


## Structure du projet

Le projet est organisé en trois modules Maven :

| Module | Rôle |
|---|---|
| `be-graphes-model` | Modèle de graphe (nœuds, arcs, chemins, lecture/écriture de cartes) |
| `be-graphes-algos` | Algorithmes de plus court chemin et structures de données |
| `be-graphes-gui` | Interface graphique de visualisation |

---


## Ce qui a été implémenté

### 1. `Path`

Dans `be-graphes-model`, les méthodes suivantes ont été implémentées :

- `getLength()` — calcule la longueur totale du chemin (somme des longueurs des arcs)
- `isValid()` — vérifie que le chemin est valide (continuité des arcs)
- `getTravelTime()` / `getMinimumTravelTime()` — calcule le temps de parcours
- `createShortestPathFromNodes()` — construit le chemin le plus court à partir d'une liste de nœuds
- `createFastestPathFromNodes()` — construit le chemin le plus rapide à partir d'une liste de nœuds

### 2. `BinaryHeap`

Dans `be-graphes-algos`, la méthode `remove()` du tas binaire générique a été implémentée.

### 3. `Label`

Classe créée entièrement pour représenter l'état d'un nœud pendant la recherche de plus court chemin :

- Stocke le coût `g` (coût réel depuis l'origine), le coût heuristique `h`, le nœud courant, l'arc prédécesseur et l'état de marquage
- `getTotalCost()` — retourne `f = g + h` utilisé pour l'ordonnancement dans le tas
- `compareTo()` — compare les labels par coût total, avec départage sur l'heuristique

### 4. `DijkstraAlgorithm`

Implémentation de l'algorithme de Dijkstra :

- `doRun()` — algorithme complet : initialisation des labels, boucle de relaxation avec tas binaire, reconstruction du chemin
- `newLabel()` — méthode fabrique surchargeable par A*
- Support des **nœuds interdits** (`boolean[] forbiddenNodes`) et d'un **paramètre lambda** (borne de coût) ajoutés pour l'intégration avec l'algorithme Marathon

### 5. `AStarAlgorithm`

Extension de `DijkstraAlgorithm` utilisant une heuristique géométrique :

- Hérite de `DijkstraAlgorithm` sans modifier `doRun()`
- Surcharge uniquement `newLabel()` pour ajouter l'heuristique :
  - Modes `LENGTH` / `PEDESTRIAN_LENGTH` : distance euclidienne jusqu'à la destination
  - Mode `TIME` : distance euclidienne divisée par la vitesse maximale du graphe
- Supporte également les nœuds interdits et le paramètre lambda

### 6. `LabelMarathon` & `MarathonAlgorithm`

Algorithme de recherche d'un circuit pédestre fermé d'environ 42 195 m :

**`LabelMarathon`** — label spécifique au Marathon :
- `getNotComebackTotalCost()` — distance restante pour atteindre la cible = `marathonLength − (g + h)`
- `compareTo()` — ordonnancement par proximité à la longueur cible (les labels dépassant la cible sont repoussés en fin de file)

**`MarathonAlgorithm`** — algorithme en deux phases :
1. **Phase aller** : exploration depuis l'origine avec `LabelMarathon` (`marathonLength = 42 195 m`, `lambda = 4 220 m`, `errorMargin = 50 m`)
2. **Phase retour** : dès que l'estimation optimiste entre dans la fenêtre λ, un A* retour est lancé en interdisant les nœuds du chemin aller
- Modes supportés : `LENGTH` et `PEDESTRIAN_LENGTH` uniquement (lève `IllegalArgumentException` pour les autres modes)
- Résultat : concaténation aller + retour si la longueur totale est dans ±50 m de 42 195 m

---


## Modes supportés

| Mode | Description |
|---|---|
| `LENGTH` | Chemin le plus court en mètres |
| `TIME` | Chemin le plus rapide (basé sur les limites de vitesse) |
| `PEDESTRIAN_LENGTH` | Chemin le plus court pour les piétons |

---


## Tests

Les tests automatisés couvrent :

- `BinaryHeapTest` — test du tas binaire
- `DijkstraAlgorithmTest` — test de Dijkstra sur plusieurs scénarios
- `AStarAlgorithmTest` — test de A* sur plusieurs scénarios
- `DijkstraVsAStarTest` — vérification que Dijkstra et A* retournent le même coût optimal
- `MarathonAlgorithmTest` — test de l'algorithme Marathon
- `PathTest` — test des méthodes de `Path`

---


## Exécution

L'interface graphique complète se lance via `MainWindow` (menu *File > Open Graph* pour charger un fichier `.mapgr`; menu *Algorithms > Shortest Path* pour lancer les algorithmes).