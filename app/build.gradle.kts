import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.koreansamjho.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.koreansamjho.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
        // Content pack version shipped in assets; surfaced in Settings > About.
        buildConfigField("int", "CONTENT_VERSION", "1")
    }

    androidResources {
        // Android 13+ per-app language picker is generated from the values-* folders.
        generateLocaleConfig = true
    }

    signingConfigs {
        create("release") {
            val props = rootProject.file("keystore.properties")
            if (props.exists()) {
                val p = Properties().apply { props.inputStream().use { load(it) } }
                storeFile = rootProject.file(p.getProperty("storeFile"))
                storePassword = p.getProperty("storePassword")
                keyAlias = p.getProperty("keyAlias")
                keyPassword = p.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (rootProject.file("keystore.properties").exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources { excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "DebugProbesKt.bin") }
    }

    lint {
        // AGP 8.7's WrongNavigateRouteDetector throws NoClassDefFoundError while
        // analysing Navigation Compose call sites. That is a crash in the check
        // itself, not a finding in this code, so the single check is disabled
        // rather than weakening lint as a whole.
        disable += "WrongNavigateRouteType"
        abortOnError = true
        checkReleaseBuilds = true
        xmlReport = true
        htmlReport = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    // Content DB is pre-compressed SQLite; keep it uncompressed for fast mmap open.
    androidResources { noCompress += "db" }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.3")

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.room.testing)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
}

/**
 * Content pipeline (docs/05-content-architecture.md).
 *
 * `content.db` is a build artifact, not a checked-in binary: it is generated from the
 * authored JSON in `content/src/` on every build. build_db.py runs validate.py first
 * and aborts on any violation, so a release literally cannot be produced from invalid
 * content — the gate is not something a developer can forget to run.
 */
val validateContent by tasks.registering(Exec::class) {
    group = "verification"
    description = "Validates all educational content"
    workingDir = rootProject.file("content")
    commandLine("python3", "tools/validate.py")
    isIgnoreExitValue = false
}

val buildContentDb by tasks.registering(Exec::class) {
    group = "build"
    description = "Validates content and regenerates app/src/main/assets/content.db"
    workingDir = rootProject.file("content")
    commandLine("python3", "tools/build_db.py")
    isIgnoreExitValue = false
    inputs.dir(rootProject.file("content/src"))
    inputs.dir(rootProject.file("content/tools"))
    outputs.file(rootProject.file("app/src/main/assets/content.db"))
}

tasks.named("preBuild") { dependsOn(buildContentDb) }
