# Vérification de l'étape 1 sur téléphone

Téléphone : VILLAON V40s (modèle V651L), Android 14 (API 34), puce Unisoc SC9863A, 4 Go. Version de l'appli : debug 0.1.0, branche `bugfix/19-fiabilite-sonnerie`.

Les résultats viennent des journaux du système (`logcat`, `dumpsys alarm`, `dumpsys audio`), de captures d'écran et de la base de l'appli copiée depuis le téléphone.

## Tests automatiques sur le téléphone

28 octobre 2026, `./gradlew connectedDebugAndroidTest` : 11 tests sur 11.

| Classe | Tests | Ce qui est vérifié |
| --- | --- | --- |
| `AlarmDaoTest` | 5 | tri, mise à jour, suppression, alarmes actives, aller-retour domaine |
| `DatabaseLocationTest` | 1 | base dans le stockage protégé par l'appareil |
| `PreferencesLocationTest` | 2 | préférences dans le stockage protégé par l'appareil |
| `AlarmSchedulerTest` | 2 | planification et annulation |
| `CreateAlarmTest` | 1 | création d'une alarme depuis l'interface |

## Scénarios pilotés par adb

| Date | Scénario | Résultat | Remarques |
| --- | --- | --- | --- |
| 28/10 06:54 | Alarme de test, écran éteint et verrouillé par code | Réussi | Alarme envoyée à 06:54:00.007, son à 06:54:00.650. L'écran de sonnerie s'affiche par-dessus le verrouillage. |
| 28/10 06:54 | Escalade du volume (sur 7) | Réussi | 4 au départ (60 %), 6 à 40 s (80 %), 7 à 100 s (100 %). |
| 28/10 06:56 | Arrêt et retour du volume d'origine | Réussi | Volume revenu à 6, service arrêté, retour à l'écran verrouillé. |
| 28/10 06:51 | Bandeau des autorisations | Réussi | « Autoriser » ouvre la demande système ; le bandeau disparaît une fois les notifications accordées. |
| 03/11 17:24 | Appli retirée des tâches récentes avant l'heure | Réussi | Processus terminé, alarme toujours planifiée. Sonnerie à 17:25:00, son à 17:25:00.579. La surcouche du VILLAON n'annule pas les alarmes au balayage. |
| 03/11 17:25 | Processus tué en pleine sonnerie (`am crash`) | Réussi | Service relancé en 3 s environ, sonnerie reprise. À 37 s le volume est passé à 6 : l'escalade compte depuis 17:25:00 et non depuis la relance. |
| 03/11 17:25 | Alarme ponctuelle déclenchée appli fermée | Réussi | Après l'arrêt, plus aucune alarme planifiée : pas d'alarme fantôme le lendemain. |
| 03/11 17:27 | Alarme désactivée avant l'heure | Réussi | L'alarme de 17:28 désactivée disparaît des alarmes du système et ne sonne pas. |
| 03/11 17:29 | Trois alarmes à la même minute, appli fermée | Réussi | 3 déclenchements reçus, un seul lecteur actif, alarmes 4 et 5 consignées `SKIPPED`, alarme 6 sonne. Aucun plantage. |
| 03/11 17:29 | Stockage avant déverrouillage | Réussi | Base et préférences présentes sous `/data/user_de/0/com.bytechnum.reveil`, absentes du stockage chiffré. |

## Défauts constatés

- **Volume d'origine perdu après une relance du service** (point mineur n°1 de la relecture, confirmé) : après le plantage provoqué à 17:25, l'arrêt a remis le volume d'alarme à 4, le niveau du palier au moment de la relance, au lieu de 6. Le volume d'alarme de l'utilisateur reste modifié. Correction proposée : enregistrer le volume d'origine avec l'état de sonnerie.
- **Bouton d'arrêt invisible** (corrigé le 28/10) : bleu sur fond bleu. Il est maintenant blanc à texte bleu.

## Non exécuté sur téléphone

- **Arrêt automatique à 15 minutes** : il impose 15 minutes de sonnerie au volume maximal. Le comportement est couvert par `RingingCoordinatorTest.fifteenMinutesStopsAndRecordsMissed`.

## Scénarios à faire par l'utilisateur

Ils demandent une présence physique ou coupent la connexion adb.

1. **Redémarrage sans déverrouiller** : créer l'alarme de test, redémarrer le téléphone, ne pas saisir le code. L'alarme doit sonner sur l'écran verrouillé.
2. **Ne pas déranger** : activer le mode, créer l'alarme de test, verrouiller. L'alarme doit sonner.
3. **Appel entrant pendant la sonnerie** : se faire appeler pendant que l'alarme sonne. La sonnerie doit se taire pendant l'appel et reprendre après.

| Date | Scénario | Résultat | Remarques |
| --- | --- | --- | --- |
| | Redémarrage sans déverrouiller | | |
| | Ne pas déranger | | |
| | Appel entrant | | |
