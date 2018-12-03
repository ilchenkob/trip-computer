package com.vitalyilchenko.tripcomputer.services

/**
 * Created by vitalyilchenko on 3/2/18.
 */
object  ConsumptionManager {

    private const val degreeInLiters = 0.235
    private const val grPerSecToLitPerHourConst: Double = 32.8722093

    private val itemsCount = (300 / TripComputerService.TriggerIntervalSec).toInt()
                             // (5 mins * 60) = 300

    private val data: DoubleArray = DoubleArray(itemsCount, { 10.0 })
    private var currentIndex: Int = 0

    fun reset() {
        data.forEachIndexed { index, _ -> data[index] = 10.0 }
    }

    fun addValues(mafValue: Double, speedValue: Int) {
        var currentConsumption: Double = 15.0 // in case when current speed = 0

        if (speedValue > 5) {
//            val mpgConstant = 71.07
//            val gallonToLiter = 378.5411784
//            val milesToKm = 1.609344

//            var currentSpeed: Double = if (speedValue > 1) speedValue.toDouble() else 1.0
//            var mpg: Double = (mpgConstant * currentSpeed) / mafValue
//            currentConsumption = gallonToLiter / (milesToKm * mpg)

            var currentSpeed: Double = speedValue.toDouble()
            currentConsumption = (mafValue * grPerSecToLitPerHourConst) / currentSpeed
        }

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