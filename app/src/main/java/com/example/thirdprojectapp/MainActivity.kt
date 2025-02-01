package com.example.thirdprojectapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.thirdprojectapp.databinding.ActivityMainBinding
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import java.io.InputStreamReader
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.location.LocationRequest
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.TextView

class MainActivity : AppCompatActivity(), OnMapReadyCallback, SensorEventListener
{   // Binding for the layout
    private lateinit var binding: ActivityMainBinding
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    // Sensors and SensorManager for temperature and humidity
    private lateinit var sensorManager: SensorManager
    private var temperatureSensor: Sensor? = null
    private var humiditySensor: Sensor? = null

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        // Temperature sensor
        temperatureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
        if (temperatureSensor == null) {
            binding.deviceTemperatureSensor.text = "Temperature sensor is not available."
        }
        // Humidity sensor
        humiditySensor = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY)
        if (humiditySensor == null) {
            binding.deviceHumiditySensor.text = "Humidity sensor is not available."
        }

        // Fetch weather data for Bialystok
        fetchWeatherData(53.11946391875196, 23.15211969372587).start()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Get the map fragment and initialize the map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        checkPermission()
    }

    override fun onResume() {
        super.onResume()
        // Register the sensors
        temperatureSensor?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        humiditySensor?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        // Unregister the sensors
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            when (event.sensor.type) {
                Sensor.TYPE_AMBIENT_TEMPERATURE -> {
                    val temperature = event.values[0]
                    binding.deviceTemperatureSensor.text = "Temperature Sensor: $temperature °C"
                }
                Sensor.TYPE_RELATIVE_HUMIDITY -> {
                    val humidity = event.values[0]
                    binding.deviceHumiditySensor.text = "Humidity Sensor: $humidity %"
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.isMyLocationEnabled = true
        mMap.uiSettings.isZoomGesturesEnabled = true
        mMap.uiSettings.isScrollGesturesEnabled = true
        mMap.uiSettings.isZoomGesturesEnabled = true
        mMap.uiSettings.isScrollGesturesEnabledDuringRotateOrZoom = true
        mMap.uiSettings.isZoomControlsEnabled = true

        // Default location and zoom level 53.11945416274829, 23.15212578972725
        val defaultLocation = LatLng(53.11945416274829, 23.15212578972725)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f))

        // Listener for map click events
        mMap.setOnMapClickListener { latLng ->
            mMap.clear()
            // Add a marker at the clicked location
            mMap.addMarker(MarkerOptions().position(latLng).title("Clicked Location"))
            // Show the latitude and longitude of the touched location
            val latitude = latLng.latitude
            val longitude = latLng.longitude
            println("Clicked Location: Latitude: $latitude, Longitude: $longitude")
            binding.longitude.text = String.format("Latitude: %.6f, Longitude: %.6f", latitude, longitude)

            fetchWeatherData(latitude, longitude).start() // Fetch weather data
        }

        // Move the camera to current location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            getLastKnownLocation()
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLastKnownLocation()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getLastKnownLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    // Move camera to current location
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f)) // Zoom level

                    // Go To Bialystok
                    val defaultLocation = LatLng(53.11945416274829, 23.15212578972725)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f))
                }
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            // Create a request with high accuracy
            val currentLocationRequest = CurrentLocationRequest.Builder()
                .setPriority(PRIORITY_HIGH_ACCURACY)
                .build()
            // Request current location
            fusedLocationClient.getCurrentLocation(currentLocationRequest, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        binding.longitude.text = String.format("Latitude: %.6f, Longitude: %.6f", location.latitude, location.longitude)
                    } else {
                        Toast.makeText(this, "Failed to get location", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error getting location: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun fetchWeatherData(latitude: Double, longitude: Double): Thread
    {
        return Thread {
            // https://api.open-meteo.com/v1/forecast?latitude=53.11946391875196&longitude=23.15211969372587&current=temperature_2m&daily=weather_code&timezone=Europe%2FBerlin
            val urlWeather = URL("https://api.open-meteo.com/v1/forecast?latitude="+
                    latitude.toString()+
                    "&longitude="+
                    longitude.toString()+
                    "&current=temperature_2m&daily=weather_code&timezone=Europe%2FBerlin")
            val connectionWeather  = urlWeather.openConnection() as HttpsURLConnection

            if(connectionWeather.responseCode == 200)
            {
                val inputSystem = connectionWeather.inputStream
                val inputStreamReader = InputStreamReader(inputSystem, "UTF-8")
                val request = Gson().fromJson(inputStreamReader, RequestWeather::class.java)
                updateWeatherUI(request)
                inputStreamReader.close()
                inputSystem.close()
            }
            else
            {
                binding.currentWeather.text = "Failed Connection"
            }
        }
    }

    private fun getWeatherDescription(code: Int): String {
        return when (code) {
            0 -> "Clear sky"
            1, 2, 3 -> "Mainly clear, partly cloudy, and overcast"
            45, 48 -> "Fog and depositing rime fog"
            51, 53, 55 -> "Drizzle: Light, moderate, and dense intensity"
            56, 57 -> "Freezing Drizzle: Light and dense intensity"
            61, 63, 65 -> "Rain: Slight, moderate, and heavy intensity"
            66, 67 -> "Freezing Rain: Light and heavy intensity"
            71, 73, 75 -> "Snow fall: Slight, moderate, and heavy intensity"
            77 -> "Snow grains"
            80, 81, 82 -> "Rain showers: Slight, moderate, and violent"
            85, 86 -> "Snow showers: Slight and heavy"
            95 -> "Thunderstorm: Slight or moderate"
            96, 99 -> "Thunderstorm with slight and heavy hail"
            else -> "Unknown weather code"
        }
    }

    @SuppressLint("DefaultLocale")
    private fun updateWeatherUI(request: RequestWeather)
    {
        runOnUiThread {
            kotlin.run {
                binding.currentWeather.text = String.format(
                    "Current Weather: %.2f %s", request.current.temperature_2m, request.current_units["temperature_2m"])

                if (request.daily.time.isNotEmpty() && request.daily.weather_code.isNotEmpty()) {
                    binding.weeklyWeather.text = String.format(
                        "\nWeather of the Day %s:\n %s \n\n Weather of the Day %s:\n %s \n\nWeather of the Day %s:\n %s \n\nWeather of the Day %s:\n %s \n" +
                        "\nWeather of the Day %s:\n %s \n\nWeather of the Day %s:\n %s \n\nWeather of the Day %s:\n %s\n",
                        request.daily.time[0], getWeatherDescription(request.daily.weather_code[0]),
                        request.daily.time[1], getWeatherDescription(request.daily.weather_code[1]),
                        request.daily.time[2], getWeatherDescription(request.daily.weather_code[2]),
                        request.daily.time[3], getWeatherDescription(request.daily.weather_code[3]),
                        request.daily.time[4], getWeatherDescription(request.daily.weather_code[4]),
                        request.daily.time[5], getWeatherDescription(request.daily.weather_code[5]),
                        request.daily.time[6], getWeatherDescription(request.daily.weather_code[6])
                    )
                } else {
                    binding.weeklyWeather.text = "Weekly weather data is unavailable"
                }
            }
        }
    }

}