package de.moekadu.tuner

import android.content.Context
import android.content.res.TypedArray
import android.os.Bundle
import android.text.InputType
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatEditText
import androidx.preference.DialogPreference
import androidx.preference.PreferenceDialogFragmentCompat
import com.google.android.material.button.MaterialButton

class ReferenceNotePreferenceDialog : PreferenceDialogFragmentCompat() {
    companion object {
        private const val REQUEST_KEY = "reference_note_preference_dialog.request_key"
        private const val PREFER_FLAT_KEY = "reference_note_preference_dialog.prefer_flat"
        fun newInstance(key: String, requestCode: String, preferFlat: Boolean): ReferenceNotePreferenceDialog{
            val args = Bundle(1)
            args.putString(ARG_KEY, key)
            args.putString(REQUEST_KEY, requestCode)
            args.putBoolean(PREFER_FLAT_KEY, preferFlat)
            val fragment = ReferenceNotePreferenceDialog()
            fragment.arguments = args
            return fragment
        }
    }

    private var referenceNoteView: NoteSelector? = null
    private var editTextView: AppCompatEditText? = null
    private var standardPitch: MaterialButton? = null
    private var restoredToneIndex: Int = Int.MAX_VALUE
    private var restoredFrequencyString: String? = null
    private var preferFlat = false

    override fun onCreate(savedInstanceState: Bundle?) {
//        Log.v("Tuner","ReferenceNotePreferenceDialog.onCreate: $preference (setting)")
//
        // restore saved state
        if (savedInstanceState != null) {
            restoredToneIndex = savedInstanceState.getInt("tone index", Int.MAX_VALUE)
            restoredFrequencyString = savedInstanceState.getString("reference frequency")
        }

        preferFlat = arguments?.getBoolean(PREFER_FLAT_KEY) ?: false
        super.onCreate(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // save current settings of preference
        outState.putString("reference frequency", editTextView?.text?.toString() ?: "")
        outState.putInt("tone index", referenceNoteView?.activeToneIndex ?: 0)
        super.onSaveInstanceState(outState)
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        var activeToneIndex = restoredToneIndex
        var currentFrequency = restoredFrequencyString?.toFloatOrNull()

        referenceNoteView = view.findViewById(R.id.reference_note)
        editTextView = view.findViewById(R.id.reference_frequency)
        standardPitch = view.findViewById(R.id.standard_pitch)

        context?.let {ctx ->
            referenceNoteView?.setNotes(-50, 50) {
                noteNames12Tone.getNoteName(ctx, it, preferFlat = preferFlat)
            }
        }

        standardPitch?.setOnClickListener {
            referenceNoteView?.setActiveTone(0, 200L)
            editTextView?.setText(440f.toString())
        }

        when (preference) {
            is ReferenceNotePreference -> {
                if (restoredToneIndex == Int.MAX_VALUE)
                    activeToneIndex = (preference as ReferenceNotePreference).value.toneIndex
                if (restoredFrequencyString == null)
                    currentFrequency = (preference as ReferenceNotePreference).value.frequency
            }
        }

        if (activeToneIndex == Int.MAX_VALUE)
            activeToneIndex = 0
        if (currentFrequency == null)
            currentFrequency = 440f

        referenceNoteView?.setActiveTone(activeToneIndex, 0L)
        editTextView?.setText(currentFrequency.toString())
        editTextView?.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            arguments?.getString(REQUEST_KEY)?.let {
                val bundle = Bundle(2)
                val toneIndex = referenceNoteView?.activeToneIndex ?: 0
                val frequency = editTextView?.text.toString().toFloatOrNull()

                if (frequency != null) {
                    bundle.putInt("tone index", toneIndex)
                    bundle.putFloat("reference frequency", frequency)
//                    Log.v("Tuner", "ReferenceNotePreference.onDialogClosed (Settings), posres=$positiveResult, bundle=$bundle, requestKey=$it")
                    (preference as ReferenceNotePreference).setValueFromData(frequency, toneIndex)
                    // setFragmentResult(it, bundle)
                }
            }
        }
    }
}

class ReferenceNotePreference(context: Context, attrs: AttributeSet?) // , defStyleAttr: Int, defStyleRef: Int)
    : DialogPreference(context, attrs, R.attr.dialogPreferenceStyle) { // , defStyleAttr, defStyleRef) {
    companion object {
        fun getFrequencyFromValue(string: String?): Float {
            val value = Value(440f, 0).apply { fromString(string) }
            return value.frequency
        }
        fun getToneIndexFromValue(string: String?): Int {
            val value = Value(440f, 0).apply { fromString(string) }
            return value.toneIndex
        }
    }
    fun interface OnReferenceNoteChangedListener {
        fun onReferenceNoteChanged(preference: ReferenceNotePreference, frequency: Float, toneIndex: Int)
    }

    private var onReferenceNoteChangedListener: OnReferenceNoteChangedListener? = null

    fun setOnReferenceNoteChangedListener(onReferenceNoteChangedListener: OnReferenceNoteChangedListener?) {
        this.onReferenceNoteChangedListener = onReferenceNoteChangedListener
    }

    class Value(var frequency: Float, var toneIndex: Int) {
        override fun toString(): String {
            return "$frequency $toneIndex"
        }
        fun fromString(string: String?) {
            if (string == null)
                return
            val values = string.split(" ")
            if (values.size != 2)
                return
            frequency = values[0].toFloatOrNull() ?: 440f
            toneIndex = values[1].toIntOrNull() ?: 0
        }
    }
    var value = Value(440.0f, 0)
        private set

    init {
        dialogLayoutResource = R.layout.reference_note_preference
    }

//    override fun getDialogLayoutResource(): Int {
//        return R.layout.reference_note_preference
//        // return super.getDialogLayoutResource()
//    }
    override fun onGetDefaultValue(a: TypedArray, index: Int): Any? {
//       Log.v("Tuner", "ReferenceNotePreference.onGetDefaultValue: ${a.getString(index)}")
       //super.onGetDefaultValue(a, index)
       return a.getString(index)
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        super.onSetInitialValue(defaultValue)
//        Log.v("Tuner", "ReferenceNotePreference.onSetInitialValue: ${defaultValue as String?}, ${value.frequency}, ${value.toneIndex}")
        // maybe we should better do a if(defaultValue is String) ....???
        val defaultValueResolved = if (defaultValue is String) defaultValue else value.toString()
        setValueFromString(getPersistedString(defaultValueResolved))
    }

    fun setValueFromString(value: String) {
//        Log.v("Tuner", "ReferenceNotePreference.onSetValueFromString: $value")
        this.value.fromString(value)
        persistString(value)
//        Log.v("Tuner", "ReferenceNotePreference.onSetValueFromString: $value, f=${this.value.frequency}, t=${this.value.toneIndex}")
        onReferenceNoteChangedListener?.onReferenceNoteChanged(this, this.value.frequency, this.value.toneIndex)
        // summary = "my new summary"
    }

    fun setValueFromData(frequency: Float, toneIndex: Int) {
        value.frequency = frequency
        value.toneIndex = toneIndex
        persistString(value.toString())
        onReferenceNoteChangedListener?.onReferenceNoteChanged(this, this.value.frequency, this.value.toneIndex)
        // summary = "my new summary"
    }
}