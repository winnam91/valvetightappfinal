package com.example.valvetight

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.Locale
import kotlin.math.abs

class LeakRateActivity : AppCompatActivity() {

    // Renamed/Added UI elements for system volume
    private lateinit var editTextSystemVolumeLiters: TextInputEditText
    private lateinit var textInputLayoutSystemVolume: TextInputLayout

    private lateinit var editTextInitialPressure: TextInputEditText
    private lateinit var textInputLayoutInitialPressure: TextInputLayout
    private lateinit var editTextFinalPressure: TextInputEditText
    private lateinit var textInputLayoutFinalPressure: TextInputLayout
    private lateinit var spinnerPressureUnits: Spinner
    private lateinit var editTextTimeDuration: TextInputEditText
    private lateinit var textInputLayoutTimeDuration: TextInputLayout
    private lateinit var spinnerTimeUnits: Spinner
    private lateinit var buttonCalculateLeakRate: Button
    private lateinit var textViewLeakRateResult: TextView

    // Removed: private var systemVolumeLiters: Double = 0.0
    // We will now get it from the EditText when needed.

    private val PA_PER_KPA = 1000.0
    private val PA_PER_BAR = 100000.0
    private val PA_PER_PSI = 6894.757
    private val STANDARD_ATMOSPHERIC_PRESSURE_PA = 101325.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leak_rate)

        initializeUI()

        // Retrieve the passed volume and pre-fill if available
        val passedVolume = intent.getDoubleExtra("TOTAL_VOLUME_LITERS", -1.0) // Use -1 or other sentinel
        if (passedVolume > 0) {
            editTextSystemVolumeLiters.setText(String.format(Locale.US, "%.1f", passedVolume))
        }

        setupListeners()
    }

    private fun initializeUI() {
        editTextSystemVolumeLiters = findViewById(R.id.editTextSystemVolumeLiters)
        textInputLayoutSystemVolume = findViewById(R.id.textInputLayoutSystemVolume)

        editTextInitialPressure = findViewById(R.id.editTextInitialPressure)
        textInputLayoutInitialPressure = findViewById(R.id.textInputLayoutInitialPressure)
        editTextFinalPressure = findViewById(R.id.editTextFinalPressure)
        textInputLayoutFinalPressure = findViewById(R.id.textInputLayoutFinalPressure)
        spinnerPressureUnits = findViewById(R.id.spinnerPressureUnits)
        editTextTimeDuration = findViewById(R.id.editTextTimeDuration)
        textInputLayoutTimeDuration = findViewById(R.id.textInputLayoutTimeDuration)
        spinnerTimeUnits = findViewById(R.id.spinnerTimeUnits)
        buttonCalculateLeakRate = findViewById(R.id.buttonCalculateLeakRate)
        textViewLeakRateResult = findViewById(R.id.textViewLeakRateResult)
    }

    // Removed displaySystemVolume() as it's now an editable field

    private fun setupListeners() {
        buttonCalculateLeakRate.setOnClickListener {
            calculateAndDisplayLeakRate()
        }
    }

    private fun calculateAndDisplayLeakRate() {
        textInputLayoutSystemVolume.error = null // Clear error for system volume
        textInputLayoutInitialPressure.error = null
        textInputLayoutFinalPressure.error = null
        textInputLayoutTimeDuration.error = null

        // --- Get System Volume from EditText ---
        val systemVolumeStr = editTextSystemVolumeLiters.text.toString()
        if (systemVolumeStr.isEmpty()) {
            textInputLayoutSystemVolume.error = getString(R.string.error_empty_field)
            Toast.makeText(this, "System Volume: " + getString(R.string.error_empty_field), Toast.LENGTH_SHORT).show()
            return
        }
        val currentSystemVolumeLiters = systemVolumeStr.toDoubleOrNull()
        if (currentSystemVolumeLiters == null || currentSystemVolumeLiters <= 0) {
            textInputLayoutSystemVolume.error = getString(R.string.error_non_positive_value)
            Toast.makeText(this, "System Volume: " + getString(R.string.error_non_positive_value), Toast.LENGTH_SHORT).show()
            return
        }
        // --- End Get System Volume ---

        val p1Str = editTextInitialPressure.text.toString()
        val p2Str = editTextFinalPressure.text.toString()
        val timeDurationStr = editTextTimeDuration.text.toString()

        if (p1Str.isEmpty()) { textInputLayoutInitialPressure.error = getString(R.string.error_empty_field); return }
        if (p2Str.isEmpty()) { textInputLayoutFinalPressure.error = getString(R.string.error_empty_field); return }
        if (timeDurationStr.isEmpty()) { textInputLayoutTimeDuration.error = getString(R.string.error_empty_field); return }

        val p1 = p1Str.toDoubleOrNull()
        val p2 = p2Str.toDoubleOrNull()
        val timeDuration = timeDurationStr.toDoubleOrNull()

        if (p1 == null) { textInputLayoutInitialPressure.error = getString(R.string.error_invalid_number); return }
        if (p2 == null) { textInputLayoutFinalPressure.error = getString(R.string.error_invalid_number); return }
        if (timeDuration == null || timeDuration <= 0) { textInputLayoutTimeDuration.error = getString(R.string.error_non_positive_value); return }

        if (p1 == p2) {
            val errorMsg = "Initial and final pressures cannot be the same for a leak test."
            textInputLayoutInitialPressure.error = errorMsg
            textInputLayoutFinalPressure.error = errorMsg
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            return
        }

        val deltaPUnconverted = abs(p1 - p2)
        val selectedPressureUnit = spinnerPressureUnits.selectedItem.toString()
        val deltaPInPa = convertPressureToPa(deltaPUnconverted, selectedPressureUnit)
        val selectedTimeUnit = spinnerTimeUnits.selectedItem.toString()
        val timeDurationInMinutes = convertTimeToMinutes(timeDuration, selectedTimeUnit)

        if (timeDurationInMinutes == 0.0) {
            Toast.makeText(this, "Time duration cannot result in zero minutes.", Toast.LENGTH_LONG).show()
            return
        }

        val leakRateSlpm = (currentSystemVolumeLiters * deltaPInPa) / (STANDARD_ATMOSPHERIC_PRESSURE_PA * timeDurationInMinutes)
        textViewLeakRateResult.text = String.format(Locale.US, "%.1f SLPM", leakRateSlpm)
    }

    private fun convertPressureToPa(value: Double, unit: String): Double {
        return when (unit) {
            "kPa" -> value * PA_PER_KPA
            "bar" -> value * PA_PER_BAR
            "psi" -> value * PA_PER_PSI
            "Pa" -> value
            else -> value
        }
    }

    private fun convertTimeToMinutes(value: Double, unit: String): Double {
        return when (unit) {
            "minutes" -> value
            "seconds" -> value / 60.0
            "hours" -> value * 60.0
            else -> value
        }
    }
}