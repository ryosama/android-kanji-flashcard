import java.util.Properties

plugins { id("com.android.application"); kotlin("android") }

// Configuration de signature privée, exclue de Git ; la même clé sert à chaque mise à jour.
val releaseSigningFile = rootProject.file("keystore.properties")
val releaseSigning = Properties().apply {
    if (releaseSigningFile.isFile) {
        releaseSigningFile.inputStream().use { load(it) }
    }
}

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

    // Aucune clé ni aucun mot de passe n'est intégré au code ou au dépôt publié.
    signingConfigs {
        if (releaseSigningFile.isFile) {
            create("release") {
                storeFile = rootProject.file(releaseSigning.getProperty("storeFile"))
                storePassword = releaseSigning.getProperty("storePassword")
                keyAlias = releaseSigning.getProperty("keyAlias")
                keyPassword = releaseSigning.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        getByName("release") {
            if (releaseSigningFile.isFile) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
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

// Refuser une release non signée ; les builds debug restent possibles sans configuration privée.
val validateReleaseSigning = tasks.register("validateReleaseSigning") {
    doLast {
        check(releaseSigningFile.isFile) {
            "Signature release absente : consulter PUBLICATION.md."
        }
        for (property in listOf("storeFile", "storePassword", "keyAlias", "keyPassword")) {
            check(!releaseSigning.getProperty(property).isNullOrBlank()) {
                "Configuration de signature incomplète : $property."
            }
        }
        check(rootProject.file(releaseSigning.getProperty("storeFile")).isFile) {
            "Clé de signature release introuvable."
        }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    dependsOn(validateReleaseSigning)
}
