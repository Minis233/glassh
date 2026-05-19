package com.minis.glassh

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.minis.glassh.ui.DashboardScreen
import com.minis.glassh.ui.GlassBackdrop
import com.minis.glassh.ui.GlasshTheme
import com.minis.glassh.ui.GlasshViewModel
import com.minis.glassh.ui.HostEditScreen
import com.minis.glassh.ui.HostListScreen
import com.minis.glassh.ui.UiScreen

class MainActivity : ComponentActivity() {

    private val vm: GlasshViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GlasshTheme {
                GlassBackdrop {
                    val screen by vm.screen.collectAsState()
                    val systemBars = WindowInsets.systemBars.asPaddingValues()
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(top = systemBars.calculateTopPadding(), bottom = systemBars.calculateBottomPadding())
                    ) {
                        when (val s = screen) {
                            is UiScreen.HostList -> HostListScreen(vm)
                            is UiScreen.HostEdit -> HostEditScreen(vm, s.hostId)
                            is UiScreen.Dashboard -> DashboardScreen(vm)
                        }
                    }
                }
            }
        }
        onBackPressedDispatcher.addCallback(
            this,
            object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    vm.back()
                }
            }
        )
    }
}
