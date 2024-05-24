plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.mvnh.rythmap"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mvnh.rythmap"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["YANDEX_CLIENT_ID"] = "23cabbbdc6cd418abb4b39c32c41195d"
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
    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)

    implementation(libs.androidx.security.crypto.v110alpha03)

    // MapLibre
    implementation(libs.android.sdk)
    implementation(libs.android.plugin.annotation.v9)

    // Yandex auth SDK
    implementation(libs.android.authsdk)

    implementation(libs.gson)

    // Apache Commons IO
    implementation(libs.commons.io)
    implementation(libs.play.services.location)

    runtimeOnly(libs.androidx.activity.ktx)
    runtimeOnly(libs.androidx.fragment.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)

    runtimeOnly(libs.androidx.lifecycle.viewmodel.ktx)
    runtimeOnly(libs.androidx.lifecycle.livedata.ktx)

    implementation(libs.coil)

    implementation(libs.androidx.recyclerview)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}