package de.moekadu.tuner.preferences

import android.content.Context
import android.content.res.TypedArray
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import android.widget.CheckBox
import android.widget.RadioGroup

import androidx.preference.DialogPreference
import androidx.preference.PreferenceDialogFragmentCompat
import de.moekadu.tuner.R

private fun notationToResourceId(notation: String) = when(notation){
    "solfege" -> R.id.solfege
    "international" -> R.id.international
    "carnatic" -> R.id.carnatic
    "hindustani" -> R.id.hindustani
    else -> R.id.standard
}
private fun resourceIdToNotation(id: Int) = when(id){
    R.id.solfege ->"solfege"
    R.id.international -> "international"
    R.id.carnatic -> "carnatic"
    R.id.hindustani -> "hindustani"
    else -> "standard"
}

class NotationPreferenceDialog : PreferenceDialogFragmentCompat() {
    companion object {
        private const val REQUEST_KEY = "notation_preference_dialog.request_key"

        fun newInstance(key: String, requestCode: String): NotationPreferenceDialog {
            val args = Bundle(2)
            args.putString(ARG_KEY, key)
            args.putString(REQUEST_KEY, requestCode)
            val fragment = NotationPreferenceDialog()
            fragment.arguments = args
            return fragment
        }
    }

    private var notationChooser: RadioGroup? = null
    private var helmholtzMode: CheckBox? = null

    private var restoredNotation: Int? = null
    private var restoredHelmholtz: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
//        Log.v("Tuner","ReferenceNotePreferenceDialog.onCreate: $preference (setting)")
//
        // restore saved state
        if (savedInstanceState != null) {
            restoredNotation = if (savedInstanceState.containsKey("notation"))
                savedInstanceState.getInt("notation")
            else
                null
            restoredHelmholtz = if (savedInstanceState.containsKey("helmholtz"))
                savedInstanceState.getBoolean("helmholtz")
            else
                null
        }

        super.onCreate(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // save current settings of preference
        outState.putInt("notation", notationChooser?.checkedRadioButtonId ?: R.id.standard)
        outState.putBoolean("helmholtz", helmholtzMode?.isChecked ?: false)
        super.onSaveInstanceState(outState)
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        notationChooser = view.findViewById(R.id.notation)
        helmholtzMode = view.findViewById(R.id.helmholtz_notation)

        when (preference) {
            is NotationPreference -> {
                if (restoredNotation == null)
                    restoredNotation = notationToResourceId((preference as NotationPreference).value.notation)
                if (restoredHelmholtz == null)
                    restoredHelmholtz = (preference as NotationPreference).value.helmholtzEnabled
            }
        }

        notationChooser?.check(restoredNotation ?: R.id.standard)
        helmholtzMode?.isChecked = restoredHelmholtz ?: false
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            val notation = resourceIdToNotation(notationChooser?.checkedRadioButtonId ?: R.id.standard)
            val helmholtzEnabled = helmholtzMode?.isChecked ?: false
            (preference as NotationPreference).setValueFromData(notation, helmholtzEnabled)
        }
    }
}

class NotationPreference(context: Context, attrs: AttributeSet?)
    : DialogPreference(context, attrs, androidx.preference.R.attr.dialogPreferenceStyle) {

    fun interface OnNotationChangedListener {
        fun onPreferenceChanged(preference: NotationPreference, value: Value, notationChanged: Boolean, helmholtzChanged: Boolean)
    }

    private var onNotationChangedListener: OnNotationChangedListener? = null

    fun setOnNotationChangedListener(onNotationChangedListener: OnNotationChangedListener?) {
        this.onNotationChangedListener = onNotationChangedListener
    }

    data class Value(var notation: String, var helmholtzEnabled: Boolean) {
        override fun toString(): String {
            var result = notation
            if (helmholtzEnabled)
                result += " helmholtzEnabled"
            return result
        }

        fun fromString(string: String?) {
            if (string == null)
                return
            val values = string.split(" ")
            helmholtzEnabled = values.contains("helmholtzEnabled")
            notation = when {
                values.contains("solfege") -> "solfege"
                values.contains("international") -> "international"
                values.contains("carnatic") -> "carnatic"
                values.contains("hindustani") -> "hindustani"
                else -> "standard"
            }
        }
    }
    var value = Value(
        notation = "standard",
        helmholtzEnabled = false
    )
        private set

    init {
        dialogLayoutResource = R.layout.notation_preference
        setPositiveButtonText(R.string.done)
        setNegativeButtonText(R.string.abort)
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any? {
//       Log.v("Tuner", "ReferenceNotePreference.onGetDefaultValue: ${a.getString(index)}")
       return a.getString(index)
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        super.onSetInitialValue(defaultValue)
//        Log.v("Tuner", "ReferenceNotePreference.onSetInitialValue: ${defaultValue as String?}, ${value.frequency}, ${value.toneIndex}")
        val defaultValueResolved = if (defaultValue is String) defaultValue else value.toString()
        setValueFromString(getPersistedString(defaultValueResolved))
    }

    private fun setValueFromString(value: String) {
//        Log.v("Tuner", "ReferenceNotePreference.onSetValueFromString: $value")
        val newValue = Value("standard", helmholtzEnabled = false)
        newValue.fromString(value)
        val notationChanged = (this.value.notation != newValue.notation)
        val helmholtzChanged = (this.value.helmholtzEnabled != newValue.helmholtzEnabled)
        this.value = newValue
        persistString(value)
        onNotationChangedListener?.onPreferenceChanged(this, this.value, notationChanged, helmholtzChanged)
    }

    fun setValueFromData(notation: String, helmholtzEnabled: Boolean) {
        val notationChanged = (notation != value.notation)
        val helmholtzChanged = (helmholtzEnabled != value.helmholtzEnabled)
        value.notation = notation
        value.helmholtzEnabled = helmholtzEnabled
        persistString(value.toString())
        onNotationChangedListener?.onPreferenceChanged(this, this.value, notationChanged, helmholtzChanged)
    }
}