package com.vitalyilchenko.tripcomputer.Models

/**
 * Created by vitalyilchenko on 2/26/18.
 */
enum class ConnectionState {
    Unknown,
    Disconnected,
    Connecting,
    Connected
}

class TripData {
    var state: ConnectionState = ConnectionState.Unknown
    var timestamp: String = "--"
    var engineTemperature: String = "--"
    var voltage: String = "--"
    var reserve: String = "--"
    var fuelConsumption: String = "--"
}