import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.jetbrains.kotlin.serialization)
}

android {
    namespace = "com.github.meypod.al_azan"
    compileSdk { version = release(36) }

    defaultConfig {
        applicationId = "com.github.meypod.al_azan"
        minSdk = 26
        compileSdk = 36
        targetSdk = 36
        versionCode = 78
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources { generateLocaleConfig = true }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

composeCompiler {
    stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("compose_stability.conf"))
}

// Bans mutable collection TYPES (MutableList/Map/Set<…>) from being exposed, which would break the
// read-only assumption behind compose_stability.conf. Local mutable builders are fine; leaking a
// mutable-typed property or parameter is not. Add `//noban` on a line to allow an exception.
val banMutableCollectionTypes by tasks.registering {
    val sources = fileTree("src/main/java") { include("**/*.kt") }
    inputs.files(sources)
    val banned = Regex("""\bMutable(List|Map|Set)\s*<""")
    doLast {
        val violations = sources.files.flatMap { file ->
            file.readLines().mapIndexedNotNull { index, line ->
                if (banned.containsMatchIn(line) && "//noban" !in line) {
                    "${file.relativeTo(projectDir)}:${index + 1}: ${line.trim()}"
                } else {
                    null
                }
            }
        }
        if (violations.isNotEmpty()) {
            throw GradleException(
                "Mutable collection types are banned (Compose stability). " +
                    "Expose read-only List/Map/Set, or add //noban:\n" + violations.joinToString("\n"),
            )
        }
    }
}

tasks.named("check") { dependsOn(banMutableCollectionTypes) }

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.dagger.hilt.android)
    implementation(libs.androidx.hilt.navigation)
    implementation(libs.androidx.work)
    implementation(libs.androidx.hilt.work)
    ksp(libs.dagger.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.tencent.mmkv)

    implementation(libs.batoulapps.adhan)

    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
