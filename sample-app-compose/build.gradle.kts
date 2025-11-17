plugins {
    base
}

description = "Aggregator project for the Compose Multiplatform sample app (Compose adapter variant)"

evaluationDependsOn(":sample-app-compose:composeApp")

tasks.named("build") {
    dependsOn(":sample-app-compose:composeApp:build")
}

tasks.named("clean") {
    dependsOn(":sample-app-compose:composeApp:clean")
}
