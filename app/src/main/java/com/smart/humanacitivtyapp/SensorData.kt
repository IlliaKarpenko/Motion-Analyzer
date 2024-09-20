package com.smart.humanacitivtyapp

import com.github.mikephil.charting.data.Entry

data class SensorData(
    val accelerometerX: List<Entry>,
    val accelerometerY: List<Entry>,
    val accelerometerZ: List<Entry>,
    val gyroscopeX: List<Entry>,
    val gyroscopeY: List<Entry>,
    val gyroscopeZ: List<Entry>,
    val activityRanges: List<Pair<Float, Float>>,
    val activityIds: List<Int>,
    val periodText: String
)