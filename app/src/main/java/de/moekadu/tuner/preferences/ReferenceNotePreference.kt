package de.moekadu.tuner.preferences

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
import de.moekadu.tuner.R
import de.moekadu.tuner.temperaments.*
import de.moekadu.tuner.views.NoteSelector

class ReferenceNotePreferenceDialog : PreferenceDialogFragmentCompat() {
    companion object {
        private const val REQUEST_KEY = "reference_note_preference_dialog.request_key"
        private const val TEMPERAMENT_TYPE_KEY = "reference_note_preference_dialog.temperament_type"
        private const val NOTE_PRINT_OPTIONS_KEY = "reference_note_preference_dialog.note_print_options_key"

        fun newInstance(key: String, requestCode: String, temperamentType: TemperamentType, notePrintOptions: MusicalNotePrintOptions): ReferenceNotePreferenceDialog {
            val args = Bundle(3)
            args.putString(ARG_KEY, key)
            args.putString(REQUEST_KEY, requestCode)
            args.putString(TEMPERAMENT_TYPE_KEY, temperamentType.toString())
            args.putString(NOTE_PRINT_OPTIONS_KEY, notePrintOptions.toString())
            val fragment = ReferenceNotePreferenceDialog()
            fragment.arguments = args
            return fragment
        }
    }

    private var referenceNoteView: NoteSelector? = null
    private var editTextView: AppCompatEditText? = null
    private var standardPitch: MaterialButton? = null
    private var restoredReferenceNoteString: String? = null
    private var restoredFrequencyString: String? = null
    private var temperamentType = TemperamentType.EDO12
    private var notePrintOptions = MusicalNotePrintOptions.None

    override fun onCreate(savedInstanceState: Bundle?) {
//        Log.v("Tuner","ReferenceNotePreferenceDialog.onCreate: $preference (setting)")
//
        // restore saved state
        if (savedInstanceState != null) {
            restoredReferenceNoteString = savedInstanceState.getString("reference note")
            restoredFrequencyString = savedInstanceState.getString("reference frequency")
        }

        val temperamentTypeString = arguments?.getString(TEMPERAMENT_TYPE_KEY) ?: TemperamentType.EDO12.toString()
        temperamentType = TemperamentType.valueOf(temperamentTypeString)
        val notePrintOptionsString = arguments?.getString(NOTE_PRINT_OPTIONS_KEY) ?: MusicalNotePrintOptions.None.toString()
        notePrintOptions = MusicalNotePrintOptions.valueOf(notePrintOptionsString)
        super.onCreate(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // save current settings of preference
        outState.putString("reference frequency", editTextView?.text?.toString() ?: "")
        outState.putString("reference note", referenceNoteView?.activeNote?.asString() ?: "")
        super.onSaveInstanceState(outState)
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        val referenceNoteString = restoredReferenceNoteString

        var activeReferenceNote = if (referenceNoteString == null) {
            null
        } else {
            try {
                MusicalNote.fromString(referenceNoteString)
            } catch (ex: RuntimeException) {
                null
            }
        }

        var currentFrequency = restoredFrequencyString?.toFloatOrNull()

        val noteNameScale = NoteNameScaleFactory.create(temperamentType)

        referenceNoteView = view.findViewById(R.id.reference_note)
        editTextView = view.findViewById(R.id.reference_frequency)
        standardPitch = view.findViewById(R.id.standard_pitch)
        standardPitch?.setOnClickListener {
            referenceNoteView?.setActiveNote(noteNameScale.defaultReferenceNote, 200L)
            editTextView?.setText(440f.toString())
        }

        when (preference) {
            is ReferenceNotePreference -> {
                if (activeReferenceNote == null)
                    activeReferenceNote = (preference as ReferenceNotePreference).value.referenceNote
                if (restoredFrequencyString == null)
                    currentFrequency = (preference as ReferenceNotePreference).value.frequency
            }
        }

        if (activeReferenceNote == null)
            activeReferenceNote = noteNameScale.defaultReferenceNote
        if (currentFrequency == null)
            currentFrequency = 440f

        // present notes from C0 to C10 (or something similar for non-standard 12-tone scales)
        val noteIndexBegin = noteNameScale.getIndexOfNote(noteNameScale.notes[0].copy(octave = 0))
        val noteIndexEnd = noteNameScale.getIndexOfNote(noteNameScale.notes[0].copy(octave = 10)) + 1
        referenceNoteView?.setNotes(noteIndexBegin, noteIndexEnd, noteNameScale, activeReferenceNote, notePrintOptions)

        editTextView?.setText(currentFrequency.toString())
        editTextView?.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            arguments?.getString(REQUEST_KEY)?.let {
                // val bundle = Bundle(2)
                val referenceNote = referenceNoteView?.activeNote
                val frequency = editTextView?.text.toString().toFloatOrNull()

                if (frequency != null && referenceNote != null) {
                    // bundle.putInt("tone index", toneIndex)
                    // bundle.putFloat("reference frequency", frequency)
//                    Log.v("Tuner", "ReferenceNotePreference.onDialogClosed (Settings), posres=$positiveResult, bundle=$bundle, requestKey=$it")
                    (preference as ReferenceNotePreference).setValueFromData(frequency, referenceNote)
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
            val value = Value(440f, null).apply { fromString(string) }
            return value.frequency
        }
        fun getReferenceNoteFromValue(string: String?): MusicalNote? {
            val value = Value(440f, null).apply { fromString(string) }
            return value.referenceNote
        }
    }
    fun interface OnReferenceNoteChangedListener {
        fun onReferenceNoteChanged(preference: ReferenceNotePreference, frequency: Float, referenceNote: MusicalNote)
    }

    private var onReferenceNoteChangedListener: OnReferenceNoteChangedListener? = null

    fun setOnReferenceNoteChangedListener(onReferenceNoteChangedListener: OnReferenceNoteChangedListener?) {
        this.onReferenceNoteChangedListener = onReferenceNoteChangedListener
    }

    class Value(var frequency: Float, var referenceNote: MusicalNote?) {
        override fun toString(): String {
            return "$frequency ${referenceNote?.asString()}"
        }
        fun fromString(string: String?) {
            if (string == null)
                return
            val values = string.split(" ")
            if (values.size != 2)
                return
            frequency = values[0].toFloatOrNull() ?: 440f
            referenceNote = try {
                MusicalNote.fromString(values[1])
            } catch (ex: RuntimeException) {
                // old versions used the note index to store the reference note,
                // we use the following code to keep compatibility
                val noteIndex = values[1].toIntOrNull()
                if (noteIndex != null) {
                    legacyNoteIndexToNote(noteIndex)
                } else {
                    null
                }
            }
        }
    }
    var value = Value(440.0f, null)
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

    private fun setValueFromString(value: String) {
//        Log.v("Tuner", "ReferenceNotePreference.onSetValueFromString: $value")
        this.value.fromString(value)
        this.value.referenceNote?.let { referenceNote ->
            persistString(value)
//        Log.v("Tuner", "ReferenceNotePreference.onSetValueFromString: $value, f=${this.value.frequency}, t=${this.value.toneIndex}")
            onReferenceNoteChangedListener?.onReferenceNoteChanged(this, this.value.frequency, referenceNote)
            // summary = "my new summary"
        }
    }

    fun setValueFromData(frequency: Float, referenceNote: MusicalNote) {
        value.frequency = frequency
        value.referenceNote = referenceNote
        persistString(value.toString())
        onReferenceNoteChangedListener?.onReferenceNoteChanged(this, this.value.frequency, referenceNote)
        // summary = "my new summary"
    }
}