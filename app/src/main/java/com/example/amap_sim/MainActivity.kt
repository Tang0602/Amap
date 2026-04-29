package com.example.amap_sim

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.amap_sim.data.local.AppTheme
import com.example.amap_sim.di.ServiceLocator
import com.example.amap_sim.ui.navigation.AmapNavGraph
import com.example.amap_sim.ui.theme.AmapSimTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            var currentTheme by remember { mutableStateOf(AppTheme.BRIGHT) }

            LaunchedEffect(Unit) {
                lifecycleScope.launch {
                    currentTheme = ServiceLocator.userDataManager.getTheme()
                }
            }

            LaunchedEffect(Unit) {
                lifecycleScope.launch {
                    while (true) {
                        kotlinx.coroutines.delay(500)
                        val newTheme = ServiceLocator.userDataManager.getTheme()
                        if (newTheme != currentTheme) {
                            currentTheme = newTheme
                        }
                    }
                }
            }

            AmapSimTheme(appTheme = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AmapNavGraph()
                }
            }
        }
    }
}
