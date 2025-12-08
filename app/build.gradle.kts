plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    alias(libs.plugins.google.gms.google.services)
}

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

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.firebase.storage)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    //Chart Library
    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.diogobernardino:williamchart:3.10.1")

    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation("com.github.franmontiel:PersistentCookieJar:v1.0.1")
    implementation(libs.persistent.cookie.jar)
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    // Retrofit untuk koneksi API
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // (Opsional tapi sangat direkomendasikan) Untuk melihat log request API
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation("com.github.AnyChart:AnyChart-Android:1.1.5")

    // Dependensi lain untuk testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Room
    implementation ("androidx.room:room-runtime:2.6.1")
    // Room + Kotlin Coroutines
    implementation ("androidx.room:room-ktx:2.6.1")
    kapt ("androidx.room:room-compiler:$room_version")

    implementation("de.hdodenhof:circleimageview:3.1.0")

    // 1. Implementasi 'bom' pake alias (yang otomatis pake 'platform')
    implementation(platform(libs.firebase.bom))

    // 2. Implementasi 'firestore' pake alias
    implementation(libs.firebase.firestore.ktx)

    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("com.google.android.material:material:1.11.0")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0") // Buat lifecycleScope
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3") // Buat coroutines

}