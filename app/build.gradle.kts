plugins {
    alias(libs.plugins.pqc.android.application)
    alias(libs.plugins.pqc.android.application.compose)
}

android {
    namespace = "com.danyastry.pqc"

    defaultConfig {
        applicationId = "com.danyastry.pqc"
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    implementation(projects.core.ui)

    implementation(projects.feature.mlkem)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material3)
}
