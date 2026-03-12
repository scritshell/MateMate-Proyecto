import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.proyectoajedrez"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.proyectoajedrez"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Leer API key
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(FileInputStream(localPropertiesFile))
        }

        val apiKey = localProperties.getProperty("NEWS_API_KEY") ?: ""
        buildConfigField("String", "NEWS_API_KEY", "\"$apiKey\"")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }

    dependencies {
        // Firebase BOM (Bill of Materials) para versiones compatibles
        implementation(platform("com.google.firebase:firebase-bom:34.6.0"))
        implementation("com.google.firebase:firebase-auth")       // Autenticación de usuarios
        implementation("com.google.firebase:firebase-firestore")  // Base de datos NoSQL

        // Retrofit para peticiones HTTP a API de noticias
        implementation("com.squareup.retrofit2:retrofit:2.9.0")
        implementation("com.squareup.retrofit2:converter-gson:2.9.0")

        // Corrutinas para operaciones asíncronas
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

        // Glide para carga y caché de imágenes
        implementation("com.github.bumptech.glide:glide:4.15.1")

        // Librería de ajedrez para lógica del juego
        implementation("com.github.bhlangonijr:chesslib:1.3.3")
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.appcompat)
        implementation(libs.material)
        implementation(libs.androidx.constraintlayout)
        implementation(libs.androidx.navigation.fragment.ktx)
        implementation(libs.androidx.navigation.ui.ktx)
        testImplementation(libs.junit)
        androidTestImplementation(libs.androidx.junit)
        androidTestImplementation(libs.androidx.espresso.core)
    }
}