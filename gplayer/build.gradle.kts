import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

group = "com.omnifret"
version = "0.1.0-SNAPSHOT"

kotlin {
    jvmToolchain(17)

    compilerOptions {
        // The transpiled Kotlin output uses redundant explicit types,
        // null assertions, and cast operations as artifacts of the
        // TypeScript-to-Kotlin translation. Suppress the noise.
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            // -Xno-*-assertions only exist in the JVM compiler. They
            // strip null checks from compiled bytecode, useful because
            // the transpiled Kotlin emits a lot of redundant
            // null-asserted call sites that have no semantic meaning on
            // JVM. Native doesn't run them in the same way, so the flags
            // would just produce warnings.
            freeCompilerArgs.addAll(
                "-Xno-call-assertions",
                "-Xno-receiver-assertions",
                "-Xno-param-assertions",
            )
        }
    }

    listOf(iosArm64(), iosSimulatorArm64(), iosX64()).forEach { target ->
        target.binaries.framework {
            baseName = "OmniFretGplayer"
            isStatic = true
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

android {
    namespace = "com.omnifret.gplayer"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Bundle the MPL-2.0 LICENSE inside the AAR's META-INF directory so binary
// consumers (the OmniFret app, anyone else who pulls the AAR) can find the
// license without having to follow the GitHub link. AGP's bundleAar tasks
// are Zip-based; we hook in via afterEvaluate so the tasks exist when we
// configure them.
afterEvaluate {
    tasks.matching { it.name.startsWith("bundle") && it.name.endsWith("Aar") }
        .configureEach {
            (this as org.gradle.api.tasks.bundling.Zip).from(rootProject.file("LICENSE")) {
                into("META-INF")
            }
        }
}
