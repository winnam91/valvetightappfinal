package com.example.valvetight

import android.content.SharedPreferences // <<< IMPORT
import android.os.Bundle
import android.widget.Button
// import android.widget.Spinner // No longer directly using its selection for units
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs

class LeakRateActivity : AppCompatActivity() {

    // UI Elements
    private lateinit var editTextSystemVolumeLiters: TextInputEditText
    private lateinit var textInputLayoutSystemVolume: TextInputLayout
    private lateinit var editTextPressureChangeRate: TextInputEditText
    private lateinit var textInputLayoutPressureChangeRate: TextInputLayout
    // private lateinit var spinnerPressureChangeRateUnits: Spinner // Removed
    private lateinit var editTextBleedDiameter: TextInputEditText
    private lateinit var textInputLayoutBleedDiameter: TextInputLayout
    // private lateinit var spinnerBleedDiameterUnits: Spinner // Removed
    private lateinit var buttonCalculateAnalysis: Button
    private lateinit var textViewLeakRateResult: TextView
    private lateinit var textViewVelocityResult: TextView

    // SharedPreferences
    private lateinit var unitPrefs: SharedPreferences // <<< ADDED

    // Current effective units (will be loaded from prefs)
    private var currentPressureUnitPreference: String = SettingsActivity.DEFAULT_VAL_PRESSURE_UNIT
    private var currentLineDiameterUnitPreference: String = SettingsActivity.DEFAULT_VAL_LINE_SIZE_UNIT


    private companion object { // Constants
        private const val PA_PER_KPA = 1000.0; private const val PA_PER_BAR = 100000.0
        private const val PA_PER_PSI = 6894.757; private const val PA_PER_MBAR = 100.0
        private const val STANDARD_ATMOSPHERIC_PRESSURE_PA = 101325.0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leak_rate)

        unitPrefs = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE) // <<< INITIALIZE SharedPreferences

        initializeUI()
        loadUnitPreferencesAndUpdateUI() // <<< LOAD PREFS

        val passedVolume = intent.getDoubleExtra("TOTAL_VOLUME_LITERS", -1.0)
        if (passedVolume > 0) {
            editTextSystemVolumeLiters.setText(String.format(Locale.US, "%.1f", passedVolume))
        }
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadUnitPreferencesAndUpdateUI() // <<< RE-LOAD PREFS when activity resumes
    }

    private fun loadUnitPreferencesAndUpdateUI() {
        currentPressureUnitPreference = unitPrefs.getString(SettingsActivity.KEY_PRESSURE_UNIT, SettingsActivity.DEFAULT_VAL_PRESSURE_UNIT) ?: SettingsActivity.DEFAULT_VAL_PRESSURE_UNIT
        currentLineDiameterUnitPreference = unitPrefs.getString(SettingsActivity.KEY_LINE_SIZE_UNIT, SettingsActivity.DEFAULT_VAL_LINE_SIZE_UNIT) ?: SettingsActivity.DEFAULT_VAL_LINE_SIZE_UNIT

        // Update suffix texts
        textInputLayoutSystemVolume.suffixText = "L" // System volume is always input in Liters
        // For Pressure Change Rate, suffix is more complex as it's "PressureUnit/time"
        // We'll assume "/min" for now based on the global pressure unit.
        // If you need "/sec" etc., the global preference needs to be more specific (e.g. store "bar/min" directly)
        // or add another preference for the time base of the rate.
        textInputLayoutPressureChangeRate.suffixText = "$currentPressureUnitPreference/min"
        textInputLayoutBleedDiameter.suffixText = currentLineDiameterUnitPreference
    }

    private fun initializeUI() {
        // ... (findViewById calls remain the same as your last full LeakRateActivity.kt,
        // but REMOVE findViewById for the spinners that were deleted from the layout)
        editTextSystemVolumeLiters = findViewById(R.id.editTextSystemVolumeLiters)
        textInputLayoutSystemVolume = findViewById(R.id.textInputLayoutSystemVolume)
        editTextPressureChangeRate = findViewById(R.id.editTextPressureChangeRate)
        textInputLayoutPressureChangeRate = findViewById(R.id.textInputLayoutPressureChangeRate)
        // spinnerPressureChangeRateUnits = findViewById(R.id.spinnerPressureChangeRateUnits) // REMOVED
        editTextBleedDiameter = findViewById(R.id.editTextBleedDiameter)
        textInputLayoutBleedDiameter = findViewById(R.id.textInputLayoutBleedDiameter)
        // spinnerBleedDiameterUnits = findViewById(R.id.spinnerBleedDiameterUnits) // REMOVED
        buttonCalculateAnalysis = findViewById(R.id.buttonCalculateAnalysis)
        textViewLeakRateResult = findViewById(R.id.textViewLeakRateResult)
        textViewVelocityResult = findViewById(R.id.textViewVelocityResult)

        // Suffix text setting moved to loadUnitPreferencesAndUpdateUI()
    }

    private fun setupListeners() { /* ... as before ... */
        buttonCalculateAnalysis.setOnClickListener { performLeakAnalysis() }
    }

    private fun performLeakAnalysis() {
        textInputLayoutSystemVolume.error = null; textInputLayoutPressureChangeRate.error = null; textInputLayoutBleedDiameter.error = null

        val systemVolumeStr = editTextSystemVolumeLiters.text.toString()
        if (systemVolumeStr.isEmpty()) { textInputLayoutSystemVolume.error = getString(R.string.error_empty_field); return }
        val systemVolumeLiters = systemVolumeStr.toDoubleOrNull()
        if (systemVolumeLiters == null || systemVolumeLiters <= 0) { textInputLayoutSystemVolume.error = getString(R.string.error_non_positive_value); return }
        val systemVolumeM3 = systemVolumeLiters * 0.001

        val pressureChangeRateStr = editTextPressureChangeRate.text.toString()
        if (pressureChangeRateStr.isEmpty()) { textInputLayoutPressureChangeRate.error = getString(R.string.error_empty_field); return }
        val pressureChangeRateValue = pressureChangeRateStr.toDoubleOrNull()
        if (pressureChangeRateValue == null) { textInputLayoutPressureChangeRate.error = getString(R.string.error_invalid_number); return }

        // <<< USE LOADED PREFERENCES FOR UNITS >>>
        // Construct the rate unit string based on global pressure preference (assuming "/min")
        val actualPressureRateUnit = "$currentPressureUnitPreference/min"
        // If your pressure_change_rate_units_array contains this exact string, it will work directly with conversion functions.
        // If not, the conversion functions need to parse currentPressureUnitPreference and assume "/min".
        // For simplicity, let's assume conversion functions will handle base pressure unit + fixed "/min"

        val pressureChangePaPerSec = convertPressureChangeRateToPaPerSec(pressureChangeRateValue, actualPressureRateUnit)
        val pressureChangeBarPerMin = convertPressureChangeRateToBarPerMin(pressureChangeRateValue, actualPressureRateUnit)


        val bleedDiameterStr = editTextBleedDiameter.text.toString()
        if (bleedDiameterStr.isEmpty()) { textInputLayoutBleedDiameter.error = getString(R.string.error_empty_field); return }
        val bleedDiameterValue = parseDimensionInput(bleedDiameterStr)
        if (bleedDiameterValue == null || bleedDiameterValue <= 0) { textInputLayoutBleedDiameter.error = getString(R.string.error_non_positive_value); return }

        // <<< USE LOADED PREFERENCE FOR BLEED DIAMETER UNIT >>>
        val bleedDiameterUnit = currentLineDiameterUnitPreference
        val bleedDiameterMeters = convertLengthToMeters(bleedDiameterValue, bleedDiameterUnit)

        // ... (rest of calculations and display as before) ...
        val dPdtPaPerMin = pressureChangePaPerSec * 60.0; val leakRateSlpm = (systemVolumeLiters * abs(dPdtPaPerMin)) / STANDARD_ATMOSPHERIC_PRESSURE_PA; textViewLeakRateResult.text = String.format(Locale.US, "%.1f SLPM", leakRateSlpm)
        if (bleedDiameterMeters == 0.0) { textViewVelocityResult.text = getString(R.string.error_bleed_diameter_zero); return }; val bleedBoreAreaM2 = 0.25 * PI * bleedDiameterMeters * bleedDiameterMeters; if (bleedBoreAreaM2 == 0.0) { textViewVelocityResult.text = getString(R.string.error_bleed_area_zero); return }
        val standardizedLeakRateM3BarPerMin = systemVolumeM3 * abs(pressureChangeBarPerMin); val standardizedLeakRateM3BarPerSec = standardizedLeakRateM3BarPerMin / 60.0; val velocityMetersPerSec = standardizedLeakRateM3BarPerSec / bleedBoreAreaM2; textViewVelocityResult.text = String.format(Locale.US, "%.2f m/s", velocityMetersPerSec)
    }

    // --- Helper Functions ---
    // Modify conversion functions to handle base pressure unit + assumed "/min" or parse combined unit
    private fun convertPressureChangeRateToPaPerSec(value: Double, combinedUnit: String): Double {
        // Example: "bar/min" -> split to "bar" and "min"
        val parts = combinedUnit.split('/')
        val pressureUnit = parts.getOrElse(0) { "" }
        val timeComponent = parts.getOrElse(1) { "min" } // Default to min if not specified

        var valueInPa = when (pressureUnit) {
            "kPa" -> value * PA_PER_KPA
            "bar" -> value * PA_PER_BAR
            "psi" -> value * PA_PER_PSI
            "mbar"-> value * PA_PER_MBAR
            "Pa"  -> value
            else  -> 0.0
        }
        // Now adjust for time component to get Pa/sec
        if (timeComponent == "min") {
            valueInPa /= 60.0
        } // if it's "sec", it's already per second. "/hour" would need valueInPa /= 3600.0

        return valueInPa
    }

    private fun convertPressureChangeRateToBarPerMin(value: Double, combinedUnit: String): Double {
        val paPerSec = convertPressureChangeRateToPaPerSec(value, combinedUnit)
        return (paPerSec * 60.0) / PA_PER_BAR // Pa/sec to Pa/min, then to bar/min
    }
    // ... (convertLengthToMeters, parseSimpleFraction, parseDimensionInput - NO CHANGES)
    private fun convertLengthToMeters(value: Double, unit: String): Double { return when (unit) { "mm" -> value / 1000.0; "cm" -> value / 100.0; "m" -> value; "inches" -> value * 0.0254; else -> value } }
    private fun parseSimpleFraction(fractionStr: String): Double? { val p = fractionStr.trim().split('/'); if (p.size == 2) { val n = p[0].toDoubleOrNull(); val d = p[1].toDoubleOrNull(); if (n != null && d != null && d != 0.0) return n / d }; return null }
    private fun parseDimensionInput(input: String): Double? { val s = input.trim(); if (s.isEmpty()) return null; s.toDoubleOrNull()?.let { return it }; if (s.contains('-') && s.contains('/')) { val p = s.split('-', limit = 2); if (p.size == 2) { val w = p[0].toDoubleOrNull(); val f = parseSimpleFraction(p[1]); if (w != null && f != null) return w + f } }; if (s.contains(' ') && s.contains('/')) { val i = s.lastIndexOf(' '); if (i > 0 && s.indexOf('/') > i) { val ws = s.substring(0, i); val fs = s.substring(i + 1); val w = ws.toDoubleOrNull(); val f = parseSimpleFraction(fs); if (w != null && f != null) return w + f } }; return parseSimpleFraction(s) }
}