package com.example.valvetight

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.Locale
import kotlin.math.PI

class MainActivity : AppCompatActivity() {

    // --- UI Elements ---
    private lateinit var spinnerComponentType: Spinner
    private lateinit var textInputLayoutComponentName: TextInputLayout
    private lateinit var editTextComponentName: TextInputEditText
    private lateinit var layoutDimensionalInputs: LinearLayout
    private lateinit var editTextDiameter: TextInputEditText
    private lateinit var textInputLayoutDiameter: TextInputLayout
    private lateinit var editTextLength: TextInputEditText
    private lateinit var textInputLayoutLength: TextInputLayout
    private lateinit var spinnerUnitsDimensions: Spinner
    private lateinit var textInputLayoutQuantity: TextInputLayout
    private lateinit var editTextQuantity: TextInputEditText
    private lateinit var buttonAddComponent: Button
    private lateinit var textViewAddedComponentsList: TextView
    private lateinit var textViewTotalVolume: TextView
    private lateinit var buttonResetAll: Button

    // --- Data ---
    private val addedComponentsDescriptions = ArrayList<String>()
    private val addedVolumesInLiters = ArrayList<Double>()
    private var totalVolumeInLiters = 0.0 // Initializer is NOT redundant here as it's modified by sum()

    // --- Constants for Component Types (using const val) ---
    private companion object { // const val needs to be in companion object or top-level
        private const val TYPE_UNKNOWN_VOLUME = "Unknown Volume"
        private const val TYPE_HOSE = "Hose"
        private const val TYPE_SPOOL_PIECE = "Spool Piece"
        private const val TYPE_T_PIECE = "T-piece"
        private const val TYPE_ELBOW = "Elbow"
        private const val TYPE_KNOCK_OUT_VESSEL = "Knock-out Vessel"
    }

    // --- Pre-defined Volumes ---
    private val predefinedVolumes = mapOf(
        TYPE_HOSE to 5.71,
        TYPE_SPOOL_PIECE to 1.0,
        TYPE_T_PIECE to 1.0,
        TYPE_ELBOW to 1.0,
        TYPE_KNOCK_OUT_VESSEL to 300.0
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initializeUI()
        setupListeners()
        updateUIForSelectedComponentType()
    }

    private fun initializeUI() {
        spinnerComponentType = findViewById(R.id.spinnerComponentType)
        textInputLayoutComponentName = findViewById(R.id.textInputLayoutComponentName)
        editTextComponentName = findViewById(R.id.editTextComponentName)
        layoutDimensionalInputs = findViewById(R.id.layoutDimensionalInputs)
        editTextDiameter = findViewById(R.id.editTextDiameter)
        textInputLayoutDiameter = findViewById(R.id.textInputLayoutDiameter)
        editTextLength = findViewById(R.id.editTextLength)
        textInputLayoutLength = findViewById(R.id.textInputLayoutLength)
        spinnerUnitsDimensions = findViewById(R.id.spinnerUnitsDimensions)
        textInputLayoutQuantity = findViewById(R.id.textInputLayoutQuantity)
        editTextQuantity = findViewById(R.id.editTextQuantity)
        buttonAddComponent = findViewById(R.id.buttonAddComponent)
        textViewAddedComponentsList = findViewById(R.id.textViewAddedComponentsList)
        textViewTotalVolume = findViewById(R.id.textViewTotalVolume)
        buttonResetAll = findViewById(R.id.buttonResetAll)
        updateAddedComponentsListDisplay()
        updateTotalVolumeDisplay()
    }

    private fun setupListeners() {
        spinnerComponentType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateUIForSelectedComponentType()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        buttonAddComponent.setOnClickListener { handleAddComponent() }
        buttonResetAll.setOnClickListener { resetAll() }
    }

    private fun updateUIForSelectedComponentType() {
        val selectedType = spinnerComponentType.selectedItem.toString()
        textInputLayoutComponentName.visibility = View.GONE
        layoutDimensionalInputs.visibility = View.GONE
        textInputLayoutQuantity.visibility = View.VISIBLE

        val quantityParams = textInputLayoutQuantity.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        val buttonParams = buttonAddComponent.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams

        if (selectedType == TYPE_UNKNOWN_VOLUME) {
            textInputLayoutComponentName.visibility = View.VISIBLE
            layoutDimensionalInputs.visibility = View.VISIBLE
            quantityParams.topToBottom = R.id.layoutDimensionalInputs
        } else {
            quantityParams.topToBottom = R.id.spinnerComponentType
        }
        textInputLayoutQuantity.layoutParams = quantityParams
        buttonParams.topToBottom = R.id.textInputLayoutQuantity
        buttonAddComponent.layoutParams = buttonParams

        textInputLayoutComponentName.error = null
        textInputLayoutDiameter.error = null
        textInputLayoutLength.error = null
        textInputLayoutQuantity.error = null
        if (selectedType != TYPE_UNKNOWN_VOLUME) {
            editTextComponentName.text?.clear()
            editTextDiameter.text?.clear()
            editTextLength.text?.clear()
        }
    }

    private fun handleAddComponent() {
        val selectedType = spinnerComponentType.selectedItem.toString()
        // Initializers removed for these as they are assigned in all paths before use
        val singleItemVolumeLiters: Double
        val descriptionPart: String
        var componentDisplayName = selectedType

        val quantityStr = editTextQuantity.text.toString()
        if (quantityStr.isEmpty()) {
            textInputLayoutQuantity.error = getString(R.string.error_empty_field); Toast.makeText(this, "Quantity " + getString(R.string.error_empty_field), Toast.LENGTH_SHORT).show(); return
        }
        val quantity = quantityStr.toIntOrNull()
        if (quantity == null || quantity <= 0) {
            textInputLayoutQuantity.error = getString(R.string.error_invalid_quantity); Toast.makeText(this, getString(R.string.error_invalid_quantity), Toast.LENGTH_SHORT).show(); return
        }
        textInputLayoutQuantity.error = null
        textInputLayoutComponentName.error = null
        textInputLayoutDiameter.error = null
        textInputLayoutLength.error = null

        try {
            if (selectedType == TYPE_UNKNOWN_VOLUME) {
                componentDisplayName = editTextComponentName.text.toString().trim()
                if (componentDisplayName.isEmpty()) {
                    textInputLayoutComponentName.error = getString(R.string.error_name_empty); Toast.makeText(this, getString(R.string.error_name_empty), Toast.LENGTH_SHORT).show(); return
                }
                val diameterStr = editTextDiameter.text.toString()
                val lengthStr = editTextLength.text.toString()
                if (diameterStr.isEmpty()) { textInputLayoutDiameter.error = getString(R.string.error_empty_field); Toast.makeText(this, "Diameter " + getString(R.string.error_empty_field), Toast.LENGTH_SHORT).show(); return }
                if (lengthStr.isEmpty()) { textInputLayoutLength.error = getString(R.string.error_empty_field); Toast.makeText(this, "Length " + getString(R.string.error_empty_field), Toast.LENGTH_SHORT).show(); return }
                val diameter = parseDimensionInput(diameterStr)
                val length = parseDimensionInput(lengthStr)
                if (diameter == null) { textInputLayoutDiameter.error = getString(R.string.error_invalid_number); Toast.makeText(this, "Diameter " + getString(R.string.error_invalid_number), Toast.LENGTH_SHORT).show(); return }
                if (diameter <= 0) { textInputLayoutDiameter.error = getString(R.string.error_non_positive_value); Toast.makeText(this, "Diameter " + getString(R.string.error_non_positive_value), Toast.LENGTH_SHORT).show(); return }
                if (length == null) { textInputLayoutLength.error = getString(R.string.error_invalid_number); Toast.makeText(this, "Length " + getString(R.string.error_invalid_number), Toast.LENGTH_SHORT).show(); return }
                if (length <= 0) { textInputLayoutLength.error = getString(R.string.error_non_positive_value); Toast.makeText(this, "Length " + getString(R.string.error_non_positive_value), Toast.LENGTH_SHORT).show(); return }
                val dimensionUnit = spinnerUnitsDimensions.selectedItem.toString()
                singleItemVolumeLiters = calculateCylinderVolumeInLiters(convertToMeters(diameter, dimensionUnit), convertToMeters(length, dimensionUnit))
                descriptionPart = "$componentDisplayName (D: $diameterStr $dimensionUnit, L: $lengthStr $dimensionUnit)"
            } else {
                singleItemVolumeLiters = predefinedVolumes[selectedType] ?: 0.0
                descriptionPart = componentDisplayName
            }

            val singleItemVolumeM3 = singleItemVolumeLiters * 0.001
            val totalVolumeForItemsLiters = singleItemVolumeLiters * quantity
            val totalVolumeForItemsM3 = totalVolumeForItemsLiters * 0.001

            val finalDescription = if (quantity > 1) {
                String.format(
                    Locale.US,
                    "%s x %d (%.3f L / %.5f m³ each): %.3f L (%.5f m³)",
                    descriptionPart, quantity, singleItemVolumeLiters, singleItemVolumeM3, totalVolumeForItemsLiters, totalVolumeForItemsM3
                )
            } else {
                String.format(
                    Locale.US,
                    "%s: %.3f L (%.5f m³)",
                    descriptionPart, totalVolumeForItemsLiters, totalVolumeForItemsM3
                )
            }
            addedComponentsDescriptions.add(finalDescription)
            addedVolumesInLiters.add(totalVolumeForItemsLiters)
            recalculateTotalVolume()
            updateAddedComponentsListDisplay()
            updateTotalVolumeDisplay()
            clearInputFields(selectedType)
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun clearInputFields(lastSelectedType: String? = null) {
        editTextComponentName.text?.clear()
        editTextDiameter.text?.clear()
        editTextLength.text?.clear()
        editTextQuantity.setText(getString(R.string.default_quantity))
        if (lastSelectedType == TYPE_UNKNOWN_VOLUME) editTextComponentName.requestFocus()
        else editTextQuantity.requestFocus()
    }

    private fun resetAll() {
        addedComponentsDescriptions.clear()
        addedVolumesInLiters.clear()
        totalVolumeInLiters = 0.0
        updateAddedComponentsListDisplay()
        updateTotalVolumeDisplay()
        spinnerComponentType.setSelection(0)
        clearInputFields()
        updateUIForSelectedComponentType()
        Toast.makeText(this, "All data reset", Toast.LENGTH_SHORT).show()
    }

    private fun recalculateTotalVolume() {
        totalVolumeInLiters = addedVolumesInLiters.sum()
    }

    private fun updateAddedComponentsListDisplay() {
        textViewAddedComponentsList.text = if (addedComponentsDescriptions.isEmpty()) ""
        else addedComponentsDescriptions.joinToString("\n")
    }

    private fun updateTotalVolumeDisplay() {
        val totalVolumeM3 = totalVolumeInLiters * 0.001
        textViewTotalVolume.text = String.format(
            Locale.US,
            "%s%.3f L (%.5f m³)",
            getString(R.string.total_volume_label_prefix), totalVolumeInLiters, totalVolumeM3
        )
    }

    private fun parseSimpleFraction(fractionStr: String): Double? {
        val parts = fractionStr.trim().split('/')
        if (parts.size == 2) {
            val numerator = parts[0].toDoubleOrNull()
            val denominator = parts[1].toDoubleOrNull()
            if (numerator != null && denominator != null && denominator != 0.0) return numerator / denominator
        }
        return null
    }

    private fun parseDimensionInput(input: String): Double? {
        val trimmedInputStr = input.trim()
        if (trimmedInputStr.isEmpty()) return null
        trimmedInputStr.toDoubleOrNull()?.let { return it } // Direct decimal

        // Mixed number with hyphen (e.g., "1-1/2")
        if (trimmedInputStr.contains('-') && trimmedInputStr.contains('/')) {
            val hyphenParts = trimmedInputStr.split('-', limit = 2)
            if (hyphenParts.size == 2) {
                val wholeNum = hyphenParts[0].toDoubleOrNull()
                val fractionVal = parseSimpleFraction(hyphenParts[1])
                if (wholeNum != null && fractionVal != null) return wholeNum + fractionVal
            }
        }

        // Mixed number with space (e.g., "1 1/2")
        if (trimmedInputStr.contains(' ') && trimmedInputStr.contains('/')) {
            val spaceIndex = trimmedInputStr.lastIndexOf(' ')
            if (spaceIndex > 0 && trimmedInputStr.indexOf('/') > spaceIndex) {
                val wholeStrPart = trimmedInputStr.substring(0, spaceIndex)
                val fractionStrPart = trimmedInputStr.substring(spaceIndex + 1)
                val wholeNum = wholeStrPart.toDoubleOrNull()
                val fractionVal = parseSimpleFraction(fractionStrPart)
                if (wholeNum != null && fractionVal != null) return wholeNum + fractionVal
            }
        }
        return parseSimpleFraction(trimmedInputStr) // Simple fraction (e.g., "1/2") or null
    }

    private fun calculateCylinderVolumeInLiters(diameterInMeters: Double, lengthInMeters: Double): Double {
        val radius = diameterInMeters / 2.0
        return PI * radius * radius * lengthInMeters * 1000
    }

    private fun convertToMeters(value: Double, unit: String): Double {
        return when (unit) {
            "mm" -> value / 1000.0; "cm" -> value / 100.0; "meters" -> value
            "inches" -> value * 0.0254; "feet" -> value * 0.3048; else -> value
        }
    }

    // Function "convertToLiters" is no longer used, so it has been removed.
}