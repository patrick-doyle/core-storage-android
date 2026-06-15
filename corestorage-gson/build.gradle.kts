plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.pdoyle.corestorage.gson"
    compileSdk = 36

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

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
    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
    @Suppress("UnstableApiUsage")
    testFixtures.enable = true
}

dependencies {

    implementation(libs.okio)
    implementation(libs.gson)
    implementation(project(":corestorage"))

    testImplementation(platform(libs.junit.jupiter.bom))
    testImplementation(testFixtures(project(":corestorage")))
    testRuntimeOnly(libs.junit.jupiter.launcher)
    testImplementation(libs.bundles.unitTests)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.androidx.espresso.core)
}