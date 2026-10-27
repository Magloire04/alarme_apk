# Étape 1 : socle et alarme fiable, plan de réalisation


**Goal :** un APK installé sur le VILLAON qui permet de créer, modifier, activer et supprimer des alarmes, et dont la sonnerie part à l'heure écran verrouillé, appli fermée et après redémarrage, avec escalade et arrêt provisoire par bouton.

**Architecture :** un module `app`, trois couches. Le domaine (`domain/`) ne dépend que de Kotlin et `java.time` : modèles, calcul d'échéance, politique d'escalade, règles de sonnerie, cas d'usage, et des interfaces (`AlarmStore`, `WakeEventStore`, `AlarmScheduling`, `Clock`) que la couche données implémente. Le service de sonnerie applique ce que le domaine décide.

**Tech Stack :** Kotlin 2.4.21 (Kotlin intégré à AGP 9), AGP 9.4.1, Gradle 9.8.2, Compose BOM 2026.09.00, Material 3, Navigation Compose 2.10.2, Hilt 2.60.1, KSP 2.3.12, Room 2.8.5, DataStore 1.3.0, coroutines 1.11.0, kotlinx-serialization 1.12.0, JUnit 4.13.2, MockK 1.14.11, Turbine 1.2.1, androidx.test (runner 1.7.0, ext-junit 1.3.0, espresso 3.7.0).

**Spec :** [cadrage.md](../cadrage.md) et [2026-10-27-reveil-v1-design.md](../specs/2026-10-27-reveil-v1-design.md). Lire les deux.

## Global Constraints

- applicationId et namespace : `com.bytechnum.reveil`. Nom affiché : « Réveil Défi ».
- minSdk 24, compileSdk 37, targetSdk 36 (exigence Play en vigueur, à confirmer dans la Play Console avant publication).
- Aucune permission INTERNET dans le manifeste.
- Room et DataStore ouverts sur `context.createDeviceProtectedStorageContext()`. AlarmReceiver, BootReceiver, AlarmService et RingingActivity sont `android:directBootAware="true"`.
- Toutes les chaînes visibles dans `res/values/strings.xml`, en français. Aucune chaîne d'interface en dur dans le code.
- Le domaine (`com.bytechnum.reveil.domain`) n'importe rien de `android.*` ni `androidx.*`.
- Paliers d'escalade (ms depuis le déclenchement) : 0 → 60 % et vibration normale ; 30 000 → 80 % et vibration forte ; 90 000 → 100 %, vibration continue, pleine luminosité ; 300 000 → son strident ; 600 000 → sortie de secours disponible ; 900 000 → arrêt, réveil consigné `MISSED`.
- Les pourcentages de volume s'appliquent au « volume maximal » des réglages (`maxVolumePercent`, 100 par défaut).
- Valeurs par défaut d'une nouvelle alarme : 07:00, aucun jour (ponctuelle), activée, libellé vide, son `CLASSIC`, vibration oui, défi `JUMP`, objectif 15, difficulté `NORMAL`.
- Jours dans `daysMask` : bit 0 = lundi … bit 6 = dimanche.
- Dépôt : `github.com/Magloire04/alarme_apk`. Gitflow : `main` (production) et `develop` (intégration), jamais de commit direct dessus après l'amorçage. Une issue GitHub par tâche, une branche `feature/{numéro d'issue}-{description-en-kebab-case}` par issue, une PR vers `develop` titrée `[#{numéro}] Description courte` (60 caractères au plus) avec les sections Objectif, Changements, Tests, Checklist auteur. PR sous 400 lignes, sinon fractionnée ou justifiée (fichiers générés). Les PR d'une même étape s'empilent (base = branche de la tâche précédente) pour ne pas attendre les fusions ; elles sont relues et fusionnées par le propriétaire du dépôt, jamais par leur auteur.
- Commits Conventional Commits : `type(scope): description`, en minuscules, sans point final, 72 caractères au plus.
- Nommage : noms complets sans abréviation (`context`, `alarmManager`, pas `context`, `am`), fonctions verbe + nom, booléens en `is`/`has`/`can`, constantes en `CONSTANT_CASE`.
- Qualité : formatage et lint Kotlin par Spotless + ktlint (`.editorconfig` commité), `./gradlew spotlessCheck lintDebug testDebugUnitTest` dans la CI. Crochet `pre-commit` commité dans `.githooks/` qui lance `spotlessCheck`.
- Aucune donnée personnelle dans les journaux (`Log`), aucun secret ni fichier de signature dans le dépôt.
- Téléphone d'essai : le VILLAON V40s vu par `adb devices` (débogage sans fil, le port change ; relire `adb devices` et passer `-s <serial>`).

## Review Focus

1. Téléphone redémarré la nuit et resté verrouillé : l'alarme doit sonner. Test : la base est créée dans le stockage protégé par l'appareil (Task 3) ; vérification manuelle par l'utilisateur (Task 8).
2. L'utilisateur baisse le volume à zéro pendant la sonnerie : le volume revient au palier en moins d'une seconde, et le volume d'origine est rétabli à l'arrêt. Test : `AlarmVolumeControllerTest` (Task 5).
3. Alarme supprimée ou désactivée alors qu'elle est planifiée : elle ne doit plus sonner. Test : `AlarmSchedulerTest` vérifie que l'intention en attente n'existe plus après `cancel` (Task 4), `SaveAlarmTest` vérifie l'annulation à la désactivation (Task 2).
4. Deux alarmes à la même minute : la première sonne, la seconde est consignée `SKIPPED` et replanifiée, sans plantage. Test : `RingingRulesTest` (Task 2) ; `HandleAlarmFiredTest` vérifie la replanification (Task 2).
5. Service tué en pleine sonnerie : il reprend au bon palier, ou consigne `MISSED` si 15 minutes sont passées. Test : `RingingRulesTest.decideOnRestart` (Task 2).

---

## Structure des fichiers

Racine `c:\wamp64\www\alarme_apk`. Code sous `app/src/main/java/com/bytechnum/reveil/`, abrégé `…/` ci-dessous.

| Fichier | Rôle |
| --- | --- |
| `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties`, wrapper | Build |
| `app/build.gradle.kts`, `app/proguard-rules.pro` | Module |
| `.gitignore`, `.github/workflows/android.yml` | Dépôt, CI inactive |
| `app/src/main/AndroidManifest.xml` | Permissions, composants |
| `…/ReveilApp.kt`, `…/MainActivity.kt` | Application Hilt, activité principale |
| `…/di/AppModule.kt`, `…/di/BindingsModule.kt` | Fournisseurs Hilt |
| `…/domain/model/Alarm.kt` | `Alarm`, `ChallengeType`, `Difficulty`, `SoundId` |
| `…/domain/model/WakeEvent.kt` | `WakeEvent`, `WakeMethod` |
| `…/domain/model/RingingState.kt` | état de sonnerie persisté |
| `…/domain/port/Ports.kt` | `AlarmStore`, `WakeEventStore`, `AlarmScheduling`, `Clock` |
| `…/domain/alarm/ComputeNextTrigger.kt` | calcul d'échéance |
| `…/domain/alarm/EscalationPolicy.kt` | paliers, `EscalationStage`, `VibrationLevel`, `computeTargetVolumeIndex` |
| `…/domain/alarm/RingingRules.kt` | décisions au déclenchement et à la reprise |
| `…/domain/usecase/*.kt` | `ScheduleNext`, `SaveAlarm`, `DeleteAlarm`, `SetAlarmEnabled`, `RescheduleAll`, `HandleAlarmFired` |
| `…/data/db/*.kt` | entités, DAO, `AppDatabase`, `DaysMask` |
| `…/data/repository/*.kt` | `RoomAlarmStore`, `RoomWakeEventStore` |
| `…/data/prefs/SettingsStore.kt`, `…/data/prefs/RingingStateStore.kt` | DataStore |
| `…/data/scheduler/AlarmScheduler.kt` | AlarmManager |
| `…/data/audio/AlarmPlayer.kt`, `AlarmVolumeController.kt`, `AlarmVibrator.kt` | son, volume, vibration |
| `…/ringing/AlarmReceiver.kt`, `BootReceiver.kt`, `AlarmService.kt`, `RingingSession.kt`, `RingingNotifications.kt`, `RingingActivity.kt` | sonnerie |
| `…/ui/theme/*`, `…/ui/navigation/AppNavHost.kt`, `…/ui/alarms/*`, `…/ui/components/PermissionBanner.kt` | interface |
| `…/util/DeviceClock.kt`, `…/util/Permissions.kt` | horloge système, état des permissions |
| `app/src/main/res/raw/alarm_soft.ogg`, `alarm_classic.ogg`, `alarm_strident.ogg` | sonneries générées |
| `tools/generate_sounds.sh` | génération des sons par ffmpeg |
| `app/src/test/…` | tests JVM |
| `app/src/androidTest/…` | tests sur le VILLAON |
| `documentations/verification.md` | résultats des vérifications manuelles |

---

### Task 0 : projet qui compile et s'ouvre sur le téléphone

**Files :**

- Create : fichiers de build listés ci-dessus, `local.properties` (ignoré par git), `AndroidManifest.xml` minimal, `ReveilApp.kt`, `MainActivity.kt`, `ui/theme/{Color,Theme,Type}.kt`, `res/values/strings.xml`, `res/values/themes.xml`, icône adaptative provisoire (`res/mipmap-anydpi-v26/ic_launcher.xml` avec un vecteur simple), `.gitignore`, `.github/workflows/android.yml`.

**Interfaces :**

- Produces : `@HiltAndroidApp class ReveilApp : Application`, `@AndroidEntryPoint class MainActivity : ComponentActivity`, `@Composable fun ReveilTheme(content: @Composable () -> Unit)`.

- [ ] **Step 1 : vérifier que le SDK est installé** : `ls "$LOCALAPPDATA/Android/Sdk/platforms"` doit lister `android-37.0`, et `build-tools` doit contenir `37.0.0`. Sinon, relancer l'installation avec le nouvel outil (`sdkmanager` est déprécié) : `"$LOCALAPPDATA/Android/Sdk/cmdline-tools/latest/bin/android.exe" --sdk="$LOCALAPPDATA/Android/Sdk" sdk install platforms/android-37.0 build-tools/37.0.0 platform-tools`. Fait le 2026-10-27.
- [ ] **Step 2 : écrire `local.properties`** avec `sdk.dir=` suivi du chemin du SDK (par défaut `%LOCALAPPDATA%\Android\Sdk`, deux-points et barres obliques inverses échappés). Ce fichier n'est pas versionné.
- [ ] **Step 3 : écrire le catalogue et les scripts Gradle.** Plugins : `com.android.application` 9.4.1, `org.jetbrains.kotlin.plugin.compose` et `org.jetbrains.kotlin.plugin.serialization` 2.4.21, `com.google.devtools.ksp` 2.3.12, `com.google.dagger.hilt.android` 2.60.1, `androidx.room` 2.8.5. Pas de plugin `org.jetbrains.kotlin.android` (Kotlin intégré à AGP 9). Bibliothèques : celles de la Tech Stack plus `androidx.core:core-ktx` 1.19.1, `androidx.activity:activity-compose` 1.13.0, `androidx.lifecycle:lifecycle-runtime-compose` et `lifecycle-viewmodel-compose` 2.11.0, `androidx.lifecycle:lifecycle-service` 2.11.0, `androidx.hilt:hilt-lifecycle-viewmodel-compose` 1.4.0, `com.google.dagger:hilt-android-testing` 2.60.1. Java 17 en cible. Debug : `ndk.abiFilters += listOf("arm64-v8a", "armeabi-v7a")`. Release : `isMinifyEnabled = true`, `isShrinkResources = true`. `room { schemaDirectory("$projectDir/schemas") }`.
- [ ] **Step 4 : générer le wrapper.** Télécharger `gradle-9.8.2-bin.zip` dans un dossier vide du scratchpad, le décompresser, puis exécuter `<gradle>/bin/gradle wrapper --gradle-version 9.8.2` à la racine du projet.
- [ ] **Step 5 : écrire `MainActivity`** qui appelle `enableEdgeToEdge()` et affiche dans `ReveilTheme` un `Scaffold` avec le texte `R.string.app_name`. Thème : primaire #405FE0, clair et sombre, `dynamicColor = false`.
- [ ] **Step 6 : compiler.** Run : `./gradlew assembleDebug`. Expected : `BUILD SUCCESSFUL`. Ajouter Spotless (ktlint) et `.editorconfig`, puis `./gradlew spotlessCheck lintDebug` doit passer. Les erreurs de compatibilité AGP 9 / Hilt / KSP se règlent ici, en gardant les versions les plus récentes qui compilent ; noter tout changement de version dans le catalogue.
- [ ] **Step 7 : installer et lancer.** Run : `adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk` puis `adb -s <serial> shell am start -n com.bytechnum.reveil/.MainActivity`. Expected : `Success`, et l'écran affiche « Réveil Défi ».
- [ ] **Step 8 : `.gitignore` et CI.** `.gitignore` Android standard (`.gradle/`, `build/`, `local.properties`, `.idea/`, `*.keystore`, `*.jks`, `keystore.properties`). Workflow `android.yml` : checkout, JDK 17 Temurin, `./gradlew testDebugUnitTest assembleDebug`.
- [ ] **Step 9 : commit.** `git init`, puis `git add -A` et `git commit -m "chore: socle du projet Android"`.

### Task 1 : modèles du domaine, jours et calcul d'échéance

**Files :**

- Create : `…/domain/model/Alarm.kt`, `…/domain/model/WakeEvent.kt`, `…/domain/model/RingingState.kt`, `…/domain/port/Ports.kt`, `…/domain/alarm/ComputeNextTrigger.kt`, `…/data/db/DaysMask.kt`
- Test : `app/src/test/java/com/bytechnum/reveil/domain/alarm/ComputeNextTriggerTest.kt`, `app/src/test/java/com/bytechnum/reveil/data/db/DaysMaskTest.kt`

**Interfaces :**

- Produces :
  - `data class Alarm(val id: Int = 0, val hour: Int, val minute: Int, val days: Set<DayOfWeek>, val enabled: Boolean, val label: String, val soundId: SoundId, val vibrate: Boolean, val challengeType: ChallengeType, val challengeTarget: Int, val difficulty: Difficulty, val createdAt: Long, val updatedAt: Long)` avec `val isRepeating get() = days.isNotEmpty()` et `companion object { fun new(nowMillis: Long): Alarm }` qui applique les valeurs par défaut des Global Constraints.
  - `enum class ChallengeType { JUMP, SQUAT, MARCH, JACK, RANDOM }`, `enum class Difficulty { EASY, NORMAL, HARD }`, `enum class SoundId { SOFT, CLASSIC, STRIDENT }`.
  - `data class WakeEvent(val id: Long = 0, val alarmId: Int, val firedAt: Long, val dismissedAt: Long?, val method: WakeMethod, val repsDone: Int, val durationSec: Int)`, `enum class WakeMethod { CHALLENGE, FALLBACK, MISSED, SKIPPED }`.
  - `data class RingingState(val alarmId: Int, val firedAt: Long)`.
  - Dans `Ports.kt` : `interface Clock { fun now(): ZonedDateTime }` ; `interface AlarmStore { fun observeAll(): Flow<List<Alarm>>; suspend fun getById(id: Int): Alarm?; suspend fun upsert(alarm: Alarm): Int; suspend fun delete(id: Int); suspend fun getEnabled(): List<Alarm> }` ; `interface WakeEventStore { suspend fun record(event: WakeEvent) }` ; `interface AlarmScheduling { fun schedule(alarmId: Int, triggerAtMillis: Long); fun cancel(alarmId: Int) }`. `Flow` vient de kotlinx.coroutines, autorisé dans le domaine.
  - `class ComputeNextTrigger @Inject constructor() { operator fun invoke(alarm: Alarm, now: ZonedDateTime): ZonedDateTime }` : prochaine occurrence strictement après `now`, dans le fuseau de `now`.
  - `object DaysMask { fun toMask(days: Set<DayOfWeek>): Int; fun toDays(mask: Int): Set<DayOfWeek> }`.

- [ ] **Step 1 : écrire les tests qui échouent.** `DaysMaskTest` : `toMask(setOf(MONDAY)) == 1`, `toMask(setOf(SUNDAY)) == 64`, `toMask(MONDAY..FRIDAY) == 31`, et aller-retour `toMask(toDays(m)) == m` pour `m` de 0 à 127. `ComputeNextTriggerTest`, avec une fabrique `alarm(hour, minute, days)` basée sur `Alarm.new(0)` :

| Test | Fuseau | now | Alarme | Attendu |
| --- | --- | --- | --- | --- |
| `oneShotLaterToday` | Africa/Porto-Novo | 2026-10-27T06:00 | 07:30, aucun jour | 2026-10-27T07:30 |
| `oneShotEarlierGoesTomorrow` | Africa/Porto-Novo | 2026-10-27T06:00 | 05:00 | 2026-10-28T05:00 |
| `sameMinuteGoesTomorrow` | Africa/Porto-Novo | 2026-10-27T07:30:00 | 07:30 | 2026-10-28T07:30 |
| `weekdaysFromFridayGoesMonday` | Africa/Porto-Novo | 2026-10-30T08:00 | 07:00, lundi-vendredi | 2026-11-02T07:00 |
| `repeatingTodayLater` | Africa/Porto-Novo | 2026-10-27T06:00 | 07:00, mardi | 2026-10-27T07:00 |
| `repeatingTodayPassedGoesNextWeek` | Africa/Porto-Novo | 2026-10-27T08:00 | 07:00, mardi | 2026-11-03T07:00 |
| `midnight` | Africa/Porto-Novo | 2026-10-27T23:59 | 00:00 | 2026-10-28T00:00 |
| `dstGapShiftsToFirstValidMinute` | Europe/Paris | 2027-03-27T23:00 | 02:30 | 2027-03-28T03:00+02:00 |
| `dstOverlapTakesFirstOccurrence` | Europe/Paris | 2026-10-25T00:00 | 02:30 | 2026-10-25T02:30+02:00 |

- [ ] **Step 2 : lancer les tests.** Run : `./gradlew testDebugUnitTest --tests "*ComputeNextTriggerTest" --tests "*DaysMaskTest"`. Expected : échec de compilation (classes absentes).
- [ ] **Step 3 : écrire les modèles, les ports, `DaysMask` et `ComputeNextTrigger`.** Pour un trou d'heure d'été : si `zone.rules.getValidOffsets(localDateTime)` est vide, prendre `zone.rules.getTransition(localDateTime).dateTimeAfter`. Pour un chevauchement, `ZonedDateTime.of` donne déjà le premier décalage. Parcourir au plus 8 jours à partir de la date de `now`.
- [ ] **Step 4 : relancer les tests.** Expected : PASS.
- [ ] **Step 5 : commit** `feat: modèles du domaine et calcul de la prochaine échéance`.

### Task 2 : escalade, règles de sonnerie et cas d'usage

**Files :**

- Create : `…/domain/alarm/EscalationPolicy.kt`, `…/domain/alarm/RingingRules.kt`, `…/domain/usecase/{ScheduleNext,SaveAlarm,DeleteAlarm,SetAlarmEnabled,RescheduleAll,HandleAlarmFired}.kt`
- Test : `app/src/test/java/com/bytechnum/reveil/domain/alarm/{EscalationPolicyTest,RingingRulesTest}.kt`, `app/src/test/java/com/bytechnum/reveil/domain/usecase/{SaveAlarmTest,HandleAlarmFiredTest}.kt`, `app/src/test/java/com/bytechnum/reveil/fakes/Fakes.kt`

**Interfaces :**

- Consumes : Task 1.
- Produces :
  - `enum class VibrationLevel { NORMAL, STRONG, CONTINUOUS }` ; `data class EscalationStage(val volumeFraction: Float, val vibration: VibrationLevel, val fullBrightness: Boolean, val strident: Boolean, val fallbackAvailable: Boolean, val stop: Boolean)` ; `object EscalationPolicy { const val MAX_RING_MS = 900_000L; fun getStageAt(elapsedMs: Long): EscalationStage }` ; `fun computeTargetVolumeIndex(maxIndex: Int, maxVolumePercent: Int, fraction: Float): Int` = `(maxIndex * maxVolumePercent / 100f * fraction).roundToInt().coerceIn(1, maxIndex)`.
  - `sealed interface FireDecision { data class Start(val state: RingingState) : FireDecision; data object Skip : FireDecision }` ; `sealed interface RestartDecision { data class Resume(val state: RingingState) : RestartDecision; data class ExpireAsMissed(val state: RingingState) : RestartDecision; data object Nothing : RestartDecision }` ; `object RingingRules { fun decideOnFire(current: RingingState?, alarmId: Int, nowMillis: Long): FireDecision; fun decideOnRestart(saved: RingingState?, nowMillis: Long): RestartDecision }`.
  - `class ScheduleNext @Inject constructor(scheduling: AlarmScheduling, compute: ComputeNextTrigger, clock: Clock) { operator fun invoke(alarm: Alarm) }`.
  - `class SaveAlarm @Inject constructor(store: AlarmStore, scheduleNext: ScheduleNext, scheduling: AlarmScheduling, clock: Clock) { suspend operator fun invoke(alarm: Alarm): Int }` : enregistre avec `updatedAt` = maintenant, puis planifie si activée, sinon annule.
  - `class DeleteAlarm` `suspend operator fun invoke(id: Int)` : annule puis supprime.
  - `class SetAlarmEnabled` `suspend operator fun invoke(id: Int, enabled: Boolean)`.
  - `class RescheduleAll` `suspend operator fun invoke()` : planifie toutes les alarmes activées.
  - `class HandleAlarmFired` `suspend operator fun invoke(alarmId: Int): Alarm?` : renvoie `null` si l'alarme n'existe plus ou est désactivée (intention périmée). Ponctuelle : la désactive. Répétée : planifie l'occurrence suivante. Appelée au déclenchement, avant la sonnerie, que la sonnerie démarre ou soit sautée.

- [ ] **Step 1 : écrire les tests qui échouent.**
  - `EscalationPolicyTest` : `getStageAt(0)` = `(0.6f, NORMAL, false, false, false, false)` ; `getStageAt(29_999) == getStageAt(0)` ; `getStageAt(30_000)` a `0.8f` et `STRONG` ; `getStageAt(90_000)` a `1.0f`, `CONTINUOUS`, `fullBrightness` ; `getStageAt(300_000).strident` ; `getStageAt(600_000).fallbackAvailable` ; `getStageAt(900_000).stop` ; `getStageAt(899_999).stop` est faux. `computeTargetVolumeIndex(7, 100, 0.6f) == 4`, `(7, 100, 1f) == 7`, `(7, 50, 0.6f) == 2`, `(7, 10, 0.6f) == 1`.
  - `RingingRulesTest` : `decideOnFire(null, 3, 1000) == Start(RingingState(3, 1000))` ; `decideOnFire(RingingState(3, 1000), 4, 2000) == Skip` ; `decideOnRestart(null, 5) == Nothing` ; `decideOnRestart(RingingState(3, 0), 899_999) == Resume(RingingState(3, 0))` ; `decideOnRestart(RingingState(3, 0), 900_000) == ExpireAsMissed(RingingState(3, 0))`.
  - `Fakes.kt` : `FakeAlarmStore` (map en mémoire), `FakeScheduling` (map `alarmId → triggerAtMillis`), `FixedClock(now: ZonedDateTime)`.
  - `SaveAlarmTest` (horloge 2026-10-27T06:00 Africa/Porto-Novo) : une alarme activée 07:30 est enregistrée et planifiée à `2026-10-27T07:30` en millis ; la même avec `enabled = false` n'a aucune entrée dans `FakeScheduling` après enregistrement, y compris si elle était planifiée avant.
  - `HandleAlarmFiredTest` (horloge 2026-10-27T07:00:05) : ponctuelle 07:00 → renvoyée, désactivée dans le store, absente de `FakeScheduling` ; répétée mardi 07:00 → planifiée à 2026-11-03T07:00 ; identifiant inconnu → `null` ; alarme désactivée → `null`.
- [ ] **Step 2 : lancer.** Run : `./gradlew testDebugUnitTest --tests "*EscalationPolicyTest" --tests "*RingingRulesTest" --tests "*SaveAlarmTest" --tests "*HandleAlarmFiredTest"`. Expected : échec de compilation.
- [ ] **Step 3 : implémenter** les classes de la liste Produces.
- [ ] **Step 4 : relancer.** Expected : PASS.
- [ ] **Step 5 : commit** `feat: escalade, règles de sonnerie et cas d'usage des alarmes`.

### Task 3 : stockage (Room, DataStore) dans le stockage protégé par l'appareil

**Files :**

- Create : `…/data/db/{AlarmEntity,WakeEventEntity,AlarmDao,WakeEventDao,AppDatabase,Mappers}.kt`, `…/data/repository/{RoomAlarmStore,RoomWakeEventStore}.kt`, `…/data/prefs/{SettingsStore,RingingStateStore}.kt`, `…/di/AppModule.kt`, `…/di/BindingsModule.kt`, `…/util/DeviceClock.kt`
- Test : `app/src/androidTest/java/com/bytechnum/reveil/data/db/{AlarmDaoTest,DatabaseLocationTest}.kt`

**Interfaces :**

- Consumes : ports et modèles de Task 1.
- Produces :
  - Table `alarms` : `id INTEGER PK autoGenerate, hour, minute, daysMask, enabled, label, soundId TEXT, vibrate, challengeType TEXT, challengeTarget, difficulty TEXT, createdAt, updatedAt`. Table `wake_events` : `id, alarmId, firedAt, dismissedAt NULL, method TEXT, repsDone, durationSec`. Index sur `wake_events.firedAt`. Version 1, `exportSchema = true`.
  - `AlarmDao` : `observeAll(): Flow<List<AlarmEntity>>` trié par `hour, minute` ; `getById(id: Int): AlarmEntity?` ; `upsert(entity: AlarmEntity): Long` ; `delete(id: Int)` ; `getEnabled(): List<AlarmEntity>`. `WakeEventDao` : `insert(entity: WakeEventEntity)` ; `getBetween(fromMillis: Long, toMillis: Long): List<WakeEventEntity>`.
  - `AppDatabase.build(context: Context): AppDatabase` : `Room.databaseBuilder(context.createDeviceProtectedStorageContext(), AppDatabase::class.java, "reveil.db")`.
  - `RoomAlarmStore : AlarmStore`, `RoomWakeEventStore : WakeEventStore`, `DeviceClock : Clock` (`ZonedDateTime.now(ZoneId.systemDefault())` à chaque appel).
  - `SettingsStore` : `val maxVolumePercent: Flow<Int>` (défaut 100), `suspend fun setMaxVolumePercent(v: Int)`. Fichier DataStore `settings` dans le stockage protégé.
  - `RingingStateStore` : `suspend fun read(): RingingState?`, `suspend fun write(state: RingingState)`, `suspend fun clear()`. Fichier DataStore `ringing` dans le stockage protégé.
  - Hilt : `AppModule` fournit `AppDatabase`, les DAO, les deux DataStore, `AlarmManager`, `NotificationManager`, `AudioManager`, et un `@ApplicationScope CoroutineScope` (`SupervisorJob() + Dispatchers.Default`). `BindingsModule` lie les ports à leurs implémentations (`AlarmScheduling` est lié en Task 4).

- [ ] **Step 1 : écrire les tests instrumentés qui échouent.** `AlarmDaoTest` (base en mémoire) : après `upsert` d'une alarme 07:00 puis d'une 06:00, `observeAll().first()` les renvoie dans l'ordre 06:00, 07:00 ; `upsert` avec le même id modifie ; `delete` retire ; `getEnabled()` exclut une alarme désactivée. `DatabaseLocationTest` : après `AppDatabase.build(context).openHelper.writableDatabase`, le fichier `context.createDeviceProtectedStorageContext().getDatabasePath("reveil.db")` existe.
- [ ] **Step 2 : lancer.** Run : `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.bytechnum.reveil.data.db.AlarmDaoTest,com.bytechnum.reveil.data.db.DatabaseLocationTest` avec `ANDROID_SERIAL=<serial>`. Expected : échec de compilation.
- [ ] **Step 3 : implémenter** entités, DAO, base, mappeurs (`DaysMask` pour les jours, `enum.name` pour les énumérations), stores, modules Hilt.
- [ ] **Step 4 : relancer.** Expected : PASS sur le VILLAON.
- [ ] **Step 5 : commit** `feat: stockage des alarmes et des réveils hors du stockage chiffré par l'utilisateur`.

### Task 4 : planification et récepteurs

**Files :**

- Create : `…/data/scheduler/AlarmScheduler.kt`, `…/ringing/AlarmReceiver.kt`, `…/ringing/BootReceiver.kt`
- Modify : `AndroidManifest.xml`, `…/di/BindingsModule.kt`, `…/ReveilApp.kt`
- Test : `app/src/androidTest/java/com/bytechnum/reveil/data/scheduler/AlarmSchedulerTest.kt`

**Interfaces :**

- Consumes : `AlarmScheduling`, `RescheduleAll`, `@ApplicationScope`.
- Produces :
  - `class AlarmScheduler @Inject constructor(@ApplicationContext context: Context, alarmManager: AlarmManager) : AlarmScheduling`. `schedule` appelle `alarmManager.setAlarmClock(AlarmClockInfo(triggerAtMillis, showIntent), fireIntent(alarmId))`. `fireIntent` : `PendingIntent.getBroadcast(context, alarmId, Intent(context, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_FIRE).putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId), FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)`. `showIntent` ouvre `MainActivity`. `cancel` annule l'alarme et l'intention. `fun canScheduleExact(): Boolean` (vrai sous API 31, sinon `alarmManager.canScheduleExactAlarms()`).
  - `AlarmReceiver` : `ACTION_FIRE`, `EXTRA_ALARM_ID`. `onReceive` démarre `AlarmService.fire(context, alarmId)` (défini en Task 6) via `ContextCompat.startForegroundService`.
  - `BootReceiver` : écoute `BOOT_COMPLETED`, `LOCKED_BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`, `MY_PACKAGE_REPLACED`, `SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`, appelle `RescheduleAll` dans `goAsync()`.
  - `ReveilApp.onCreate` lance `RescheduleAll` dans le scope applicatif.
  - Manifeste : `USE_EXACT_ALARM`, `SCHEDULE_EXACT_ALARM` (`maxSdkVersion="32"`), `RECEIVE_BOOT_COMPLETED`, `POST_NOTIFICATIONS`, `VIBRATE`, `WAKE_LOCK`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SYSTEM_EXEMPTED`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `USE_FULL_SCREEN_INTENT`. Les deux récepteurs : `exported="false"` sauf `BootReceiver` (`exported="true"` requis pour les diffusions système), `directBootAware="true"`.

- [ ] **Step 1 : écrire le test qui échoue.** `AlarmSchedulerTest` : `schedule(42, now + 3_600_000)` puis `PendingIntent.getBroadcast(context, 42, <même intent>, FLAG_NO_CREATE or FLAG_IMMUTABLE)` n'est pas nul ; après `cancel(42)`, il est nul.
- [ ] **Step 2 : lancer** (commande de Task 3 avec la classe `AlarmSchedulerTest`). Expected : échec.
- [ ] **Step 3 : implémenter** le planificateur, les récepteurs (un `AlarmService` vide qui appelle `stopSelf()` suffit pour compiler, remplacé en Task 6), le manifeste, la liaison Hilt.
- [ ] **Step 4 : relancer.** Expected : PASS.
- [ ] **Step 5 : commit** `feat: planification exacte et replanification au démarrage`.

### Task 5 : son, volume et vibration

**Files :**

- Create : `tools/generate_sounds.sh`, `app/src/main/res/raw/{alarm_soft,alarm_classic,alarm_strident}.ogg`, `…/data/audio/{AlarmPlayer,AlarmVolumeController,AlarmVibrator}.kt`
- Test : `app/src/test/java/com/bytechnum/reveil/data/audio/AlarmVolumeControllerTest.kt`

**Interfaces :**

- Produces :
  - `interface AlarmStreamVolume { val maxIndex: Int; fun current(): Int; fun set(index: Int) }` et `class AudioManagerStreamVolume(audioManager: AudioManager) : AlarmStreamVolume` sur `STREAM_ALARM`.
  - `class AlarmVolumeController(stream: AlarmStreamVolume) { fun begin(); fun apply(targetIndex: Int); fun enforce(); fun restore() }` : `begin` mémorise le volume d'origine, `apply` fixe la cible et l'applique, `enforce` remet la cible si le volume a bougé, `restore` remet l'origine.
  - `class AlarmPlayer @Inject constructor(@ApplicationContext context: Context, audioManager: AudioManager) { fun start(sound: SoundId, onFocusLost: () -> Unit, onFocusGained: () -> Unit); fun switchTo(sound: SoundId); fun pause(); fun resume(); fun stop() }` : `MediaPlayer` en boucle, `AudioAttributes` `USAGE_ALARM` / `CONTENT_TYPE_SONIFICATION`, focus audio `AUDIOFOCUS_GAIN_TRANSIENT` ; perte de focus → `onFocusLost`, retour → `onFocusGained`.
  - `class AlarmVibrator @Inject constructor(@ApplicationContext context: Context) { fun vibrate(level: VibrationLevel); fun stop() }` : motifs répétés, NORMAL `[0, 500, 1000]`, STRONG `[0, 800, 400]`, CONTINUOUS `[0, 1000, 100]` ; amplitude maximale si l'appareil la gère.
  - Sons, mono 22 050 Hz, Vorbis : `alarm_soft` deux notes sinus 523 Hz et 659 Hz avec fondu, cycle de 2 s ; `alarm_classic` quatre bips 880 Hz de 150 ms séparés de 100 ms puis 600 ms de silence ; `alarm_strident` onde carrée alternant 1 000 Hz et 1 400 Hz toutes les 120 ms, cycle de 1 s. Chacun sous 50 Ko.

- [ ] **Step 1 : écrire le test qui échoue.** `AlarmVolumeControllerTest` avec un faux flux (max 7, courant 3) : `begin()` puis `apply(4)` → courant 4 ; le test fixe le courant à 0, `enforce()` → 4 ; `restore()` → 3.
- [ ] **Step 2 : lancer.** Run : `./gradlew testDebugUnitTest --tests "*AlarmVolumeControllerTest"`. Expected : échec.
- [ ] **Step 3 : implémenter** les trois classes, écrire et exécuter `tools/generate_sounds.sh` (ffmpeg, `lavfi`, `libvorbis`).
- [ ] **Step 4 : relancer.** Expected : PASS. Vérifier `ls -la app/src/main/res/raw` : trois fichiers, chacun sous 50 Ko.
- [ ] **Step 5 : commit** `feat: sonneries, volume imposé et vibration`.

### Task 6 : service de sonnerie et écran de sonnerie

**Files :**

- Create : `…/ringing/{AlarmService,RingingSession,RingingNotifications,RingingActivity}.kt`, `…/ui/ringing/RingingScreen.kt`
- Modify : `AndroidManifest.xml`, `res/values/strings.xml`

**Interfaces :**

- Consumes : `RingingRules`, `EscalationPolicy`, `computeTargetVolumeIndex`, `HandleAlarmFired`, `AlarmStore`, `WakeEventStore`, `RingingStateStore`, `SettingsStore`, `AlarmPlayer`, `AlarmVolumeController`, `AlarmVibrator`.
- Produces :
  - `data class RingingUiState(val alarmId: Int, val label: String, val firedAt: Long, val stage: EscalationStage)` ; `@Singleton class RingingSession @Inject constructor() { val state: StateFlow<RingingUiState?>; fun update(state: RingingUiState?) }`.
  - `AlarmService` (`@AndroidEntryPoint`, `LifecycleService`), `companion object { fun fire(context: Context, alarmId: Int): Intent; fun stopProvisional(context: Context) }`, actions `ACTION_FIRE` et `ACTION_STOP_PROVISIONAL`, `START_STICKY`.
  - Comportement :
    - `ACTION_FIRE` : passer au premier plan immédiatement (notification de Task 6), puis `HandleAlarmFired(alarmId)` ; `null` → arrêt si rien ne sonne. Puis `RingingRules.decideOnFire(stateStore.read(), …)` : `Skip` → enregistrer `WakeEvent(method = SKIPPED, firedAt = now, dismissedAt = null, repsDone = 0, durationSec = 0)` et continuer la sonnerie en cours ; `Start` → écrire l'état, démarrer.
    - Intent nul (redémarrage par le système) : `RingingRules.decideOnRestart` ; `Resume` → reprendre avec le `firedAt` d'origine ; `ExpireAsMissed` → consigner `MISSED`, effacer, s'arrêter ; `Nothing` → s'arrêter.
    - Démarrage de sonnerie : wake lock partiel (timeout 16 min), `volume.begin()`, `player.start(alarm.soundId, …)`, vibration si `alarm.vibrate`, puis boucle d'une seconde : `stage = EscalationPolicy.getStageAt(now - firedAt)` ; à chaque changement de palier, `volume.apply(computeTargetVolumeIndex(...))`, niveau de vibration, `switchTo(STRIDENT)` si `strident` ; `volume.enforce()` à chaque tour sauf en pause ; `session.update(...)` ; `stop` → consigner `MISSED` avec `dismissedAt = now` et `durationSec`, terminer.
    - Perte de focus audio : pause du son et de la vibration ; retour : reprise. Le temps continue.
    - `ACTION_STOP_PROVISIONAL` : terminer sans consigner (bouton provisoire de l'étape 1).
    - Terminer : `player.stop()`, `vibrator.stop()`, `volume.restore()`, `stateStore.clear()`, `session.update(null)`, libérer le wake lock, `stopForeground(STOP_FOREGROUND_REMOVE)`, `stopSelf()`.
    - Premier plan : sous API 34, `startForeground(id, notif)` ; à partir de 34, type `FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED`, et en cas de `SecurityException`, `FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK`. Manifeste : `foregroundServiceType="systemExempted|mediaPlayback"`, `directBootAware="true"`, `exported="false"`.
  - `RingingNotifications` : canal `alarm_ringing`, importance HIGH, sans son ni vibration propres, visibilité publique. Notification catégorie `CATEGORY_ALARM`, `ongoing`, `fullScreenIntent` et `contentIntent` vers `RingingActivity`.
  - `RingingActivity` (`@AndroidEntryPoint`) : `setShowWhenLocked(true)` et `setTurnScreenOn(true)` à partir d'API 27, drapeaux de fenêtre équivalents en dessous ; `FLAG_KEEP_SCREEN_ON` ; retour arrière sans effet ; `screenBrightness = 1f` quand `stage.fullBrightness` ; se ferme quand `RingingSession.state` passe à `null`. Manifeste : `launchMode="singleInstance"`, `excludeFromRecents="true"`, `taskAffinity=""`, `showWhenLocked`, `turnScreenOn`, `directBootAware="true"`.
  - `RingingScreen(state: RingingUiState, onStopProvisional: () -> Unit)` : heure en très grand, libellé, un seul bouton « Arrêter (provisoire) ».

- [ ] **Step 1 : implémenter** session, notifications, service, activité, écran.
- [ ] **Step 2 : compiler et lancer les tests unitaires.** Run : `./gradlew testDebugUnitTest assembleDebug`. Expected : `BUILD SUCCESSFUL`, tous les tests PASS.
- [ ] **Step 3 : vérification rapide sur le téléphone.** Le service n'est pas exporté, donc il ne se déclenche pas par adb ; le vrai essai passe par l'alarme de test de la Task 7. Ici, installer l'APK, ouvrir l'appli, et vérifier que `adb -s <serial> logcat -d -s AndroidRuntime` ne montre aucune exception.
- [ ] **Step 4 : commit** `feat: service de sonnerie au premier plan et écran par-dessus le verrouillage`.

### Task 7 : écrans des alarmes, permissions et alarme de test

**Files :**

- Create : `…/ui/navigation/AppNavHost.kt`, `…/ui/alarms/{AlarmListScreen,AlarmListViewModel,AlarmEditScreen,AlarmEditViewModel}.kt`, `…/ui/components/PermissionBanner.kt`, `…/util/Permissions.kt`
- Modify : `…/MainActivity.kt`, `res/values/strings.xml`
- Test : `app/src/test/java/com/bytechnum/reveil/ui/alarms/AlarmEditViewModelTest.kt`, `app/src/androidTest/java/com/bytechnum/reveil/ui/CreateAlarmTest.kt`, `app/src/androidTest/java/com/bytechnum/reveil/HiltTestRunner.kt`

**Interfaces :**

- Consumes : `AlarmStore`, `SaveAlarm`, `DeleteAlarm`, `SetAlarmEnabled`, `ComputeNextTrigger`, `Clock`, `AlarmScheduler.canScheduleExact()`.
- Produces :
  - Routes `@Serializable data object AlarmListRoute` et `@Serializable data class AlarmEditRoute(val alarmId: Int? = null)`.
  - `AlarmListViewModel` : `val state: StateFlow<AlarmListState>` avec `data class AlarmListState(val alarms: List<Alarm>, val next: NextAlarm?)`, `data class NextAlarm(val alarm: Alarm, val triggerAt: ZonedDateTime)` ; `fun setEnabled(id: Int, enabled: Boolean)` ; `fun createTestAlarm(label: String)` : alarme ponctuelle à l'heure courante + 1 minute (minute arrondie au-dessus), libellé fourni par l'écran depuis `R.string.test_alarm_label`, enregistrée par `SaveAlarm`.
  - `AlarmEditViewModel(savedStateHandle, store, saveAlarm, deleteAlarm, clock)` : `val state: StateFlow<Alarm>` (alarme chargée, ou `Alarm.new` si `alarmId` nul) ; setters `setTime(hour, minute)`, `toggleDay(day: DayOfWeek)`, `setLabel`, `setSound`, `setVibrate`, `setChallenge`, `setTarget` (borné 5 à 50), `setDifficulty` ; `fun save(onDone: () -> Unit)` ; `fun delete(onDone: () -> Unit)`.
  - Liste : prochaine alarme en évidence (« Sonne dans 7 h 12 min »), cartes avec heure, jours, libellé, interrupteur ; bouton + ; action « Alarme de test (1 min) » dans la barre du haut ; `PermissionBanner` au-dessus de la liste.
  - Édition : `TimePicker` Material 3 en 24 h, puces L M M J V S D, libellé, son (avec écoute de 3 s), vibration, défi (liste déroulante, cinq valeurs dont « Au hasard »), objectif (curseur 5-50), difficulté (boutons segmentés), Enregistrer, Supprimer (édition seulement).
  - `Permissions` : `fun findMissingPermissions(context: Context): Set<MissingPermission>` avec `enum class MissingPermission { NOTIFICATIONS, FULL_SCREEN, EXACT_ALARM }` (notifications : API 33+ et non accordée ; plein écran : API 34+ et `!canUseFullScreenIntent()` ; alarme exacte : `!canScheduleExact()`). Chaque élément du bandeau ouvre la demande ou le réglage (`ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT`, `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`). Le bandeau se recalcule à chaque `ON_RESUME`.

- [ ] **Step 1 : écrire les tests qui échouent.**
  - `AlarmEditViewModelTest` (faux store, `SaveAlarm` construit avec les faux de Task 2, `Dispatchers.setMain`) : sans `alarmId`, l'état vaut les valeurs par défaut ; `setTarget(3)` donne 5 et `setTarget(80)` donne 50 ; après `setTime(6, 45)`, `toggleDay(MONDAY)` et `save`, le store contient une alarme 06:45 lundi, `FakeScheduling` a une entrée, et `onDone` est appelé.
  - `CreateAlarmTest` (Hilt, `createAndroidComposeRule<MainActivity>()`, `HiltTestRunner` déclaré dans `testInstrumentationRunner`) : toucher le bouton +, toucher « Enregistrer », la liste affiche « 07:00 ».
- [ ] **Step 2 : lancer.** Expected : échec de compilation.
- [ ] **Step 3 : implémenter** navigation, écrans, ViewModels, bandeau.
- [ ] **Step 4 : relancer** les tests unitaires puis `CreateAlarmTest` sur le VILLAON. Expected : PASS.
- [ ] **Step 5 : commit** `feat: liste et édition des alarmes, bandeau des permissions, alarme de test`.

### Task 8 : vérification sur le VILLAON

**Files :**

- Create : `documentations/verification.md`

- [ ] **Step 1 : installer** la build de debug et ouvrir l'appli. Accorder les permissions du bandeau.
- [ ] **Step 2 : scénarios pilotés par adb**, chacun avec « Alarme de test (1 min) » :
  1. Écran éteint et verrouillé (`adb shell input keyevent KEYCODE_SLEEP`) : l'écran s'allume, la sonnerie part, l'écran de sonnerie est par-dessus le verrouillage (`adb shell dumpsys window | grep -i mCurrentFocus` montre `RingingActivity`).
  2. Appli retirée des tâches récentes avant l'heure : la sonnerie part. Si elle ne part pas, noter le comportement de la surcouche pour le guide de fiabilité.
  3. Volume d'alarme à 0 avant l'heure (`adb shell media volume --stream 4 --set 0`) : le son est audible ; après l'arrêt, `media volume --stream 4 --get` renvoie 0.
  4. Escalade : laisser sonner et relever `media volume --stream 4 --get` à 10 s, 40 s et 100 s (environ 60 %, 80 %, 100 % du maximum).
  5. Deux alarmes à la même minute : une seule sonnerie, aucun plantage dans `adb logcat -s AndroidRuntime`.
  6. `adb shell am kill` n'agit pas sur un service au premier plan : tuer le processus avec `adb shell am crash com.bytechnum.reveil` pendant la sonnerie, vérifier que la sonnerie reprend.
  7. Laisser sonner 15 minutes : arrêt seul.
- [ ] **Step 3 : scénarios confiés à l'utilisateur**, avec ces consignes à lui transmettre :
  1. Redémarrage : créer l'alarme de test réglée 5 minutes plus tard, redémarrer le téléphone, ne pas le déverrouiller. L'alarme doit sonner sur l'écran verrouillé.
  2. Mode Ne pas déranger activé : l'alarme sonne.
  3. Appel entrant pendant la sonnerie : la sonnerie s'interrompt puis reprend après l'appel.
- [ ] **Step 4 : consigner** chaque scénario (date, résultat, remarque) dans `documentations/verification.md`.
- [ ] **Step 5 : commit** `docs: vérification de l'étape 1 sur le VILLAON V40s`.
