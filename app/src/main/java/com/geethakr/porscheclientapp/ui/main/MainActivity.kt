package com.geethakr.porscheclientapp.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.geethakr.porscheclientapp.protobuf.SensorDataProto.SensorData
import com.geethakr.porscheclientapp.ui.configSensor.ConfigSensorScreen
import com.geethakr.porscheclientapp.ui.navigation.Screens
import com.geethakr.porscheclientapp.ui.theme.PorscheClientAppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.Socket

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PorscheClientAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ClientApp(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

lateinit var msgLogList: MutableList<String>

@Composable
fun ClientApp(name: String, modifier: Modifier = Modifier) {
    val coroutineScope = rememberCoroutineScope()
    val clientSocket = remember { mutableStateOf<Socket?>(null) }
    var currentScreen by remember { mutableStateOf<Screens>(Screens.Main) }
    val data = remember { mutableStateOf<SensorData?>(null) }

    Box(modifier = modifier) {
        //Handling the navigation based on the callback
        when (currentScreen) {
            Screens.ConfigSensor -> ConfigSensorScreen(
                data,
                coroutineScope,
                clientSocket)

            Screens.Main -> MainScreen(
                data,
                coroutineScope,
                clientSocket,
                onNavigate = { currentScreen = Screens.ConfigSensor })
        }
    }
}


