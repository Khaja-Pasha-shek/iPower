plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.pipower"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.pipower"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    //seekbar
    implementation ("me.tankery.lib:circularSeekBar:1.4.0")
    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation (libs.gson)

    //Location
    implementation ("com.google.android.gms:play-services-location:20.0.0")
    //okhttp
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation(libs.datastore.core.android)


    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}