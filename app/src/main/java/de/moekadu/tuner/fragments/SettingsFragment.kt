/*
 * Copyright 2020 Michael Moessner
 *
 * This file is part of Tuner.
 *
 * Tuner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tuner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Tuner.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.moekadu.tuner.fragments

import android.content.res.Configuration
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.*
import de.moekadu.tuner.MainActivity
import de.moekadu.tuner.R
import de.moekadu.tuner.dialogs.AboutDialog
import de.moekadu.tuner.dialogs.ResetSettingsDialog
import de.moekadu.tuner.notedetection.percentToPitchHistoryDuration
import de.moekadu.tuner.preferences.*
import de.moekadu.tuner.temperaments.MusicalNotePrintOptions
import de.moekadu.tuner.temperaments.getTuningNameResourceId
import de.moekadu.tuner.temperaments.toCharSequence
import kotlin.math.pow
import kotlin.math.roundToInt

fun indexToWindowSize(index: Int): Int {
  return 2f.pow(7 + index).roundToInt()
}

fun indexToTolerance(index: Int): Int {
  return when(index) {
    0 -> 1
    1 -> 2
    2 -> 3
    3 -> 5
    4 -> 7
    5 -> 10
    6 -> 15
    7 -> 20
    else -> throw RuntimeException("Invalid index for tolerance")
  }
}

fun nightModeStringToID(string: String) = when(string){
    "dark" -> AppCompatDelegate.MODE_NIGHT_YES
    "light" -> AppCompatDelegate.MODE_NIGHT_NO
    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
}

class SettingsFragment : PreferenceFragmentCompat() {

  private var preferFlatPreference: SwitchPreferenceCompat? = null
  private var referenceNotePreference: Preference? = null
  private var temperamentPreference: Preference? = null
  private var appearancePreference: AppearancePreference? = null
//  override fun onCreate(savedInstanceState: Bundle?) {
//    super.onCreate(savedInstanceState)
//
//    setFragmentResultListener("reference_note_tag") {key, result ->
//      Log.v("Tuner", "SettingsFragment: request result: $key, $result")
//    }
//
//  }

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    setPreferencesFromResource(R.xml.preferences, rootKey)
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
      val pref = PreferenceManager.getDefaultSharedPreferences(requireContext())

      TemperamentPreferenceDialog2.setupFragmentResultListener(parentFragmentManager, viewLifecycleOwner, pref,
          requireContext(),
          printOption = {
              val preferFlat = preferFlatPreference?.isChecked ?: false
              if (preferFlat) MusicalNotePrintOptions.PreferFlat else MusicalNotePrintOptions.PreferSharp
          },
          onPreferenceChanged = {
              val preferFlat = preferFlatPreference?.isChecked ?: false
              setTemperamentAndReferenceNoteSummary(it, preferFlat)
          }
      )
      ReferenceNotePreferenceDialog2.setupFragmentResultListener(parentFragmentManager, viewLifecycleOwner, pref) {
          val preferFlat = preferFlatPreference?.isChecked ?: false
          setTemperamentAndReferenceNoteSummary(it, preferFlat)
      }

      appearancePreference = findPreference("appearance")
          ?: throw RuntimeException("No appearance preference")

      appearancePreference?.setOnAppearanceChangedListener { _, newValue, modeChanged, blackNightChanged, useSystemColorsChanged ->
          setAppearanceSummary(newValue.mode)
          AppCompatDelegate.setDefaultNightMode(newValue.mode)
          var recreate = useSystemColorsChanged
          if (blackNightChanged) {
              val uiMode = resources.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)
              if (uiMode == Configuration.UI_MODE_NIGHT_YES || uiMode == Configuration.UI_MODE_NIGHT_UNDEFINED)
                  recreate = true
          }
          if (recreate)
              (activity as MainActivity?)?.recreate()
      }
      setAppearanceSummary(appearancePreference?.value?.mode)

    val screenOnPreference = findPreference<SwitchPreferenceCompat?>("screenon")
      ?: throw RuntimeException("No screenon preference")

    screenOnPreference.setOnPreferenceChangeListener { _, newValue ->
      val screenOn = newValue as Boolean
      if (screenOn)
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      else
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      activity?.let {
          if (it is MainActivity)
              it.setStatusAndNavigationBarColors()
      }
      true
    }

    preferFlatPreference = findPreference("prefer_flat") ?: throw RuntimeException("No prefer_flat preference")
    preferFlatPreference?.setOnPreferenceChangeListener { _, newValue ->
//      Log.v("Tuner", "SettingsFragment: preferFlatPreference changed")
        setTemperamentAndReferenceNoteSummary(preferFlat = newValue as Boolean)
        true
    }

    referenceNotePreference = findPreference("reference_note") ?: throw RuntimeException("no reference_note preference")
    referenceNotePreference?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
        val preferFlat = preferFlatPreference?.isChecked ?: false
        val printOption = if (preferFlat) MusicalNotePrintOptions.PreferFlat else MusicalNotePrintOptions.PreferSharp
        val currentPrefs = TemperamentAndReferenceNoteValue.fromSharedPreferences(pref)
        val dialog = ReferenceNotePreferenceDialog2.newInstance(
            currentPrefs,
            warningMessage = null,
            printOption
        )

        dialog.show(parentFragmentManager, "tag")
        false
    }
    //setTemperamentAndReferenceNoteSummary() // This is called after temperamentPreference already

    temperamentPreference = findPreference("temperament") ?: throw RuntimeException("no temperament preference")
    temperamentPreference?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
        val preferFlat = preferFlatPreference?.isChecked ?: false
        val printOption = if (preferFlat) MusicalNotePrintOptions.PreferFlat else MusicalNotePrintOptions.PreferSharp
        val currentPrefs = TemperamentAndReferenceNoteValue.fromSharedPreferences(pref)
        val dialog = TemperamentPreferenceDialog2.newInstance(
            currentPrefs,
            printOption
        )

        dialog.show(parentFragmentManager, "tag")
        false
    }
      setTemperamentAndReferenceNoteSummary()

    val tolerance = findPreference<SeekBarPreference>("tolerance_in_cents") ?: throw RuntimeException("No tolerance preference")
    tolerance.setOnPreferenceChangeListener { preference, newValue ->
      preference.summary = getString(R.string.tolerance_summary, indexToTolerance(newValue as Int))
      true
    }
    tolerance.summary = getString(R.string.tolerance_summary,  indexToTolerance(tolerance.value))

    val numMovingAverage = findPreference<SeekBarPreference>("num_moving_average") ?: throw RuntimeException("No num_moving_average preference")
    numMovingAverage.setOnPreferenceChangeListener { preference, newValue ->
      preference.summary = resources.getQuantityString(R.plurals.num_moving_average_summary, newValue as Int, newValue)
      true
    }
    numMovingAverage.summary = resources.getQuantityString(R.plurals.num_moving_average_summary, numMovingAverage.value, numMovingAverage.value)

    val windowingFunction = findPreference<ListPreference?>("windowing")
    windowingFunction?.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()

    val windowSize = findPreference<SeekBarPreference>("window_size") ?: throw RuntimeException("No window_size preference")
    windowSize.setOnPreferenceChangeListener { preference, newValue ->
      preference.summary = getWindowSizeSummary(newValue as Int)
      true
    }
    windowSize.summary = getWindowSizeSummary(windowSize.value)

    val overlap = findPreference<SeekBarPreference?>("overlap") ?: throw RuntimeException("No overlap preference")
    overlap.setOnPreferenceChangeListener { preference, newValue ->
      preference.summary = getString(R.string.percent, newValue as Int)
      true
    }
    overlap.summary = getString(R.string.percent, overlap.value)

    val pitchHistoryDuration = findPreference<SeekBarPreference>("pitch_history_duration") ?: throw RuntimeException("No pitch history duration preference")
    pitchHistoryDuration.setOnPreferenceChangeListener { preference, newValue ->
      preference.summary = getPitchHistoryDurationSummary(newValue as Int)
      true
    }
    pitchHistoryDuration.summary = getPitchHistoryDurationSummary(pitchHistoryDuration.value)

    val maxNoise = findPreference<SeekBarPreference>("max_noise") ?: throw RuntimeException("No max noise preference")
    maxNoise.setOnPreferenceChangeListener { preference, newValue ->
      preference.summary = getMaxNoiseSummary(newValue as Int)
      true
    }
    maxNoise.summary = getMaxNoiseSummary(maxNoise.value)

    val pitchHistoryNumFaultyValues = findPreference<SeekBarPreference>("pitch_history_num_faulty_values") ?: throw RuntimeException("No pitch history num fault values preference")
    pitchHistoryNumFaultyValues.setOnPreferenceChangeListener { preference, newValue ->
      preference.summary = resources.getQuantityString(
        R.plurals.pitch_history_num_faulty_values_summary, newValue as Int, newValue
      )
      true
    }
    pitchHistoryNumFaultyValues.summary = resources.getQuantityString(
      R.plurals.pitch_history_num_faulty_values_summary,
      pitchHistoryNumFaultyValues.value, pitchHistoryNumFaultyValues.value
    )

    val resetSettings = findPreference<Preference>("setdefault") ?: throw RuntimeException("No reset settings preference")

    resetSettings.onPreferenceClickListener = Preference.OnPreferenceClickListener {
        val dialog = ResetSettingsDialog()
        dialog.show(parentFragmentManager, "tag")
        false
    }

    val aboutPreference = findPreference<Preference>("about") ?: throw RuntimeException("no about preference available")
    aboutPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
        val dialog = AboutDialog()
        dialog.show(parentFragmentManager, "tag")
        false
    }

    return super.onCreateView(inflater, container, savedInstanceState)
  }

  override fun onDisplayPreferenceDialog(preference: Preference) {
    if (parentFragmentManager.findFragmentByTag("reference_note_tag") != null)
      return

    when (preference) {
//      is ReferenceNotePreference -> {
//        val preferFlat = preferFlatPreference?.isChecked ?: false
//        val printOption = if (preferFlat) MusicalNotePrintOptions.PreferFlat else MusicalNotePrintOptions.PreferSharp
//        val temperamentType = temperamentPreference?.value?.temperamentType ?: TemperamentType.EDO12
//        val dialog = ReferenceNotePreferenceDialog.newInstance(preference.key, "reference_note_tag", temperamentType, printOption)
//        dialog.show(parentFragmentManager, "reference_note_tag")
//        dialog.setTargetFragment(this, 0)
//      }
//      is TemperamentPreference -> {
//        val preferFlat = preferFlatPreference?.isChecked ?: false
//        val printOption = if (preferFlat) MusicalNotePrintOptions.PreferFlat else MusicalNotePrintOptions.PreferSharp
//        val dialog = TemperamentPreferenceDialog.newInstance(preference.key, "temperament_tag", printOption)
//        dialog.show(parentFragmentManager, "temperament_tag")
//        dialog.setTargetFragment(this, 0)
//      }
      is AppearancePreference -> {
          val dialog = AppearancePreferenceDialog.newInstance(preference.key, "appearance_tag")
          dialog.show(parentFragmentManager, "appearance_tag")
          dialog.setTargetFragment(this, 0)
      }
      else -> super.onDisplayPreferenceDialog(preference)
    }
  }

  override fun onResume() {
      super.onResume()
      activity?.let {
          it.setTitle(R.string.settings)
          if (it is MainActivity)
              it.setStatusAndNavigationBarColors()
      }
  }

  private fun getWindowSizeSummary(windowSizeIndex: Int): String {
    val s = indexToWindowSize(windowSizeIndex)
    // the factor 2 in the next line is used since only one wave inside the window is not enough for
    // accurate frequency finding.
    return "$s " + getString(R.string.samples) + " (" + getString(R.string.minimum_frequency) + getString(R.string.hertz, 2 * 44100f / s) + ")"
  }

  private fun getPitchHistoryDurationSummary(percent: Int): String {
    val s = percentToPitchHistoryDuration(percent)
    return getString(R.string.seconds, s)
  }

  private fun getMaxNoiseSummary(percent: Int): String {
    return getString(R.string.max_noise_summary, percent)
  }

  private fun setAppearanceSummary(mode: Int?) {
      val summary = when (mode) {
          AppCompatDelegate.MODE_NIGHT_YES -> getString(R.string.dark_appearance)
          AppCompatDelegate.MODE_NIGHT_NO -> getString(R.string.light_appearance)
          else -> getString(R.string.system_appearance)
      }
      appearancePreference?.summary = summary
  }

  private fun setTemperamentAndReferenceNoteSummary(preferenceValue: TemperamentAndReferenceNoteValue? = null, preferFlat: Boolean? = null) {
//    Log.v("Tuner", "SettingsFragment.setReferenceNoteSummary: frequency=$frequency, toneIndex=$toneIndex, preferFlat=$preferFlat, f2=${referenceNotePreference?.value?.frequency}, t2=${referenceNotePreference?.value?.toneIndex}")
    context?.let { ctx ->
        val pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val currentPrefs = preferenceValue ?: TemperamentAndReferenceNoteValue.fromSharedPreferences(pref)
        val pF = preferFlat ?: (preferFlatPreference?.isChecked ?: false)
        val printOption = if (pF) MusicalNotePrintOptions.PreferFlat else MusicalNotePrintOptions.PreferSharp

        val referenceNoteSummary = SpannableStringBuilder().append(currentPrefs.referenceNote.toCharSequence(ctx, printOption, true))
            .append(" = ${getString(R.string.hertz_str, currentPrefs.referenceFrequency)}")
        referenceNotePreference?.summary = referenceNoteSummary
        temperamentPreference?.summary = ctx.getString(
            R.string.tuning_summary_no_desc,
            ctx.getString(getTuningNameResourceId(currentPrefs.temperamentType)),
            currentPrefs.rootNote.toCharSequence(ctx, printOption, withOctave = false)
        )
    }
  }
}
