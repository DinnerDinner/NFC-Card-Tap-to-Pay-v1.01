plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.nfccardtaptopayv101"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.nfccardtaptopayv101"
        minSdk = 24
        targetSdk = 36
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose BOM - this manages all Compose versions
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")

    // Lifecycle & ViewModel - use consistent versions
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Navigation - use consistent version
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // Activity
    implementation("androidx.activity:activity-compose:1.8.2")

    // Networking - use consistent OkHttp version
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // UI Libraries
    implementation("com.airbnb.android:lottie-compose:6.1.0")
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("com.github.dhaval2404:imagepicker:2.1")
    implementation("com.google.accompanist:accompanist-flowlayout:0.30.1")
    implementation("com.google.accompanist:accompanist-permissions:0.31.5-beta")

    // Camera & ML Kit - use consistent versions
    implementation("androidx.camera:camera-core:1.3.0")
    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")
    implementation("androidx.camera:camera-extensions:1.3.0")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // JSON
    implementation("org.json:json:20231013")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}



//plugins {
//    alias(libs.plugins.android.application)
//    alias(libs.plugins.kotlin.android)
//    alias(libs.plugins.kotlin.compose)
//}
//
//android {
//    namespace = "com.example.nfccardtaptopayv101"
//    compileSdk = 36
//
//    defaultConfig {
//        applicationId = "com.example.nfccardtaptopayv101"
//        minSdk = 24
//        targetSdk = 36
//        versionCode = 1
//        versionName = "1.0"
//
//        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
//    }
//
//    buildTypes {
//        release {
//            isMinifyEnabled = false
//            proguardFiles(
//                getDefaultProguardFile("proguard-android-optimize.txt"),
//                "proguard-rules.pro"
//            )
//        }
//    }
//    compileOptions {
//        sourceCompatibility = JavaVersion.VERSION_11
//        targetCompatibility = JavaVersion.VERSION_11
//    }
//    kotlinOptions {
//        jvmTarget = "11"
//    }
//    buildFeatures {
//        compose = true
//    }
//}
//
//dependencies {
//
//    implementation(libs.androidx.core.ktx)
//    implementation(libs.androidx.lifecycle.runtime.ktx)
//    implementation(libs.androidx.activity.compose)
//    implementation(platform(libs.androidx.compose.bom))
//    implementation(libs.androidx.ui)
//    implementation(libs.androidx.ui.graphics)
//    implementation(libs.androidx.ui.tooling.preview)
//    implementation(libs.androidx.material3)
//    testImplementation(libs.junit)
//    androidTestImplementation(libs.androidx.junit)
//    androidTestImplementation(libs.androidx.espresso.core)
//    androidTestImplementation(platform(libs.androidx.compose.bom))
//    androidTestImplementation(libs.androidx.ui.test.junit4)
//    debugImplementation(libs.androidx.ui.tooling)
//    debugImplementation(libs.androidx.ui.test.manifest)
//    implementation("com.squareup.okhttp3:okhttp:4.10.0")
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
//    implementation("androidx.navigation:navigation-compose:2.5.3")
//    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
//    implementation("androidx.compose.material:material-icons-extended:<compose_version>")
//    implementation("androidx.compose.material3:material3:<version>")
//    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
//    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
//    implementation("androidx.compose.material3:material3:1.1.0")
//    implementation("androidx.navigation:navigation-compose:2.5.3")
//    implementation("com.airbnb.android:lottie-compose:6.1.0")
//    implementation("io.coil-kt:coil-compose:2.4.0") // Image loading
//    implementation("com.github.dhaval2404:imagepicker:2.1") // Image picker
//    implementation("com.squareup.okhttp3:okhttp:4.12.0")    // Networking
//    implementation("com.google.accompanist:accompanist-flowlayout:0.30.1")
//    implementation("androidx.compose.foundation:foundation:1.4.3")
//    implementation("io.coil-kt:coil-compose:2.4.0")
//
//    implementation("com.google.mlkit:barcode-scanning:17.3.0")
//    implementation("androidx.camera:camera-camera2:1.3.0")
//    implementation("androidx.camera:camera-lifecycle:1.3.0")
//    implementation("androidx.camera:camera-view:1.3.0")
//    implementation("androidx.camera:camera-core:1.3.0")
//    implementation("com.squareup.retrofit2:retrofit:2.9.0")
//    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
//
//
//    implementation("com.google.mlkit:barcode-scanning:17.2.0")
//    implementation("androidx.camera:camera-core:1.3.0")
//    implementation("androidx.camera:camera-camera2:1.3.0")
//    implementation("androidx.camera:camera-lifecycle:1.3.0")
//    implementation("androidx.camera:camera-view:1.3.0")
//    implementation("androidx.camera:camera-extensions:1.3.0")
//    implementation("com.google.accompanist:accompanist-permissions:0.31.5-beta")
//
//// Navigation
//    implementation("androidx.navigation:navigation-compose:2.7.6")
//    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
//// Image loading
//    implementation("io.coil-kt:coil-compose:2.5.0")
//// Network
//    implementation("com.squareup.retrofit2:retrofit:2.9.0")
//    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
//    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
//// Camera and file access
//    implementation("androidx.activity:activity-compose:1.8.2")
//// JSON parsing (if not already included)
//    implementation("org.json:json:20231013")
//
//}