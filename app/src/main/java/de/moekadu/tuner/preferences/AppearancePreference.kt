package de.moekadu.tuner.preferences

import android.content.Context
import android.content.res.TypedArray
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import android.widget.CheckBox
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.DialogPreference
import androidx.preference.PreferenceDialogFragmentCompat
import de.moekadu.tuner.R

private fun nightModeStringToID(string: String) = when(string){
    "dark" -> AppCompatDelegate.MODE_NIGHT_YES
    "light" -> AppCompatDelegate.MODE_NIGHT_NO
    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
}
private fun nightModeIDToString(id: Int) = when(id){
    AppCompatDelegate.MODE_NIGHT_YES -> "dark"
    AppCompatDelegate.MODE_NIGHT_NO -> "light"
    else -> "auto"
}
private fun nightModeIDToResourceId(id: Int) = when(id){
    AppCompatDelegate.MODE_NIGHT_YES -> R.id.dark
    AppCompatDelegate.MODE_NIGHT_NO -> R.id.light
    else -> R.id.auto
}
private fun nightModeResourceIdToID(id: Int) = when(id){
    R.id.dark ->AppCompatDelegate.MODE_NIGHT_YES
    R.id.light ->AppCompatDelegate.MODE_NIGHT_NO
    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
}

class AppearancePreferenceDialog : PreferenceDialogFragmentCompat() {
    companion object {
        private const val REQUEST_KEY = "appearance_preference_dialog.request_key"

        fun newInstance(key: String, requestCode: String): AppearancePreferenceDialog {
            val args = Bundle(2)
            args.putString(ARG_KEY, key)
            args.putString(REQUEST_KEY, requestCode)
            val fragment = AppearancePreferenceDialog()
            fragment.arguments = args
            return fragment
        }
    }

    private var modeChooser: RadioGroup? = null
    private var blackNight: CheckBox? = null
    private var systemColorAccents: CheckBox? = null

    private var restoredMode: Int? = null
    private var restoredBlackNight: Boolean? = null
    private var restoredSystemColorAccents: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
//        Log.v("Tuner","ReferenceNotePreferenceDialog.onCreate: $preference (setting)")
//
        // restore saved state
        if (savedInstanceState != null) {
            restoredMode = if (savedInstanceState.containsKey("mode"))
                savedInstanceState.getInt("mode")
            else
                null
            restoredBlackNight = if (savedInstanceState.containsKey("blackNightEnabled"))
                savedInstanceState.getBoolean("blackNightEnabled")
            else
                null
            restoredSystemColorAccents = if (savedInstanceState.containsKey("useSystemColorAccents"))
                savedInstanceState.getBoolean("useSystemColorAccents")
            else
                null
        }

        super.onCreate(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // save current settings of preference
        outState.putInt("mode", modeChooser?.checkedRadioButtonId ?: R.id.auto)
        outState.putBoolean("blackNightEnabled", blackNight?.isChecked ?: false)
        outState.putBoolean("useSystemColorAccents", systemColorAccents?.isChecked ?: true)
        super.onSaveInstanceState(outState)
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        modeChooser = view.findViewById(R.id.appearance_mode)
        blackNight = view.findViewById(R.id.black_night_mode)
        systemColorAccents = view.findViewById(R.id.system_color_accents)

        when (preference) {
            is AppearancePreference -> {
                if (restoredMode == null)
                    restoredMode = nightModeIDToResourceId((preference as AppearancePreference).value.mode)
                if (restoredBlackNight == null)
                    restoredBlackNight = (preference as AppearancePreference).value.blackNightEnabled
                if (restoredSystemColorAccents == null)
                    restoredSystemColorAccents = (preference as AppearancePreference).value.useSystemColorAccents
            }
        }

        modeChooser?.check(restoredMode ?: R.id.auto)
        blackNight?.isChecked = restoredBlackNight ?: false
        systemColorAccents?.isChecked = restoredSystemColorAccents ?: true

        if (Build.VERSION.SDK_INT < 31) // color accents are supported only starting fom API 31 / android 12
            systemColorAccents?.visibility = View.GONE

        modeChooser?.setOnCheckedChangeListener { group, checkedId ->
            val mode = nightModeResourceIdToID(checkedId)
            val blackNightEnabled = blackNight?.isChecked ?: false
            val useSystemColorAccents = systemColorAccents?.isChecked ?: true
            (preference as AppearancePreference).setValueFromData(mode, blackNightEnabled, useSystemColorAccents)
        }
        blackNight?.setOnCheckedChangeListener { buttonView, blackNightEnabled ->
            val mode = nightModeResourceIdToID(modeChooser?.checkedRadioButtonId ?: R.id.auto)
            val useSystemColorAccents = systemColorAccents?.isChecked ?: true
            (preference as AppearancePreference).setValueFromData(mode, blackNightEnabled, useSystemColorAccents)
        }
        systemColorAccents?.setOnCheckedChangeListener { buttonView, useSystemColorAccents ->
            val mode = nightModeResourceIdToID(modeChooser?.checkedRadioButtonId ?: R.id.auto)
            val blackNightEnabled = blackNight?.isChecked ?: false
            (preference as AppearancePreference).setValueFromData(mode, blackNightEnabled, useSystemColorAccents)
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {

    }
}

class AppearancePreference(context: Context, attrs: AttributeSet?)
    : DialogPreference(context, attrs, R.attr.dialogPreferenceStyle) {

    fun interface OnAppearanceChangedListener {
        fun onPreferenceChanged(preference: AppearancePreference, value: Value, modeChanged: Boolean, blackNightChanged: Boolean, useSystemColorsChanged: Boolean)
    }

    private var onAppearanceChangedListener: OnAppearanceChangedListener? = null

    fun setOnAppearanceChangedListener(onAppearanceChangedListener: OnAppearanceChangedListener?) {
        this.onAppearanceChangedListener = onAppearanceChangedListener
    }

    data class Value(var mode: Int, var blackNightEnabled: Boolean, var useSystemColorAccents: Boolean) {
        override fun toString(): String {
            var result = nightModeIDToString(mode)
            if (blackNightEnabled)
                result += " blackNightEnabled"
            if (!useSystemColorAccents)
                result += " noSystemColorAccents"
            return result
        }

        fun fromString(string: String?) {
            if (string == null)
                return
            val values = string.split(" ")
            blackNightEnabled = values.contains("blackNightEnabled")
            useSystemColorAccents = !(values.contains("noSystemColorAccents"))
            val modeString = if (values.contains("dark"))
                "dark"
            else if (values.contains("light"))
                "light"
            else
                "auto"
            mode = nightModeStringToID(modeString)
        }
    }
    var value = Value(nightModeStringToID("auto"),
        blackNightEnabled = false,
        useSystemColorAccents = true
    )
        private set

    init {
        dialogLayoutResource = R.layout.appearance_preference
        setPositiveButtonText(R.string.done)
        negativeButtonText = null
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any? {
//       Log.v("Tuner", "ReferenceNotePreference.onGetDefaultValue: ${a.getString(index)}")
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
        val newValue = Value(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, blackNightEnabled = false, useSystemColorAccents = true)
        newValue.fromString(value)
        val modeChanged = (this.value.mode != newValue.mode)
        val blackNightChanged = (this.value.blackNightEnabled != newValue.blackNightEnabled)
        val useSystemColorsChanged = (this.value.useSystemColorAccents != newValue.useSystemColorAccents)
        this.value = newValue
        persistString(value)
//        Log.v("Tuner", "ReferenceNotePreference.onSetValueFromString: $value, f=${this.value.frequency}, t=${this.value.toneIndex}")
        onAppearanceChangedListener?.onPreferenceChanged(this, this.value, modeChanged, blackNightChanged, useSystemColorsChanged)
    }

    fun setValueFromData(mode: Int, blackNightEnabled: Boolean, useSystemColorAccents: Boolean) {
        val modeChanged = (mode != value.mode)
        val blackNightChanged = (blackNightEnabled != value.blackNightEnabled)
        val useSystemColorsChanged = (useSystemColorAccents != value.useSystemColorAccents)
        value.mode = mode
        value.blackNightEnabled = blackNightEnabled
        value.useSystemColorAccents = useSystemColorAccents
        persistString(value.toString())

        onAppearanceChangedListener?.onPreferenceChanged(this, this.value, modeChanged, blackNightChanged, useSystemColorsChanged)
    }
}