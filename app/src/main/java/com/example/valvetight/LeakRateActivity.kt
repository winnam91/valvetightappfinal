package com.example.valvetight

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button // Ensure Button is imported
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
    private lateinit var editTextBleedDiameter: TextInputEditText
    private lateinit var textInputLayoutBleedDiameter: TextInputLayout
    private lateinit var buttonCalculateAnalysis: Button
    private lateinit var textViewLeakRateResult: TextView
    private lateinit var textViewVelocityResult: TextView
    private lateinit var buttonLeakAnalysisDone: Button // <<< NEW DONE BUTTON

    private lateinit var unitPrefs: SharedPreferences
    private var currentPressureUnitPreference: String = SettingsActivity.DEFAULT_VAL_PRESSURE_UNIT
    private var currentLineDiameterUnitPreference: String = SettingsActivity.DEFAULT_VAL_LINE_SIZE_UNIT

    private companion object {
        private const val PA_PER_KPA = 1000.0; private const val PA_PER_BAR = 100000.0
        private const val PA_PER_PSI = 6894.757; private const val PA_PER_MBAR = 100.0
        private const val STANDARD_ATMOSPHERIC_PRESSURE_PA = 101325.0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setFinishOnTouchOutside(true) // <<< ALLOW DISMISSING BY TAPPING OUTSIDE
        setContentView(R.layout.activity_leak_rate)

        unitPrefs = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE)
        initializeUI()
        // loadUnitPreferencesAndUpdateUI() is called in onResume

        val passedVolume = intent.getDoubleExtra("TOTAL_VOLUME_LITERS", -1.0)
        if (passedVolume > 0) {
            editTextSystemVolumeLiters.setText(String.format(Locale.US, "%.1f", passedVolume))
        }
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadUnitPreferencesAndUpdateUI()
    }

    private fun loadUnitPreferencesAndUpdateUI() {
        currentPressureUnitPreference = unitPrefs.getString(SettingsActivity.KEY_PRESSURE_UNIT, SettingsActivity.DEFAULT_VAL_PRESSURE_UNIT) ?: SettingsActivity.DEFAULT_VAL_PRESSURE_UNIT
        currentLineDiameterUnitPreference = unitPrefs.getString(SettingsActivity.KEY_LINE_SIZE_UNIT, SettingsActivity.DEFAULT_VAL_LINE_SIZE_UNIT) ?: SettingsActivity.DEFAULT_VAL_LINE_SIZE_UNIT

        textInputLayoutSystemVolume.suffixText = "L"
        textInputLayoutPressureChangeRate.suffixText = "$currentPressureUnitPreference/min" // Assuming rate is per minute
        textInputLayoutBleedDiameter.suffixText = currentLineDiameterUnitPreference
    }

    private fun initializeUI() {
        editTextSystemVolumeLiters = findViewById(R.id.editTextSystemVolumeLiters)
        textInputLayoutSystemVolume = findViewById(R.id.textInputLayoutSystemVolume)
        editTextPressureChangeRate = findViewById(R.id.editTextPressureChangeRate)
        textInputLayoutPressureChangeRate = findViewById(R.id.textInputLayoutPressureChangeRate)
        editTextBleedDiameter = findViewById(R.id.editTextBleedDiameter)
        textInputLayoutBleedDiameter = findViewById(R.id.textInputLayoutBleedDiameter)
        buttonCalculateAnalysis = findViewById(R.id.buttonCalculateAnalysis)
        textViewLeakRateResult = findViewById(R.id.textViewLeakRateResult)
        textViewVelocityResult = findViewById(R.id.textViewVelocityResult)
        buttonLeakAnalysisDone = findViewById(R.id.buttonLeakAnalysisDone) // <<< INITIALIZE DONE BUTTON
    }

    private fun setupListeners() {
        buttonCalculateAnalysis.setOnClickListener {
            performLeakAnalysis()
        }
        buttonLeakAnalysisDone.setOnClickListener { // <<< LISTENER FOR DONE BUTTON
            finish() // Simply close this activity
        }
    }

    private fun performLeakAnalysis() {
        // ... (performLeakAnalysis logic remains the same as the last full version)
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
        val actualPressureRateUnit = "$currentPressureUnitPreference/min"
        val pressureChangePaPerSec = convertPressureChangeRateToPaPerSec(pressureChangeRateValue, actualPressureRateUnit)
        val pressureChangeBarPerMin = convertPressureChangeRateToBarPerMin(pressureChangeRateValue, actualPressureRateUnit)
        val bleedDiameterStr = editTextBleedDiameter.text.toString()
        if (bleedDiameterStr.isEmpty()) { textInputLayoutBleedDiameter.error = getString(R.string.error_empty_field); return }
        val bleedDiameterValue = parseDimensionInput(bleedDiameterStr)
        if (bleedDiameterValue == null || bleedDiameterValue <= 0) { textInputLayoutBleedDiameter.error = getString(R.string.error_non_positive_value); return }
        val bleedDiameterUnit = currentLineDiameterUnitPreference
        val bleedDiameterMeters = convertLengthToMeters(bleedDiameterValue, bleedDiameterUnit)
        val dPdtPaPerMin = pressureChangePaPerSec * 60.0; val leakRateSlpm = (systemVolumeLiters * abs(dPdtPaPerMin)) / STANDARD_ATMOSPHERIC_PRESSURE_PA; textViewLeakRateResult.text = String.format(Locale.US, "%.1f SLPM", leakRateSlpm)
        if (bleedDiameterMeters == 0.0) { textViewVelocityResult.text = getString(R.string.error_bleed_diameter_zero); return }; val bleedBoreAreaM2 = 0.25 * PI * bleedDiameterMeters * bleedDiameterMeters; if (bleedBoreAreaM2 == 0.0) { textViewVelocityResult.text = getString(R.string.error_bleed_area_zero); return }
        val standardizedLeakRateM3BarPerMin = systemVolumeM3 * abs(pressureChangeBarPerMin); val standardizedLeakRateM3BarPerSec = standardizedLeakRateM3BarPerMin / 60.0; val velocityMetersPerSec = standardizedLeakRateM3BarPerSec / bleedBoreAreaM2; textViewVelocityResult.text = String.format(Locale.US, "%.2f m/s", velocityMetersPerSec)
    }

    // --- Helper Functions (convertPressureChangeRateToPaPerSec, convertPressureChangeRateToBarPerMin, convertLengthToMeters, parse helpers) ---
    // NO CHANGES NEEDED IN THESE HELPERS FROM THE LAST FULL VERSION
    private fun convertPressureChangeRateToPaPerSec(value: Double, combinedUnit: String): Double { val parts = combinedUnit.split('/'); val pressureUnit = parts.getOrElse(0) { "" }; val timeComponent = parts.getOrElse(1) { "min" }; var valueInPa = when (pressureUnit) { "kPa" -> value * PA_PER_KPA; "bar" -> value * PA_PER_BAR; "psi" -> value * PA_PER_PSI; "mbar"-> value * PA_PER_MBAR; "Pa"  -> value; else  -> 0.0 }; if (timeComponent == "min") { valueInPa /= 60.0 }; return valueInPa }
    private fun convertPressureChangeRateToBarPerMin(value: Double, combinedUnit: String): Double { val paPerSec = convertPressureChangeRateToPaPerSec(value, combinedUnit); return (paPerSec * 60.0) / PA_PER_BAR }
    private fun convertLengthToMeters(value: Double, unit: String): Double { return when (unit) { "mm" -> value / 1000.0; "cm" -> value / 100.0; "m" -> value; "inches" -> value * 0.0254; else -> value } }
    private fun parseSimpleFraction(fractionStr: String): Double? { val p = fractionStr.trim().split('/'); if (p.size == 2) { val n = p[0].toDoubleOrNull(); val d = p[1].toDoubleOrNull(); if (n != null && d != null && d != 0.0) return n / d }; return null }
    private fun parseDimensionInput(input: String): Double? { val s = input.trim(); if (s.isEmpty()) return null; s.toDoubleOrNull()?.let { return it }; if (s.contains('-') && s.contains('/')) { val p = s.split('-', limit = 2); if (p.size == 2) { val w = p[0].toDoubleOrNull(); val f = parseSimpleFraction(p[1]); if (w != null && f != null) return w + f } }; if (s.contains(' ') && s.contains('/')) { val i = s.lastIndexOf(' '); if (i > 0 && s.indexOf('/') > i) { val ws = s.substring(0, i); val fs = s.substring(i + 1); val w = ws.toDoubleOrNull(); val f = parseSimpleFraction(fs); if (w != null && f != null) return w + f } }; return parseSimpleFraction(s) }
}