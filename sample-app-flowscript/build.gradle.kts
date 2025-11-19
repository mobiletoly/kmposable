plugins {
    base
}

description = "Aggregator project for the NavFlow Script sample app"

evaluationDependsOn(":sample-app-flowscript:composeApp")

tasks.named("build") {
    dependsOn(":sample-app-flowscript:composeApp:build")
}

tasks.named("clean") {
    dependsOn(":sample-app-flowscript:composeApp:clean")
}
