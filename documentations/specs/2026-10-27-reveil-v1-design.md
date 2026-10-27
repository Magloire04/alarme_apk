# Conception V1 : réveil à défi par caméra

Date : 27 octobre 2026. Base : [cadrage.md](../cadrage.md) version 1.0.

Ce document ne répète pas le cadrage. Il fixe ce que la séance de lancement a décidé et les choix de réalisation qui manquaient. En cas de contradiction, ce document l'emporte sur le cadrage.

Les décisions de l'utilisateur sont marquées **(décidé)**. Les choix de réalisation sont marqués **(choix technique)** et restent à valider en relisant.

## 1. Intention

L'utilisateur veut une application réellement utilisable sur son téléphone : une alarme qui sonne à l'heure, par-dessus l'écran verrouillé, et qui ne s'arrête qu'après le défi caméra. Pas une maquette ni un écran de démonstration. **(décidé)**

Le périmètre est toute la V1 du cadrage, jalons J0 à J4.

Hors de ce chantier :

- J5 : réglage des seuils sur plusieurs morphologies et matrice manuelle multi-marques. Il faut des humains devant de vrais téléphones. Le code le prépare (outil d'enregistrement de séquences, section 4.4).
- J6 : publication. Le code le prépare (build release AAB minifiée, signature par une clé fournie plus tard, aucune clé dans le dépôt).

Échec, même si tout compile : une alarme qui ne sonne pas sur le téléphone d'essai écran verrouillé, après un redémarrage, ou application fermée depuis les tâches récentes.

Pour ce dernier cas, certaines surcouches traitent le balayage dans les tâches récentes comme un arrêt forcé, qui annule toutes les alarmes, et aucun code ne peut l'empêcher. Le critère est donc vérifié une fois appliquées les étapes du guide de fiabilité pour la marque (verrouiller l'appli dans les tâches récentes, désactiver le nettoyage automatique). Si le VILLAON se comporte ainsi, sa fiche du guide le dit. **(choix technique)**

## 2. Décisions de la séance

- Éditeur : TECHNUM (bytechnum.com). Identifiant d'application et namespace : `com.bytechnum.reveil`. Il ne changera plus après publication. **(décidé : éditeur ; choix technique : suffixe `reveil`, neutre vis-à-vis du futur nom commercial)**
- Nom affiché provisoire : « Réveil Défi ». Le nom définitif reste la décision ouverte n°1 du cadrage. **(choix technique)**
- Téléphone d'essai : VILLAON V40s (modèle V651L), Android 14 (API 34), Unisoc SC9863A, 4 Go, écran 720 × 1600, arm64-v8a, caméra avant présente, connecté en débogage adb sans fil. **(décidé)**
- Livraison en quatre étapes. Chaque étape se termine par un APK installé et vérifié sur ce téléphone :
  1. Socle et alarme fiable (J0 et J1). Un bouton d'arrêt provisoire remplace le défi tant que la caméra n'existe pas. **(proposé, non contesté)**
  2. Caméra et sauts (J2) : écran d'essai, squelette, compteur, puis branchement sur la sonnerie. Le bouton provisoire disparaît.
  3. Squats, pas sur place, jumping jacks, anti-triche, escalade complète, sortie de secours (J3).
  4. Onboarding, âge et consentement, permissions, guide de fiabilité, historique, réglages (J4).
- Outillage : pas d'Android Studio. Le SDK Android est installé en ligne de commande dans `%LOCALAPPDATA%\Android\Sdk`, la compilation passe par le wrapper Gradle depuis le dossier du projet et l'installation par adb. JDK 21 Temurin déjà présent. **(choix technique, annoncé, non contesté)**

## 3. Socle technique

- Projet à la racine de `c:\wamp64\www\alarme_apk` (le dossier `documentations/` reste à côté). Un seul module `app`, packages du cadrage section 9. **(choix technique)**
- Versions : les dernières stables au moment de la création, vérifiées en ligne sur Google Maven et Maven Central, pas de mémoire. Catalogue `gradle/libs.versions.toml`. compileSdk : dernière version stable du SDK. targetSdk : niveau exigé par Play à la date de création. minSdk 24 (cadrage).
- KSP pour Room et Hilt.
- Build debug limitée à arm64-v8a et armeabi-v7a pour garder un APK léger. Release : AAB, R8 activé.
- Dépôt GitHub `Magloire04/alarme_apk`, conventions Gitflow : branches `main` et `develop`, une issue par tâche, une branche `feature/` et une PR par issue. Les issues GitHub remplacent Jira. Workflow GitHub Actions : formatage, lint, tests unitaires et compilation à chaque PR. **(décidé)**
- Police système, pas de police embarquée, pour la taille de l'APK. Couleur primaire provisoire : bleu TECHNUM #405FE0, thème clair et sombre. **(choix technique, identité définitive ouverte)**
- Textes en français dans `res/values/strings.xml` (langue par défaut), aucune chaîne en dur dans le code, pour préparer les langues locales de la V2.

## 4. Choix de réalisation

### 4.1 Moteur d'alarme (Android 14 d'abord)

- Permissions d'alarme exacte : `USE_EXACT_ALARM` (API 33 et plus, accordée à l'installation pour une appli de réveil) et `SCHEDULE_EXACT_ALARM` limitée à `maxSdkVersion 32`. Planification par `setAlarmClock`.
- Service au premier plan de type `systemExempted` avec `FOREGROUND_SERVICE_SYSTEM_EXEMPTED`. Android autorise ce type aux applis de réveil qui détiennent la permission d'alarme exacte. `mediaPlayback` est aussi déclaré et sert de repli si un appareil refuse `systemExempted`. Sous API 34, pas de type passé. **(choix technique)**
- Affichage plein écran : sur API 34 et plus, l'appli vérifie `canUseFullScreenIntent()` et, si besoin, envoie vers le réglage dédié. Si l'intention plein écran est quand même refusée au moment de sonner, la notification reste en importance maximale avec la sonnerie active, et un toucher ouvre l'écran de sonnerie.
- Démarrage avant déverrouillage (direct boot) : Room et DataStore vivent dans le stockage protégé par l'appareil, les récepteurs, le service et l'écran de sonnerie sont `directBootAware`, et l'appli écoute `LOCKED_BOOT_COMPLETED` en plus de `BOOT_COMPLETED`. Raison : un téléphone redémarré la nuit (mise à jour, coupure) reste verrouillé jusqu'au matin, et sans cela l'alarme ne sonnerait pas. Aucune donnée sensible n'est stockée (alarmes, historique, tranche d'âge). **(choix technique)**
- Replanification sur : `BOOT_COMPLETED`, `LOCKED_BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`, `MY_PACKAGE_REPLACED`, changement d'état de la permission d'alarme exacte, et à chaque ouverture de l'appli par sécurité.
- Calcul d'échéance dans le domaine avec `java.time` et le fuseau courant. Heure qui n'existe pas (passage à l'heure d'été) : décalée à la première minute valide suivante. Heure en double : première occurrence.
- Alarme sans jour répété : se désactive après avoir sonné.
- État de sonnerie persisté (identifiant de l'alarme, heure de déclenchement, défi tiré, répétitions faites). Si le système tue le service, il redémarre et reprend l'escalade au bon palier, calculé depuis l'heure de déclenchement.
- Volume : au déclenchement, l'appli mémorise le volume du flux ALARM, applique l'escalade, le remonte s'il est baissé (vérification chaque seconde) et restaure le volume d'origine à l'arrêt. Les paliers du cadrage (60 %, 80 %, maximum) sont relatifs au « volume maximal » des réglages.
- Appel entrant : à la perte du focus audio, sonnerie et vibration en pause. Reprise à la fin de l'appel. Le compteur d'escalade continue.
- Deux alarmes au même moment ou pendant une sonnerie : la seconde est ignorée, consignée `SKIPPED` (nouvelle valeur de `WakeEvent.method`), et sa prochaine occurrence est planifiée. Une alarme de secours à 6 h 05 derrière celle de 6 h 00 ne doit pas casser la série de quelqu'un qui finit son défi à 6 h 07. **(choix technique)**
- Durée maximale de sonnerie : 15 minutes, puis arrêt et consignation « manquée ». La sortie de secours reste disponible de la 10e à la 15e minute. Le modèle `WakeEvent.method` gagne la valeur `MISSED`. **(décidé)**

### 4.2 Escalade

Le tableau du cadrage s'applique (0 s, 30 s, 90 s, 5 min, 10 min), complété d'un dernier palier : à 15 minutes, arrêt de la sonnerie et réveil consigné comme manqué. Le passage en pleine luminosité de l'écran à 90 s se fait par la fenêtre de l'écran de sonnerie. Le clignotement de la lampe torche, optionnel dans le cadrage, est laissé hors V1. **(choix technique)**

### 4.3 Défi caméra

- Caméra avant. L'utilisateur pose le téléphone debout à environ deux mètres et se voit à l'écran.
- ML Kit Pose Detection, modèle de base embarqué, mode flux. CameraX ImageAnalysis 640 × 480, stratégie « garder la dernière image ». Les points sont convertis en coordonnées normalisées entre 0 et 1, corrigées de la rotation et de l'effet miroir.
- Luminosité : moyenne du plan Y de l'image.
- Compteurs dans `domain/challenge`, sans dépendance Android, seuils du cadrage comme point de départ.
- Difficulté (Facile, Normal, Difficile) : multiplie l'amplitude exigée (0,8 ; 1,0 ; 1,2). L'objectif de répétitions est réglé à part, 15 par défaut, de 5 à 50. **(choix technique)**
- Option « Défi au hasard » : un des quatre défis tiré au moment de la sonnerie. **(choix technique, répond au cadrage section 8)**
- Faible lumière : quand le retour est TOO_DARK, le pourtour de l'écran de défi passe en blanc à luminosité maximale pour éclairer la personne.
- Caméra impossible (permission refusée, ouverture en échec, détecteur indisponible, y compris avant le premier déverrouillage) : la sortie de secours devient disponible tout de suite. L'alarme ne doit jamais rester bloquée sans issue.
- Temps de traitement par image mesuré et affiché dans l'écran d'essai. Au-delà de 100 ms par image en moyenne, l'analyse saute une image sur deux.

### 4.4 Outil d'enregistrement (builds de debug uniquement)

Dans l'écran d'essai, un bouton enregistre les PoseFrame (points, confiances, horodatages, luminosité) dans un fichier JSON du dossier externe propre à l'appli, récupérable par `adb pull`. Aucune image n'est enregistrée. Ces fichiers servent de séquences de test (cadrage section 11) et au réglage des seuils en J5. Absent des builds release. **(choix technique)**

En attendant de vrais enregistrements, les tests unitaires des compteurs utilisent des séquences synthétiques générées par le code de test.

### 4.5 Sortie de secours

Disponible à 10 minutes, ou tout de suite si la caméra est impossible. Trois calculs à résoudre d'affilée (nombre à deux chiffres multiplié par un nombre à un chiffre). Une erreur remplace le calcul en cours par un nouveau. Consignée `FALLBACK`. Pas de code fixe, qu'on finirait par connaître par cœur. **(choix technique)**

### 4.6 Sonneries

Trois sons originaux synthétisés pendant le build du projet (ffmpeg, présent sur la machine) : « Doux », « Classique », « Strident ». Le son strident sert au palier des 5 minutes. Libres de droits par construction, en attendant la décision ouverte n°4. **(choix technique)**

### 4.7 Interface

- Écrans du cadrage section 6, Navigation Compose. Gros boutons, peu de texte.
- Animations de démonstration des défis : bonhomme en fil de fer dessiné en Compose, aucun fichier d'animation. **(choix technique)**
- Écran « À propos » : publié par TECHNUM.

### 4.8 Onboarding, âge, consentement

- Ordre : trois écrans d'introduction, tranche d'âge, consentement parental si moins de 13 ans, permissions, guide de fiabilité avec alarme de test.
- Consentement parental : texte provisoire (décision ouverte n°2). Le parent confirme en saisissant son année de naissance, qui doit correspondre à 18 ans ou plus. Sans consentement, l'onboarding ne va pas plus loin et affiche « Reviens avec un parent ». **(choix technique)**
- La tranche d'âge reste sur l'appareil, comme prévu au cadrage.

### 4.9 Guide de fiabilité

- Marque détectée par `Build.MANUFACTURER`. Fiches : Tecno, Infinix et itel (même surcouche), Samsung, Xiaomi / Redmi / POCO, et une fiche générique pour les autres (dont VILLAON).
- Chaque fiche donne les étapes et des boutons qui ouvrent le bon réglage, avec repli sur la page de l'appli dans les paramètres si l'écran constructeur n'existe pas.
- Pas de permission `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (exposée à un refus Play). L'appli ouvre la liste d'optimisation de batterie et explique quoi toucher. **(choix technique)**
- Alarme de test : sonne dans une minute, l'utilisateur verrouille son téléphone et attend. Le résultat (a sonné ou non) est affiché au retour.

### 4.10 Historique et série

Calendrier mensuel : jour réussi par le défi, jour terminé par la sortie de secours, jour manqué. La série compte les jours consécutifs où chaque réveil s'est terminé par le défi. Un jour sans alarme ne casse pas la série. Une sortie de secours ou un réveil manqué la cassent. Les événements `SKIPPED` n'apparaissent ni dans le calendrier ni dans le calcul de la série. **(choix technique)**

## 5. Vérification

- Tests unitaires JVM : calcul d'échéance (cas du cadrage et changements d'heure), compteurs sur séquences, paliers d'escalade avec horloge simulée, série, générateur de calculs de secours.
- Tests instrumentés sur le VILLAON : DAO Room, création d'une alarme dans l'interface.
- Vérification manuelle à la fin de chaque étape, pilotée par adb : alarme à une minute écran verrouillé, appli retirée des tâches récentes, redémarrage du téléphone, volume à zéro, mode Ne pas déranger. Résultat consigné dans `documentations/verification.md`.
- Ce qui ne peut pas se vérifier par adb est fait par l'utilisateur, avec une courte liste d'étapes fournie à chaque fois : le test de redémarrage (le débogage sans fil se coupe au redémarrage et le rallumer exige de déverrouiller, ce qui fausserait le test du démarrage avant déverrouillage), et les défis caméra des étapes 2 et 3. **(choix technique)**
- Seuil d'acceptation des étapes 2 et 3, avant le réglage fin de J5 : l'utilisateur fait 15 répétitions de chaque défi devant le VILLAON, le compteur ne s'écarte pas de plus de 2. Les seuils provisoires penchent vers l'indulgence : au réveil, une vraie répétition non comptée est pire qu'une triche possible, et l'anti-triche (cohérence du squelette, fréquence maximale) couvre les cas grossiers. La difficulté « Difficile » sert à durcir. **(choix technique)**

## 6. Laissé au constructeur

Découpage interne des classes au-delà de ce que nomme le cadrage, valeurs exactes des seuils, détails visuels, formulation des textes d'interface dans l'esprit « gros boutons, peu de texte ».

## 7. Décisions toujours ouvertes (cadrage section 14)

Nom et identité visuelle définitifs, texte réel du consentement parental et âge minimum, nature du compte développeur, sonneries définitives, défi QR code (V2), modèle économique.
