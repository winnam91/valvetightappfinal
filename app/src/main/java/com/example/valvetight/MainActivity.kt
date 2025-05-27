package com.example.valvetight

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.view.LayoutInflater
import android.widget.ImageView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.Locale
import kotlin.math.PI
import androidx.core.view.isVisible

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
    private lateinit var textInputLayoutQuantity: TextInputLayout
    private lateinit var editTextQuantity: TextInputEditText
    private lateinit var buttonAddComponent: Button
    private lateinit var textViewAddedComponentsList: TextView
    private lateinit var textViewTotalVolume: TextView
    private lateinit var buttonResetAll: Button
    private lateinit var buttonGoToLeakRateCalc: Button
    private lateinit var buttonOpenSettings: Button

    // --- Data ---
    private val addedComponentsDescriptions = ArrayList<CharSequence>()
    private val addedVolumesInLiters = ArrayList<Double>()
    private var totalVolumeInLiters = 0.0

    // SharedPreferences
    private lateinit var unitPrefs: SharedPreferences

    // Current effective units
    private var currentDimensionUnit: String = SettingsActivity.DEFAULT_VAL_DIMENSION_UNIT
    private var currentLineDiameterUnit: String = SettingsActivity.DEFAULT_VAL_LINE_SIZE_UNIT

    // Lint flagged 'Companion' as unused, but its members are utilized.
    private companion object {
        private const val TYPE_UNKNOWN_VOLUME = "Unknown Volume"
        private const val TYPE_HOSE = "Hose"
        private const val TYPE_SPOOL_PIECE = "Spool Piece"
        private const val TYPE_T_PIECE = "T-piece"
        private const val TYPE_ELBOW = "Elbow"
        private const val TYPE_KNOCK_OUT_VESSEL = "Knock-out Vessel"
    }

    private val predefinedVolumes = mapOf(
        TYPE_HOSE to 5.71, TYPE_SPOOL_PIECE to 1.0, TYPE_T_PIECE to 1.0,
        TYPE_ELBOW to 1.0, TYPE_KNOCK_OUT_VESSEL to 300.0
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        unitPrefs = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE)
        initializeUI()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadUnitPreferencesAndUpdateUI()
        updateUIForSelectedComponentType() // Crucial to call this after prefs are loaded
    }

    private fun loadUnitPreferencesAndUpdateUI() {
        currentDimensionUnit = unitPrefs.getString(SettingsActivity.KEY_DIMENSION_UNIT, SettingsActivity.DEFAULT_VAL_DIMENSION_UNIT) ?: SettingsActivity.DEFAULT_VAL_DIMENSION_UNIT
        currentLineDiameterUnit = unitPrefs.getString(SettingsActivity.KEY_LINE_SIZE_UNIT, SettingsActivity.DEFAULT_VAL_LINE_SIZE_UNIT) ?: SettingsActivity.DEFAULT_VAL_LINE_SIZE_UNIT
        // Update suffix texts immediately
        textInputLayoutDiameter.suffixText = currentLineDiameterUnit
        textInputLayoutLength.suffixText = currentDimensionUnit
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
        textInputLayoutQuantity = findViewById(R.id.textInputLayoutQuantity)
        editTextQuantity = findViewById(R.id.editTextQuantity)
        buttonAddComponent = findViewById(R.id.buttonAddComponent)
        textViewAddedComponentsList = findViewById(R.id.textViewAddedComponentsList)
        textViewTotalVolume = findViewById(R.id.textViewTotalVolume)
        buttonResetAll = findViewById(R.id.buttonResetAll)
        buttonGoToLeakRateCalc = findViewById(R.id.buttonGoToLeakRateCalc)
        buttonOpenSettings = findViewById(R.id.buttonOpenSettings)
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
        buttonGoToLeakRateCalc.setOnClickListener {
            val intent = Intent(this, LeakRateActivity::class.java)
            if (totalVolumeInLiters > 0) {
                intent.putExtra("TOTAL_VOLUME_LITERS", totalVolumeInLiters)
            }
            startActivity(intent)
        }
        buttonOpenSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun showCustomToast(message: CharSequence, duration: Int) {
        val inflater = LayoutInflater.from(this)
        // Pass null as the root ViewGroup, as this layout is for a Toast
        val layout = inflater.inflate(R.layout.custom_toast_layout, null)

        val icon = layout.findViewById<ImageView>(R.id.toast_icon)
        icon.setImageResource(R.drawable.valvetight_logo_icon) // XML already sets this

        val text = layout.findViewById<TextView>(R.id.toast_text)
        text.text = message

        Toast(applicationContext).apply {
            this.duration = duration
            view = layout
            show()
        }
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
        } else { // Pre-defined components
            // When dimensional inputs are GONE, Quantity should be below the spinner's CARD
            quantityParams.topToBottom = R.id.cardComponentTypeSpinner // <<< CORRECTED ANCHOR ID
        }
        textInputLayoutQuantity.layoutParams = quantityParams // Apply changes to Quantity layout
        buttonParams.topToBottom = R.id.textInputLayoutQuantity // AddComponent button is always below Quantity
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
        // Suffix text update is handled by loadUnitPreferencesAndUpdateUI, which is called in onResume
        // and also after spinner selection changes visibility of fields.
        // To be absolutely sure:
        if (layoutDimensionalInputs.isVisible) {
            textInputLayoutDiameter.suffixText = currentLineDiameterUnit
            textInputLayoutLength.suffixText = currentDimensionUnit
        }
    }

    private fun handleAddComponent() {
        // ... (rest of handleAddComponent code - no changes needed here from the last full version)
        val selectedType = spinnerComponentType.selectedItem.toString()
        val singleItemVolumeLiters: Double
        val descriptionPart: String
        var componentDisplayName = selectedType

        val quantityStr = editTextQuantity.text.toString()
        if (quantityStr.isEmpty()) { textInputLayoutQuantity.error = getString(R.string.error_empty_field); showCustomToast("Quantity " + getString(R.string.error_empty_field), Toast.LENGTH_SHORT); return }
        val quantity = quantityStr.toIntOrNull()
        if (quantity == null || quantity <= 0) { textInputLayoutQuantity.error = getString(R.string.error_invalid_quantity); showCustomToast(getString(R.string.error_invalid_quantity), Toast.LENGTH_SHORT); return }

        textInputLayoutQuantity.error = null
        textInputLayoutComponentName.error = null
        textInputLayoutDiameter.error = null
        textInputLayoutLength.error = null

        try {
            if (selectedType == TYPE_UNKNOWN_VOLUME) {
                componentDisplayName = editTextComponentName.text.toString().trim()
                if (componentDisplayName.isEmpty()) { textInputLayoutComponentName.error = getString(R.string.error_name_empty); showCustomToast(getString(R.string.error_name_empty), Toast.LENGTH_SHORT); return }

                val diameterStr = editTextDiameter.text.toString()
                val lengthStr = editTextLength.text.toString()
                if (diameterStr.isEmpty()) { textInputLayoutDiameter.error = getString(R.string.error_empty_field); showCustomToast("Diameter " + getString(R.string.error_empty_field), Toast.LENGTH_SHORT); return }
                if (lengthStr.isEmpty()) { textInputLayoutLength.error = getString(R.string.error_empty_field); showCustomToast("Length " + getString(R.string.error_empty_field), Toast.LENGTH_SHORT); return }

                val diameter = parseDimensionInput(diameterStr)
                val length = parseDimensionInput(lengthStr)
                if (diameter == null) { textInputLayoutDiameter.error = getString(R.string.error_invalid_number); showCustomToast("Diameter " + getString(R.string.error_invalid_number), Toast.LENGTH_SHORT); return }
                if (diameter <= 0) { textInputLayoutDiameter.error = getString(R.string.error_non_positive_value); showCustomToast("Diameter " + getString(R.string.error_non_positive_value), Toast.LENGTH_SHORT); return }
                if (length == null) { textInputLayoutLength.error = getString(R.string.error_invalid_number); showCustomToast("Length " + getString(R.string.error_invalid_number), Toast.LENGTH_SHORT); return }
                if (length <= 0) { textInputLayoutLength.error = getString(R.string.error_non_positive_value); showCustomToast("Length " + getString(R.string.error_non_positive_value), Toast.LENGTH_SHORT); return }

                val diameterUnitToUse = currentLineDiameterUnit
                val lengthUnitToUse = currentDimensionUnit

                val diameterMeters = convertToMeters(diameter, diameterUnitToUse)
                val lengthMeters = convertToMeters(length, lengthUnitToUse)

                singleItemVolumeLiters = calculateCylinderVolumeInLiters(diameterMeters, lengthMeters)
                descriptionPart = "$componentDisplayName (D: $diameterStr $diameterUnitToUse, L: $lengthStr $lengthUnitToUse)"
            } else {
                singleItemVolumeLiters = predefinedVolumes[selectedType] ?: 0.0
                descriptionPart = componentDisplayName
            }

            val singleItemVolumeM3 = singleItemVolumeLiters * 0.001
            val totalVolumeForItemsLiters = singleItemVolumeLiters * quantity
            // val totalVolumeForItemsM3 = totalVolumeForItemsLiters * 0.001 // Not used in new logic directly for final string.

            // --- START MODIFIED CODE for finalDescription ---
            val spannableBuilder = SpannableStringBuilder()

            // 1. Component Name (Bold)
            val nameToBold = if (selectedType == TYPE_UNKNOWN_VOLUME) componentDisplayName else descriptionPart
            val boldSpan = StyleSpan(Typeface.BOLD)
            val spannableName = SpannableString(nameToBold)
            spannableName.setSpan(boldSpan, 0, nameToBold.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannableBuilder.append(spannableName)

            // 2. Rest of the description (Normal)
            val detailsString: String
            if (selectedType == TYPE_UNKNOWN_VOLUME) {
                // For unknown volume, descriptionPart already contains dimensions, which we don't want bold with the name.
                // The 'nameToBold' was componentDisplayName. We need to append the rest of the original descriptionPart structure.
                // Original descriptionPart for unknown: "$componentDisplayName (D: $diameterStr $diameterUnitToUse, L: $lengthStr $lengthUnitToUse)"
                // We already have componentDisplayName bold. Now add the non-bold part.
                val diameterStr = editTextDiameter.text.toString() // Re-fetch for safety, though already available
                val lengthStr = editTextLength.text.toString()   // Re-fetch for safety
                val diameterUnitToUse = currentLineDiameterUnit
                val lengthUnitToUse = currentDimensionUnit
                val unknownDetails = " (D: $diameterStr $diameterUnitToUse, L: $lengthStr $lengthUnitToUse)"
                detailsString = if (quantity > 1) {
                    String.format(Locale.US, "%s x %d (%.1f L / %.3f m³ each): %.1f L (%.3f m³)",
                        unknownDetails, quantity, singleItemVolumeLiters, singleItemVolumeLiters * 0.001, totalVolumeForItemsLiters, totalVolumeForItemsLiters * 0.001)
                } else {
                    String.format(Locale.US, "%s: %.1f L (%.3f m³)",
                        unknownDetails, totalVolumeForItemsLiters, totalVolumeForItemsLiters * 0.001)
                }
            } else { // Pre-defined components
                // For pre-defined, 'nameToBold' was 'descriptionPart' (which is just the component type name)
                detailsString = if (quantity > 1) {
                    String.format(Locale.US, " x %d (%.1f L / %.3f m³ each): %.1f L (%.3f m³)",
                        quantity, singleItemVolumeLiters, singleItemVolumeLiters * 0.001, totalVolumeForItemsLiters, totalVolumeForItemsLiters * 0.001)
                } else {
                    String.format(Locale.US, ": %.1f L (%.3f m³)",
                        totalVolumeForItemsLiters, totalVolumeForItemsLiters * 0.001)
                }
            }
            spannableBuilder.append(detailsString)
            addedComponentsDescriptions.add(spannableBuilder)
            // --- END MODIFIED CODE for finalDescription ---
            addedVolumesInLiters.add(totalVolumeForItemsLiters)
            recalculateTotalVolume()
            updateAddedComponentsListDisplay()
            updateTotalVolumeDisplay()
            clearInputFields(selectedType)
        } catch (e: Exception) {
            showCustomToast("Error: ${e.localizedMessage}", Toast.LENGTH_LONG)
        }
    }

    private fun clearInputFields(lastSelectedType: String? = null) { /* ... as before ... */
        editTextComponentName.text?.clear(); editTextDiameter.text?.clear(); editTextLength.text?.clear()
        editTextQuantity.setText(getString(R.string.default_quantity))
        textInputLayoutDiameter.suffixText = currentLineDiameterUnit
        textInputLayoutLength.suffixText = currentDimensionUnit
        if (lastSelectedType == TYPE_UNKNOWN_VOLUME) editTextComponentName.requestFocus() else editTextQuantity.requestFocus()
    }

    private fun resetAll() { /* ... as before ... */
        addedComponentsDescriptions.clear(); addedVolumesInLiters.clear(); totalVolumeInLiters = 0.0
        updateAddedComponentsListDisplay(); updateTotalVolumeDisplay()
        spinnerComponentType.setSelection(0)
        clearInputFields()
        // updateUIForSelectedComponentType() // Already called by spinner listener
        showCustomToast("All data reset", Toast.LENGTH_SHORT)
    }

    private fun recalculateTotalVolume() { /* ... as before ... */ totalVolumeInLiters = addedVolumesInLiters.sum() }
    // --- START MODIFIED CODE ---
    private fun updateAddedComponentsListDisplay() {
        if (addedComponentsDescriptions.isEmpty()) {
            textViewAddedComponentsList.text = ""
        } else {
            val builder = SpannableStringBuilder()
            addedComponentsDescriptions.forEachIndexed { index, charSequence ->
                builder.append(charSequence)
                if (index < addedComponentsDescriptions.size - 1) {
                    builder.append("\n")
                }
            }
            textViewAddedComponentsList.text = builder
        }
    }
    // --- END MODIFIED CODE ---
    private fun updateTotalVolumeDisplay() { /* ... as before ... */ val totalVolumeM3 = totalVolumeInLiters * 0.001; textViewTotalVolume.text = String.format( Locale.US, "%s%.1f L (%.3f m³)", getString(R.string.total_volume_label_prefix), totalVolumeInLiters, totalVolumeM3 ) }
    private fun parseSimpleFraction(fractionStr: String): Double? { /* ... as before ... */ val parts = fractionStr.trim().split('/'); if (parts.size == 2) { val num = parts[0].toDoubleOrNull(); val den = parts[1].toDoubleOrNull(); if (num != null && den != null && den != 0.0) return num / den }; return null }
    private fun parseDimensionInput(input: String): Double? { /* ... as before ... */ val s = input.trim(); if (s.isEmpty()) return null; s.toDoubleOrNull()?.let { return it }; if (s.contains('-') && s.contains('/')) { val p = s.split('-', limit = 2); if (p.size == 2) { val w = p[0].toDoubleOrNull(); val f = parseSimpleFraction(p[1]); if (w != null && f != null) return w + f } }; if (s.contains(' ') && s.contains('/')) { val i = s.lastIndexOf(' '); if (i > 0 && s.indexOf('/') > i) { val ws = s.substring(0, i); val fs = s.substring(i + 1); val w = ws.toDoubleOrNull(); val f = parseSimpleFraction(fs); if (w != null && f != null) return w + f } }; return parseSimpleFraction(s) }
    private fun calculateCylinderVolumeInLiters(diameterInMeters: Double, lengthInMeters: Double): Double { /* ... as before ... */ val r = diameterInMeters / 2.0; return PI * r * r * lengthInMeters * 1000 }
    private fun convertToMeters(value: Double, unit: String): Double { /* ... as before ... */ return when (unit) { "mm" -> value / 1000.0; "cm" -> value / 100.0; "meters" -> value; "inches" -> value * 0.0254; "feet" -> value * 0.3048; else -> value } }
}