package com.vitalyilchenko.tripcomputer.services

/**
 * Created by vitalyilchenko on 3/2/18.
 */
object  ConsumptionManager {

    private const val degreeInLiters = 0.235
    private const val itemsCount = 43 // 43 frames taken every 7 second gives data for the last 5 minutes of road

    private val data: DoubleArray = DoubleArray(itemsCount, { 10.0 })
    private var currentIndex: Int = 0

    fun addValues(mafValue: Double, speedValue: Int) {
        val mpgConstant = 71.07
        val gallonToLiter = 378.5411784
        val milesToKm = 1.609344

        var currentSpeed: Double = if (speedValue > 1) speedValue.toDouble() else 0.5
        var mpg: Double = (mpgConstant * speedValue) / mafValue
        var currentConsumption: Double = gallonToLiter / (milesToKm * mpg)

        data[currentIndex++] = currentConsumption
        if (currentIndex == itemsCount)
            currentIndex = 0
    }

    fun getReserveDistance(fuelLevel: Int): Int {
        var averageConsumption = data.average()
        var litersLeft = fuelLevel * degreeInLiters
        var result = (litersLeft / averageConsumption) * 100
        return result.toInt()
    }

    fun getAverageConsumption(): String {
        return "%.1f".format(data.average())
    }
}