package com.mannanhosen.gymflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.mannanhosen.gymflow.presentation.GymFlowApp
import com.mannanhosen.gymflow.presentation.MainViewModel
import com.mannanhosen.gymflow.ui.theme.GymFlowTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            GymFlowTheme(themeMode = themeMode) {
                GymFlowApp(viewModel = viewModel)
            }
        }
    }
}