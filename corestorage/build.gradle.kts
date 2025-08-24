plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.junit5)
}

android {
    namespace = "com.pdoyle.corestorage"
    compileSdk = 36

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
    @Suppress("UnstableApiUsage")
    testFixtures.enable = true

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.kotlin.serialization.json)
    implementation(libs.okio)

    testFixturesImplementation(libs.okio)
    testFixturesImplementation(libs.kotlin.serialization.json)

    testImplementation(platform(libs.junit.jupiter.bom))
    testImplementation(libs.bundles.unitTests)
    testRuntimeOnly(libs.junit.jupiter.launcher)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(testFixtures(project))
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.androidx.espresso.core)
}