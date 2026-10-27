# Réveil Défi

Application Android de réveil. La sonnerie ne s'arrête qu'une fois un défi physique validé par la caméra : sauts, squats, pas sur place ou jumping jacks. Le nom est provisoire.

Publiée par TECHNUM. Public visé : Afrique francophone, 9 à 35 ans, téléphones d'entrée de gamme. L'application fonctionne sans connexion. Les images de la caméra sont analysées sur le téléphone, jamais enregistrées ni envoyées.

## État

Étape 1 en cours : socle du projet et alarme fiable. Voir le [plan de l'étape 1](documentations/plans/2026-10-27-etape1-socle-alarme.md).

## Documents

- [Cadrage technique](documentations/cadrage.md)
- [Conception de la V1](documentations/specs/2026-10-27-reveil-v1-design.md)
- [Plan de l'étape 1](documentations/plans/2026-10-27-etape1-socle-alarme.md)

## Prérequis

- JDK 17 ou plus récent.
- SDK Android avec la plateforme 37 et les build-tools 37.0.0.
- Un fichier `local.properties` à la racine, non versionné, contenant `sdk.dir=` suivi du chemin du SDK.

## Commandes

```bash
./gradlew assembleDebug          # APK de debug
./gradlew testDebugUnitTest      # tests unitaires
./gradlew connectedDebugAndroidTest   # tests sur un téléphone branché
./gradlew spotlessApply          # formatage du code Kotlin
./gradlew spotlessCheck lintDebug     # vérifications de la CI
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Contribuer

- `main` correspond à la production, `develop` à l'intégration. Aucun commit direct sur ces deux branches.
- Chaque tâche a son issue GitHub et sa branche : `feature/{numéro d'issue}-{description-en-kebab-case}` (ou `bugfix/`, `hotfix/`).
- Messages de commit au format Conventional Commits : `type(scope): description`, en minuscules, sans point final, 72 caractères au plus.
- Une PR par branche, vers `develop`, titrée `[#{numéro}] Description courte`, avec les sections du [modèle de PR](.github/pull_request_template.md). Moins de 400 lignes, sinon la PR est fractionnée ou l'écart est justifié dans sa description.
- Activer le crochet de formatage local une fois par clone : `git config core.hooksPath .githooks`.

### Écarts aux conventions, décidés le 27 octobre 2026

- Les tickets sont des issues GitHub. Les branches portent donc le numéro d'issue, et non une clé de ticket Jira.
- Le projet est mené par une seule personne, et GitHub n'autorise pas l'approbation de ses propres PR. Le propriétaire du dépôt relit chaque PR avant de la fusionner.
- Le premier commit de `main` a été fait directement, pour amorcer le dépôt vide.
