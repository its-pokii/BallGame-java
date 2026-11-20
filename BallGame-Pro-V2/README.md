# BALLGAME PRO V2 — Neon Arcade

Un mini-jeu **100 % Java 21 + Swing**, autonome et utilisable **sans Internet, Maven, dépendances externes ni installation de bibliothèque**. Il s'appuie sur le `BallGame.java` de départ, conservé dans `docs/original/BallGame.java`.

## Lancer le jeu

**Windows 11 (avec Java 21 installé) :**

1. Pour utiliser le JAR fourni directement : double-cliquez sur `BallGame-Pro-V2.jar` ou ouvrez PowerShell dans son dossier :
   ```powershell
   java -jar .\BallGame-Pro-V2.jar
   ```
2. Pour recompiler à partir du code source (ZIP) : extrayez le dossier, double-cliquez sur `build.bat`, puis sur `run.bat`.
3. Dans IntelliJ IDEA : `File > Open` et sélectionnez le dossier extrait ; configurez `Project SDK = JDK 21`. Ouvrez `src/main/java/com/ballgame/pro/BallGame.java` et lancez sa méthode `main`.

**Linux / macOS :** `./build.sh` puis `./run.sh` (JDK 21 requis ; environnement graphique requis).

Si Windows indique `java` ou `javac` non reconnu, installez un **JDK 21** et ajoutez son dossier `bin` au `PATH` (ou configurez `JAVA_HOME`).

## Modes et règles

| Fonction | Comportement |
|---|---|
| **Arcade** | 5 minutes de jeu ; une nouvelle balle toutes les 10 secondes ; le jeu se termine après 5 minutes ou 3 vies perdues. |
| **Survival** | Pas de limite de temps ; une nouvelle balle apparaît de plus en plus souvent ; le jeu se termine quand les 3 vies sont perdues. |
| **3 vies** | Une vie est perdue **seulement lorsque toutes les balles en jeu tombent**. Une nouvelle balle apparaît après la perte de vie. |
| **Score & combos** | +10 pts / rebond, multiplicateur augmenté de 1 tous les 5 rebonds consécutifs, maximum x5. Le bonus X2 double les points gagnés. |
| **Niveaux** | Progression automatique toutes les 35 s (Arcade) ou 28 s (Survival). Les balles accélèrent, et des obstacles mobiles apparaissent à partir du niveau 2. |
| **Difficulté** | Facile, Normal, Difficile, Extrême, avec une vitesse de départ adaptée. |
| **Bonus** | Un bonus lâché tous les 7 rebonds : LARGE (raquette agrandie, 12 s), SLOW (balles ralenties, 9 s), MULTI (+2 balles, limite de 10), X2 (points doublés, 12 s), +1 (vie, max 3). Attrapez-le avec la raquette. |
| **Records** | Top 5 par mode et difficulté, sauvegardé après une fin de partie (même en cas de défaite). |
| **Audio** | Musique électronique et sons originaux, intégrés au JAR. La partie reste utilisable même sans périphérique audio. |

Les collisions balle/murs/plafond/raquette et le contrôle à la souris de la version initiale sont conservés.

## Commandes

| Touche | Action |
|---|---|
| Souris | Déplacer la raquette (dans la zone de jeu), activer les boutons |
| `←`, `→`, `A`, `D` | Déplacer la raquette |
| `P`, `Espace`, `Échap` | Pause / reprise |
| `Entrée` | Jouer, rejouer ou reprendre |
| `R` | Rejouer depuis Game Over |
| `M` | Musique on/off |
| `S` | Effets sonores on/off |
| `F11` | Mode plein écran / fenêtre |
| `1`, `2` | Choisir Arcade / Survival depuis le menu |

## Structure

```text
BallGame-Pro-V2/
├── src/main/java/com/ballgame/pro/
│   ├── BallGame.java          # Jeu, physique, menus, HUD, effets visuels et contrôles
│   ├── AudioEngine.java       # Effets et musique WAV intégrés
│   ├── AudioGenerator.java    # Générateur WAV 100 % Java 21
│   └── ScoreStore.java        # Sauvegarde locale top 5
├── src/main/resources/
│   ├── audio/*.wav            # Sons et musique originaux
│   └── icon.png               # Icône de fenêtre
├── docs/original/BallGame.java # Fichier Java initial de référence
├── build.bat / build.sh       # Compilation Java 21 puis création du JAR
├── run.bat / run.sh           # Lancement du jeu
└── README.md
```

Le fichier de scores est créé automatiquement sous :

- Windows : `C:\Users\VOTRE_NOM\.ballgame-pro-v2\records.properties`
- Linux/macOS : `~/.ballgame-pro-v2/records.properties`

Le fichier n'est pas supprimé lors d'une mise à jour du JAR. Le programme n'envoie aucune donnée sur Internet.

## Génération des sons (100 % Java, sans Python)

Les 7 fichiers `.wav` sont déjà inclus dans le projet et dans le JAR : la génération
n'est **pas nécessaire** pour jouer. Pour les régénérer à partir du code Java 21 :

```powershell
# Depuis le dossier extrait :
java src/main/java/com/ballgame/pro/AudioGenerator.java
.\build.bat
```

Ou, après compilation (fichiers WAV générés dans `src/main/resources/audio`) :

```powershell
java -cp .\dist\BallGame-Pro-V2.jar com.ballgame.pro.AudioGenerator
```

La génération utilise seulement `javax.sound.sampled`, pas Python, pas de dépendance
externe et pas de connexion Internet. Les scripts `.bat` / `.sh` ne servent qu'à
lancer la compilation du code Java.

## Compilation manuelle

```powershell
# Depuis le dossier extrait, sur Windows :
.\build.bat
java -jar .\dist\BallGame-Pro-V2.jar
```

## Précisions

- Le jeu utilise `Swing Timer` pour les mises à jour à ~60 FPS, en s'appuyant sur un delta-temps plafonné, ce qui permet une pause sans perte de temps.
- Audio via `javax.sound.sampled`. Sur un PC sans dispositif audio, les effets sont simplement désactivés sans faire planter le jeu.
- L'interface est dessinée avec `Graphics2D` et s'adapte aux changements de taille de la fenêtre.
- Les contrôles, les animations et la logique ne nécessitent **aucune dépendance tierce**.
- Une image d'aperçu est disponible dans `docs/`.
