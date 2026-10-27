# Cadrage technique : application de réveil à défi par caméra

Version 1.0 du 27 octobre 2026. Nom de l'application : provisoire. Plateforme : Android, Kotlin natif.

## 1. Objectif et périmètre

### Objectif

Une application de réveil dont la sonnerie ne s'arrête que lorsque la caméra a validé un défi physique (sauts, squats, pas sur place, bras levés) choisi par l'utilisateur au moment du réglage de l'alarme.

### Décisions déjà prises

- Cible : Afrique francophone, utilisateurs de 9 à 35 ans.
- Android uniquement pour la V1.
- V1 gratuite et sans publicité.
- Sévérité : l'alarme est très difficile à ignorer, mais l'utilisateur n'est jamais enfermé (sortie de secours inconfortable).
- Traitement de la vidéo entièrement sur l'appareil, aucune image stockée ni envoyée.
- Fonctionnement 100 % hors ligne.

### Inclus dans la V1

- Alarmes multiples, répétition par jours de la semaine, sonnerie et vibration.
- Quatre défis : sauts, squats, pas sur place, jumping jacks (bras et jambes).
- Escalade progressive de l'alarme tant que le défi n'est pas réussi.
- Sortie de secours après un délai.
- Historique simple des réveils et série de jours réussis.
- Interface en français, guide de fiabilité par marque de téléphone.

### Exclu de la V1

- iOS, compte utilisateur, synchronisation en ligne, publicité, achats intégrés.
- Défis à gestes fins (brossage de dents, reconnaissance d'objets).
- Langues locales (prévues en V2).

## 2. Contraintes qui structurent la conception

| Contrainte | Conséquence technique |
| --- | --- |
| Téléphones d'entrée de gamme (Tecno, Infinix, itel, Samsung A) | Modèle de détection léger, images réduites, 10 à 15 images par seconde, APK compact |
| Chambres exiguës, corps rarement visible en entier | Défis détectables avec le haut du corps, avertissement de cadrage clair |
| Faible luminosité au réveil | Mesure de luminosité de l'image, message « allume la lumière », écran en blanc comme éclairage d'appoint |
| Constructeurs agressifs sur la batterie | Guide pas à pas par marque, vérification de la fiabilité au premier lancement |
| Données mobiles coûteuses | Aucun accès réseau nécessaire, permission Internet absente |
| Utilisateurs dès 9 ans | Déclaration d'âge, consentement parental, règles Play pour les enfants, aucune collecte de données |
| Utilisateur à moitié endormi | Écrans très simples, gros boutons, peu de texte |

## 3. Choix technologiques

| Domaine | Choix | Raison |
| --- | --- | --- |
| Langage | Kotlin | Contrôle fin des alarmes et de la caméra |
| Interface | Jetpack Compose, Material 3 | Rapide à construire, thème clair et sombre |
| Architecture | MVVM, flux unidirectionnel (StateFlow) | Testable, lisible |
| Injection | Hilt | Standard Android |
| Base locale | Room | Alarmes et historique |
| Préférences | DataStore | Réglages et déclaration d'âge |
| Caméra | CameraX (Preview et ImageAnalysis) | Simple et compatible avec beaucoup d'appareils |
| Détection de pose | ML Kit Pose Detection (modèle de base, mode flux) | Modèle embarqué, hors ligne, 33 points du corps |
| Planification | AlarmManager avec setAlarmClock | Exempté du mode Doze, fiable |
| Son | MediaPlayer ou ExoPlayer sur le flux ALARM | Volume indépendant du média |
| Tests | JUnit, MockK, Turbine, Compose UI Test | Couverture logique et interface |

Niveaux d'API : minSdk 24 (Android 7) pour couvrir les téléphones anciens. Le targetSdk doit respecter l'exigence du Play Store au moment de la publication (à vérifier dans la Play Console). Publication au format AAB.

Alternative à évaluer plus tard : MediaPipe Pose, plus configurable mais plus lourd à intégrer. ML Kit suffit pour la V1.

## 4. Architecture

### Vue d'ensemble

Trois couches, un seul module Gradle pour la V1 avec des packages qui pourront devenir des modules.

- **Présentation** : écrans Compose, ViewModels, états d'interface.
- **Domaine** : modèles métier, cas d'usage, logique des défis (aucune dépendance Android).
- **Données** : Room, DataStore, planificateur d'alarmes, lecteur audio, caméra.

La logique de comptage des répétitions vit dans le domaine, sans dépendance à ML Kit. Elle reçoit des « images de pose » (PoseFrame) et renvoie une progression. Cela permet de la tester avec des séquences enregistrées, sans téléphone.

### Flux du déclenchement d'une alarme

1. L'utilisateur enregistre une alarme. Le planificateur calcule la prochaine échéance et appelle setAlarmClock.
2. À l'heure dite, AlarmReceiver reçoit l'intention et démarre AlarmService au premier plan.
3. AlarmService prend un wake lock, lance la sonnerie sur le flux ALARM, déclenche la vibration et affiche une notification avec intention plein écran.
4. RingingActivity s'affiche par-dessus l'écran verrouillé et ouvre ChallengeScreen.
5. CameraX envoie les images à PoseAnalyzer, qui produit des PoseFrame.
6. Le RepCounter du défi choisi met à jour la progression. L'interface affiche le squelette et le compteur.
7. À l'objectif atteint, le service arrête sonnerie et vibration, enregistre le réveil dans l'historique, puis planifie la prochaine occurrence.
8. Sans réussite après le délai prévu, la sortie de secours devient disponible (voir section 7).

### Diagramme de composants (texte)

```
UI (Compose) <-> ViewModels <-> UseCases (domaine) <-> Repositories
                                      |                    |
                              RepCounter (défis)     Room / DataStore
                                      ^
   CameraX -> PoseAnalyzer (ML Kit) -> PoseFrame

AlarmScheduler -> AlarmReceiver -> AlarmService -> RingingActivity
```

## 5. Modèle de données

### Alarm (Room)

- id, hour, minute, daysMask (bits lundi à dimanche), enabled, label
- soundId, vibrate, escalationProfile
- challengeType (JUMP, SQUAT, MARCH, JACK), challengeTarget (nombre de répétitions), difficulty
- createdAt, updatedAt

### WakeEvent (Room)

- id, alarmId, firedAt, dismissedAt
- method (CHALLENGE ou FALLBACK), repsDone, durationSec

### Réglages (DataStore)

- onboardingDone, ageBracket (moins de 13, 13 à 17, 18 et plus), parentalConsentGiven
- maxVolumePercent, language, batteryGuideSeen

La tranche d'âge reste uniquement sur l'appareil et sert à adapter l'expérience et les écrans de consentement.

## 6. Liste des écrans

### Premier lancement

1. **Accueil / onboarding** : trois écrans courts qui expliquent le principe.
2. **Tranche d'âge et consentement parental** : si l'utilisateur déclare moins de 13 ans, demande d'accord d'un parent avant d'activer la caméra.
3. **Permissions** : caméra, notifications, alarmes, affichage plein écran. Chaque demande est expliquée par une phrase simple.
4. **Guide de fiabilité** : détection de la marque du téléphone et étapes pour autoriser le démarrage automatique et désactiver l'économie de batterie.

### Usage courant

5. **Liste des alarmes** : prochaine alarme en évidence, interrupteur par alarme, bouton d'ajout.
6. **Création et édition d'une alarme** : heure, jours, sonnerie, vibration, défi, objectif, difficulté.
7. **Choix du défi** : cartes avec animation de démonstration, difficulté et conseils d'espace nécessaires.
8. **Essai du défi** : même écran caméra que le jour J, mais sans sonnerie, pour s'entraîner.
9. **Historique et statistiques** : calendrier des réveils réussis, série en cours.
10. **Réglages** : volume maximal, langue, sonneries, guide de fiabilité, confidentialité, à propos.

### Au déclenchement

11. **Écran de sonnerie** : plein écran, heure, bouton unique « Commencer le défi » (aucun bouton pour arrêter ou reporter).
12. **Écran de défi caméra** : aperçu, squelette en superposition, compteur central, messages (« recule un peu », « allume la lumière », « on te voit mal »).
13. **Succès** : félicitations, durée, série, bouton pour fermer.
14. **Sortie de secours** : code ou calculs à saisir, disponible après le délai.

### Navigation

Pile principale : onboarding, liste, édition, choix du défi, essai. Le flux de déclenchement vit dans RingingActivity, séparée de l'activité principale, pour apparaître sur l'écran verrouillé sans passer par la navigation de l'application.

## 7. Moteur d'alarme et fiabilité

C'est la partie la plus importante du produit.

### Planification

- setAlarmClock (échéance exacte, exemptée du mode Doze, visible par le système).
- Replanifier après redémarrage (BOOT\_COMPLETED), changement d'heure ou de fuseau (TIME\_SET, TIMEZONE\_CHANGED) et mise à jour de l'application.
- Calcul de la prochaine échéance dans le domaine, avec des tests pour les cas limites (changement d'heure, alarme sans jour répété, minuit).

### Déclenchement

- Service au premier plan avec un type déclaré (mediaPlayback ou specialUse, à justifier dans la déclaration Play).
- Notification de catégorie alarme, importance maximale, intention plein écran. Sur Android 14 et plus, la permission d'intention plein écran est accordée aux applications de réveil, à confirmer sur la console.
- Wake lock partiel le temps de la sonnerie.
- Flux audio USAGE\_ALARM avec prise du focus audio. Volume remis au niveau configuré si l'utilisateur le baisse.

### Escalade (très difficile à ignorer)

| Temps depuis le déclenchement | Action |
| --- | --- |
| 0 s | Sonnerie à 60 % du volume maximal, vibration |
| 30 s | Volume à 80 %, vibration plus forte |
| 90 s | Volume maximal, vibration continue, écran en pleine luminosité |
| 5 min | Changement de sonnerie vers un son plus strident |
| 10 min | Sortie de secours disponible (code ou calculs) |

Le flash de la lampe peut servir avant le début du défi, mais il est coupé quand la caméra s'ouvre pour éviter les conflits d'usage.

### Cas limites à couvrir

- Application tuée par le système ou par l'utilisateur pendant la sonnerie : le service redémarre et reprend.
- Appel entrant : la sonnerie reprend à la fin de l'appel.
- Mode Ne pas déranger : le flux ALARM le traverse normalement, à vérifier sur plusieurs marques.
- Batterie faible ou téléphone éteint : impossible à gérer, signalé au guide de fiabilité.

## 8. Moteur de défi par caméra

### Pipeline

1. CameraX ImageAnalysis, résolution 640 par 480, stratégie « garder la dernière image ».
2. PoseAnalyzer appelle ML Kit en mode flux et convertit le résultat en PoseFrame (points, confiance, horodatage).
3. Un RepCounter spécifique à chaque défi traite les PoseFrame.
4. L'état de progression remonte à l'interface par un StateFlow.

### Interfaces du domaine (extrait)

```kotlin
data class Landmark(val x: Float, val y: Float, val confidence: Float)

data class PoseFrame(
    val timestampMs: Long,
    val landmarks: Map<LandmarkType, Landmark>,
    val brightness: Float
)

enum class Feedback { OK, BODY_NOT_VISIBLE, TOO_DARK, TOO_CLOSE, TOO_FAR }

data class ChallengeProgress(val reps: Int, val target: Int, val feedback: Feedback)

interface RepCounter {
    fun onFrame(frame: PoseFrame): ChallengeProgress
    fun reset()
}
```

### Principe de détection des sauts (extrait)

Les mesures sont normalisées par la longueur du buste (milieu des épaules vers milieu des hanches) pour que le comptage ne dépende pas de la distance à la caméra.

```kotlin
class JumpCounter(private val target: Int) : RepCounter {
    private var baselineHipY = 0f
    private var calibratedFrames = 0
    private var inAir = false
    private var lastRepMs = 0L
    private var reps = 0

    override fun onFrame(frame: PoseFrame): ChallengeProgress {
        val hips = frame.hipsMidY() ?: return progress(Feedback.BODY_NOT_VISIBLE)
        val torso = frame.torsoLength() ?: return progress(Feedback.BODY_NOT_VISIBLE)
        if (frame.brightness < MIN_BRIGHTNESS) return progress(Feedback.TOO_DARK)

        if (calibratedFrames < CALIBRATION_FRAMES) {
            baselineHipY = (baselineHipY * calibratedFrames + hips) / (calibratedFrames + 1)
            calibratedFrames++
            return progress(Feedback.OK)
        }
        val lift = (baselineHipY - hips) / torso   // l'axe Y de l'image pointe vers le bas
        if (!inAir && lift > UP_THRESHOLD) inAir = true
        if (inAir && lift < DOWN_THRESHOLD) {
            inAir = false
            if (frame.timestampMs - lastRepMs > MIN_REP_INTERVAL_MS) {
                reps++
                lastRepMs = frame.timestampMs
            }
        }
        return progress(Feedback.OK)
    }
    // reset(), progress(), constantes : UP_THRESHOLD ~ 0.12, DOWN_THRESHOLD ~ 0.04, MIN_REP_INTERVAL_MS ~ 400
}
```

Les seuils sont des points de départ, à régler sur des enregistrements réels de plusieurs morphologies.

### Défis de la V1

| Défi | Signal principal | Points requis |
| --- | --- | --- |
| Sauts | Montée puis descente des hanches | Épaules, hanches |
| Squats | Descente des hanches vers les genoux | Hanches, genoux |
| Pas sur place | Alternance de levée des genoux | Hanches, genoux |
| Jumping jacks | Poignets au-dessus des épaules et écartement des chevilles | Épaules, poignets (chevilles optionnelles) |

### Anti-triche et robustesse

- Les points requis doivent avoir une confiance suffisante, sinon le message de cadrage s'affiche et le comptage se met en pause.
- Fréquence maximale plausible (par exemple 3 répétitions par seconde), au-delà elle est ignorée.
- Calibration de deux secondes en début de défi pour la position de repos.
- Cohérence du squelette (longueurs des segments stables), pour écarter une photo agitée ou un téléphone secoué.
- Un défi aléatoire parmi ceux choisis peut être proposé aux utilisateurs qui le souhaitent.

### Performance sur téléphone modeste

- Analyse sur un thread dédié, abandon des images en retard.
- Réduction automatique de la cadence si le temps de traitement dépasse un seuil.
- Mesure du temps par image dans l'écran d'essai, pour détecter à l'avance un appareil trop faible.

## 9. Structure du projet Kotlin

Un seul module `app` pour la V1. Le nom du package est un exemple à remplacer.

```
reveil/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/libs.versions.toml
└── app/
    ├── build.gradle.kts
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── res/ (strings fr, icônes, sons, animations de démonstration)
        │   └── java/com/example/reveil/
        │       ├── ReveilApp.kt
        │       ├── MainActivity.kt
        │       ├── di/ (modules Hilt)
        │       ├── ui/
        │       │   ├── theme/
        │       │   ├── navigation/
        │       │   ├── onboarding/
        │       │   ├── alarms/ (liste, édition)
        │       │   ├── challenges/ (choix, essai, écran caméra)
        │       │   ├── history/
        │       │   ├── settings/
        │       │   └── components/
        │       ├── ringing/
        │       │   ├── RingingActivity.kt
        │       │   ├── AlarmService.kt
        │       │   ├── AlarmReceiver.kt
        │       │   ├── BootReceiver.kt
        │       │   └── EscalationController.kt
        │       ├── domain/
        │       │   ├── model/ (Alarm, WakeEvent, ChallengeType, PoseFrame)
        │       │   ├── usecase/ (SaveAlarm, ComputeNextTrigger, RecordWake)
        │       │   └── challenge/ (RepCounter, JumpCounter, SquatCounter, MarchCounter, JackCounter)
        │       ├── data/
        │       │   ├── db/ (Room : AlarmDao, WakeEventDao)
        │       │   ├── prefs/ (DataStore)
        │       │   ├── repository/
        │       │   ├── scheduler/ (AlarmScheduler)
        │       │   └── audio/ (AlarmPlayer)
        │       ├── vision/
        │       │   ├── CameraController.kt
        │       │   ├── PoseAnalyzer.kt
        │       │   ├── PoseMapper.kt
        │       │   └── BrightnessMeter.kt
        │       └── util/ (OemGuide, Clock, Permissions)
        ├── test/ (RepCounter sur séquences JSON, calcul d'échéance)
        └── androidTest/ (planification, base de données, interface)
```

### Extrait du planificateur

```kotlin
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val clock: Clock
) {
    private val am = ctx.getSystemService(AlarmManager::class.java)

    fun schedule(alarm: Alarm) {
        val triggerAt = ComputeNextTrigger(alarm, clock.now())
        val fire = PendingIntent.getBroadcast(
            ctx, alarm.id,
            Intent(ctx, AlarmReceiver::class.java).putExtra(EXTRA_ALARM_ID, alarm.id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val show = PendingIntent.getActivity(
            ctx, alarm.id,
            Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, show), fire)
    }
}
```

### Dépendances principales (sans versions figées)

À gérer par le catalogue de versions, en prenant les dernières stables au démarrage : Compose BOM, Material 3, Navigation Compose, Lifecycle, Hilt, Room, DataStore, CameraX (core, camera2, lifecycle, view), ML Kit pose-detection, Kotlin coroutines, JUnit, MockK, Turbine.

### Permissions du manifeste

- CAMERA
- POST\_NOTIFICATIONS
- VIBRATE
- WAKE\_LOCK
- RECEIVE\_BOOT\_COMPLETED
- FOREGROUND\_SERVICE et le type de service déclaré
- USE\_FULL\_SCREEN\_INTENT
- USE\_EXACT\_ALARM ou SCHEDULE\_EXACT\_ALARM selon la décision finale (setAlarmClock peut suffire)
- Aucune permission INTERNET

## 10. Vie privée, sécurité et conformité Play Store

- **Vidéo** : traitée en mémoire, jamais enregistrée ni transmise. À écrire clairement dans la politique de confidentialité.
- **Formulaire Sécurité des données** : « aucune donnée collectée » si l'application reste hors ligne et sans analyse tierce.
- **Mineurs** : cible 9 à 35 ans, donc audience mixte incluant des enfants. À vérifier dans la Play Console : déclaration du public cible, exigences de la politique Familles, et règles des bibliothèques tierces (ML Kit est accepté tant qu'aucune donnée ne sort de l'appareil).
- **Déclarations spéciales** : service au premier plan, intention plein écran, alarmes exactes. Prévoir une courte vidéo de démonstration.
- **Compte développeur** : un compte personnel neuf impose un test fermé (au moins 12 testeurs pendant 14 jours, à confirmer) avant la production. Un compte organisation en est généralement dispensé.
- **Sécurité** : application sans compte, sans réseau, surface d'attaque minimale. Activer la minification et la vérification de signature de l'AAB.

## 11. Stratégie de tests

### Tests automatiques

- **RepCounter** : séquences de poses enregistrées en JSON (saut propre, saut faible, personne qui s'assoit, photo agitée), avec le nombre de répétitions attendu.
- **Calcul d'échéance** : alarme unique, répétée, changement d'heure, fuseau différent.
- **Room et DataStore** : migrations et lecture.
- **Interface** : création d'une alarme, essai d'un défi.

### Matrice manuelle (indispensable)

- Téléphones : Tecno, Infinix, itel, Samsung A, Xiaomi, Pixel en référence.
- Scénarios : écran verrouillé, application tuée, redémarrage, mode économie de batterie, Ne pas déranger, volume à zéro, appel entrant, nuit complète de test.
- Collecter le taux d'alarmes réussies à l'heure exacte sur chaque marque, c'est le critère de qualité numéro un.

## 12. Plan de livraison

| Jalon | Contenu | Durée indicative |
| --- | --- | --- |
| J0 | Projet, catalogue de versions, CI, charte visuelle | 1 semaine |
| J1 | Alarme fiable de bout en bout (planification, service, plein écran, reboot) | 2 semaines |
| J2 | Caméra et détection des sauts, écran d'essai | 2 semaines |
| J3 | Autres défis, anti-triche, escalade | 2 semaines |
| J4 | Écrans, onboarding, âge et consentement, guide de fiabilité | 2 semaines |
| J5 | Tests sur appareils, réglages des seuils, corrections | 2 semaines |
| J6 | Fiche Play Store, politique de confidentialité, test fermé, production | 3 semaines |

Les durées supposent un développeur à temps partiel et sont à ajuster.

## 13. Risques et réponses

| Risque | Gravité | Réponse |
| --- | --- | --- |
| Alarme qui ne sonne pas sur certaines marques | Élevée | Matrice de tests, guide par marque, vérification au premier lancement |
| Détection instable en faible lumière | Moyenne | Mesure de luminosité, message, écran blanc d'appoint, essai préalable |
| Utilisateur frustré qui désinstalle | Moyenne | Sortie de secours, difficulté réglable, défis à objectif progressif |
| Refus de la fiche Play (mineurs, permissions) | Moyenne | Déclarations préparées, vidéo de démonstration, test fermé précoce |
| Téléphone trop lent pour la détection | Moyenne | Réduction de cadence, mesure à l'essai, défi alternatif sans caméra (QR code) en V2 |
| Triche par photo ou secousse | Faible | Contrôles de cohérence du squelette et de fréquence |

## 14. Décisions encore ouvertes

1. Nom de l'application et identité visuelle.
2. Âge minimum réel autorisé et texte du consentement parental.
3. Compte développeur : personnel ou organisation.
4. Sonneries : créations originales ou sons libres de droits.
5. Défi alternatif sans caméra pour les téléphones trop faibles (QR code à scanner dans une autre pièce).
6. Modèle économique après la V1.

## 15. Prochaine étape recommandée

Créer le projet Android Studio avec la structure ci-dessus, puis réaliser le jalon J1 (alarme fiable) avant de toucher à la caméra : une alarme qui ne sonne pas rend tout le reste inutile.
