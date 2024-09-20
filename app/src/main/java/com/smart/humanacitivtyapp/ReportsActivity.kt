package com.smart.humanacitivtyapp

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReportsActivity : AppCompatActivity() {

    private lateinit var pieChart: PieChart
    private val KEY_FILE_URI = "file_uri"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reports)

        // Set up the toolbar as the action bar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Get the URI from SharedPreferences
        val sharedPreferences = getSharedPreferences("app_preferences", MODE_PRIVATE)
        val lastFileUri = sharedPreferences.getString(KEY_FILE_URI, null)

        // If the URI is available, process the CSV file
        lastFileUri?.let {
            val uri = Uri.parse(it)
            loadSensorData(uri)
        } ?: run {
            // Handle the case where the URI is not found
            Toast.makeText(this, "No CSV file selected. Please choose a file.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getActivityColor(activityId: Int): Int {
        return when (activityId) {
            5 -> ContextCompat.getColor(this, R.color.flat_yellow)
            1 -> ContextCompat.getColor(this, R.color.flat_green)
            2 -> ContextCompat.getColor(this, R.color.flat_blue)
            12 -> ContextCompat.getColor(this, R.color.flat_red)
            else -> ContextCompat.getColor(this, R.color.flat_purple)
        }
    }

    private fun populatePieChart(activityIds: List<Int>, activityRanges: List<Pair<Float, Float>>, periodText: String) {
        val activityTimes = mutableMapOf<Int, Float>() // Map to store total time for each activity

        // Calculate total time for each activity ID
        for (i in activityIds.indices) {
            val activityId = activityIds[i]
            val timeRange = activityRanges[i]
            val duration = timeRange.second - timeRange.first // Calculate duration for this activity

            // Accumulate the duration for each activity ID
            activityTimes[activityId] = (activityTimes[activityId] ?: 0f) + duration
        }

        val pieEntries = mutableListOf<PieEntry>()
        val colors = mutableListOf<Int>() // List to hold the colors

        // Prepare PieEntries based on total time
        for ((activityId, totalTime) in activityTimes) {
            val color = getActivityColor(activityId)
            pieEntries.add(PieEntry(totalTime, when (activityId) {
                1 -> "Standing"
                2 -> "Sitting"
                5 -> "Stand-Sit"
                12 -> "Walking"
                else -> "Unknown"
            }))
            colors.add(color) // Add the color to the colors list
        }

        // Set up the PieChart here
        pieChart = findViewById<PieChart>(R.id.pieChart)
        val pieDataSet = PieDataSet(pieEntries, "").apply {
            this.colors = colors // Assign the list of colors directly
            valueTextSize = 16f
            setDrawValues(true)// Remove section legends
        }

        pieChart.legend.textSize = 16f
        pieChart.data = PieData(pieDataSet)
        pieChart.description.isEnabled = false // Disable the description label
        pieChart.invalidate() // Refresh the chart

        // Update the activity breakdown message
        findViewById<TextView>(R.id.tvActivityBreakdown).text = "$periodText"
    }

    private fun loadSensorData(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            val sensorData = CsvUtils.processCsvFile(uri, contentResolver)
            withContext(Dispatchers.Main) {
                sensorData?.let { data ->
                    populatePieChart(data.activityIds, data.activityRanges, data.periodText)
                }
            }
        }
    }

    // Handle the back button click in the toolbar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Navigate back when the home (back) button is pressed
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}