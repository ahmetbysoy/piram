package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import com.example.core.theme.BgDark
import com.example.core.theme.HFTPyramidTheme
import com.example.presentation.navigation.AppNavigation
import com.example.presentation.screens.pyramid.PyramidViewModel

class MainActivity : ComponentActivity() {

    private val pyramidViewModel: PyramidViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HFTPyramidTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BgDark)
                        .safeDrawingPadding()
                ) {
                    AppNavigation(pyramidViewModel = pyramidViewModel)
                }
            }
        }
    }
}

