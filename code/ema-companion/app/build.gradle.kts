plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kotlin.compose)
}

val repoRoot = rootDir.parentFile.parentFile
tasks.register<Copy>("copyUserGuideAssets") {
    from(repoRoot.resolve("docs/user-guide"))
    into(layout.projectDirectory.dir("src/main/assets/user-guide"))
}
tasks.named("preBuild") { dependsOn("copyUserGuideAssets") }

android {
    namespace = "com.schlueternetz.emacompanion"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.schlueternetz.emacompanion"
        minSdk = 31
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val stubPort = (project.findProperty("STUB_PORT") ?: System.getenv("STUB_PORT") ?: "8080").toString()
        buildConfigField("String", "STUB_PORT", "\"$stubPort\"")
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    packaging {
        resources {
            excludes += "META-INF/NOTICE.md"
            excludes += "META-INF/LICENSE.md"
        }
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.security.crypto)
    implementation(libs.markwon.core)
    implementation(libs.markwon.image)
    implementation(libs.markwon.ext.tables)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.android.mail)
    implementation(libs.android.activation)
    implementation(libs.mpandroidchart)
    implementation(libs.swiperefreshlayout)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    testImplementation(libs.greenmail)
    testImplementation(libs.androidx.navigation.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.accessibility.test.framework)
    testImplementation(libs.androidx.espresso.accessibility)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.work.testing)
    testImplementation("com.schlueternetz.emaapistub:ema-api-stub:1.0")
    testImplementation(libs.androidx.glance.appwidget.testing)
    debugImplementation(libs.androidx.fragment.testing)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
