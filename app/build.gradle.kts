plugins { id("com.android.application"); kotlin("android") }
android {
    namespace = "fr.kanjiflashcards"
    compileSdk = 36
    defaultConfig {
        applicationId = "fr.kanjiflashcards"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    sourceSets["main"].assets.srcDir(rootProject.file("listes/francais"))
    lint { abortOnError = true }
}
dependencies { implementation(project(":core")) }
