plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.0"
    id("org.jetbrains.intellij") version "1.15.0"
}

group = "com.example.viewDebug"
version = "1.2"

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    // 开发时运行的idea版本，即sdk版本
    // 2021.1.1
    // 2021.2
    // 2021.2.1
    // 2022.2.5
    // 2022.3.1
    // 2023.2.2
    version.set("2022.3.1")
    type.set("IC") // Target IDE Platform

    // 依赖的插件，idea插件网站查看，对应plugin id
    plugins.set(listOf("org.jetbrains.kotlin", "com.intellij.gradle", "org.jetbrains.android"))
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        /*sinceBuild.set("212")
        untilBuild.set("222.*")*/
        sinceBuild.set("209")
        untilBuild.set("252.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
