// PROTOTYPE: throwaway speech-to-text check for the ticket
// the voice issue (built-in on-device speech recognizer check). Not the product.
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
rootProject.name = "stt-check"
include(":app")
