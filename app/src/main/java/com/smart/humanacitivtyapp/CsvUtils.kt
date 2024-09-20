package com.smart.humanacitivtyapp

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.github.mikephil.charting.data.Entry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvUtils {
    @JvmStatic
    suspend fun processCsvFile(uri: Uri, contentResolver: ContentResolver): SensorData? {
        return withContext(Dispatchers.IO) {
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val csvData = reader.readLines()

                    // Lists to hold the accelerometer and gyroscope data
                    val accelerometerX = mutableListOf<Entry>()
                    val accelerometerY = mutableListOf<Entry>()
                    val accelerometerZ = mutableListOf<Entry>()
                    val gyroscopeX = mutableListOf<Entry>()
                    val gyroscopeY = mutableListOf<Entry>()
                    val gyroscopeZ = mutableListOf<Entry>()

                    // List to hold activity time ranges
                    val activityRanges = mutableListOf<Pair<Float, Float>>()  // Pair<startTime, endTime>
                    val activityIds = mutableListOf<Int>() // List to hold activity IDs for each range

                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

                    // Variable to keep track of the cumulative time
                    var cumulativeTimeSeconds = 0f

                    // Variables to keep track of the min and max timestamps
                    var minTimestamp: Long? = null
                    var maxTimestamp: Long? = null

                    for (i in 1 until csvData.size) {  // Start from 1 to skip the header, if present
                        val row = csvData[i].split(",")

                        // Ensure the row has enough columns to avoid ArrayIndexOutOfBoundsException
                        if (row.size < 1805) {
                            Log.e("CSVProcessing", "Skipping row $i: Not enough columns. Expected at least 1805, found ${row.size}.")
                            continue
                        }

                        try {
                            // Parse the start and end timestamps
                            val startTime = dateFormat.parse(row[0])?.time ?: 0L
                            val endTime = dateFormat.parse(row[1])?.time ?: 0L

                            // Update min and max timestamps
                            if (minTimestamp == null || startTime < minTimestamp) minTimestamp = startTime
                            if (maxTimestamp == null || endTime > maxTimestamp) maxTimestamp = endTime

                            // Calculate the total duration in milliseconds and time step per reading
                            val durationMillis = endTime - startTime
                            val timeStepMillis = durationMillis / 300f  // 300 data points per 3 seconds

                            // Extract the activity ID (column index 2)
                            val activityId = row[2].toInt()

                            // Store the activity start and end times along with its ID
                            activityRanges.add(Pair(cumulativeTimeSeconds, cumulativeTimeSeconds + durationMillis / 1000f))
                            activityIds.add(activityId)

                            // Generate time-based entries for accelerometer and gyroscope data
                            for (j in 0 until 300) {
                                val elapsedTimeMillis = j * timeStepMillis
                                val timeInSeconds = (elapsedTimeMillis / 1000f) + cumulativeTimeSeconds

                                // Add entries with time on X-axis
                                accelerometerX.add(Entry(timeInSeconds, row[5 + j].toFloat()))
                                accelerometerY.add(Entry(timeInSeconds, row[305 + j].toFloat()))
                                accelerometerZ.add(Entry(timeInSeconds, row[605 + j].toFloat()))
                                gyroscopeX.add(Entry(timeInSeconds, row[905 + j].toFloat()))
                                gyroscopeY.add(Entry(timeInSeconds, row[1205 + j].toFloat()))
                                gyroscopeZ.add(Entry(timeInSeconds, row[1505 + j].toFloat()))
                            }

                            // Update cumulative time with the duration of this row (3 seconds)
                            cumulativeTimeSeconds += durationMillis / 1000f

                        } catch (e: Exception) {
                            Log.e("CSVProcessing", "Error processing row $i: ${e.message}")
                            continue
                        }
                    }

                    // Format the period for display
                    val dateFormatDisplay = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val timeFormatDisplay = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    val periodText = if (minTimestamp != null && maxTimestamp != null) {
                        "Data Period: ${dateFormatDisplay.format(Date(minTimestamp))} to ${timeFormatDisplay.format(
                            Date(maxTimestamp)
                        )}"
                    } else {
                        "Data Period: Not available"
                    }

                    // Log the sizes of the lists
                    Log.d("CSVProcessing", "Accelerometer X size: ${accelerometerX.size}")
                    Log.d("CSVProcessing", "Accelerometer Y size: ${accelerometerY.size}")
                    Log.d("CSVProcessing", "Accelerometer Z size: ${accelerometerZ.size}")
                    Log.d("CSVProcessing", "Gyroscope X size: ${gyroscopeX.size}")
                    Log.d("CSVProcessing", "Gyroscope Y size: ${gyroscopeY.size}")
                    Log.d("CSVProcessing", "Gyroscope Z size: ${gyroscopeZ.size}")

                    // Return the processed data
                    return@use SensorData(
                        accelerometerX, accelerometerY, accelerometerZ,
                        gyroscopeX, gyroscopeY, gyroscopeZ,
                        activityRanges, activityIds, periodText
                    )
                }
            } catch (e: Exception) {
                Log.e("FilePicker", "Error processing file", e)
                return@withContext null
            }
        }
    }
}