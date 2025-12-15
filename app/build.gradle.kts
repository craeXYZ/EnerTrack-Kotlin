plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.gms.google-services")
}

// DEFINISI GLOBAL SATU TEMPAT
val room_version = "2.6.1"

android {
    namespace = "com.enertrack"




    compileSdk = 36

    defaultConfig {
        applicationId = "com.enertrack"
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
        viewBinding = true
    }
}

dependencies {

    // --- DEPENDENSI UMUM ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.firebase.storage) // Jika pakai storage

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Chart Libraries
    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.diogobernardino:williamchart:3.10.1")
    implementation("com.github.AnyChart:AnyChart-Android:1.1.5")

    // Utilities
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation("com.github.franmontiel:PersistentCookieJar:v1.0.1")
    implementation(libs.persistent.cookie.jar)
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // Retrofit (Network)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // --- FIREBASE (VERSI BOM 33.7.0) ---
    // Import BoM (Mengatur semua versi Firebase)
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))

    // Gunakan library utama (KTX sudah deprecated/merge di versi baru)
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-database-ktx")

    // --- ROOM DATABASE (Konsisten pakai variabel room_version) ---
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    kapt("androidx.room:room-compiler:$room_version")

    // UI Components
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("com.google.android.material:material:1.11.0")

    // Lifecycle & Coroutines
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}