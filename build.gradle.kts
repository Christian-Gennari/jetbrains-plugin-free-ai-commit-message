plugins {
    id("java")
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.serialization") version "1.9.24"
    id("org.jetbrains.intellij.platform") version "2.1.0"
}

group = property("pluginGroup").toString()
version = property("pluginVersion").toString()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.1")
        bundledPlugin("Git4Idea")
        instrumentationTools()
        pluginVerifier()
        zipSigner()
    }
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(17)
}

intellijPlatform {
    pluginConfiguration {
        id = "com.christiangennari.freeaicommitmessage"
        name = property("pluginName").toString()
        version = property("pluginVersion").toString()
        vendor {
            name = "Christian Gennari"
            url = "https://github.com/Christian-Gennari"
        }
        ideaVersion {
            sinceBuild = property("pluginSinceBuild").toString()
            untilBuild = provider { null }
        }
    }
    pluginVerification {
        ides {
            ide("2024.1")
            ide("2024.2")
            ide("2024.3")
        }
    }
    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }
    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
}

tasks.test {
    useJUnitPlatform()
}
