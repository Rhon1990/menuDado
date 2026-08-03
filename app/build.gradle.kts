plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

fun ProviderFactory.optionalConfig(
    gradlePropertyName: String,
    environmentVariableName: String
): String = gradleProperty(gradlePropertyName)
    .orElse(environmentVariable(environmentVariableName))
    .orElse("")
    .get()

fun String.asAndroidStringValue(): String = "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

val debugGoogleWebClientId = providers.optionalConfig(
    gradlePropertyName = "menudadoDebugGoogleWebClientId",
    environmentVariableName = "MENUDADO_DEBUG_GOOGLE_WEB_CLIENT_ID"
)
val productionGoogleWebClientId = providers.optionalConfig(
    gradlePropertyName = "menudadoProductionGoogleWebClientId",
    environmentVariableName = "MENUDADO_PRODUCTION_GOOGLE_WEB_CLIENT_ID"
)
val rewardedAiAdUnitId = providers.optionalConfig(
    gradlePropertyName = "menudadoRewardedAiAdUnitId",
    environmentVariableName = "MENUDADO_REWARDED_AI_AD_UNIT_ID"
).ifBlank { "ca-app-pub-2347852335093406/5192361313" }

android {
    namespace = "com.menudado"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.menudado"
        minSdk = 23
        targetSdk = 36
        versionCode = 15
        versionName = "1.3.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "GEMINI_MODEL", "\"gemini-2.5-flash-lite\"")
        buildConfigField("String", "APP_CHECK_PROVIDER", "\"play_integrity\"")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "MenuDado Debug")
            resValue("string", "google_web_client_id", debugGoogleWebClientId.asAndroidStringValue())
            buildConfigField("String", "APP_CHECK_PROVIDER", "\"debug\"")
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField(
                "String",
                "HOME_INLINE_BANNER_AD_UNIT_ID",
                "\"ca-app-pub-3940256099942544/9214589741\""
            )
            buildConfigField(
                "String",
                "REWARDED_AI_AD_UNIT_ID",
                "\"ca-app-pub-3940256099942544/5224354917\""
            )
        }

        release {
            resValue(
                "string",
                "google_web_client_id",
                productionGoogleWebClientId.asAndroidStringValue()
            )
            manifestPlaceholders["admobAppId"] = "ca-app-pub-2347852335093406~9294645476"
            buildConfigField(
                "String",
                "HOME_INLINE_BANNER_AD_UNIT_ID",
                "\"ca-app-pub-2347852335093406/2270906270\""
            )
            buildConfigField(
                "String",
                "REWARDED_AI_AD_UNIT_ID",
                rewardedAiAdUnitId.asAndroidStringValue()
            )
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        create("releaseDebuggable") {
            initWith(getByName("release"))
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField(
                "String",
                "HOME_INLINE_BANNER_AD_UNIT_ID",
                "\"ca-app-pub-3940256099942544/9214589741\""
            )
            buildConfigField(
                "String",
                "REWARDED_AI_AD_UNIT_ID",
                "\"ca-app-pub-3940256099942544/5224354917\""
            )
            buildConfigField("String", "APP_CHECK_PROVIDER", "\"debug\"")
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
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.05.01"))

    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.fragment:fragment:1.8.9")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.room:room-ktx:2.8.3")
    implementation("androidx.room:room-runtime:2.8.3")
    implementation("com.google.firebase:firebase-ai:17.12.1")
    implementation("com.google.firebase:firebase-analytics:22.1.2")
    implementation("com.google.firebase:firebase-appcheck-playintegrity:18.0.0")
    implementation("com.google.firebase:firebase-auth:23.1.0")
    implementation("com.google.firebase:firebase-config:22.1.2")
    implementation("com.google.firebase:firebase-firestore:25.1.1")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")
    implementation("com.google.android.gms:play-services-ads:24.7.0")
    implementation("com.google.android.ump:user-messaging-platform:3.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    kapt("androidx.room:room-compiler:2.8.3")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    androidTestImplementation("androidx.room:room-testing:2.8.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.05.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("com.google.firebase:firebase-appcheck-debug:18.0.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    "releaseDebuggableImplementation"("com.google.firebase:firebase-appcheck-debug:18.0.0")
    "releaseDebuggableImplementation"("androidx.compose.ui:ui-tooling")
    "releaseDebuggableImplementation"("androidx.compose.ui:ui-test-manifest")
}
