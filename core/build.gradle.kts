plugins { kotlin("jvm") }
kotlin { jvmToolchain(17) }
// Les tests du moteur n'ont besoin ni d'Android ni d'une bibliothèque externe.
tasks.register<JavaExec>("checkEngine") {
    dependsOn("testClasses")
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("fr.kanjiflashcards.core.EngineChecksKt")
    args(rootProject.file("listes/francais").absolutePath)
}
tasks.named("check") { dependsOn("checkEngine") }
