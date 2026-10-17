package com.danyastry.pqc

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.danyastry.pqc.catalog.FeatureCatalog
import com.danyastry.pqc.catalog.FeatureRegistry
import com.danyastry.pqc.core.ui.theme.PqcTheme
import com.danyastry.pqc.navigation.PqcNavHost

class MainActivity : ComponentActivity() {

    private val catalog = FeatureCatalog(FeatureRegistry.features, deviceSdk = Build.VERSION.SDK_INT)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PqcTheme {
                PqcNavHost(catalog)
            }
        }
    }
}
