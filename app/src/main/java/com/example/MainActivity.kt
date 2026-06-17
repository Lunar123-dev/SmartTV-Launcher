package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.ui.LauncherViewModel
import com.example.ui.TVLauncherScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private lateinit var viewModel: LauncherViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    viewModel = ViewModelProvider(this)[LauncherViewModel::class.java]

    setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          TVLauncherScreen(viewModel = viewModel)
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    if (::viewModel.isInitialized) {
      viewModel.loadInstalledApps()
    }
  }
}

