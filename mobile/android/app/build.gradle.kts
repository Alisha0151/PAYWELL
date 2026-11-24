plugins {
    id("com.android.application")
    id("kotlin-android")
    id("dev.flutter.flutter-gradle-plugin")
}
android {
    namespace = "com.paywell.app"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    defaultConfig {
        applicationId = "com.paywell.app"
        minSdk = flutter.minSdkVersion   // flutter_secure_storage requires >= 23
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }
    buildTypes {
        release {
            // TODO: configure a real release signingConfig (keystore via key.properties, never committed).
            signingConfig = signingConfigs.getByName("debug")
            // Shrinking is intentionally NOT set here. Flutter's Gradle plugin configures the release build type
            // (code shrinking + resource shrinking together). Forcing isMinifyEnabled = false here while the plugin
            // enables resource shrinking made AGP 9 fail at configuration time: "Removing unused resources requires
            // unused code shrinking to be turned on". Debug builds never shrink (AGP default).
        }
    }
}
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}
flutter { source = "../.." }
