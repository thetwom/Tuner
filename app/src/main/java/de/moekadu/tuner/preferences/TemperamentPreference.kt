package de.moekadu.tuner.preferences

import android.content.Context
import android.content.res.TypedArray
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.TextView
import androidx.preference.DialogPreference
import androidx.preference.PreferenceDialogFragmentCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import de.moekadu.tuner.R
import de.moekadu.tuner.temperaments.*
import de.moekadu.tuner.views.NoteSelector
import kotlin.math.log
import kotlin.math.pow

class TemperamentPreferenceDialog : PreferenceDialogFragmentCompat() {
    companion object {
        private const val REQUEST_KEY = "temperament_preference_dialog.request_key"
        private const val NOTE_PRINT_OPTIONS_KEY = "reference_note_preference_dialog.note_print_options_key"

        fun newInstance(key: String, requestCode: String, notePrintOptions: MusicalNotePrintOptions): TemperamentPreferenceDialog {
            val args = Bundle(3)
            args.putString(ARG_KEY, key)
            args.putString(REQUEST_KEY, requestCode)
            args.putString(NOTE_PRINT_OPTIONS_KEY, notePrintOptions.toString())
            val fragment = TemperamentPreferenceDialog()
            fragment.arguments = args
            return fragment
        }

        private fun computeCent(ratio: Float): Float {
            val centRatio = 2.0.pow(1.0/1200).toFloat()
            return log(ratio, centRatio)
        }

    }

    private var temperamentSpinner: Spinner? = null
    private var rootNoteSelector: NoteSelector? = null
//    private var rootNoteTitle: TextView? = null
    private var noteTable: RecyclerView? = null
    private val tableAdapter = TemperamentTableAdapter()
    private var circleOfFifths: RecyclerView? = null
    private var circleOfFifthsAdapter = TemperamentCircleOfFifthsAdapter()
    private var circleOfFifthsDesc: TextView? = null
    private var circleOfFifthsTitle: TextView? = null
    private var resetToDefaultButton: MaterialButton? = null

    private var notePrintOptions = MusicalNotePrintOptions.None

    private var centArray = FloatArray(0) { 0f }
    private var ratioArray: Array<RationalNumber>? = null

    private var musicalScale: MusicalScale? = null
    private var restoredRootNoteString: String? = null
    private var restoredTemperamentType: TemperamentType? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        if (savedInstanceState != null) {
            restoredRootNoteString = savedInstanceState.getString("root note")
            val restoredTemperamentString = savedInstanceState.getString("temperament")
            restoredTemperamentType = if (restoredTemperamentString == null)
                null
            else
                TemperamentType.valueOf(restoredTemperamentString)
        }

        val notePrintOptionsString = arguments?.getString(NOTE_PRINT_OPTIONS_KEY) ?: MusicalNotePrintOptions.None.toString()
        notePrintOptions = MusicalNotePrintOptions.valueOf(notePrintOptionsString)
        super.onCreate(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // save current settings of preference
        val spinnerItem = temperamentSpinner?.selectedItem
        if (spinnerItem is TemperamentProperties)
            outState.putString("temperament", spinnerItem.temperamentType.toString())
        outState.putString("root note", rootNoteSelector?.activeNote?.asString() ?: "")
        super.onSaveInstanceState(outState)
    }
    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        val rootNoteString = restoredRootNoteString
        var activeRootNote = if (rootNoteString == null) {
            null
        } else {
            try {
                MusicalNote.fromString(rootNoteString)
            } catch (ex: RuntimeException) {
                null
            }
        }

        temperamentSpinner = view.findViewById(R.id.spinner)
        rootNoteSelector = view.findViewById(R.id.root_note)
//        rootNoteTitle = view.findViewById(R.id.root_note_title)
        noteTable = view.findViewById(R.id.note_table)
        noteTable?.itemAnimator = null
        circleOfFifths = view.findViewById(R.id.circle_of_fifths)
        circleOfFifths?.itemAnimator = null
        circleOfFifthsDesc = view.findViewById(R.id.circle_of_fifths_desc)
        circleOfFifthsTitle = view.findViewById(R.id.circle_of_fifths_title)
        resetToDefaultButton = view.findViewById(R.id.reset)

        resetToDefaultButton?.setOnClickListener {
            setNewMusicalScale(TemperamentType.EDO12, null, resetToDefaultRootNote = true)
        }

        when (preference) {
            is TemperamentPreference -> {
                if (activeRootNote == null)
                    activeRootNote = (preference as TemperamentPreference).value.rootNote
                if (restoredTemperamentType == null)
                    restoredTemperamentType = (preference as TemperamentPreference).value.temperamentType
            }
        }

        context?.let { ctx ->
            temperamentSpinner?.adapter = TemperamentSpinnerAdapter(ctx)
            noteTable?.layoutManager = LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false)
            noteTable?.adapter = tableAdapter
            circleOfFifths?.layoutManager = LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false)
            circleOfFifths?.adapter = circleOfFifthsAdapter
        }

        // resetToDefaultRootNote will only take place if activeRootNote is null ...
        setNewMusicalScale(restoredTemperamentType ?: TemperamentType.EDO12, activeRootNote, resetToDefaultRootNote = true)

        temperamentSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val temperamentType = TemperamentType.values()[position]
                setNewMusicalScale(temperamentType, null, resetToDefaultRootNote = false)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) { }
        }

        rootNoteSelector?.noteChangedListener = NoteSelector.NoteChangedListener {
            musicalScale?.noteNameScale?.let { noteNameScale ->
                updateCircleOfFifthNoteNames(it, noteNameScale)
                updateCentAndRatioTable(it, noteNameScale, centArray, ratioArray)
            }
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            arguments?.getString(REQUEST_KEY)?.let {
                val rootNote = rootNoteSelector?.activeNote
                val temperamentType = (temperamentSpinner?.selectedItem as TemperamentProperties?)?.temperamentType ?: TemperamentType.EDO12

                (preference as TemperamentPreference).setValueFromData(temperamentType, rootNote)
                // setFragmentResult(it, bundle)
            }
        }
    }

    /** Set all views to a match the temperament.
     * @param temperamentType Type of new temperament
     * @param newSelectedRootNote New root note which should be used for selection or null
     *   if no specific note is needed.
     * @param resetToDefaultRootNote If true we set the selected root note to the first note
     *   of the scale. This is ignored if newSelectedRootNote is not null.
     */
    private fun setNewMusicalScale(temperamentType: TemperamentType, newSelectedRootNote: MusicalNote?, resetToDefaultRootNote: Boolean) {
        // update spinner
        val spinnerIndex = TemperamentType.values().indexOfFirst { it == temperamentType }
        require(spinnerIndex >= 0)
        temperamentSpinner?.setSelection(spinnerIndex)

        // update the temperament properties, therefore we need a musicalScale where the root note and the
        // reference note are the same
        val defaultReferenceNote = NoteNameScaleFactory.getDefaultReferenceNote(temperamentType)
        val musicalScaleLocal = MusicalScaleFactory.create(
            temperamentType,
            defaultReferenceNote,
            referenceFrequency = 440f,
            rootNote = defaultReferenceNote,
            frequencyMin = 440f,
            frequencyMax = 2.5f * 440f) // normally up to 2*440f would be enough for one octave, but lets play safe here
        require(musicalScaleLocal.noteIndexBegin <= 0) // default reference note is index 0 per definition
        // we needed to include the octave (so +1) and since "end" is not included, we need another +1
        require(musicalScaleLocal.noteIndexEnd >= musicalScaleLocal.numberOfNotesPerOctave + 2)
        musicalScale = musicalScaleLocal

        computeCentAndRatioArrays(musicalScaleLocal)

        val firstPossibleRootNote = musicalScaleLocal.noteNameScale.notes[0].copy(octave = 4)
        val lastPossibleRootNote = musicalScaleLocal.noteNameScale.notes.last().copy(octave = 4)
        val noteIndexBegin = musicalScaleLocal.noteNameScale.getIndexOfNote(firstPossibleRootNote)
        val noteIndexEnd = musicalScaleLocal.noteNameScale.getIndexOfNote(lastPossibleRootNote) + 1
        require(noteIndexBegin != Int.MAX_VALUE)
        require(noteIndexEnd != Int.MAX_VALUE)

        val rootNote = if (newSelectedRootNote != null)
            newSelectedRootNote
        else if (resetToDefaultRootNote)
            musicalScaleLocal.noteNameScale.notes[0].copy(octave = 4)
        else
            null

        // set the new note scale in the root note selector and select the required note
        rootNoteSelector?.setNotes(noteIndexBegin, noteIndexEnd, musicalScaleLocal.noteNameScale, rootNote, notePrintOptions)

        val selectedRootNote = rootNoteSelector?.activeNote ?: musicalScaleLocal.noteNameScale.notes[0].copy(octave = 4)
        updateCentAndRatioTable(selectedRootNote, musicalScaleLocal.noteNameScale, centArray, ratioArray)
        updateCircleOfFifthNoteNames(selectedRootNote, musicalScaleLocal.noteNameScale)
        updateCircleOfFifthCorrections(musicalScaleLocal.circleOfFifths)
    }

    /** Compute the cents of the temperament.
     * This function updates the following attributes: centArray, ratioArray
     *  @param musicalScale Musical scale of the temperament, where the reference note must
     *    be the same as the root note.
     */
    private fun computeCentAndRatioArrays(musicalScale: MusicalScale) {
        require(musicalScale.rootNote == musicalScale.referenceNote)
        val referenceFrequencyIndex = musicalScale.getNoteIndex(musicalScale.referenceNote)
        centArray =  FloatArray(musicalScale.numberOfNotesPerOctave + 1) {
            computeCent(musicalScale.getNoteFrequency(it + referenceFrequencyIndex) / musicalScale.referenceFrequency)
        }
        require(centArray[0] < 1e-3) // first not should be 0 cent
        ratioArray = musicalScale.rationalNumberRatios
    }

    private fun updateCentAndRatioTable(rootNote: MusicalNote, noteNameScale: NoteNameScale,
                                        centArray: FloatArray, ratioArray: Array<RationalNumber>?) {
        val ctx = context ?: return
        val rootNoteIndex = noteNameScale.getIndexOfNote(rootNote)
        //require(rootNoteIndex >= 0)

        tableAdapter.setEntries(
            Array(centArray.size) {
                // delete octave index, so that it is not printed
                val note = noteNameScale.getNoteOfIndex(rootNoteIndex + it)
                note.toCharSequence(ctx, withOctave = false)
            },
            centArray,
            ratioArray
        )
    }

    private fun updateCircleOfFifthNoteNames(rootNote:MusicalNote, noteNameScale: NoteNameScale) {
        circleOfFifthsAdapter.setEntries(rootNote, noteNameScale, null)
    }

    private fun updateCircleOfFifthCorrections(fifths: TemperamentCircleOfFifths?) {
        if (fifths == null) {
            circleOfFifths?.visibility = View.GONE
            circleOfFifthsDesc?.visibility = View.GONE
            circleOfFifthsTitle?.visibility = View.GONE
            return
        }
        circleOfFifths?.visibility = View.VISIBLE
        circleOfFifthsDesc?.visibility = View.VISIBLE
        circleOfFifthsTitle?.visibility = View.VISIBLE
        circleOfFifthsAdapter.setEntries(null, null, fifths)
    }
}

class TemperamentPreference(context: Context, attrs: AttributeSet?)
    : DialogPreference(context, attrs, R.attr.dialogPreferenceStyle) {
    companion object {
        fun getTemperamentFromValue(string: String?): TemperamentType {
            val value = Value(TemperamentType.EDO12, null).apply { fromString(string) }
            return value.temperamentType
        }
        fun getRootNoteFromValue(string: String?): MusicalNote? {
            val value = Value(TemperamentType.EDO12, null).apply { fromString(string) }
            return value.rootNote
        }
    }
    fun interface OnTemperamentChangedListener {
        fun onTemperamentChanged(
            preference: TemperamentPreference,
            oldTemperament: TemperamentType,
            temperamentType: TemperamentType,
            rootNote: MusicalNote)
    }

    private var onTemperamentChangedListener: OnTemperamentChangedListener? = null

    fun setOnTemperamentChangedListener(onTemperamentChangedListener: OnTemperamentChangedListener?) {
        this.onTemperamentChangedListener = onTemperamentChangedListener
    }

    class Value (var temperamentType: TemperamentType, var rootNote: MusicalNote?) {
        override fun toString(): String {
            return "$temperamentType ${rootNote?.asString()}"
        }
        fun fromString(string: String?) {
            if (string == null)
                return
            val values = string.split(" ")
            if (values.size != 2)
                return
            temperamentType = TemperamentType.valueOf(values[0])
            rootNote = try {
                MusicalNote.fromString(values[1])
            } catch (ex: RuntimeException) {
                // old versions used the note index to store the root note,
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

    var value = Value(TemperamentType.EDO12, null)
        private set

    init {
        dialogLayoutResource = R.layout.temperament_preference
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any? {
//       Log.v("Tuner", "TemperamentPreference.onGetDefaultValue: ${a.getString(index)}")
        return a.getString(index)
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        super.onSetInitialValue(defaultValue)
//        Log.v("Tuner", "TemperamentPreference.onSetInitialValue: ${defaultValue as String?}, ${value.tuning}, ${value.rootNote}")
        val defaultValueResolved = if (defaultValue is String) defaultValue else value.toString()
        setValueFromString(getPersistedString(defaultValueResolved))
    }

    private fun setValueFromString(value: String) {
//        Log.v("Tuner", "TemperamentPreference.onSetValueFromString: $value")
        val oldTemperamentType = this.value.temperamentType
        this.value.fromString(value)
        this.value.rootNote?.let { rootNote ->
            persistString(value)
//        Log.v("Tuner", "TemperamentPreference.onSetValueFromString: $value, f=${this.value.frequency}, t=${this.value.toneIndex}")
            onTemperamentChangedListener?.onTemperamentChanged(
                this,
                oldTemperamentType,
                this.value.temperamentType, rootNote)
            // summary = "my new summary"
        }
    }

    fun setValueFromData(temperamentType: TemperamentType, rootNote: MusicalNote?) {
        val oldTemperamentType = value.temperamentType
        value.temperamentType = temperamentType
        if (rootNote != null) {
            value.rootNote = rootNote
            persistString(value.toString())
            onTemperamentChangedListener?.onTemperamentChanged(
                this, oldTemperamentType, this.value.temperamentType, rootNote)
            // summary = "my new summary"
        }
    }
}