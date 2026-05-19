import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.vanniktech.maven.publish)
}

// Release version is driven by the git tag on CI: tag `v0.2.0` publishes `0.2.0`.
// Override locally with `-Pversion=...`; otherwise builds use the literal below.
val libVersion: String =
    (System.getenv("RELEASE_VERSION") ?: findProperty("version") as String?)
        ?.removePrefix("v")
        ?.takeUnless { it.isBlank() || it == "unspecified" }
        ?: "0.1.0"

group = "io.github.nadeemiqbal"
version = libVersion

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate()

    androidTarget {
        publishLibraryVariants("release")
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
            }
        }
    }

    jvm("desktop") {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
            }
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.animation)
            implementation(compose.ui)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.coroutines)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }

        val skikoTest by creating {
            dependsOn(commonTest.get())
            dependencies {
                @OptIn(ExperimentalComposeLibrary::class)
                implementation(compose.uiTest)
            }
        }
        listOf("desktopTest", "iosX64Test", "iosArm64Test", "iosSimulatorArm64Test").forEach { name ->
            named(name) { dependsOn(skikoTest) }
        }
    }
}

android {
    namespace = "io.github.nadeemiqbal.debugbar"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

tasks.matching { it.name == "wasmJsBrowserTest" || it.name == "wasmJsNodeTest" }
    .configureEach { enabled = false }

mavenPublishing {
    publishToMavenCentral()

    if (
        project.hasProperty("signingInMemoryKey") ||
        project.hasProperty("signing.keyId")
    ) {
        signAllPublications()
    }

    coordinates("io.github.nadeemiqbal", "debug-bar", libVersion)

    pom {
        name.set("DebugBar")
        description.set(
            "An in-app debug drawer for Compose Multiplatform apps — a Square Cascade-style " +
                "developer menu with a pluggable section system. Built-in sections: feature flag " +
                "toggles, network request log, environment switcher, log viewer, device info, " +
                "screenshot+log+state bundle export, KV storage inspector, performance HUD. " +
                "Auto-tree-shaken in release builds; activates via shake / 3-finger tap / " +
                "keyboard shortcut. One drawer for Android, iOS, Desktop, and Web.",
        )
        inceptionYear.set("2026")
        url.set("https://github.com/NadeemIqbal/debug-bar")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("NadeemIqbal")
                name.set("Nadeem Iqbal")
                email.set("mr_nadeem_iqbal@yahoo.com")
                url.set("https://github.com/NadeemIqbal")
            }
        }
        scm {
            url.set("https://github.com/NadeemIqbal/debug-bar")
            connection.set("scm:git:git://github.com/NadeemIqbal/debug-bar.git")
            developerConnection.set("scm:git:ssh://git@github.com/NadeemIqbal/debug-bar.git")
        }
    }
}
