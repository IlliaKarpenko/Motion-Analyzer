package com.smart.humanacitivtyapp

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate

class ReportsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reports)

        // Set the Toolbar as the ActionBar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Enable the back button in the toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.baseline_arrow_back) // Optional, you can set your own back icon

        // Set the title of the toolbar
        supportActionBar?.title = "Activity Report"

        val pieChart = findViewById<PieChart>(R.id.pieChart)

        // Sample data - replace with actual data
        val activityDurations = mapOf(
            "Sitting" to 120f, // minutes
            "Walking" to 90f,
            "Running" to 45f,
            "Cycling" to 60f
        )

        // Convert data to PieEntry format
        val entries = ArrayList<PieEntry>()
        for ((activity, duration) in activityDurations) {
            entries.add(PieEntry(duration, activity))
        }

        // Create PieDataSet
        val dataSet = PieDataSet(entries, "Activity Duration")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList() // Set colors for slices

        // Create PieData
        val data = PieData(dataSet)

        // Set data and update chart
        pieChart.data = data
        pieChart.invalidate() // Refresh the chart

        // Customize the chart (optional)
        pieChart.description = Description().apply {
            text = "Total Duration of Activities"
        }
        pieChart.isRotationEnabled = true // Enable rotation
        pieChart.animateY(1000) // Animation
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