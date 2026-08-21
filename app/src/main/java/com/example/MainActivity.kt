package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.core.theme.BgDark
import com.example.core.theme.HFTPyramidTheme
import com.example.presentation.navigation.AppNavigation
import com.example.presentation.screens.pyramid.PyramidViewModel

class MainActivity : ComponentActivity() {

    private val pyramidViewModel: PyramidViewModel by viewModels()

    // API 33+ için bildirim izni (ilk açılışta sorulur)
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* izin sonucu gerekmez */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        maybeRequestNotificationPermission()
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

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
