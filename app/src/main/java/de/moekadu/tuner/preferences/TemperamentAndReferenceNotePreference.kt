package de.moekadu.tuner.preferences

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import de.moekadu.tuner.R
import de.moekadu.tuner.misc.DefaultValues
import de.moekadu.tuner.preferenceResources
import de.moekadu.tuner.temperaments.*
import de.moekadu.tuner.views.NoteSelector
import kotlin.math.log
import kotlin.math.pow

data class TemperamentAndReferenceNoteValue (
    val temperamentType: TemperamentType,
    val rootNote: MusicalNote,
    val referenceNote: MusicalNote,
    val referenceFrequency: String) {
    override fun toString(): String {
        return "$temperamentType ${rootNote.asString()} ${referenceNote.asString()} $referenceFrequency"
    }

    companion object {
        const val TEMPERAMENT_AND_REFERENCE_NOTE_PREFERENCE_KEY = "temperament_and_reference_note.key"

        fun fromSharedPreferences(pref: SharedPreferences): TemperamentAndReferenceNoteValue {
            val value = fromString(pref.getString(TEMPERAMENT_AND_REFERENCE_NOTE_PREFERENCE_KEY, null))
            if (value != null)
                return value

            val referenceNoteLegacy = ReferenceNotePreferenceValueLegacy.obtain(pref)
            val temperamentLegacy = TemperamentPreferenceValueLegacy.obtain(pref)

            val temperamentType = temperamentLegacy?.temperamentType ?: DefaultValues.TEMPERAMENT
            return TemperamentAndReferenceNoteValue(
                temperamentType,
                temperamentLegacy?.rootNote ?: NoteNameScaleFactory.create(temperamentType).notes[0],
                referenceNoteLegacy?.referenceNote ?: NoteNameScaleFactory.getDefaultReferenceNote(temperamentType),
                referenceNoteLegacy?.frequency?.toString() ?: DefaultValues.REFERENCE_FREQUENCY_STRING
            )
        }

        fun fromString(string: String?): TemperamentAndReferenceNoteValue? {
            if (string == null)
                return null
            val values = string.split(" ")
            if (values.size != 4)
                return null
            val temperamentType = try {
                TemperamentType.valueOf(values[0])
            } catch (ex: IllegalArgumentException) {
                TemperamentType.EDO12
            }
            val rootNote = try {
                MusicalNote.fromString(values[1])
            } catch (ex: RuntimeException) {
                return null
            }
            val referenceNote = try {
                MusicalNote.fromString(values[2])
            } catch (ex: RuntimeException) {
                return null
            }
            val referenceFrequency = if (values[3].toFloatOrNull() == null)
                return null
            else
                values[3]
            return TemperamentAndReferenceNoteValue(temperamentType, rootNote, referenceNote, referenceFrequency)
        }
    }
}

/** Old class which stored tempereamnt and root note. This is only
 * needed to read the old preferences when updating the version.
 */
class TemperamentPreferenceValueLegacy (val temperamentType: TemperamentType, val rootNote: MusicalNote?) {
    companion object {
        fun obtain(pref: SharedPreferences): TemperamentPreferenceValueLegacy? {
            val string = pref.getString("temperament", null) ?: return null
            val values = string.split(" ")
            if (values.size != 2)
                return null
            val temperamentType = TemperamentType.valueOf(values[0])
            val rootNote = try {
                MusicalNote.fromString(values[1])
            } catch (ex: RuntimeException) {
                // old versions used the note index to store the root note,
                // we use the following code to keep compatibility
                val noteIndex = values[1].toIntOrNull()
                if (noteIndex != null) {
                    legacyNoteIndexToNote(noteIndex)
                } else {
                    return null
                }
            }
            return TemperamentPreferenceValueLegacy(temperamentType, rootNote)
        }
    }
}

/** Old class which stored reference note and frequency. This is only
 * needed to read the old preferences when updating the version.
 */
class ReferenceNotePreferenceValueLegacy (val referenceNote: MusicalNote, val frequency: Float) {
    companion object {
        fun obtain(pref: SharedPreferences): ReferenceNotePreferenceValueLegacy? {
            val string = pref.getString("reference_note", null) ?: return null
            val values = string.split(" ")
            if (values.size != 2)
                return null
            val frequency = values[0].toFloatOrNull() ?: return null
            val referenceNote = try {
                MusicalNote.fromString(values[1])
            } catch (ex: RuntimeException) {
                // old versions used the note index to store the reference note,
                // we use the following code to keep compatibility
                val noteIndex = values[1].toIntOrNull()
                if (noteIndex != null) {
                    legacyNoteIndexToNote(noteIndex)
                } else {
                    return null
                }
            }
            return ReferenceNotePreferenceValueLegacy(referenceNote, frequency)
        }
    }
}

class ReferenceNotePreferenceDialog: DialogFragment() {
    companion object {
        private const val REQUEST_KEY = "reference_note_preference_dialog.request_key"
        private const val CURRENT_VALUE_KEY = "reference_note_preference_dialog.current_value_key"
        private const val WARNING_MESSAGE_KEY = "reference_note_preference_dialog.warning_message_key"

        fun newInstance(currentValue: TemperamentAndReferenceNoteValue,
                        warningMessage: String?): ReferenceNotePreferenceDialog {
            val args = Bundle(3)
            args.putString(CURRENT_VALUE_KEY, currentValue.toString())
            if (warningMessage != null)
                args.putString(WARNING_MESSAGE_KEY, warningMessage)
            val fragment = ReferenceNotePreferenceDialog()
            fragment.arguments = args
            return fragment
        }

        fun setupFragmentResultListener(
            fragmentManager: FragmentManager,
            lifecycleOwner: LifecycleOwner,
            onPreferenceChanged: (TemperamentAndReferenceNoteValue) -> Unit
        ) {
            fragmentManager.setFragmentResultListener(REQUEST_KEY, lifecycleOwner) { _, bundle ->
                val newPrefsString = bundle.getString(CURRENT_VALUE_KEY) ?: throw RuntimeException("No value set")
                val newPrefs = TemperamentAndReferenceNoteValue.fromString(newPrefsString) ?: throw RuntimeException("Invalid value")
                onPreferenceChanged(newPrefs)
            }
        }
    }

    private var referenceNoteView: NoteSelector? = null
    private var editTextView: AppCompatEditText? = null
    private var standardPitch: MaterialButton? = null
    private var initialPrefs: TemperamentAndReferenceNoteValue? = null
    private var savedReferenceNote: MusicalNote? = null
    private var savedFrequencyString: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
//        Log.v("Tuner","ReferenceNotePreferenceDialog.onCreate: $preference (setting)")
//
        val initialPrefsString = arguments?.getString(CURRENT_VALUE_KEY)
        initialPrefs = TemperamentAndReferenceNoteValue.fromString(initialPrefsString)

        // restore saved state
        if (savedInstanceState != null) {
            val referenceNoteString = savedInstanceState.getString("reference note")
            savedReferenceNote = if (referenceNoteString == null) null else MusicalNote.fromString(referenceNoteString)
            savedFrequencyString = savedInstanceState.getString("reference frequency")
        }
//        Log.v("Tuner", "ReferenceNoteDialog2.onCreate = initialPrefs = $initialPrefs")

        super.onCreate(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // save current settings of preference
        editTextView?.text?.toString()?.let {
            outState.putString("reference frequency", it)
        }
        referenceNoteView?.activeNote?.asString()?.let {
            outState.putString("reference note", it)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(R.layout.reference_note_preference, null)
        val dialog = AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.reference_frequency)
            setView(view)
            setPositiveButton(R.string.done) { _, _ ->
                val bundle = Bundle(1)
                val referenceNote = referenceNoteView?.activeNote ?: initialPrefs?.referenceNote ?: throw RuntimeException("No value available for reference note")
                val referenceFrequencyString = editTextView?.text?.toString() ?: initialPrefs?.referenceFrequency ?: throw RuntimeException("No value available for reference frequency")

                bundle.putString(CURRENT_VALUE_KEY, TemperamentAndReferenceNoteValue(
                    initialPrefs!!.temperamentType,
                    initialPrefs!!.rootNote,
                    referenceNote,
                    if (referenceFrequencyString.toFloatOrNull() == null) initialPrefs!!.referenceFrequency else referenceFrequencyString // TODO: print a message, if the input is invalid??
                ).toString())
                setFragmentResult(REQUEST_KEY, bundle)
            }
            setNegativeButton(R.string.abort) { _, _ ->
                dismiss()
            }
        }.create()

        val noteNameScale = NoteNameScaleFactory.create(initialPrefs!!.temperamentType)

        val warningMessageView = view.findViewById<TextView>(R.id.warning_message)
        val warningMessage = arguments?.getString(WARNING_MESSAGE_KEY, null)
        if (warningMessage == null) {
            warningMessageView.visibility = View.GONE
        } else {
            warningMessageView.visibility = View.VISIBLE
            warningMessageView.text = warningMessage
        }
        referenceNoteView = view.findViewById(R.id.reference_note)
        editTextView = view.findViewById(R.id.reference_frequency)

        standardPitch = view.findViewById(R.id.standard_pitch)
        standardPitch?.setOnClickListener {
            referenceNoteView?.setActiveNote(noteNameScale.defaultReferenceNote, 200L)
            editTextView?.setText(DefaultValues.REFERENCE_FREQUENCY_STRING)
        }

        // present notes from C0 to C10 (or something similar for non-standard 12-tone scales)
        val noteIndexBegin = noteNameScale.getIndexOfNote(noteNameScale.notes[0].copy(octave = 0))
        val noteIndexEnd = noteNameScale.getIndexOfNote(noteNameScale.notes[0].copy(octave = 10)) + 1
        referenceNoteView?.setNotes(
            noteIndexBegin,
            noteIndexEnd,
            noteNameScale,
            savedReferenceNote ?: initialPrefs!!.referenceNote,
            requireContext().preferenceResources.noteNamePrinter.value)

        editTextView?.setText(savedFrequencyString ?: initialPrefs!!.referenceFrequency)
        editTextView?.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

        return dialog
    }
}

class TemperamentPreferenceDialog : DialogFragment() {
    companion object {
        private const val REQUEST_KEY = "temperament_preference_dialog.request_key"
        private const val CURRENT_VALUE_KEY = "reference_note_preference_dialog.current_value_key"

        fun newInstance(currentValue: TemperamentAndReferenceNoteValue): TemperamentPreferenceDialog {
            val args = Bundle(2)
            args.putString(CURRENT_VALUE_KEY, currentValue.toString())
//            Log.v("Tuner", "TemperamentPreferenceDialog2.newInstance: currentValue = $currentValue")
            val fragment = TemperamentPreferenceDialog()
            fragment.arguments = args
            return fragment
        }

        fun setupFragmentResultListener(fragmentManager: FragmentManager,
                                        lifecycleOwner: LifecycleOwner,
                                        context: Context, // needed for getting string resources
                                        previousPreferences: () -> TemperamentAndReferenceNoteValue,
                                        onPreferenceChanged: (TemperamentAndReferenceNoteValue) -> Unit) {

            fragmentManager.setFragmentResultListener(REQUEST_KEY, lifecycleOwner) { _, bundle ->
                val oldPrefs = previousPreferences() // TemperamentAndReferenceNoteValue.fromSharedPreferences(pref)
                val newPrefsString = bundle.getString(CURRENT_VALUE_KEY) ?: throw RuntimeException("No value set")
                val newPrefs = TemperamentAndReferenceNoteValue.fromString(newPrefsString) ?: throw RuntimeException("Invalid value")

                val newPrefNoteNameScale = NoteNameScaleFactory.create(newPrefs.temperamentType)

                if (newPrefNoteNameScale.hasNote(newPrefs.referenceNote)) {
                    onPreferenceChanged(newPrefs)
                } else {
                    // fire reference note dialog to get a compatible reference note
                    val oldPrefNoteNameScale = NoteNameScaleFactory.create(oldPrefs.temperamentType)
                    val initialPrefsWithCompatibleReferenceNote = newPrefs.copy(
                        referenceNote = newPrefNoteNameScale.getClosestNote(newPrefs.referenceNote, oldPrefNoteNameScale)
                    )
//                    Log.v("Tuner", "TemperamentPreferenceDialog2.setupFragmentResultListener: initialPrefsWithCompatibleReferenceNote = $initialPrefsWithCompatibleReferenceNote")
                    val dialog = ReferenceNotePreferenceDialog.newInstance(
                        initialPrefsWithCompatibleReferenceNote,
                        warningMessage = context.getString(R.string.new_temperament_requires_adapting_reference_note)
                    )
                    dialog.show(fragmentManager, "tag")
                }
            }
        }

        private fun computeCent(ratio: Float): Float {
            val centRatio = 2.0.pow(1.0/1200).toFloat()
            return log(ratio, centRatio)
        }
    }

    private var temperamentSpinner: Spinner? = null
    private var rootNoteSelector: NoteSelector? = null
    private var noteTable: RecyclerView? = null
    private val tableAdapter = TemperamentTableAdapter()
    private var circleOfFifths: RecyclerView? = null
    private var circleOfFifthsAdapter = TemperamentCircleOfFifthsAdapter()
    private var circleOfFifthsDesc: TextView? = null
    private var circleOfFifthsTitle: TextView? = null
    private var resetToDefaultButton: MaterialButton? = null

    private var centArray = FloatArray(0) { 0f }
    private var ratioArray: Array<RationalNumber>? = null

    private var musicalScale: MusicalScale? = null
    private var initialPrefs: TemperamentAndReferenceNoteValue? = null

    private var savedRootNote: MusicalNote? = null
    private var savedTemperamentType: TemperamentType? = null


    override fun onCreate(savedInstanceState: Bundle?) {

        val initialPrefsString = arguments?.getString(CURRENT_VALUE_KEY)
        initialPrefs = TemperamentAndReferenceNoteValue.fromString(initialPrefsString)

        if (savedInstanceState != null) {
            val rootNoteString = savedInstanceState.getString("root note")
            savedRootNote = if (rootNoteString == null) null else MusicalNote.fromString(rootNoteString)
            val temperamentTypeString = savedInstanceState.getString("temperament")
            savedTemperamentType = if (temperamentTypeString == null) null else TemperamentType.valueOf(temperamentTypeString)
        }

        super.onCreate(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // save current settings of preference
        val spinnerItem = temperamentSpinner?.selectedItem
        if (spinnerItem is TemperamentProperties)
            outState.putString("temperament", spinnerItem.temperamentType.toString())
        rootNoteSelector?.activeNote?.asString()?.let {
            outState.putString("root note", it)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(R.layout.temperament_preference, null)
        val dialog = AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.temperament)
            setView(view)
            setPositiveButton(R.string.done) { _, _ ->
                val bundle = Bundle(1)
                val spinnerItem = temperamentSpinner?.selectedItem
                val temperamentType = if (spinnerItem is TemperamentProperties)
                    spinnerItem.temperamentType
                else
                    null
                val rootNote = rootNoteSelector?.activeNote

                bundle.putString(CURRENT_VALUE_KEY, TemperamentAndReferenceNoteValue(
                    temperamentType ?: initialPrefs!!.temperamentType,
                    rootNote ?: initialPrefs!!.rootNote,
                    initialPrefs!!.referenceNote,
                    initialPrefs!!.referenceFrequency
                ).toString()
                )
                setFragmentResult(REQUEST_KEY, bundle)
            }
            setNegativeButton(R.string.abort) { _, _ ->
                dismiss()
            }
        }.create()

        temperamentSpinner = view.findViewById(R.id.spinner) ?: throw RuntimeException("NO TEMPERAMENT SPNNER")
        rootNoteSelector = view.findViewById(R.id.root_note)
        noteTable = view.findViewById(R.id.note_table)
        noteTable?.itemAnimator = null
        circleOfFifths = view.findViewById(R.id.circle_of_fifths)
        circleOfFifths?.itemAnimator = null
        circleOfFifthsDesc = view.findViewById(R.id.circle_of_fifths_desc)
        circleOfFifthsTitle = view.findViewById(R.id.circle_of_fifths_title)
        resetToDefaultButton = view.findViewById(R.id.reset)

        resetToDefaultButton?.setOnClickListener {
            setNewMusicalScale(DefaultValues.TEMPERAMENT, null, resetToDefaultRootNote = true)
        }

        context?.let { ctx ->
            temperamentSpinner?.adapter = TemperamentSpinnerAdapter(ctx)
            noteTable?.layoutManager = LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false)
            noteTable?.adapter = tableAdapter
            circleOfFifths?.layoutManager = LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false)
            circleOfFifths?.adapter = circleOfFifthsAdapter
        }

//        Log.v("Tuner", "TemperamentPreferenceDialog2.onCreateDialog: savedRootNote = $savedRootNote, initialRootNote = ${initialPrefs?.rootNote}")
        // resetToDefaultRootNote will only take place if initialRootNote is null ...
        setNewMusicalScale(
            savedTemperamentType ?: initialPrefs!!.temperamentType,
            savedRootNote ?: initialPrefs!!.rootNote,
            resetToDefaultRootNote = true)

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

        return dialog
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
            referenceFrequency = DefaultValues.REFERENCE_FREQUENCY,
            rootNote = defaultReferenceNote,
            frequencyMin = DefaultValues.REFERENCE_FREQUENCY,
            frequencyMax = 2.5f * DefaultValues.REFERENCE_FREQUENCY) // normally up to 2*440f would be enough for one octave, but lets play safe here
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
            newSelectedRootNote.copy(octave = 4)
        else if (resetToDefaultRootNote)
            musicalScaleLocal.noteNameScale.notes[0].copy(octave = 4)
        else
            null

        // set the new note scale in the root note selector and select the required note
        rootNoteSelector?.setNotes(noteIndexBegin, noteIndexEnd, musicalScaleLocal.noteNameScale,
            rootNote, requireContext().preferenceResources.noteNamePrinter.value)

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
        require(centArray[0] < 1e-3) // first note should be 0 cent
        ratioArray = musicalScale.rationalNumberRatios
    }

    private fun updateCentAndRatioTable(rootNote: MusicalNote, noteNameScale: NoteNameScale,
                                        centArray: FloatArray, ratioArray: Array<RationalNumber>?) {
        val ctx = context ?: return
        val noteNamePrinter = ctx.preferenceResources.noteNamePrinter.value
        val rootNoteIndex = noteNameScale.getIndexOfNote(rootNote)
        //require(rootNoteIndex >= 0)

        tableAdapter.setEntries(
            Array(centArray.size) {
                // delete octave index, so that it is not printed
                val note = noteNameScale.getNoteOfIndex(rootNoteIndex + it)
                noteNamePrinter.noteToCharSequence(note, withOctave = false) ?: ""
            },
            centArray,
            ratioArray
        )
    }

    private fun updateCircleOfFifthNoteNames(rootNote:MusicalNote, noteNameScale: NoteNameScale) {
        circleOfFifthsAdapter.setEntries(rootNote, noteNameScale, null,
            requireContext().preferenceResources.noteNamePrinter.value)
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
        circleOfFifthsAdapter.setEntries(null, null, fifths,
            requireContext().preferenceResources.noteNamePrinter.value)
    }
}
