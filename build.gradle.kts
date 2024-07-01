plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.0"
    id("org.jetbrains.intellij") version "1.15.0"
}

group = "com.example.viewDebug"
version = "1.6"

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
    // 2023.2.6
    version.set("2024.1.1")
    type.set("IC") // Target IDE Platform

    // 依赖的插件，idea插件网站查看，对应plugin id
    // IDEA 2024.1.1 版本进行了变更，没有自带org.jetbrains.android插件，需要自己指定版本
    // todo 需要验证，当Android studio升级后，这里指定了Android插件版本会产生什么影响
    plugins.set(listOf("org.jetbrains.kotlin", "com.intellij.gradle", "org.jetbrains.android:241.15989.150"))
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
        // 由于241版本大变更，不再兼容前面的版本了
        sinceBuild.set("241")
        // 指定一个很高的版本，放在安装不上，安装后有问题再说
        untilBuild.set("282.*")
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
