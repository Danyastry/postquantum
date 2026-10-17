plugins {
    alias(libs.plugins.pqc.android.library.compose)
}

android {
    namespace = "com.danyastry.pqc.core.ui"
}

dependencies {
    api(libs.androidx.material3)
}
