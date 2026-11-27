import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.japaneixxx.freya"
    compileSdk = 34

    val releaseKeystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
    val releaseKeystorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
    val releaseKeyAlias = System.getenv("ANDROID_KEY_ALIAS")
    val releaseKeyPassword = System.getenv("ANDROID_KEY_PASSWORD")

    signingConfigs {
        create("release") {
            if (releaseKeystorePath != null && releaseKeystorePassword != null &&
                releaseKeyAlias != null && releaseKeyPassword != null
            ) {
                storeFile = file(releaseKeystorePath)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    defaultConfig {
        applicationId = "com.japaneixxx.freya"
        minSdk = 26
        targetSdk = 34
        versionCode = 33
        versionName = "1.11.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        // Compose Multiplatform 1.6.11 usa o compilador 1.5.14.
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    // O código Android continua em src/main, sem mover arquivos.
    sourceSets["main"].manifest.srcFile("src/main/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/main/res")
}

// O plugin KMP redefine o manifesto para src/androidMain depois da configuração do AGP.
afterEvaluate {
    android.sourceSets.getByName("main").manifest.srcFile("src/main/AndroidManifest.xml")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions { jvmTarget = "17" }
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "freya.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            // A UI e os modelos ficam onde já estão. Apenas os arquivos realmente
            // multiplataforma entram no commonMain; o restante é incluído por target.
            kotlin.srcDir("src/main/java")
            kotlin.include(
                "com/japaneixxx/freya/data/InvestCalc.kt",
                "com/japaneixxx/freya/data/Versions.kt"
            )
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
            }
        }

        val androidMain by getting {
            kotlin.srcDir("src/main/java")
            kotlin.exclude(
                "com/japaneixxx/freya/data/InvestCalc.kt",
                "com/japaneixxx/freya/data/Versions.kt"
            )
            dependencies {
                implementation("androidx.activity:activity-compose:1.9.0")
                implementation("androidx.fragment:fragment-ktx:1.8.2")
                implementation("androidx.core:core-ktx:1.13.1")
                implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
                implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")

                implementation("androidx.room:room-runtime:2.6.1")
                implementation("androidx.room:room-ktx:2.6.1")

                implementation("com.squareup.okhttp3:okhttp:4.12.0")
                implementation("androidx.security:security-crypto:1.1.0-alpha06")
                implementation("com.google.errorprone:error_prone_annotations:2.26.1")
                implementation("androidx.biometric:biometric:1.1.0")
                implementation("androidx.work:work-runtime-ktx:2.9.0")
            }
        }

        val wasmJsMain by getting {
            // Reaproveita os mesmos arquivos de UI e de regras puras no navegador.
            kotlin.srcDir("src/main/java")
            kotlin.exclude(
                // já compilados no commonMain
                "com/japaneixxx/freya/data/InvestCalc.kt",
                "com/japaneixxx/freya/data/Versions.kt",
                // implementações Android substituídas por shims/no-ops
                "com/japaneixxx/freya/Alerts.kt",
                "com/japaneixxx/freya/BalanceWidget.kt",
                "com/japaneixxx/freya/Biometric.kt",
                "com/japaneixxx/freya/CalendarSync.kt",
                "com/japaneixxx/freya/MainActivity.kt",
                "com/japaneixxx/freya/Shortcuts.kt",
                "com/japaneixxx/freya/WidgetConfigActivity.kt",
                "com/japaneixxx/freya/data/BackupCrypto.kt",
                "com/japaneixxx/freya/data/Db.kt",
                "com/japaneixxx/freya/data/MarketRates.kt",
                "com/japaneixxx/freya/data/PinHasher.kt",
                "com/japaneixxx/freya/data/PluggyApi.kt",
                "com/japaneixxx/freya/data/Repository.kt",
                "com/japaneixxx/freya/data/Updater.kt",
                "com/japaneixxx/freya/ui/AppViewModel.kt",
                "com/japaneixxx/freya/ui/FormatCompat.kt"
            )
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
            }
        }

        val androidUnitTest by getting {
            kotlin.srcDir("src/test/java")
            dependencies {
                implementation("junit:junit:4.13.2")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
                implementation("org.json:json:20240303")
            }
        }
    }
}

// Exporta o schema do Room (app/schemas): base para testar migrações nas próximas versões do banco.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    add("kspAndroid", "androidx.room:room-compiler:2.6.1")
}
