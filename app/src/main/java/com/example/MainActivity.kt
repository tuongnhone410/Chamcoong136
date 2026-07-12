package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.data.db.AppDatabase
import com.example.data.repository.TimeRepository
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PayslipScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.TimeSnapProTheme
import com.example.viewmodel.TimeSnapViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(this)
        val repository = TimeRepository(database.timeEntryDao(), database.userConfigDao())

        val viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(TimeSnapViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return TimeSnapViewModel(repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }

        setContent {
            TimeSnapProTheme {
                MainApp(viewModelFactory)
            }
        }
    }
}

@Composable
fun MainApp(factory: ViewModelProvider.Factory) {
    val viewModel: TimeSnapViewModel = viewModel(factory = factory)
    var currentScreen by remember { mutableStateOf("home") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentScreen == "home",
                    onClick = { currentScreen = "home" },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Trang Chủ") },
                    label = { Text("Trang Chủ") },
                    modifier = Modifier.testTag("nav_home")
                )
                NavigationBarItem(
                    selected = currentScreen == "history",
                    onClick = { currentScreen = "history" },
                    icon = { Icon(Icons.Default.List, contentDescription = "Lịch Sử") },
                    label = { Text("Lịch Sử") },
                    modifier = Modifier.testTag("nav_history")
                )
                NavigationBarItem(
                    selected = currentScreen == "payslip",
                    onClick = { currentScreen = "payslip" },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Bảng Lương") },
                    label = { Text("Bảng Lương") },
                    modifier = Modifier.testTag("nav_payslip")
                )
                NavigationBarItem(
                    selected = currentScreen == "settings",
                    onClick = { currentScreen = "settings" },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Cài Đặt") },
                    label = { Text("Cài Đặt") },
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentScreen) {
                "home" -> HomeScreen(viewModel)
                "history" -> HistoryScreen(viewModel)
                "payslip" -> PayslipScreen(viewModel)
                "settings" -> SettingsScreen(viewModel)
            }
        }
    }
}
