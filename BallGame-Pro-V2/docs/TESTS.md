# Tests de validation

Testé avec OpenJDK 21 (compilation `javac --release 21` et lancement `java -jar`).

## Smoke test graphique

Le programme `tests/SmokeTest.java` pilote le jeu dans une fenêtre Swing avec `java.awt.Robot` et vérifie :

- lancement depuis le menu ;
- 3 vies et une balle au départ ;
- pause/reprise et arrêt effectif du chronomètre ;
- progression des niveaux ;
- activation des obstacles ;
- fin du mode Arcade après 300 secondes simulées ;
- retour au menu et lancement du mode Survival.

Les captures d'écran sont disponibles dans `docs/preview-*.png`.

## Test de gameplay

`tests/PhysicsTest.java` utilise des méthodes réflexives pour valider, sans interaction manuelle :

- collision et rebond sur la raquette ;
- attribution des points et augmentation du combo ;
- apparition des power-ups et activation du bonus LARGE ;
- génération de deux balles supplémentaires par MULTI ;
- progression des niveaux et obstacles ;
- perte des trois vies, fin de partie et sauvegarde du record.

## Compilation des tests (facultatif)

```bash
./build.sh
mkdir -p build/tests
javac --release 21 -cp dist/BallGame-Pro-V2.jar -d build/tests tests/*.java
java -Djava.awt.headless=true -cp 'build/tests:dist/BallGame-Pro-V2.jar' PhysicsTest
java -cp 'build/tests:dist/BallGame-Pro-V2.jar' SmokeTest
```

Sous Windows, remplacez `:` dans le classpath par `;` et lancez `SmokeTest` sur un bureau graphique.

**Remarque :** Les tests créent de faux scores locaux. Pour les garder séparés de votre propre classement, lancez les tests avec `-Duser.home=...` pointant vers un dossier temporaire.
