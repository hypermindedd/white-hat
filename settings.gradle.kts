pluginManagement { repositories { google(); gradlePluginPortal(); mavenCentral() } }
dependencyResolutionManagement { repositories { google { content { includeGroupByRegex("(androidx|com[.]android|com[.]google[.]testing[.]platform)[.].*") } }; mavenCentral() } }
rootProject.name = "white-hat"
include(":shared:contracts", ":shared:client")
include(":server:domain", ":server:application", ":server:adapters", ":server:api")
include(":apps:desktop")
if (providers.gradleProperty("whiteHatAndroid").orNull == "true") include(":apps:android", ":apps:android:client")
