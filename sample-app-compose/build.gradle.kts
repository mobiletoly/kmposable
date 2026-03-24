plugins {
    base
}

description = "Aggregator project for the Compose Multiplatform sample app (Compose adapter variant)"

evaluationDependsOn(":sample-app-compose:composeApp")
evaluationDependsOn(":sample-app-compose:androidApp")

tasks.named("build") {
    dependsOn(":sample-app-compose:composeApp:build")
    dependsOn(":sample-app-compose:androidApp:build")
}

tasks.named("clean") {
    dependsOn(":sample-app-compose:composeApp:clean")
    dependsOn(":sample-app-compose:androidApp:clean")
}
