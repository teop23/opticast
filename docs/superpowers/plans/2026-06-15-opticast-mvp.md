# Opticast MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build Opticast, an open-source Android app that streams the phone camera + mic to any user-specified server over RTMP or SRT, with background streaming, adaptive bitrate, auto-reconnect, and encrypted credential storage.

**Architecture:** Layered. A `Broadcaster` interface hides the RootEncoder library so all logic/UI is testable against a `FakeBroadcaster`. Pure-logic units (connection model, validation, repository, adaptive bitrate, reconnect, view model) are built with strict TDD. The RootEncoder adapter, foreground service, and Compose UI are integration code verified manually against a local MediaMTX relay.

**Tech Stack:** Kotlin, Jetpack Compose, Coroutines/StateFlow, DataStore (profiles), Jetpack Security `EncryptedSharedPreferences` (secrets), RootEncoder 2.7.4 (`com.github.pedroSG94.RootEncoder:library:2.7.4`). Tests: JUnit4, kotlinx-coroutines-test, Turbine. License: GPLv3.

**Conventions:**
- Package: `com.opticast` (placeholder — switch to a namespace you control, e.g. `io.github.<you>.opticast`, before the first public release).
- minSdk 26, compileSdk/targetSdk 34.
- Every git commit message ends with a trailer line: `Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>`
- Run unit tests with `./gradlew testDebugUnitTest` (Windows: `.\gradlew.bat`).

---

## File structure

```
opticast/
  LICENSE                         GPLv3 text
  settings.gradle.kts
  build.gradle.kts                root
  gradle/libs.versions.toml       version catalog
  app/
    build.gradle.kts
    src/main/AndroidManifest.xml
    src/main/java/com/opticast/
      OpticastApp.kt              Application
      model/
        Connection.kt             profile data + StreamProtocol + endpoint builder
        StreamState.kt            sealed stream state + StreamStats
      data/
        ProfileStore.kt           interface (non-secret persistence)
        SecretStore.kt            interface (secret persistence)
        ConnectionRepository.kt   combines the two stores
        DataStoreProfileStore.kt  DataStore impl
        EncryptedSecretStore.kt   EncryptedSharedPreferences impl
      stream/
        Broadcaster.kt            interface + events
        RootEncoderBroadcaster.kt GenericStream impl
        AdaptiveBitrateController.kt
        ReconnectController.kt
      service/
        StreamingService.kt       foreground service
      ui/
        StreamViewModel.kt
        LiveScreen.kt
        ConnectionsScreen.kt
        SettingsScreen.kt
        OpticastNavHost.kt
      util/
        ConnectionValidator.kt
    src/test/java/com/opticast/
      model/ConnectionTest.kt
      util/ConnectionValidatorTest.kt
      data/ConnectionRepositoryTest.kt
      stream/AdaptiveBitrateControllerTest.kt
      stream/ReconnectControllerTest.kt
      ui/StreamViewModelTest.kt
      fakes/FakeBroadcaster.kt
      fakes/InMemoryStores.kt
```

---

## Task 0: Project scaffold

**Files:**
- Create: `opticast/settings.gradle.kts`, `opticast/build.gradle.kts`, `opticast/gradle/libs.versions.toml`, `opticast/app/build.gradle.kts`, `opticast/app/src/main/AndroidManifest.xml`, `opticast/LICENSE`
- Create: `opticast/.gitignore`

- [ ] **Step 1: Init git and .gitignore**

```bash
cd opticast
git init
printf "*.iml\n.gradle\n/local.properties\n/.idea\n/build\n/app/build\n/captures\n.externalNativeBuild\n.cxx\n" > .gitignore
```

- [ ] **Step 2: Add GPLv3 LICENSE**

Download the canonical text:
```bash
curl -L https://www.gnu.org/licenses/gpl-3.0.txt -o LICENSE
```
Expected: a `LICENSE` file ~35KB starting with "GNU GENERAL PUBLIC LICENSE / Version 3".

- [ ] **Step 3: Version catalog** — `gradle/libs.versions.toml`

```toml
[versions]
agp = "8.5.2"
kotlin = "2.0.20"
coreKtx = "1.13.1"
lifecycle = "2.8.4"
activityCompose = "1.9.1"
composeBom = "2024.08.00"
datastore = "1.1.1"
securityCrypto = "1.1.0-alpha06"
coroutines = "1.8.1"
rootencoder = "2.7.4"
junit = "4.13.2"
coroutinesTest = "1.8.1"
turbine = "1.1.0"

[libraries]
core-ktx = { module = "androidx.core:core-ktx", version.ref = "coreKtx" }
lifecycle-runtime-ktx = { module = "androidx.lifecycle:lifecycle-runtime-ktx", version.ref = "lifecycle" }
lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activityCompose" }
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-material3 = { module = "androidx.compose.material3:material3" }
compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
security-crypto = { module = "androidx.security:security-crypto", version.ref = "securityCrypto" }
coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
rootencoder = { module = "com.github.pedroSG94.RootEncoder:library", version.ref = "rootencoder" }
junit = { module = "junit:junit", version.ref = "junit" }
coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutinesTest" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

- [ ] **Step 4: Root build + settings**

`build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
}
```

`settings.gradle.kts`:
```kotlin
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google(); mavenCentral()
        maven { url = uri("https://jitpack.io") } // RootEncoder
    }
}
rootProject.name = "Opticast"
include(":app")
```

- [ ] **Step 5: App build file** — `app/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.opticast"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.opticast"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.datastore.preferences)
    implementation(libs.security.crypto)
    implementation(libs.coroutines.android)
    implementation(libs.rootencoder)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
}
```

- [ ] **Step 6: Manifest with permissions**

`app/src/main/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_CAMERA" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

    <uses-feature android:name="android.hardware.camera.any" android:required="true" />

    <application
        android:name=".OpticastApp"
        android:allowBackup="false"
        android:label="Opticast"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.NoActionBar">

        <activity
            android:name=".ui.MainActivity"
            android:exported="true"
            android:screenOrientation="fullSensor">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".service.StreamingService"
            android:exported="false"
            android:foregroundServiceType="camera|microphone" />
    </application>
</manifest>
```

- [ ] **Step 7: Verify it builds**

Run: `.\gradlew.bat :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`. (First run downloads dependencies including RootEncoder from JitPack.)

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "chore: scaffold Opticast Android project (GPLv3, RootEncoder 2.7.4)

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 1: Connection model + endpoint builder

**Files:**
- Create: `app/src/main/java/com/opticast/model/Connection.kt`
- Test: `app/src/test/java/com/opticast/model/ConnectionTest.kt`

- [ ] **Step 1: Write the failing test**

`ConnectionTest.kt`:
```kotlin
package com.opticast.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectionTest {

    private fun base() = Connection(
        id = "1", name = "Home relay", protocol = StreamProtocol.RTMP,
        host = "192.168.178.208", port = 1935, path = "live/ride",
        secret = null, width = 1280, height = 720, fps = 30,
        videoBitrate = 2_500_000, audioBitrate = 128_000
    )

    @Test fun `rtmp endpoint without secret`() {
        assertEquals("rtmp://192.168.178.208:1935/live/ride", base().toEndpoint())
    }

    @Test fun `rtmp endpoint embeds user-pass secret`() {
        val c = base().copy(secret = "user:pass")
        assertEquals("rtmp://user:pass@192.168.178.208:1935/live/ride", c.toEndpoint())
    }

    @Test fun `srt endpoint puts path in streamid and passphrase in query`() {
        val c = base().copy(protocol = StreamProtocol.SRT, port = 8890, secret = "myphrase")
        assertEquals(
            "srt://192.168.178.208:8890?streamid=publish/live/ride&passphrase=myphrase",
            c.toEndpoint()
        )
    }

    @Test fun `srt endpoint without passphrase omits query passphrase`() {
        val c = base().copy(protocol = StreamProtocol.SRT, port = 8890, secret = null)
        assertEquals(
            "srt://192.168.178.208:8890?streamid=publish/live/ride",
            c.toEndpoint()
        )
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.opticast.model.ConnectionTest"`
Expected: FAIL — `Connection` / `StreamProtocol` unresolved.

- [ ] **Step 3: Write minimal implementation**

`Connection.kt`:
```kotlin
package com.opticast.model

enum class StreamProtocol { RTMP, SRT }

data class Connection(
    val id: String,
    val name: String,
    val protocol: StreamProtocol,
    val host: String,
    val port: Int,
    val path: String,
    val secret: String?,          // RTMP "user:pass" or SRT passphrase
    val width: Int,
    val height: Int,
    val fps: Int,
    val videoBitrate: Int,        // bps
    val audioBitrate: Int         // bps
) {
    fun toEndpoint(): String = when (protocol) {
        StreamProtocol.RTMP -> {
            val auth = if (secret.isNullOrBlank()) "" else "$secret@"
            "rtmp://$auth$host:$port/$path"
        }
        StreamProtocol.SRT -> {
            val pass = if (secret.isNullOrBlank()) "" else "&passphrase=$secret"
            "srt://$host:$port?streamid=publish/$path$pass"
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.opticast.model.ConnectionTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/opticast/model/Connection.kt app/src/test/java/com/opticast/model/ConnectionTest.kt
git commit -m "feat: connection model with RTMP/SRT endpoint builder

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 2: Connection validation + unencrypted-transport warning

**Files:**
- Create: `app/src/main/java/com/opticast/util/ConnectionValidator.kt`
- Test: `app/src/test/java/com/opticast/util/ConnectionValidatorTest.kt`

- [ ] **Step 1: Write the failing test**

`ConnectionValidatorTest.kt`:
```kotlin
package com.opticast.util

import com.opticast.model.Connection
import com.opticast.model.StreamProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionValidatorTest {

    private fun c() = Connection("1", "n", StreamProtocol.RTMP, "relay.example.com",
        1935, "live/x", null, 1280, 720, 30, 2_500_000, 128_000)

    @Test fun `valid connection has no errors`() {
        assertTrue(ConnectionValidator.validate(c()).errors.isEmpty())
    }

    @Test fun `blank host is an error`() {
        assertTrue(ConnectionValidator.validate(c().copy(host = " ")).errors.isNotEmpty())
    }

    @Test fun `port out of range is an error`() {
        assertTrue(ConnectionValidator.validate(c().copy(port = 70000)).errors.isNotEmpty())
    }

    @Test fun `plain rtmp to remote host warns about encryption`() {
        val r = ConnectionValidator.validate(c())
        assertTrue(r.warnings.any { it.contains("unencrypted", ignoreCase = true) })
    }

    @Test fun `plain rtmp to localhost does not warn`() {
        val r = ConnectionValidator.validate(c().copy(host = "127.0.0.1"))
        assertEquals(emptyList<String>(), r.warnings)
    }

    @Test fun `srt with passphrase to remote does not warn`() {
        val r = ConnectionValidator.validate(
            c().copy(protocol = StreamProtocol.SRT, port = 8890, secret = "phrase")
        )
        assertEquals(emptyList<String>(), r.warnings)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.opticast.util.ConnectionValidatorTest"`
Expected: FAIL — `ConnectionValidator` unresolved.

- [ ] **Step 3: Write minimal implementation**

`ConnectionValidator.kt`:
```kotlin
package com.opticast.util

import com.opticast.model.Connection
import com.opticast.model.StreamProtocol

data class ValidationResult(val errors: List<String>, val warnings: List<String>)

object ConnectionValidator {

    private val LOCAL_HOSTS = setOf("127.0.0.1", "localhost", "::1")

    fun validate(c: Connection): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (c.name.isBlank()) errors += "Name is required"
        if (c.host.isBlank()) errors += "Host is required"
        if (c.port !in 1..65535) errors += "Port must be between 1 and 65535"
        if (c.width <= 0 || c.height <= 0) errors += "Resolution must be positive"
        if (c.fps !in 1..120) errors += "FPS must be between 1 and 120"
        if (c.videoBitrate <= 0) errors += "Video bitrate must be positive"

        val remote = c.host.trim().lowercase() !in LOCAL_HOSTS &&
            !c.host.startsWith("192.168.") && !c.host.startsWith("10.")
        if (remote) {
            val encrypted = when (c.protocol) {
                StreamProtocol.RTMP -> false // plain RTMP is never encrypted
                StreamProtocol.SRT -> !c.secret.isNullOrBlank() // passphrase => AES
            }
            if (!encrypted) {
                warnings += "Stream and credentials will be sent unencrypted to a remote host. " +
                    "Use SRT with a passphrase or an RTMPS server."
            }
        }
        return ValidationResult(errors, warnings)
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.opticast.util.ConnectionValidatorTest"`
Expected: PASS (6 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/opticast/util/ConnectionValidator.kt app/src/test/java/com/opticast/util/ConnectionValidatorTest.kt
git commit -m "feat: connection validation with unencrypted-transport warning

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 3: Connection repository over store interfaces

**Files:**
- Create: `app/src/main/java/com/opticast/data/ProfileStore.kt`, `SecretStore.kt`, `ConnectionRepository.kt`
- Create: `app/src/test/java/com/opticast/fakes/InMemoryStores.kt`
- Test: `app/src/test/java/com/opticast/data/ConnectionRepositoryTest.kt`

- [ ] **Step 1: Define the store interfaces (no test — pure interfaces)**

`ProfileStore.kt`:
```kotlin
package com.opticast.data

import com.opticast.model.Connection
import kotlinx.coroutines.flow.Flow

/** Persists everything about a Connection EXCEPT its secret. */
interface ProfileStore {
    fun profiles(): Flow<List<Connection>>
    suspend fun upsert(connection: Connection)      // secret field ignored here
    suspend fun delete(id: String)
}
```

`SecretStore.kt`:
```kotlin
package com.opticast.data

/** Persists secrets (passwords / passphrases) in encrypted storage, keyed by connection id. */
interface SecretStore {
    fun get(id: String): String?
    fun put(id: String, secret: String?)
    fun remove(id: String)
}
```

- [ ] **Step 2: Write the failing test (with in-memory fakes)**

`InMemoryStores.kt`:
```kotlin
package com.opticast.fakes

import com.opticast.data.ProfileStore
import com.opticast.data.SecretStore
import com.opticast.model.Connection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryProfileStore : ProfileStore {
    private val state = MutableStateFlow<List<Connection>>(emptyList())
    override fun profiles() = state.asStateFlow()
    override suspend fun upsert(connection: Connection) {
        val stripped = connection.copy(secret = null)
        state.value = state.value.filter { it.id != connection.id } + stripped
    }
    override suspend fun delete(id: String) {
        state.value = state.value.filter { it.id != id }
    }
}

class InMemorySecretStore : SecretStore {
    val map = mutableMapOf<String, String>()
    override fun get(id: String) = map[id]
    override fun put(id: String, secret: String?) {
        if (secret == null) map.remove(id) else map[id] = secret
    }
    override fun remove(id: String) { map.remove(id) }
}
```

`ConnectionRepositoryTest.kt`:
```kotlin
package com.opticast.data

import app.cash.turbine.test
import com.opticast.fakes.InMemoryProfileStore
import com.opticast.fakes.InMemorySecretStore
import com.opticast.model.Connection
import com.opticast.model.StreamProtocol
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConnectionRepositoryTest {

    private val profiles = InMemoryProfileStore()
    private val secrets = InMemorySecretStore()
    private val repo = ConnectionRepository(profiles, secrets)

    private fun conn(id: String, secret: String?) = Connection(
        id, "n", StreamProtocol.RTMP, "h", 1935, "p", secret,
        1280, 720, 30, 2_500_000, 128_000
    )

    @Test fun `save then read rehydrates secret from secret store`() = runTest {
        repo.save(conn("a", "user:pass"))
        repo.connections().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("user:pass", list.first().secret)
        }
    }

    @Test fun `secret is stored in secret store, not profile store`() = runTest {
        repo.save(conn("a", "topsecret"))
        assertNull(profiles.profiles().value.first().secret) // profile store stripped
        assertEquals("topsecret", secrets.get("a"))           // secret store has it
    }

    @Test fun `delete removes profile and secret`() = runTest {
        repo.save(conn("a", "s"))
        repo.delete("a")
        repo.connections().test { assertEquals(0, awaitItem().size) }
        assertNull(secrets.get("a"))
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.opticast.data.ConnectionRepositoryTest"`
Expected: FAIL — `ConnectionRepository` unresolved.

- [ ] **Step 4: Write minimal implementation**

`ConnectionRepository.kt`:
```kotlin
package com.opticast.data

import com.opticast.model.Connection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ConnectionRepository(
    private val profileStore: ProfileStore,
    private val secretStore: SecretStore
) {
    /** Profiles with their secrets rehydrated from the encrypted store. */
    fun connections(): Flow<List<Connection>> =
        profileStore.profiles().map { list ->
            list.map { it.copy(secret = secretStore.get(it.id)) }
        }

    suspend fun save(connection: Connection) {
        secretStore.put(connection.id, connection.secret)
        profileStore.upsert(connection)           // store strips the secret itself
    }

    suspend fun delete(id: String) {
        profileStore.delete(id)
        secretStore.remove(id)
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.opticast.data.ConnectionRepositoryTest"`
Expected: PASS (3 tests).

- [ ] **Step 6: Add the real persistence implementations (integration code, no unit test)**

`DataStoreProfileStore.kt`:
```kotlin
package com.opticast.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.opticast.model.Connection
import com.opticast.model.StreamProtocol
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "opticast_profiles")
private val PROFILES_KEY = stringPreferencesKey("profiles_json")

class DataStoreProfileStore(private val context: Context) : ProfileStore {

    override fun profiles(): Flow<List<Connection>> =
        context.dataStore.data.map { prefs -> decode(prefs[PROFILES_KEY] ?: "[]") }

    override suspend fun upsert(connection: Connection) {
        val stripped = connection.copy(secret = null)
        context.dataStore.edit { prefs ->
            val current = decode(prefs[PROFILES_KEY] ?: "[]")
                .filter { it.id != stripped.id } + stripped
            prefs[PROFILES_KEY] = encode(current)
        }
    }

    override suspend fun delete(id: String) {
        context.dataStore.edit { prefs ->
            val current = decode(prefs[PROFILES_KEY] ?: "[]").filter { it.id != id }
            prefs[PROFILES_KEY] = encode(current)
        }
    }

    private fun encode(list: List<Connection>): String {
        val arr = JSONArray()
        list.forEach { c ->
            arr.put(JSONObject().apply {
                put("id", c.id); put("name", c.name); put("protocol", c.protocol.name)
                put("host", c.host); put("port", c.port); put("path", c.path)
                put("width", c.width); put("height", c.height); put("fps", c.fps)
                put("videoBitrate", c.videoBitrate); put("audioBitrate", c.audioBitrate)
            })
        }
        return arr.toString()
    }

    private fun decode(json: String): List<Connection> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Connection(
                id = o.getString("id"), name = o.getString("name"),
                protocol = StreamProtocol.valueOf(o.getString("protocol")),
                host = o.getString("host"), port = o.getInt("port"), path = o.getString("path"),
                secret = null,
                width = o.getInt("width"), height = o.getInt("height"), fps = o.getInt("fps"),
                videoBitrate = o.getInt("videoBitrate"), audioBitrate = o.getInt("audioBitrate")
            )
        }
    }
}
```

`EncryptedSecretStore.kt`:
```kotlin
package com.opticast.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class EncryptedSecretStore(context: Context) : SecretStore {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "opticast_secrets",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun get(id: String): String? = prefs.getString(id, null)
    override fun put(id: String, secret: String?) {
        prefs.edit().apply { if (secret == null) remove(id) else putString(id, secret) }.apply()
    }
    override fun remove(id: String) { prefs.edit().remove(id).apply() }
}
```

- [ ] **Step 7: Build to confirm integration code compiles**

Run: `.\gradlew.bat :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/opticast/data app/src/test/java/com/opticast/data app/src/test/java/com/opticast/fakes/InMemoryStores.kt
git commit -m "feat: connection repository with encrypted secret storage

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 4: Broadcaster interface, stream state, and FakeBroadcaster

**Files:**
- Create: `app/src/main/java/com/opticast/model/StreamState.kt`
- Create: `app/src/main/java/com/opticast/stream/Broadcaster.kt`
- Create: `app/src/test/java/com/opticast/fakes/FakeBroadcaster.kt`

This task defines contracts only; it is verified by the tasks that consume it (5, 6, 7). No standalone test.

- [ ] **Step 1: Stream state + stats**

`StreamState.kt`:
```kotlin
package com.opticast.model

sealed interface StreamState {
    data object Idle : StreamState
    data object Connecting : StreamState
    data object Live : StreamState
    data class Reconnecting(val attempt: Int) : StreamState
    data class Error(val reason: String) : StreamState
}

data class StreamStats(
    val bitrateBps: Long = 0,
    val fps: Int = 0,
    val droppedFrames: Long = 0,
    val uptimeSeconds: Long = 0
)
```

- [ ] **Step 2: Broadcaster interface**

`Broadcaster.kt`:
```kotlin
package com.opticast.stream

import com.opticast.model.Connection
import com.opticast.model.StreamState
import com.opticast.model.StreamStats
import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction over the streaming engine. The only thing that touches RootEncoder is the
 * production implementation; everything else (UI, controllers) depends on this interface.
 */
interface Broadcaster {
    val state: StateFlow<StreamState>
    val stats: StateFlow<StreamStats>

    /** Reported bitrate from the network layer (bps), for adaptive bitrate. */
    val reportedBitrate: StateFlow<Long>

    fun start(connection: Connection)
    fun stop()
    fun switchCamera()
    fun setVideoBitrate(bps: Int)
    fun setMuted(muted: Boolean)
    fun setTorch(on: Boolean)
    fun release()
}
```

- [ ] **Step 3: FakeBroadcaster for tests**

`FakeBroadcaster.kt`:
```kotlin
package com.opticast.fakes

import com.opticast.model.Connection
import com.opticast.model.StreamState
import com.opticast.model.StreamStats
import com.opticast.stream.Broadcaster
import kotlinx.coroutines.flow.MutableStateFlow

class FakeBroadcaster : Broadcaster {
    override val state = MutableStateFlow<StreamState>(StreamState.Idle)
    override val stats = MutableStateFlow(StreamStats())
    override val reportedBitrate = MutableStateFlow(0L)

    var startedWith: Connection? = null
    var lastBitrate: Int? = null
    var muted = false
    var torch = false
    var switchCount = 0
    var released = false

    override fun start(connection: Connection) {
        startedWith = connection
        state.value = StreamState.Connecting
    }
    override fun stop() { state.value = StreamState.Idle }
    override fun switchCamera() { switchCount++ }
    override fun setVideoBitrate(bps: Int) { lastBitrate = bps }
    override fun setMuted(muted: Boolean) { this.muted = muted }
    override fun setTorch(on: Boolean) { torch = on }
    override fun release() { released = true }

    // Test helpers
    fun emitState(s: StreamState) { state.value = s }
    fun emitReportedBitrate(bps: Long) { reportedBitrate.value = bps }
}
```

- [ ] **Step 4: Build to confirm it compiles**

Run: `.\gradlew.bat :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/opticast/model/StreamState.kt app/src/main/java/com/opticast/stream/Broadcaster.kt app/src/test/java/com/opticast/fakes/FakeBroadcaster.kt
git commit -m "feat: Broadcaster interface, stream state, and test fake

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 5: AdaptiveBitrateController

Algorithm: on each reported-bitrate sample, if the link is sustaining near the target, step bitrate up toward the configured max; if the reported bitrate drops below a fraction of the current target, step down toward the configured min. Bounded, with discrete steps to avoid oscillation.

**Files:**
- Create: `app/src/main/java/com/opticast/stream/AdaptiveBitrateController.kt`
- Test: `app/src/test/java/com/opticast/stream/AdaptiveBitrateControllerTest.kt`

- [ ] **Step 1: Write the failing test**

`AdaptiveBitrateControllerTest.kt`:
```kotlin
package com.opticast.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveBitrateControllerTest {

    private fun controller() = AdaptiveBitrateController(
        minBps = 500_000, maxBps = 4_000_000, startBps = 2_000_000, stepBps = 250_000
    )

    @Test fun `starts at start bitrate`() {
        assertEquals(2_000_000, controller().currentBps)
    }

    @Test fun `healthy link steps up but not above max`() {
        val c = controller()
        // reported >= 90% of target => step up
        repeat(20) { c.onReportedBitrate((c.currentBps * 0.95).toLong()) }
        assertEquals(4_000_000, c.currentBps)
    }

    @Test fun `congested link steps down but not below min`() {
        val c = controller()
        // reported < 50% of target => step down
        repeat(20) { c.onReportedBitrate((c.currentBps * 0.2).toLong()) }
        assertEquals(500_000, c.currentBps)
    }

    @Test fun `step down emits new target to callback`() {
        var emitted = -1
        val c = AdaptiveBitrateController(500_000, 4_000_000, 2_000_000, 250_000) { emitted = it }
        c.onReportedBitrate(100_000) // very congested
        assertEquals(1_750_000, emitted)
        assertEquals(1_750_000, c.currentBps)
    }

    @Test fun `stable mid-range link does not change bitrate`() {
        val c = controller()
        // between 50% and 90% => hold
        c.onReportedBitrate((c.currentBps * 0.7).toLong())
        assertEquals(2_000_000, c.currentBps)
    }

    @Test fun `current bitrate always within bounds`() {
        val c = controller()
        repeat(100) { c.onReportedBitrate(if (it % 2 == 0) 10_000_000 else 1) }
        assertTrue(c.currentBps in 500_000..4_000_000)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.opticast.stream.AdaptiveBitrateControllerTest"`
Expected: FAIL — `AdaptiveBitrateController` unresolved.

- [ ] **Step 3: Write minimal implementation**

`AdaptiveBitrateController.kt`:
```kotlin
package com.opticast.stream

/**
 * Pure-logic adaptive bitrate. Fed reported bitrate samples (bps); emits a new target bitrate
 * via [onChange] when it steps up or down. Caller wires onChange to Broadcaster.setVideoBitrate.
 */
class AdaptiveBitrateController(
    private val minBps: Int,
    private val maxBps: Int,
    startBps: Int,
    private val stepBps: Int,
    private val onChange: (Int) -> Unit = {}
) {
    var currentBps: Int = startBps.coerceIn(minBps, maxBps)
        private set

    fun onReportedBitrate(reportedBps: Long) {
        val ratio = if (currentBps == 0) 0.0 else reportedBps.toDouble() / currentBps
        val next = when {
            ratio < 0.5 -> (currentBps - stepBps)
            ratio >= 0.9 -> (currentBps + stepBps)
            else -> currentBps
        }.coerceIn(minBps, maxBps)

        if (next != currentBps) {
            currentBps = next
            onChange(next)
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.opticast.stream.AdaptiveBitrateControllerTest"`
Expected: PASS (6 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/opticast/stream/AdaptiveBitrateController.kt app/src/test/java/com/opticast/stream/AdaptiveBitrateControllerTest.kt
git commit -m "feat: adaptive bitrate controller

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 6: ReconnectController

Backoff schedule with a cap and a max attempt count. Pure logic using a suspend `delay` injected as a lambda so tests use virtual time.

**Files:**
- Create: `app/src/main/java/com/opticast/stream/ReconnectController.kt`
- Test: `app/src/test/java/com/opticast/stream/ReconnectControllerTest.kt`

- [ ] **Step 1: Write the failing test**

`ReconnectControllerTest.kt`:
```kotlin
package com.opticast.stream

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ReconnectControllerTest {

    @Test fun `backoff schedule grows and caps`() {
        val c = ReconnectController(baseMs = 1000, capMs = 5000, maxAttempts = 6)
        assertEquals(1000, c.delayForAttempt(1))
        assertEquals(2000, c.delayForAttempt(2))
        assertEquals(4000, c.delayForAttempt(3))
        assertEquals(5000, c.delayForAttempt(4)) // capped
        assertEquals(5000, c.delayForAttempt(5)) // capped
    }

    @Test fun `runs reconnect attempts until success`() = runTest {
        val c = ReconnectController(baseMs = 1000, capMs = 5000, maxAttempts = 6)
        var attempts = 0
        val delays = mutableListOf<Long>()
        val ok = c.run(
            delayFn = { delays += it },
            attempt = { attempts++; attempts >= 3 } // succeeds on 3rd
        )
        assertEquals(true, ok)
        assertEquals(3, attempts)
        assertEquals(listOf(1000L, 2000L), delays) // delay before attempts 2 and 3
    }

    @Test fun `gives up after max attempts`() = runTest {
        val c = ReconnectController(baseMs = 1000, capMs = 5000, maxAttempts = 3)
        var attempts = 0
        val ok = c.run(delayFn = {}, attempt = { attempts++; false })
        assertEquals(false, ok)
        assertEquals(3, attempts)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.opticast.stream.ReconnectControllerTest"`
Expected: FAIL — `ReconnectController` unresolved.

- [ ] **Step 3: Write minimal implementation**

`ReconnectController.kt`:
```kotlin
package com.opticast.stream

class ReconnectController(
    private val baseMs: Long,
    private val capMs: Long,
    private val maxAttempts: Int
) {
    /** Exponential backoff: base * 2^(attempt-1), capped at capMs. Attempt is 1-based. */
    fun delayForAttempt(attempt: Int): Long {
        val raw = baseMs shl (attempt - 1).coerceAtMost(30)
        return raw.coerceAtMost(capMs)
    }

    /**
     * Tries [attempt] up to [maxAttempts] times. Waits delayForAttempt(n) BEFORE attempts 2..n.
     * Returns true on first success, false if all attempts fail.
     */
    suspend fun run(delayFn: suspend (Long) -> Unit, attempt: suspend () -> Boolean): Boolean {
        for (n in 1..maxAttempts) {
            if (n > 1) delayFn(delayForAttempt(n - 1))
            if (attempt()) return true
        }
        return false
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.opticast.stream.ReconnectControllerTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/opticast/stream/ReconnectController.kt app/src/test/java/com/opticast/stream/ReconnectControllerTest.kt
git commit -m "feat: reconnect controller with capped exponential backoff

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 7: StreamViewModel

Holds UI state, selected connection, and orchestrates the broadcaster. Tested against `FakeBroadcaster` and the in-memory repository.

**Files:**
- Create: `app/src/main/java/com/opticast/ui/StreamViewModel.kt`
- Test: `app/src/test/java/com/opticast/ui/StreamViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

`StreamViewModelTest.kt`:
```kotlin
package com.opticast.ui

import app.cash.turbine.test
import com.opticast.data.ConnectionRepository
import com.opticast.fakes.FakeBroadcaster
import com.opticast.fakes.InMemoryProfileStore
import com.opticast.fakes.InMemorySecretStore
import com.opticast.model.Connection
import com.opticast.model.StreamProtocol
import com.opticast.model.StreamState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StreamViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val broadcaster = FakeBroadcaster()
    private val repo = ConnectionRepository(InMemoryProfileStore(), InMemorySecretStore())
    private lateinit var vm: StreamViewModel

    private fun conn(id: String) = Connection(id, "n", StreamProtocol.RTMP, "127.0.0.1",
        1935, "live/x", null, 1280, 720, 30, 2_500_000, 128_000)

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        vm = StreamViewModel(broadcaster, repo)
    }

    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun `goLive with a selected connection starts the broadcaster`() = runTest {
        vm.selectConnection(conn("a"))
        vm.goLive()
        assertEquals("a", broadcaster.startedWith?.id)
    }

    @Test fun `goLive with no selection sets an error and does not start`() = runTest {
        vm.goLive()
        assertEquals(null, broadcaster.startedWith)
        assertEquals(StreamState.Error("No connection selected"), vm.uiState.value.streamState)
    }

    @Test fun `broadcaster state propagates to ui state`() = runTest {
        vm.uiState.test {
            assertEquals(StreamState.Idle, awaitItem().streamState)
            broadcaster.emitState(StreamState.Live)
            assertEquals(StreamState.Live, awaitItem().streamState)
        }
    }

    @Test fun `toggleMute forwards to broadcaster and updates ui`() = runTest {
        vm.toggleMute()
        assertEquals(true, broadcaster.muted)
        assertEquals(true, vm.uiState.value.muted)
    }

    @Test fun `stop stops the broadcaster`() = runTest {
        vm.selectConnection(conn("a")); vm.goLive()
        vm.stop()
        assertEquals(StreamState.Idle, broadcaster.state.value)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.opticast.ui.StreamViewModelTest"`
Expected: FAIL — `StreamViewModel` unresolved.

- [ ] **Step 3: Write minimal implementation**

`StreamViewModel.kt`:
```kotlin
package com.opticast.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opticast.data.ConnectionRepository
import com.opticast.model.Connection
import com.opticast.model.StreamState
import com.opticast.model.StreamStats
import com.opticast.stream.Broadcaster
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UiState(
    val streamState: StreamState = StreamState.Idle,
    val stats: StreamStats = StreamStats(),
    val selected: Connection? = null,
    val muted: Boolean = false,
    val torch: Boolean = false,
    val connections: List<Connection> = emptyList()
)

class StreamViewModel(
    private val broadcaster: Broadcaster,
    private val repository: ConnectionRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch { broadcaster.state.collect { s -> _ui.update { it.copy(streamState = s) } } }
        viewModelScope.launch { broadcaster.stats.collect { s -> _ui.update { it.copy(stats = s) } } }
        viewModelScope.launch {
            repository.connections().collect { list -> _ui.update { it.copy(connections = list) } }
        }
    }

    fun selectConnection(c: Connection) { _ui.update { it.copy(selected = c) } }

    fun goLive() {
        val c = _ui.value.selected
        if (c == null) {
            _ui.update { it.copy(streamState = StreamState.Error("No connection selected")) }
            return
        }
        broadcaster.start(c)
    }

    fun stop() = broadcaster.stop()
    fun switchCamera() = broadcaster.switchCamera()

    fun toggleMute() {
        val next = !_ui.value.muted
        broadcaster.setMuted(next)
        _ui.update { it.copy(muted = next) }
    }

    fun toggleTorch() {
        val next = !_ui.value.torch
        broadcaster.setTorch(next)
        _ui.update { it.copy(torch = next) }
    }

    suspend fun save(c: Connection) = repository.save(c)
    suspend fun delete(id: String) = repository.delete(id)
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.opticast.ui.StreamViewModelTest"`
Expected: PASS (5 tests).

- [ ] **Step 5: Run the full unit suite**

Run: `.\gradlew.bat testDebugUnitTest`
Expected: PASS — all tests from Tasks 1,2,3,5,6,7 green.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/opticast/ui/StreamViewModel.kt app/src/test/java/com/opticast/ui/StreamViewModelTest.kt
git commit -m "feat: stream view model orchestrating broadcaster and repository

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 8: RootEncoderBroadcaster (real engine)

Integration code — the only file touching RootEncoder. Verified manually in Task 12. API confirmed against RootEncoder 2.7.4: `GenericStream(context, connectChecker)`, `prepareVideo/prepareAudio`, `startStream(url)` (protocol auto-detected), `stopStream`, `startPreview(surfaceView)`, `switchCamera`, `setVideoBitrateOnFly`, `disableAudio/enableAudio`, `release`, `isStreaming`. The `ConnectChecker` callbacks are exactly: `onConnectionStarted`, `onConnectionSuccess`, `onConnectionFailed`, `onNewBitrate`, `onDisconnect`, `onAuthError`, `onAuthSuccess`.

> If a helper name differs in 2.7.4 (e.g. torch is `enableLantern()/disableLantern()`), confirm against the StreamBase wiki page and adjust — the core start/stop/prepare/ConnectChecker API above is verified.

**Files:**
- Create: `app/src/main/java/com/opticast/stream/RootEncoderBroadcaster.kt`

- [ ] **Step 1: Implement the broadcaster**

`RootEncoderBroadcaster.kt`:
```kotlin
package com.opticast.stream

import android.content.Context
import android.view.SurfaceView
import com.opticast.model.Connection
import com.opticast.model.StreamState
import com.opticast.model.StreamStats
import com.pedro.common.ConnectChecker
import com.pedro.library.generic.GenericStream
import kotlinx.coroutines.flow.MutableStateFlow

class RootEncoderBroadcaster(context: Context) : Broadcaster, ConnectChecker {

    override val state = MutableStateFlow<StreamState>(StreamState.Idle)
    override val stats = MutableStateFlow(StreamStats())
    override val reportedBitrate = MutableStateFlow(0L)

    private val stream = GenericStream(context, this).apply {
        getGlInterface().autoHandleOrientation = true
    }
    private var startedAtMs = 0L

    override fun start(connection: Connection) {
        if (stream.isStreaming) return
        val video = stream.prepareVideo(
            width = connection.width, height = connection.height,
            bitrate = connection.videoBitrate, fps = connection.fps, rotation = 0
        )
        val audio = stream.prepareAudio(
            sampleRate = 44100, isStereo = true, bitrate = connection.audioBitrate
        )
        if (!video || !audio) {
            state.value = StreamState.Error("Failed to prepare encoder")
            return
        }
        state.value = StreamState.Connecting
        startedAtMs = System.currentTimeMillis()
        stream.startStream(connection.toEndpoint())
    }

    override fun stop() {
        if (stream.isStreaming) stream.stopStream()
        state.value = StreamState.Idle
    }

    /** Called by the service/UI to render preview; safe to skip for background streaming. */
    fun attachPreview(surfaceView: SurfaceView) { stream.startPreview(surfaceView) }
    fun detachPreview() { if (stream.isOnPreview) stream.stopPreview() }

    override fun switchCamera() { stream.switchCamera() }
    override fun setVideoBitrate(bps: Int) { stream.setVideoBitrateOnFly(bps) }
    override fun setMuted(muted: Boolean) { if (muted) stream.disableAudio() else stream.enableAudio() }
    override fun setTorch(on: Boolean) { if (on) stream.enableLantern() else stream.disableLantern() }
    override fun release() { if (stream.isStreaming) stream.stopStream(); stream.release() }

    // ConnectChecker
    override fun onConnectionStarted(url: String) { state.value = StreamState.Connecting }
    override fun onConnectionSuccess() { state.value = StreamState.Live }
    override fun onConnectionFailed(reason: String) { state.value = StreamState.Error(reason) }
    override fun onNewBitrate(bitrate: Long) {
        reportedBitrate.value = bitrate
        val uptime = (System.currentTimeMillis() - startedAtMs) / 1000
        stats.value = stats.value.copy(bitrateBps = bitrate, uptimeSeconds = uptime)
    }
    override fun onDisconnect() { state.value = StreamState.Idle }
    override fun onAuthError() { state.value = StreamState.Error("Auth error") }
    override fun onAuthSuccess() { /* no-op */ }
}
```

- [ ] **Step 2: Build**

Run: `.\gradlew.bat :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`. If any helper method name (e.g. `enableLantern`, `setVideoBitrateOnFly`) is unresolved, open the RootEncoder StreamBase source/wiki and use the 2.7.4 name, then rebuild.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/opticast/stream/RootEncoderBroadcaster.kt
git commit -m "feat: RootEncoder-backed broadcaster (GenericStream RTMP/SRT)

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 9: StreamingService (foreground service)

Owns the broadcaster, wake lock, and notification so streaming survives screen-off / app backgrounding.

**Files:**
- Create: `app/src/main/java/com/opticast/service/StreamingService.kt`

- [ ] **Step 1: Implement the service**

`StreamingService.kt`:
```kotlin
package com.opticast.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.opticast.stream.Broadcaster

class StreamingService : Service() {

    companion object {
        const val CHANNEL_ID = "opticast_stream"
        const val NOTIF_ID = 1
        // Set by the app graph before binding/starting (see Task 11).
        @Volatile var broadcaster: Broadcaster? = null
    }

    private val binder = LocalBinder()
    inner class LocalBinder : android.os.Binder() {
        fun broadcaster(): Broadcaster? = StreamingService.broadcaster
    }

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundCompat()
        acquireWakeLock()
        return START_STICKY
    }

    private fun startForegroundCompat() {
        createChannel()
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Opticast")
            .setContentText("Streaming")
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // 34
            startForeground(
                NOTIF_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun createChannel() {
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
            mgr.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Streaming", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Opticast::stream").also {
            it.acquire(4 * 60 * 60 * 1000L) // 4h safety cap
        }
    }

    override fun onDestroy() {
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }
}
```

- [ ] **Step 2: Build**

Run: `.\gradlew.bat :app:compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/opticast/service/StreamingService.kt
git commit -m "feat: foreground streaming service with wake lock and typed FGS

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 10: Compose UI

Three screens + permission handling. Integration code; verified visually and in Task 12.

**Files:**
- Create: `app/src/main/java/com/opticast/ui/MainActivity.kt`, `LiveScreen.kt`, `ConnectionsScreen.kt`, `SettingsScreen.kt`, `OpticastNavHost.kt`

- [ ] **Step 1: NavHost + MainActivity**

`OpticastNavHost.kt`:
```kotlin
package com.opticast.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun OpticastNavHost(vm: StreamViewModel) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "live") {
        composable("live") { LiveScreen(vm, onConnections = { nav.navigate("connections") }) }
        composable("connections") { ConnectionsScreen(vm, onBack = { nav.popBackStack() }) }
    }
}
```

> Add the Navigation-Compose dependency to the catalog/app build (`androidx.navigation:navigation-compose:2.8.0`) if not present, then rebuild.

`MainActivity.kt`:
```kotlin
package com.opticast.ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import com.opticast.OpticastApp
import com.opticast.service.StreamingService

class MainActivity : ComponentActivity() {

    private val vm: StreamViewModel by viewModels {
        (application as OpticastApp).viewModelFactory()
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* result handled by UI re-query; mic denial => video-only path */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StreamingService.broadcaster = (application as OpticastApp).broadcaster
        startService(Intent(this, StreamingService::class.java))
        permissionLauncher.launch(
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS)
        )
        setContent { OpticastNavHost(vm) }
    }
}
```

- [ ] **Step 2: LiveScreen**

`LiveScreen.kt`:
```kotlin
package com.opticast.ui

import android.view.SurfaceView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opticast.model.StreamState
import com.opticast.stream.RootEncoderBroadcaster

@Composable
fun LiveScreen(vm: StreamViewModel, onConnections: () -> Unit) {
    val ui by vm.uiState.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        // Preview surface (only the RootEncoder impl can render; safe no-op for fakes)
        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).also { sv ->
                    (vm.broadcasterForPreview() as? RootEncoderBroadcaster)?.attachPreview(sv)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Column(Modifier.align(Alignment.TopStart).padding(12.dp)) {
            val label = when (val s = ui.streamState) {
                StreamState.Idle -> "Idle"
                StreamState.Connecting -> "Connecting…"
                StreamState.Live -> "LIVE"
                is StreamState.Reconnecting -> "Reconnecting (#${s.attempt})"
                is StreamState.Error -> "Error: ${s.reason}"
            }
            AssistChip(onClick = {}, label = { Text(label) })
            if (ui.streamState is StreamState.Live) {
                Text("${ui.stats.bitrateBps / 1000} kbps · ${ui.stats.uptimeSeconds}s")
            }
            ui.selected?.let { Text("Target: ${it.name}") }
        }

        Row(Modifier.align(Alignment.BottomCenter).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onConnections) { Text("Targets") }
            Button(onClick = { vm.switchCamera() }) { Text("Flip") }
            Button(onClick = { vm.toggleMute() }) { Text(if (ui.muted) "Unmute" else "Mute") }
            Button(onClick = { vm.toggleTorch() }) { Text("Torch") }
            if (ui.streamState is StreamState.Live || ui.streamState is StreamState.Connecting) {
                Button(onClick = { vm.stop() }) { Text("Stop") }
            } else {
                Button(onClick = { vm.goLive() }) { Text("Go Live") }
            }
        }
    }
}
```

> Add `fun broadcasterForPreview(): Broadcaster = broadcaster` to `StreamViewModel` (exposes the instance for preview attachment).

- [ ] **Step 3: ConnectionsScreen + SettingsScreen (editor)**

`ConnectionsScreen.kt`:
```kotlin
package com.opticast.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opticast.model.Connection
import com.opticast.model.StreamProtocol
import com.opticast.util.ConnectionValidator
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun ConnectionsScreen(vm: StreamViewModel, onBack: () -> Unit) {
    val ui by vm.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<Connection?>(null) }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row { Button(onClick = onBack) { Text("Back") }
              Spacer(Modifier.width(8.dp))
              Button(onClick = { editing = blankConnection() }) { Text("New") } }
        LazyColumn(Modifier.weight(1f)) {
            items(ui.connections) { c ->
                ListItem(
                    headlineContent = { Text(c.name) },
                    supportingContent = { Text("${c.protocol} ${c.host}:${c.port}/${c.path}") },
                    modifier = Modifier.clickable { vm.selectConnection(c); onBack() }
                )
                Row {
                    TextButton(onClick = { editing = c }) { Text("Edit") }
                    TextButton(onClick = { scope.launch { vm.delete(c.id) } }) { Text("Delete") }
                }
            }
        }
    }

    editing?.let { conn ->
        ConnectionEditorDialog(
            initial = conn,
            onDismiss = { editing = null },
            onSave = { scope.launch { vm.save(it); editing = null } }
        )
    }
}

private fun blankConnection() = Connection(
    id = UUID.randomUUID().toString(), name = "", protocol = StreamProtocol.SRT,
    host = "", port = 8890, path = "live/stream", secret = null,
    width = 1280, height = 720, fps = 30, videoBitrate = 2_500_000, audioBitrate = 128_000
)

@Composable
fun ConnectionEditorDialog(initial: Connection, onDismiss: () -> Unit, onSave: (Connection) -> Unit) {
    var c by remember { mutableStateOf(initial) }
    val result = ConnectionValidator.validate(c)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(enabled = result.errors.isEmpty(), onClick = { onSave(c) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Connection") },
        text = {
            Column {
                OutlinedTextField(c.name, { c = c.copy(name = it) }, label = { Text("Name") })
                Row {
                    FilterChip(c.protocol == StreamProtocol.RTMP,
                        { c = c.copy(protocol = StreamProtocol.RTMP) }, { Text("RTMP") })
                    Spacer(Modifier.width(8.dp))
                    FilterChip(c.protocol == StreamProtocol.SRT,
                        { c = c.copy(protocol = StreamProtocol.SRT) }, { Text("SRT") })
                }
                OutlinedTextField(c.host, { c = c.copy(host = it) }, label = { Text("Host") })
                OutlinedTextField(c.port.toString(),
                    { c = c.copy(port = it.toIntOrNull() ?: 0) }, label = { Text("Port") })
                OutlinedTextField(c.path, { c = c.copy(path = it) }, label = { Text("App/Path") })
                OutlinedTextField(c.secret ?: "",
                    { c = c.copy(secret = it.ifBlank { null }) },
                    label = { Text(if (c.protocol == StreamProtocol.SRT) "Passphrase" else "user:pass") })
                result.errors.forEach { Text("• $it", color = MaterialTheme.colorScheme.error) }
                result.warnings.forEach { Text("⚠ $it", color = MaterialTheme.colorScheme.error) }
            }
        }
    )
}
```

`SettingsScreen.kt`:
```kotlin
package com.opticast.ui

// Placeholder route for default video/audio params; deferred to post-MVP polish.
// Defaults live on each Connection (set in the editor), so no global settings are required for v1.
```

> Settings has no v1 requirement (per-connection params cover it). The file documents that decision; no screen is wired. Remove if you prefer not to keep an empty file.

- [ ] **Step 4: Build**

Run: `.\gradlew.bat :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/opticast/ui
git commit -m "feat: Compose UI - live screen, connection editor, navigation

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 11: Application wiring (manual DI)

Wires the real implementations together and exposes a ViewModel factory.

**Files:**
- Create: `app/src/main/java/com/opticast/OpticastApp.kt`

- [ ] **Step 1: Implement the application graph**

`OpticastApp.kt`:
```kotlin
package com.opticast

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.opticast.data.ConnectionRepository
import com.opticast.data.DataStoreProfileStore
import com.opticast.data.EncryptedSecretStore
import com.opticast.stream.Broadcaster
import com.opticast.stream.RootEncoderBroadcaster
import com.opticast.ui.StreamViewModel

class OpticastApp : Application() {

    lateinit var broadcaster: Broadcaster
        private set
    private lateinit var repository: ConnectionRepository

    override fun onCreate() {
        super.onCreate()
        broadcaster = RootEncoderBroadcaster(this)
        repository = ConnectionRepository(
            DataStoreProfileStore(this),
            EncryptedSecretStore(this)
        )
    }

    fun viewModelFactory(): ViewModelProvider.Factory = viewModelFactory {
        initializer { StreamViewModel(broadcaster, repository) }
    }
}
```

- [ ] **Step 2: Build & full test run**

Run: `.\gradlew.bat :app:assembleDebug testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, all unit tests pass.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/opticast/OpticastApp.kt
git commit -m "feat: application graph wiring real broadcaster and repository

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 12: Manual end-to-end verification against MediaMTX

No code — a verification checklist run on a real device. (MediaMTX is already running locally from the phone-cam setup; see `motolive/docs/06-setup-phone-cam.md`.)

- [ ] **Step 1: Install on a physical device**

Run: `.\gradlew.bat installDebug`
Expected: app installs; grant Camera, Microphone, Notifications when prompted.

- [ ] **Step 2: Create a connection and go live (local RTMP)**

- In the app: New → RTMP, host = your PC LAN IP, port 1935, path `live/ride`, no secret. Save, tap to select.
- Confirm the editor shows the ⚠ unencrypted warning for a remote host but NOT for a `192.168.x` LAN host.
- Tap Go Live. In OBS, the Media Source `rtmp://<ip>:1935/live/ride` should show the phone feed. State chip shows LIVE; kbps/uptime tick.

- [ ] **Step 3: Screen-off survival (the high-risk path)**

- While LIVE, lock the phone screen. Confirm in OBS the stream **keeps running** (foreground-service notification persists; feed continues).
- Unlock; confirm preview re-renders and the stream never dropped.

- [ ] **Step 4: SRT path**

- New → SRT, port 8890, passphrase set. Confirm no unencrypted warning. Go live; confirm OBS/MediaMTX receives the SRT stream.

- [ ] **Step 5: Reconnect**

- While LIVE over RTMP, stop MediaMTX, wait, restart it. Confirm the app surfaces an error/reconnecting state and recovers (or cleanly reports failure after max attempts). Note: wiring the `ReconnectController` into the service's disconnect handling is the follow-up if recovery is not yet automatic.

- [ ] **Step 6: Secret persistence**

- Force-stop and reopen the app. Confirm saved connections reappear and streaming still authenticates (secret rehydrated from `EncryptedSecretStore`).

- [ ] **Step 7: Tag the milestone**

```bash
git tag -a v0.1.0 -m "Opticast MVP: phone camera RTMP/SRT broadcaster"
```

---

## Execution outcome (2026-06-16)

Tasks 0–11 implemented and committed; full unit suite (27 tests, 6 classes) passes and the debug APK assembles. **Version corrections discovered during the build** (the pre-build estimates in this plan were wrong — RootEncoder 2.7.4 forced them):

- **compileSdk/targetSdk → 36** (RootEncoder 2.7.4 AAR metadata requires compiling against API 36).
- **AGP → 8.11.1**, **Gradle wrapper → 8.13** (needed to support compileSdk 36).
- **Kotlin → 2.3.21** (RootEncoder 2.7.4 ships Kotlin 2.3 metadata + pulls kotlin-stdlib 2.3.21 / coroutines 1.11.0; older Kotlin can't read it). Required migrating `kotlinOptions { jvmTarget }` → the `kotlin { compilerOptions { jvmTarget } }` DSL.
- **`gradle.properties`** with `android.useAndroidX=true` was required (omitted from the original scaffold).
- **RootEncoder helper API:** in 2.7.4 camera/audio control is on the *sources*, not `StreamBase`. Correct calls: `(stream.videoSource as Camera2Source).switchCamera()/enableLantern()/disableLantern()`, `(stream.audioSource as MicrophoneSource).mute()/unMute()`, and `stream.setVideoBitrateOnFly(int)` (on StreamBase). Tap-to-focus/zoom (deferred) use `Camera2Source.tapToFocus(view, ev)` / `setZoom(ev)`.

Task 12 (on-device camera/streaming verification) still pending — requires a physical Android device.

## Notes for the implementer

- **Reconnect wiring:** Tasks 6 builds the controller and Task 12 Step 5 tests recovery, but auto-reconnect is fully wired only when the service observes `onConnectionFailed`/`onDisconnect` and invokes `ReconnectController.run { broadcaster.start(currentConnection) }`. If Step 5 shows no auto-recovery, add that observer in `StreamingService` (or a small `StreamCoordinator`) — it's a known integration point, not a new design.
- **AdaptiveBitrate wiring:** similarly, connect `RootEncoderBroadcaster.reportedBitrate` → `AdaptiveBitrateController.onReportedBitrate` → `broadcaster.setVideoBitrate`. Do this in the same coordinator that owns reconnect.
- **Navigation dependency:** Task 10 uses `navigation-compose`; add `androidx.navigation:navigation-compose:2.8.0` to the catalog and app build if the build complains.
- **RootEncoder helper names:** start/stop/prepare/ConnectChecker are verified for 2.7.4. Torch/mute/bitrate-on-fly helper names (`enableLantern`, `disableAudio`, `setVideoBitrateOnFly`) are the established StreamBase names; if any fails to resolve, confirm against the 2.7.4 StreamBase source and adjust.
- **Tap-to-focus / pinch-zoom (spec core, not yet in a task):** the Live screen wires flip/mute/torch but not focus/zoom. Add them on the `AndroidView` SurfaceView using RootEncoder's `tapToFocus(motionEvent)` and `setZoom(motionEvent)` (or `setZoomScale`) via a `pointerInteropFilter`/touch listener, calling through a small `Broadcaster.tapToFocus()/setZoom()` extension. Small follow-up, no design change.
- **Log redaction (security spec):** never log `connection.toEndpoint()` or `secret` — they contain credentials. Keep the no-analytics/no-crash-SDK stance; if you add any logging, redact the userinfo/passphrase first.
