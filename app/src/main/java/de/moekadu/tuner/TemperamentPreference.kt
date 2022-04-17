package de.moekadu.tuner

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
import kotlin.math.log
import kotlin.math.max
import kotlin.math.pow

class TemperamentPreferenceDialog : PreferenceDialogFragmentCompat() {
    companion object {
        private const val REQUEST_KEY = "temperament_preference_dialog.request_key"
        private const val PREFER_FLAT_KEY = "reference_note_preference_dialog.prefer_flat"
        fun newInstance(key: String, requestCode: String, preferFlat: Boolean): TemperamentPreferenceDialog{
            val args = Bundle(3)
            args.putString(ARG_KEY, key)
            args.putString(REQUEST_KEY, requestCode)
            args.putBoolean(PREFER_FLAT_KEY, preferFlat)
            val fragment = TemperamentPreferenceDialog()
            fragment.arguments = args
            return fragment
        }

        private fun computeCent(ratio: Float): Float {
            val centRatio = 2.0.pow(1.0/1200).toFloat()
            return log(ratio, centRatio)
        }
    }

    private var spinner: Spinner? = null
    private var rootNote: NoteSelector? = null
    private var rootNoteTitle: TextView? = null
    private var noteTable: RecyclerView? = null
    private val tableAdapter = TemperamentTableAdapter()

    private var preferFlat = false

    private var centArray = Array(0) { 0f }

    private var restoredRootNote = Int.MAX_VALUE
    private var restoredTemperament: Tuning? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        if (savedInstanceState != null) {
            restoredRootNote = savedInstanceState.getInt("root note", Int.MAX_VALUE)
            val restoredTemperamentString = savedInstanceState.getString("temperament")
            restoredTemperament = if (restoredTemperamentString == null)
                null
            else
                Tuning.valueOf(restoredTemperamentString)
        }

        preferFlat = arguments?.getBoolean(PREFER_FLAT_KEY) ?: false
        super.onCreate(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // save current settings of preference
        val spinnerItem = spinner?.selectedItem
        if (spinnerItem is TemperamentProperties)
            outState.putString("temperament", spinnerItem.tuning.toString())
        outState.putInt("root note", rootNote?.activeToneIndex ?: -9)
        super.onSaveInstanceState(outState)
    }
    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        spinner = view.findViewById(R.id.spinner)
        rootNote = view.findViewById(R.id.root_note)
        rootNoteTitle = view.findViewById(R.id.root_note_title)
        noteTable = view.findViewById(R.id.note_table)

        when (preference) {
            is TemperamentPreference -> {
                if (restoredRootNote == Int.MAX_VALUE)
                    restoredRootNote = (preference as TemperamentPreference).value.rootNote
                if (restoredTemperament == null)
                    restoredTemperament = (preference as TemperamentPreference).value.tuning
            }
        }

        if (restoredRootNote == Int.MAX_VALUE)
            restoredRootNote = -9
        if (restoredTemperament == null)
            restoredTemperament = Tuning.EDO12

        context?.let { ctx ->
            spinner?.adapter = TemperamentSpinnerAdapter(ctx)

            rootNote?.setNotes(-9, 3) {
                noteNames12Tone.getNoteName(ctx, it, preferFlat = preferFlat, withOctaveIndex = false)
            }

            noteTable?.layoutManager = LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false)
            noteTable?.adapter = tableAdapter
        }

        rootNote?.setActiveTone(restoredRootNote, 0L)
        rootNote?.toneChangedListener = NoteSelector.ToneChangedListener {
            // TOOD: update only on up
            updateTable()
        }
        val spinnerIndex = max(0, Tuning.values().indexOfFirst { it == restoredTemperament })
        spinner?.setSelection(spinnerIndex)
        spinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val tuningType = Tuning.values()[position]

                centArray = computeCentArray(tuningType)
                updateTable()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }

        centArray = computeCentArray(tuningType = restoredTemperament ?: Tuning.EDO12)
        updateTable()
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            arguments?.getString(REQUEST_KEY)?.let {
                val rootNote = rootNote?.activeToneIndex ?: -9
                val tuning = (spinner?.selectedItem as TemperamentProperties?)?.tuning ?: Tuning.EDO12

                (preference as TemperamentPreference).setValueFromData(tuning, rootNote)
                // setFragmentResult(it, bundle)
            }
        }
    }

    private fun computeCentArray(tuningType: Tuning): Array<Float> {
        val tuning = TuningFactory.create(tuningType, 0, 0, 440f)

        return Array(tuning.getNumberOfNotesPerOctave() + 1) {
            computeCent(tuning.getNoteFrequency(it) / tuning.getNoteFrequency(0))
        }
    }

    private fun updateTable() {
        val ctx = context ?: return
        val rootNoteValue = rootNote?.activeToneIndex?: -9

        tableAdapter.setEntries(
            Array(centArray.size) {
                noteNames12Tone.getNoteName(ctx, rootNoteValue + it, preferFlat = preferFlat, withOctaveIndex = false)
            },
            centArray,
            null
        )
    }
}

class TemperamentPreference(context: Context, attrs: AttributeSet?)
    : DialogPreference(context, attrs, R.attr.dialogPreferenceStyle) {
    companion object {
        fun getTuningFromValue(string: String?): Tuning {
            val value = Value(Tuning.EDO12, -9).apply { fromString(string) }
            return value.tuning
        }
        fun getRootNoteIndexFromValue(string: String?): Int {
            val value = Value(Tuning.EDO12, -9).apply { fromString(string) }
            return value.rootNote
        }
    }
    fun interface OnTemperamentChangedListener {
        fun onTemperamentChanged(preference: TemperamentPreference, tuning: Tuning, rootNote: Int)
    }

    private var onTemperamentChangedListener: OnTemperamentChangedListener? = null

    fun setOnTemperamentChangedListener(onTemperamentChangedListener: OnTemperamentChangedListener?) {
        this.onTemperamentChangedListener = onTemperamentChangedListener
    }

    class Value (var tuning: Tuning, var rootNote: Int) {
        override fun toString(): String {
            return "$tuning $rootNote"
        }
        fun fromString(string: String?) {
            if (string == null)
                return
            val values = string.split(" ")
            if (values.size != 2)
                return
            tuning = Tuning.valueOf(values[0])
            rootNote = values[1].toIntOrNull() ?: -9
        }
    }

    var value = Value(Tuning.EDO12, -9)
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
        this.value.fromString(value)
        persistString(value)
//        Log.v("Tuner", "TemperamentPreference.onSetValueFromString: $value, f=${this.value.frequency}, t=${this.value.toneIndex}")
        onTemperamentChangedListener?.onTemperamentChanged(this, this.value.tuning, this.value.rootNote)
        // summary = "my new summary"
    }

    fun setValueFromData(tuning: Tuning, rootNote: Int) {
        value.tuning = tuning
        value.rootNote = rootNote
        persistString(value.toString())
        onTemperamentChangedListener?.onTemperamentChanged(this, this.value.tuning, this.value.rootNote)
        // summary = "my new summary"
    }

}