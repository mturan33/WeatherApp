package com.example.thirdprojectapp

class RequestWeather {
    // Json response:
    // "current_units": {"temperature_2m": "C"},
    // "current": {"temperature_2m": -1.6},
    // "daily": {
    //    "time": [
    //      "2025-01-14",
    //      "2025-01-15",
    //      "2025-01-16",
    //      "2025-01-17",
    //      "2025-01-18",
    //      "2025-01-19",
    //      "2025-01-20"
    //    ],
    //    "weather_code": [61, 73, 3, 3, 3, 3, 3]
    //  }

    var current_units: Map<String, String> = mapOf()
    var current: CurrentWeather = CurrentWeather()
    var daily: WeeklyWeather = WeeklyWeather()
}