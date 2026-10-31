plugins {
    id("com.android.application")
}

android {
    namespace = "com.flashlightwidget"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.flashlightwidget"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            // Signed with the debug key so the release APK installs without extra setup.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}
