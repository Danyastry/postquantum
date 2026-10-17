plugins {
    alias(libs.plugins.pqc.android.library)
}

android {
    namespace = "com.danyastry.pqc.core.pqc"
}

dependencies {
    api(libs.bouncycastle.provider)
}
