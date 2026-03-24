plugins {
    base
}

description = "Aggregator project for the NavFlow Script sample app"

evaluationDependsOn(":sample-app-flowscript:composeApp")
evaluationDependsOn(":sample-app-flowscript:androidApp")

tasks.named("build") {
    dependsOn(":sample-app-flowscript:composeApp:build")
    dependsOn(":sample-app-flowscript:androidApp:build")
}

tasks.named("clean") {
    dependsOn(":sample-app-flowscript:composeApp:clean")
    dependsOn(":sample-app-flowscript:androidApp:clean")
}
