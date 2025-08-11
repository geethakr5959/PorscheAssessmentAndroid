package com.geethakr.porscheclientapp.ui.configSensor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.geethakr.porscheclientapp.protobuf.SensorDataProto.SensorData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.Socket

@Composable
fun ConfigSensorScreen(
    sensorData: MutableState<SensorData?>,
    coroutineScope: CoroutineScope,
    serverSocket: MutableState<Socket?>, //onBack: () -> Unit
) {
    val pressure = remember { mutableStateOf(sensorData.value?.pressure?.toString() ?: "") }
    val tpms = remember { mutableStateOf(sensorData.value?.tpms?.toString() ?: "") }
    val temperature = remember { mutableStateOf(sensorData.value?.temperature?.toString() ?: "") }
    val lightsOn = remember { mutableStateOf(sensorData.value?.lightsOn ?: false) }
    val fuelLevel = remember { mutableStateOf(sensorData.value?.fuelLevel ?: 50) }


    //Ping the URL for temp : https://api.open-meteo.com/v1/forecast?latitude=52.52&longitude=13.41&current=temperature_2m



    Column(
        modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        //Pressure field
        OutlinedTextField(
            value = pressure.value.toString(),
            onValueChange = { pressure.value = it },
            label = { Text("Pressure Sensor (psi)") },
            modifier = Modifier.fillMaxWidth()
        )

        //Lights Toggle field
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Lights")
            Switch(
                checked = lightsOn.value,
                onCheckedChange = { lightsOn.value = it }
            )
        }

        //TPMS field
        OutlinedTextField(
            value = tpms.value,
            onValueChange = { tpms.value = it },
            label = { Text("TPMS Value") },
            modifier = Modifier.fillMaxWidth()
        )

        //Temperature field
        OutlinedTextField(
            value = temperature.value,
            onValueChange = { temperature.value = it },
            label = { Text("Temperature Sensor (°C)") },
            modifier = Modifier.fillMaxWidth()
        )

        //Fuel slider field
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Fuel Sensor Level: ${fuelLevel.value}%")
            Slider(
                value = fuelLevel.value.toFloat(),
                onValueChange = { fuelLevel.value = it.toInt() },
                valueRange = 0f..100f
            )
        }

        //Submit
        Button(
            onClick = {
                println("Collected Data:")
                println("Pressure: $pressure")
                println("Lights: ${if (lightsOn.value) "On" else "Off"}")
                println("TPMS: $tpms")
                println("Temperature: $temperature")
                println("Fuel Level: ${fuelLevel}%")
                sensorData.value = SensorData.newBuilder()
                    .setPressure(pressure.value?.toFloat() ?: 0.0f)
                    .setLightsOn(lightsOn.value)
                    .setTpms(tpms.value.toFloat())
                    .setTemperature(temperature.value.toFloat())
                    .setFuelLevel(fuelLevel.value)
                    .build()

                coroutineScope.launch(Dispatchers.IO) {
                    serverSocket.value.let { socket ->
                        serverSocket.value?.let {
                            sendMessage(
                                socket = it, sensorData.value ?: SensorData.getDefaultInstance()
                            )
                        }
                    }
                }
                //onBack()
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Submit")
        }
    }
}

/**
 * Function to send the message from client to the server
 */
suspend fun sendMessage(socket: Socket, sensorData: SensorData) {
    withContext(Dispatchers.IO) {
        try {
            sensorData.writeDelimitedTo(socket.getOutputStream())
            socket
                .getOutputStream().flush()
//            val writer = PrintWriter(socket.getOutputStream(), true)
//            writer.println(sensorData.writeDelimitedTo(socket.getOutputStream()))
        } catch (e: Exception) {
            println("Error sending message: ${e.message}")
            e.printStackTrace()
        }
    }
}

//fun parseJSONTemperature() : Float {

//    val jObj = JSONObject("val")
//    val stringVal = "{
//        "latitude":52.52,
//        "longitude":13.419998,
//        "generationtime_ms":0.020503997802734375,
//        "utc_offset_seconds":0,
//        "timezone":"GMT",
//        "timezone_abbreviation":"GMT",
//        "elevation":38.0,
//        "current_units":
//        {"time":"iso8601",
//            "interval":"seconds",
//            "temperature_2m":"°C"},
//        "current":{"time":"2025-08-08T08:45","interval":900,"temperature_2m":22.2}}"
//    val jArray = JSONArray(jObj)
//}