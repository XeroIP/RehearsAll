package com.rehearsall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import dagger.hilt.android.AndroidEntryPoint
import com.rehearsall.ui.theme.RehearsAllTheme
import com.rehearsall.ui.navigation.RehearsAllNavGraph

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)

            RehearsAllTheme {
                RehearsAllNavGraph(windowSizeClass = windowSizeClass)
            }
        }
    }
}
