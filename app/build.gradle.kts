plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("androidx.baselineprofile")
}

android {
    namespace = "com.exploradorxp.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.exploradorxp.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 57
        versionName = "0.1.0-alpha.57"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("permanentRelease") {
            val keystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
            val keystorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
            val alias = System.getenv("ANDROID_KEY_ALIAS")
            val keyPasswordValue = System.getenv("ANDROID_KEY_PASSWORD")

            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                keyAlias = alias
                keyPassword = keyPasswordValue
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("permanentRelease")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        // Build instalável usado nos testes e releases de desenvolvimento.
        // A partir da alpha.42 ele usa a chave permanente reconstruída a partir
        // dos GitHub Actions Secrets para permitir atualização sobre versões futuras.
        create("performance") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("permanentRelease")
            matchingFallbacks += listOf("release")
        }

        // Variantes criadas pelo plugin de Baseline Profile. Mantêm o comportamento
        // de Release, mas usam a chave debug apenas para geração/medição local e no CI.
        create("benchmarkRelease") {
            signingConfig = signingConfigs.getByName("debug")
        }
        create("nonMinifiedRelease") {
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("net.lingala.zip4j:zip4j:2.11.5")
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    implementation("androidx.media3:media3-exoplayer:1.9.4")
    implementation("androidx.media3:media3-ui:1.9.4")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")

    baselineProfile(project(":baselineprofile"))
}


baselineProfile {
    // Um único perfil atende Release e o APK performance usado nos testes da alpha.
    mergeIntoMain = true
}
