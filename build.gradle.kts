plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.5.0"
}

group = "com.yonyou.ncc"
version = "1.0.3"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

dependencies {
    intellijPlatform {
        // 直接使用本机已安装的 IDEA CE 2025.1，避免重复下载整个 IDE 发行包
        local("/Applications/IntelliJ IDEA CE.app")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "251"
            untilBuild = "253.*"
        }
    }

    pluginVerification {
        ides {
            // 用本机这套 IDEA CE 2025.1 做校验，避免额外下载平台发行包
            local("/Applications/IntelliJ IDEA CE.app")
        }
    }
}

tasks {
    buildSearchableOptions {
        enabled = false
    }
}
