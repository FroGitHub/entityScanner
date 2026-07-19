import org.jetbrains.intellij.platform.gradle.tasks.RunIdeTask

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.5.0"
}

group = "frog"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        create("IC", "2025.2")
        bundledPlugin("com.intellij.java")
        jetbrainsRuntime()
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }
}

intellijPlatform {
    pluginConfiguration {
        id = "frog.entityScanner"
        name = "EntityScanner"
        ideaVersion { sinceBuild = "251" }
        changeNotes = "Initial version"
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    withType<RunIdeTask> {
        jvmArgs("-Dsun.java2d.uiScale=1.0")
    }
}

//tasks {
//    withType<JavaCompile> {
//        sourceCompatibility = "21"
//        targetCompatibility = "21"
//    }
//
//    withType<RunIdeTask> {
//        // Вимикаємо агент корутин, який ламає дебаг
//        systemProperty("idea.kotlinx.coroutines.debug", "false")
//
//        jvmArgs("-Dsun.java2d.uiScale=1.0")
//    }
//}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
