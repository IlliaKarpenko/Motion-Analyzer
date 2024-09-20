package com.smart.humanacitivtyapp

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader
import android.graphics.Color
import android.view.MenuItem
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.app.ActivityCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IFillFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Locale

// Define highlight colors as constants
// Import the color resources
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.github.mikephil.charting.components.LimitLine
import com.google.android.material.navigation.NavigationView
import java.util.Date

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private val REQUEST_CODE_STORAGE_PERMISSION = 1
    private val REQUEST_CODE_PICK_CSV = 2

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbar: Toolbar

    private lateinit var dataSetAccX: LineDataSet
    private lateinit var dataSetAccY: LineDataSet
    private lateinit var dataSetAccZ: LineDataSet
    private lateinit var dataSetGyroX: LineDataSet
    private lateinit var dataSetGyroY: LineDataSet
    private lateinit var dataSetGyroZ: LineDataSet

    // Key for SharedPreferences
    private val PREFERENCES_FILE = "app_preferences"
    private val KEY_FILE_URI = "file_uri"
    private val KEY_CHECKBOX_ACCEL_X = "checkbox_accel_x"
    private val KEY_CHECKBOX_ACCEL_Y = "checkbox_accel_y"
    private val KEY_CHECKBOX_ACCEL_Z = "checkbox_accel_z"
    private val KEY_CHECKBOX_GYRO_X = "checkbox_gyro_x"
    private val KEY_CHECKBOX_GYRO_Y = "checkbox_gyro_y"
    private val KEY_CHECKBOX_GYRO_Z = "checkbox_gyro_z"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setup Toolbar
        drawerLayout = findViewById(R.id.drawerLayout)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Setup DrawerLayout and NavigationView
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        val navigationView: NavigationView = findViewById(R.id.navView)
        navigationView.setNavigationItemSelectedListener(this)

        // Set default selected item
        navigationView.menu.findItem(R.id.nav_main_dashboard).isChecked = true

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_CODE_STORAGE_PERMISSION
            )
        }

        // Load saved preferences for file URI and checkbox states
        val sharedPreferences = getSharedPreferences(PREFERENCES_FILE, MODE_PRIVATE)

        // Restore the last selected file URI and process it if available
        val lastFileUri = sharedPreferences.getString(KEY_FILE_URI, null)
        if (lastFileUri != null) {
            val fileUri = Uri.parse(lastFileUri)
            processCsvFile(fileUri) // Process the saved file
        }

        // Restore checkbox states
        findViewById<CheckBox>(R.id.cbAccelerometerX).isChecked = sharedPreferences.getBoolean(KEY_CHECKBOX_ACCEL_X, false)
        findViewById<CheckBox>(R.id.cbAccelerometerY).isChecked = sharedPreferences.getBoolean(KEY_CHECKBOX_ACCEL_Y, false)
        findViewById<CheckBox>(R.id.cbAccelerometerZ).isChecked = sharedPreferences.getBoolean(KEY_CHECKBOX_ACCEL_Z, false)
        findViewById<CheckBox>(R.id.cbGyroscopeX).isChecked = sharedPreferences.getBoolean(KEY_CHECKBOX_GYRO_X, false)
        findViewById<CheckBox>(R.id.cbGyroscopeY).isChecked = sharedPreferences.getBoolean(KEY_CHECKBOX_GYRO_Y, false)
        findViewById<CheckBox>(R.id.cbGyroscopeZ).isChecked = sharedPreferences.getBoolean(KEY_CHECKBOX_GYRO_Z, false)

        // Setup listeners to update checkbox states in SharedPreferences
        setupCheckboxListeners(sharedPreferences)

        // Set up the file picker button
        findViewById<Button>(R.id.btnPickFile).setOnClickListener {
            openFilePicker()
        }
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_CSV && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                // Persist the URI permission to access the file later
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

                // Save the URI in SharedPreferences
                val sharedPreferences = getSharedPreferences(PREFERENCES_FILE, MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putString(KEY_FILE_URI, uri.toString())
                editor.apply()

                // Process the file
                processCsvFile(uri)
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_main_dashboard -> {
                // Already on the MainActivity, no action needed
            }
            R.id.nav_activity_report -> {
                // Open ReportsActivity
                startActivity(Intent(this, ReportsActivity::class.java))
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"  // Adjust to the file type you expect, e.g. "text/csv"
        }
        startActivityForResult(intent, REQUEST_CODE_PICK_CSV)
    }

    private fun saveCheckboxState(key: String, isChecked: Boolean) {
        val sharedPreferences = getSharedPreferences(PREFERENCES_FILE, MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean(key, isChecked)
        editor.apply()
    }


    private fun setupCheckboxListeners(sharedPreferences: SharedPreferences) {
        findViewById<CheckBox>(R.id.cbAccelerometerX).setOnCheckedChangeListener { _, isChecked ->
            saveCheckboxState(KEY_CHECKBOX_ACCEL_X, isChecked)
            updateChartVisibility() // Update charts as needed
        }
        findViewById<CheckBox>(R.id.cbAccelerometerY).setOnCheckedChangeListener { _, isChecked ->
            saveCheckboxState(KEY_CHECKBOX_ACCEL_Y, isChecked)
            updateChartVisibility()
        }
        findViewById<CheckBox>(R.id.cbAccelerometerZ).setOnCheckedChangeListener { _, isChecked ->
            saveCheckboxState(KEY_CHECKBOX_ACCEL_Z, isChecked)
            updateChartVisibility()
        }
        findViewById<CheckBox>(R.id.cbGyroscopeX).setOnCheckedChangeListener { _, isChecked ->
            saveCheckboxState(KEY_CHECKBOX_GYRO_X, isChecked)
            updateChartVisibility()
        }
        findViewById<CheckBox>(R.id.cbGyroscopeY).setOnCheckedChangeListener { _, isChecked ->
            saveCheckboxState(KEY_CHECKBOX_GYRO_Y, isChecked)
            updateChartVisibility()
        }
        findViewById<CheckBox>(R.id.cbGyroscopeZ).setOnCheckedChangeListener { _, isChecked ->
            saveCheckboxState(KEY_CHECKBOX_GYRO_Z, isChecked)
            updateChartVisibility()
        }
    }

    private fun processCsvFile(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val contentResolver = contentResolver
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
                        "Data Period: ${dateFormatDisplay.format(Date(minTimestamp))} to ${timeFormatDisplay.format(Date(maxTimestamp))}"
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

                    // Switch to the main thread to update the chart UI
                    withContext(Dispatchers.Main) {
                        // Update the period text view
                        val periodTextView = findViewById<TextView>(R.id.tvDataPeriod)
                        periodTextView.text = periodText

                        setupChart(
                            accelerometerX, accelerometerY, accelerometerZ,
                            gyroscopeX, gyroscopeY, gyroscopeZ,
                            activityRanges, activityIds
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("FilePicker", "Error processing file", e)
                    Toast.makeText(this@MainActivity, "Error processing file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupChart(
        accX: List<Entry>, accY: List<Entry>, accZ: List<Entry>,
        gyroX: List<Entry>, gyroY: List<Entry>, gyroZ: List<Entry>,
        activityRanges: List<Pair<Float, Float>>,  // List of time ranges for activities
        activityIds: List<Int>  // Corresponding activity IDs
    ) {
        val chart = findViewById<LineChart>(R.id.lineChart)

        // Clear previous data and configurations
        chart.clear()
        chart.data = null
        chart.notifyDataSetChanged()

        try {
            // Initially disable the checkboxes
            disableCheckboxes()
            // Define LineDataSets for Accelerometer and Gyroscope data
            dataSetAccX = LineDataSet(accX, "Accelerometer X").apply {
                color = ColorTemplate.COLORFUL_COLORS[0]
                lineWidth = 1f
                setDrawCircles(false)
                setDrawValues(false)
            }
            dataSetAccY = LineDataSet(accY, "Accelerometer Y").apply {
                color = ColorTemplate.COLORFUL_COLORS[1]
                lineWidth = 1f
                setDrawCircles(false)
                setDrawValues(false)
            }
            dataSetAccZ = LineDataSet(accZ, "Accelerometer Z").apply {
                color = ColorTemplate.COLORFUL_COLORS[2]
                lineWidth = 1f
                setDrawCircles(false)
                setDrawValues(false)
            }
            dataSetGyroX = LineDataSet(gyroX, "Gyroscope X").apply {
                color = ColorTemplate.COLORFUL_COLORS[3]
                lineWidth = 1f
                setDrawCircles(false)
                setDrawValues(false)
            }
            dataSetGyroY = LineDataSet(gyroY, "Gyroscope Y").apply {
                color = ColorTemplate.COLORFUL_COLORS[4]
                lineWidth = 1f
                setDrawCircles(false)
                setDrawValues(false)
            }
            dataSetGyroZ = LineDataSet(gyroZ, "Gyroscope Z").apply {
                color = ColorTemplate.JOYFUL_COLORS[0]
                lineWidth = 1f
                setDrawCircles(false)
                setDrawValues(false)
            }

            val lineData = LineData(dataSetAccX, dataSetAccY, dataSetAccZ, dataSetGyroX, dataSetGyroY, dataSetGyroZ)
            chart.data = lineData

            // General chart settings
            //chart.description.isEnabled = false  // Disable description text
            chart.description.text = "Accelerometer and Gyroscope Data"
            chart.setDrawGridBackground(false)
            chart.legend.isEnabled = false
            chart.description.isEnabled = false  // Disable description text
            chart.isHighlightPerDragEnabled = false  // Disable crosshair cursor on drag
            chart.isHighlightPerTapEnabled = false  // Disable crosshair cursor on tap

            // Drag and scale settings
            chart.isDragEnabled = false
            chart.isDragXEnabled = true
            chart.setPinchZoom(true) // Enable pinch zoom for both axes
            chart.isDoubleTapToZoomEnabled = true
            chart.viewPortHandler.setDragOffsetY(0f) // Prevent vertical dragging

            chart.setScaleEnabled(false) // Disable scaling for both axes
            chart.setScaleXEnabled(true) // Enable scaling only for the X-axis
            chart.setScaleYEnabled(false) // Disable scaling for the Y-axis

            // X-axis settings
            val xAxis = chart.xAxis
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    // Convert seconds to a readable time format (optional)
                    return String.format("%.2f s", value)
                }
            }
            xAxis.setDrawGridLines(true)
            xAxis.gridColor = Color.LTGRAY
            xAxis.gridLineWidth = 0.5f
            xAxis.axisLineWidth = 1f
            xAxis.textColor = Color.WHITE

            // Y-axis settings
            val yAxis = chart.axisLeft
            xAxis.setDrawGridLines(true)
            xAxis.gridColor = Color.LTGRAY
            xAxis.gridLineWidth = 0.5f
            yAxis.axisMinimum = -15f  // Set Y-axis minimum value
            yAxis.axisMaximum = 15f   // Set Y-axis maximum value
            yAxis.setDrawLabels(true)
            yAxis.setCenterAxisLabels(true)
            yAxis.textColor = Color.WHITE

            // Ensure Y-axis is centered around 0
            yAxis.axisLineWidth = 0.5f
            yAxis.axisLineColor = Color.GRAY

            // Disable right axis
            chart.axisRight.isEnabled = false

            // Loop through activity ranges to create highlight sets
            for (i in activityRanges.indices) {
                val activityId = activityIds[i]
                val range = activityRanges[i]

                // Create a fill color for the activity region
                val fillColor = when (activityId) {
                    5 -> ContextCompat.getColor(this, R.color.highlight_stand_sit)
                    1 -> ContextCompat.getColor(this, R.color.highlight_standing)
                    2 -> ContextCompat.getColor(this, R.color.highlight_sitting)
                    12 -> ContextCompat.getColor(this, R.color.highlight_walking)
                    else -> ContextCompat.getColor(this, R.color.highlight_default)
                }

                // Create a dataset for the highlighted region
                val highlightSet = LineDataSet(listOf(
                    Entry(range.first, chart.axisLeft.axisMinimum),  // Starting point of the range
                    Entry(range.second, chart.axisLeft.axisMinimum),  // Ending point of the range
                    Entry(range.second, chart.axisLeft.axisMaximum),  // Ending point at top
                    Entry(range.first, chart.axisLeft.axisMaximum)   // Starting point at top
                ), "Activity Highlight").apply {
                    setDrawFilled(true)  // Enable the fill
                    setFillColor(fillColor)  // Set the fill color
                    fillAlpha = 30  // Set transparency
                    setDrawCircles(false)  // Remove any dots from the highlighted regions
                    color = Color.TRANSPARENT  // Set the line color to transparent
                    lineWidth = 0.0f
                    setDrawValues(false)  // Don't display any values on this dataset

                    // Use fillFormatter to fill the entire vertical space
                    fillFormatter = IFillFormatter { _, _ -> chart.axisLeft.axisMaximum }
                }

                // Add the dataset to the chart
                chart.data.addDataSet(highlightSet)
            }

            // Refresh the chart
            chart.invalidate()
            chart.zoom(8f, 1f, 0f, 0f)

            // Set initial visibility of checkboxes and chart
            enableCheckboxes()

            // Set initial visibility
            updateChartVisibility()
        } catch (e: Exception) {
            Log.e("setupChart", "Error setting up chart", e)
        }
    }

    private fun lockVerticalDrag(chart: LineChart) {
        // Disable vertical dragging
        chart.viewPortHandler.setDragOffsetY(0f)
        chart.isDragYEnabled = false // Disable Y-axis dragging
    }

    private fun updateChartVisibility() {
        val chart = findViewById<LineChart>(R.id.lineChart)
        val cbX = findViewById<CheckBox>(R.id.cbAccelerometerX)
        val cbY = findViewById<CheckBox>(R.id.cbAccelerometerY)
        val cbZ = findViewById<CheckBox>(R.id.cbAccelerometerZ)

        val cbGyroX = findViewById<CheckBox>(R.id.cbGyroscopeX)
        val cbGyroY = findViewById<CheckBox>(R.id.cbGyroscopeY)
        val cbGyroZ = findViewById<CheckBox>(R.id.cbGyroscopeZ)

        // Set visibility based on checkbox states
        dataSetAccX.isVisible = cbX.isChecked
        dataSetAccY.isVisible = cbY.isChecked
        dataSetAccZ.isVisible = cbZ.isChecked

        dataSetGyroX.isVisible = cbGyroX.isChecked
        dataSetGyroY.isVisible = cbGyroY.isChecked
        dataSetGyroZ.isVisible = cbGyroZ.isChecked

        // Notify the chart that data has changed
        chart.data.notifyDataChanged()
        chart.notifyDataSetChanged()
        chart.invalidate()  // Refresh the chart
    }

    private fun disableCheckboxes() {
        // Disable checkboxes
        findViewById<CheckBox>(R.id.cbAccelerometerX).isEnabled = false
        findViewById<CheckBox>(R.id.cbAccelerometerY).isEnabled = false
        findViewById<CheckBox>(R.id.cbAccelerometerZ).isEnabled = false
        findViewById<CheckBox>(R.id.cbGyroscopeX).isEnabled = false
        findViewById<CheckBox>(R.id.cbGyroscopeY).isEnabled = false
        findViewById<CheckBox>(R.id.cbGyroscopeZ).isEnabled = false
    }

    private fun enableCheckboxes() {
        // Enable checkboxes
        findViewById<CheckBox>(R.id.cbAccelerometerX).isEnabled = true
        findViewById<CheckBox>(R.id.cbAccelerometerY).isEnabled = true
        findViewById<CheckBox>(R.id.cbAccelerometerZ).isEnabled = true
        findViewById<CheckBox>(R.id.cbGyroscopeX).isEnabled = true
        findViewById<CheckBox>(R.id.cbGyroscopeY).isEnabled = true
        findViewById<CheckBox>(R.id.cbGyroscopeZ).isEnabled = true
    }
}