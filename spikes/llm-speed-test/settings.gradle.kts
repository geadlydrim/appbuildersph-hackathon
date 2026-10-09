// PROTOTYPE: throwaway LLM speed test for the ticket
// "Which LLM and runtime pass the speed test on the demo phone?". Not the product.
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "llm-speed-test"
include(":app")
