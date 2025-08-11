package com.geethakr.porscheclientapp.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geethakr.porscheclientapp.protobuf.SensorDataProto.SensorData
import com.geethakr.porscheclientapp.ui.configSensor.ConfigSensorScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.Socket

@Composable
fun MainScreen(
    sensorData: MutableState<SensorData?>,
    coroutineScope: CoroutineScope,
    serverSocket: MutableState<Socket?>, onNavigate: () -> Unit
) {
    var showDialog = remember { mutableStateOf<Boolean>(false) }

    Row {

        //Leftside Pane UI
        Surface(modifier = Modifier.width(300.dp), color = Color.Gray) {
            Column(modifier = Modifier.fillMaxHeight()) {
                ShowConnectButton(sensorData, coroutineScope, serverSocket)
                ShowConfigSensorButton(showDialog)
                ShowDisconnectButton()
            }
        }

        //Rightside Pane UI
        Surface(
            modifier = Modifier
                .background(Color.Black)
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(10.dp),
            color = Color.Black
        ) {
            msgLogList =
                remember {
                    mutableStateListOf(
                        "Client not connected with server. " +
                                "Click on Connect to start the connection with server!"
                    )
                }
            ShowExchangingMessageList(msgLogList.toList())
        }
    }

    if(showDialog.value) {
            AlertDialog(
                onDismissRequest = { showDialog.value = false },
                title = { Text(text = "Configure Sensor Data") },
                text = { ConfigSensorScreen(sensorData,coroutineScope,serverSocket) },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = {
                        showDialog.value = false
                    }) {
                        Text("Close")
                    }
                }
            )

    }
}

/**
 * Composable function to handle the connect button ui and behavior
 */
@Composable
private fun ShowConnectButton(
    sensorData: MutableState<SensorData?>,
    coroutineScope: CoroutineScope,
    serverSocket: MutableState<Socket?>,
) {
    val receiveJob = remember { mutableStateOf<Job?>(null) }

    Button(
        onClick = { connectToServer(sensorData, coroutineScope, serverSocket, receiveJob) },
        modifier = Modifier.width(300.dp),
        enabled = true,
    ) { Text("Connect to Server") }

}

/**
 * Composable function to handle the Configure Sensor button ui and behavior
 */
@Composable
private fun ShowConfigSensorButton(showDialog: MutableState<Boolean>) {
    Button(
        onClick = { showDialog.value = true },
        modifier = Modifier.width(300.dp),
        enabled = true,
    ) { Text("Configure Sensors") }
}


/**
 * Composable function to handle the disconnect button ui and behavior
 */
@Composable
private fun ShowDisconnectButton() {
    Button(
        onClick = {},
        modifier = Modifier.width(300.dp),
        enabled = true,
    ) { Text("Disconnect") }
}


/**
 * Composable function to show the right side pane UI that displays the list of messages exchanged between
 * the emulator and UI
 */
@Composable
fun ShowExchangingMessageList(list: List<String>) {
    LazyColumn(state = LazyListState(list.size)) {
        var color: Color
        items(items = list) {
            if (it.isNotEmpty()) {
                color = Color.White
                Text(text = it, color = color, fontSize = 16.sp)
            }
        }
    }
}

/**
 * Function to handle the server connection and msg exchanging
 */
fun connectToServer(
    data: MutableState<SensorData?>,
    coroutineScope: CoroutineScope,
    serverSocket: MutableState<Socket?>,
    receiveJob: MutableState<Job?>,
) {

    val ipAddress = "192.168.1.4"
    val port = 6666

    msgLogList.add("Connecting to server...")

    coroutineScope.launch {
        withContext(Dispatchers.IO) {
            try {

                val socket = Socket(ipAddress, port)

                //saving the socket for later use as well
                serverSocket.value = socket

                withContext(Dispatchers.Main) {
                    msgLogList.add("Connected to ${socket.inetAddress.hostAddress}")
                }

                receiveJob.value = launch(Dispatchers.IO) {
                    handleServerConnection(socket) { sensorData ->
                        data.value = sensorData
                        msgLogList.add("Received message from server:")
                        msgLogList.add(
                            "TPMS:${sensorData.tpms}, Pressure:${sensorData.pressure}, " +
                                    "Lights:${sensorData.lightsOn}, Temperature:${sensorData.temperature}," +
                                    " Fuel Level:${sensorData.fuelLevel}"
                        )

                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    msgLogList.add("Error: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }
}

/**
 * Function to handle the server socket connection and manage the message received
 */
suspend fun handleServerConnection(socket: Socket, onMessageReceived: (SensorData) -> Unit) {
    withContext(Dispatchers.IO) {
        try {
            val input: InputStream = socket.getInputStream()

            while (isActive) {
                val sensorData = SensorData.parseDelimitedFrom(input)
                if (sensorData != null) {
                    onMessageReceived(sensorData)
                } else {
                    break
                }
            }
        } catch (e: Exception) {
            msgLogList.add("Error receiving message: ${e.message}")
            e.printStackTrace()
        }
    }
}