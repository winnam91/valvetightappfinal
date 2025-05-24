package com.example.valvetight

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit

class SettingsActivity : AppCompatActivity() {

    private lateinit var spinnerPrefPressureUnit: Spinner
    private lateinit var spinnerPrefDimensionUnit: Spinner
    private lateinit var spinnerPrefLineSizeUnit: Spinner
    private lateinit var buttonSettingsDone: Button

    private lateinit var sharedPreferences: SharedPreferences

    // Define keys for SharedPreferences and default values
    companion object {
        const val PREFS_NAME = "UnitPrefsValvetightApp" // Made name more unique
        const val KEY_PRESSURE_UNIT = "pref_pressure_unit"
        const val KEY_DIMENSION_UNIT = "pref_dimension_unit"
        const val KEY_LINE_SIZE_UNIT = "pref_line_size_unit"

        // Default units if nothing is saved yet (must match items in your string arrays)
        const val DEFAULT_VAL_PRESSURE_UNIT = "bar"    // From pressure_units_global_array
        const val DEFAULT_VAL_DIMENSION_UNIT = "meters"   // From dimension_units_array
        const val DEFAULT_VAL_LINE_SIZE_UNIT = "inches"  // From bleed_diameter_units_array
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Allow dismissing by tapping outside (since it's a dialog-themed activity)
        this.setFinishOnTouchOutside(true)
        setContentView(R.layout.activity_settings)

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        initializeUI()
        loadPreferencesAndSetupSpinners() // Combined loading and listener setup
    }

    private fun initializeUI() {
        spinnerPrefPressureUnit = findViewById(R.id.spinnerPrefPressureUnit)
        spinnerPrefDimensionUnit = findViewById(R.id.spinnerPrefDimensionUnit)
        spinnerPrefLineSizeUnit = findViewById(R.id.spinnerPrefLineSizeUnit)
        buttonSettingsDone = findViewById(R.id.buttonSettingsDone)

        buttonSettingsDone.setOnClickListener {
            finish() // Close the activity
        }
    }

    private fun loadPreferencesAndSetupSpinners() {
        // Pressure Unit Preference
        val pressureUnits = resources.getStringArray(R.array.pressure_units_global_array)
        val pressureAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, pressureUnits)
        pressureAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPrefPressureUnit.adapter = pressureAdapter
        val savedPressureUnit = sharedPreferences.getString(KEY_PRESSURE_UNIT, DEFAULT_VAL_PRESSURE_UNIT)
        spinnerPrefPressureUnit.setSelection(pressureUnits.indexOf(savedPressureUnit).coerceAtLeast(0)) // Set selection

        spinnerPrefPressureUnit.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                savePreference(KEY_PRESSURE_UNIT, pressureUnits[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Dimension Unit Preference
        val dimensionUnits = resources.getStringArray(R.array.dimension_units_array)
        val dimensionAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, dimensionUnits)
        dimensionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPrefDimensionUnit.adapter = dimensionAdapter
        val savedDimensionUnit = sharedPreferences.getString(KEY_DIMENSION_UNIT, DEFAULT_VAL_DIMENSION_UNIT)
        spinnerPrefDimensionUnit.setSelection(dimensionUnits.indexOf(savedDimensionUnit).coerceAtLeast(0))

        spinnerPrefDimensionUnit.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                savePreference(KEY_DIMENSION_UNIT, dimensionUnits[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Line Size (Bleed Diameter) Unit Preference
        val lineSizeUnits = resources.getStringArray(R.array.bleed_diameter_units_array)
        val lineSizeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, lineSizeUnits)
        lineSizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPrefLineSizeUnit.adapter = lineSizeAdapter
        val savedLineSizeUnit = sharedPreferences.getString(KEY_LINE_SIZE_UNIT, DEFAULT_VAL_LINE_SIZE_UNIT)
        spinnerPrefLineSizeUnit.setSelection(lineSizeUnits.indexOf(savedLineSizeUnit).coerceAtLeast(0))

        spinnerPrefLineSizeUnit.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                savePreference(KEY_LINE_SIZE_UNIT, lineSizeUnits[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun savePreference(key: String, value: String) {
        sharedPreferences.edit { putString(key, value) }
        // Toast.makeText(this, getString(R.string.prefs_saved_toast) + " ($value)", Toast.LENGTH_SHORT).show()
    }
}