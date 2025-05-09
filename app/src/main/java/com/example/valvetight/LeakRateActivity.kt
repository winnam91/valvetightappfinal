package com.example.valvetight

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.Locale
import kotlin.math.abs // For absolute pressure difference

class LeakRateActivity : AppCompatActivity() {

    private lateinit var textViewSystemVolumeValue: TextView
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

    private var systemVolumeLiters: Double = 0.0

    // Constants for pressure conversion to Pascals (Pa)
    private val PA_PER_KPA = 1000.0
    private val PA_PER_BAR = 100000.0
    private val PA_PER_PSI = 6894.757
    private val STANDARD_ATMOSPHERIC_PRESSURE_PA = 101325.0 // Standard atm pressure in Pascals

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leak_rate)

        // Retrieve the passed volume
        systemVolumeLiters = intent.getDoubleExtra("TOTAL_VOLUME_LITERS", 0.0)

        initializeUI()
        displaySystemVolume()
        setupListeners()

        // Set activity title (optional, as it's in the layout too)
        // title = getString(R.string.leak_rate_activity_title) // If you had an ActionBar
    }

    private fun initializeUI() {
        textViewSystemVolumeValue = findViewById(R.id.textViewSystemVolumeValue)
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

    private fun displaySystemVolume() {
        val volumeM3 = systemVolumeLiters * 0.001
        textViewSystemVolumeValue.text = String.format(
            Locale.US,
            "%.1f L (%.4f m³)",
            systemVolumeLiters,
            volumeM3
        )
    }

    private fun setupListeners() {
        buttonCalculateLeakRate.setOnClickListener {
            calculateAndDisplayLeakRate()
        }
    }

    private fun calculateAndDisplayLeakRate() {
        // Clear previous errors
        textInputLayoutInitialPressure.error = null
        textInputLayoutFinalPressure.error = null
        textInputLayoutTimeDuration.error = null

        // Get inputs
        val p1Str = editTextInitialPressure.text.toString()
        val p2Str = editTextFinalPressure.text.toString()
        val timeDurationStr = editTextTimeDuration.text.toString()

        // Validate inputs
        if (p1Str.isEmpty()) { textInputLayoutInitialPressure.error = getString(R.string.error_empty_field); return }
        if (p2Str.isEmpty()) { textInputLayoutFinalPressure.error = getString(R.string.error_empty_field); return }
        if (timeDurationStr.isEmpty()) { textInputLayoutTimeDuration.error = getString(R.string.error_empty_field); return }

        val p1 = p1Str.toDoubleOrNull()
        val p2 = p2Str.toDoubleOrNull()
        val timeDuration = timeDurationStr.toDoubleOrNull()

        if (p1 == null) { textInputLayoutInitialPressure.error = getString(R.string.error_invalid_number); return }
        if (p2 == null) { textInputLayoutFinalPressure.error = getString(R.string.error_invalid_number); return }
        if (timeDuration == null || timeDuration <= 0) { textInputLayoutTimeDuration.error = getString(R.string.error_non_positive_value); return }

        // P1 should ideally be greater than P2 for a leak out scenario.
        // If P2 > P1, it might be a leak in, or just mis-entry. We'll take absolute difference for ΔP.
        // Or, you can enforce P1 > P2. For simplicity, let's use absolute diff and assume it's a pressure drop.
        if (p1 == p2) {
            val errorMsg = "Initial and final pressures cannot be the same for a leak test."
            textInputLayoutInitialPressure.error = errorMsg
            textInputLayoutFinalPressure.error = errorMsg
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            return
        }

        val deltaPUnconverted = abs(p1 - p2) // Absolute difference in pressure

        // Convert pressure to Pascals
        val selectedPressureUnit = spinnerPressureUnits.selectedItem.toString()
        val deltaPInPa = convertPressureToPa(deltaPUnconverted, selectedPressureUnit)

        // Convert time to minutes
        val selectedTimeUnit = spinnerTimeUnits.selectedItem.toString()
        val timeDurationInMinutes = convertTimeToMinutes(timeDuration, selectedTimeUnit)

        if (timeDurationInMinutes == 0.0) { // Avoid division by zero
            Toast.makeText(this, "Time duration cannot result in zero minutes.", Toast.LENGTH_LONG).show()
            return
        }

        // Calculate Leak Rate: Q_std = (V_system * ΔP) / (P_atm * Δt)
        // V_system in Liters
        // ΔP in Pa
        // P_atm in Pa (STANDARD_ATMOSPHERIC_PRESSURE_PA)
        // Δt in minutes
        // Result will be in SLPM (Standard Liters Per Minute)

        val leakRateSlpm = (systemVolumeLiters * deltaPInPa) / (STANDARD_ATMOSPHERIC_PRESSURE_PA * timeDurationInMinutes)

        textViewLeakRateResult.text = String.format(Locale.US, "%.1f SLPM", leakRateSlpm)
    }

    private fun convertPressureToPa(value: Double, unit: String): Double {
        return when (unit) {
            "kPa" -> value * PA_PER_KPA
            "bar" -> value * PA_PER_BAR
            "psi" -> value * PA_PER_PSI
            "Pa" -> value
            else -> value // Should not happen
        }
    }

    private fun convertTimeToMinutes(value: Double, unit: String): Double {
        return when (unit) {
            "minutes" -> value
            "seconds" -> value / 60.0
            "hours" -> value * 60.0
            else -> value // Should not happen
        }
    }
}