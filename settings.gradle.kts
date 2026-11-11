pluginManagement { repositories { gradlePluginPortal(); mavenCentral() } }
dependencyResolutionManagement { repositories { mavenCentral() } }
rootProject.name = "white-hat"
include(":shared:contracts")
include(":server:domain", ":server:application", ":server:adapters", ":server:api")
