package de.moekadu.tuner

import android.Manifest
import android.content.SharedPreferences
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.invoke
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import java.lang.ref.WeakReference
import kotlin.math.pow

class TunerFragment : Fragment() {

  companion object {
    const val sampleRate = 44100

    //const val processingBufferSize = 16384
    const val processingBufferSize = 4096

    //const val processingBufferSize = 8192
    const val numBufferForPostprocessing = 2

    const val minimumFrequency = 25.0f
    const val maximumFrequency = 4500.0f
  }

  private var record: AudioRecord? = null
  private var recordReader: RecordReaderThread? = null
  private val dummyAudioBuffer = FloatArray(processingBufferSize)
  // val audioTimestamp = AudioTimestamp()

  /// Buffer, where we store the audio data
  private var audioData: CircularRecordData? = null

  private var preprocessor: PreprocessorThread? = null
  private var preprocessingResults: ProcessingResultBuffer<PreprocessorThread.PreprocessingResults>? = null

  private var postprocessor: PostprocessorThread? = null
  private var postprocessingResults: ProcessingResultBuffer<PostprocessorThread.PostprocessingResults>? = null

  //val overlapFraction = 4
  private val overlapFraction = 2

  private val tuningFrequencies = TuningFrequencies()
//    private var volumeMeter: VolumeMeter? = null

  private var spectrumPlot: PlotView? = null
  private val spectrumPlotXMarks = FloatArray(FrequencyBasedPitchDetectorPrep.NUM_MAX_HARMONIC)

  private var correlationPlot: PlotView? = null
  private val correlationPlotXMarks = FloatArray(FrequencyBasedPitchDetectorPrep.NUM_MAX_HARMONIC)

//    private var frequencyText: TextView? = null

  private var pitchPlot: PlotView? = null
  private val pitchHistory = PitchHistory(150)
  private val currentPitchPoint = FloatArray(2)

  private var currentTargetPitchIndex = pitchHistory.currentEstimatedToneIndex
  private val currentPitchPlotRange = floatArrayOf(
    tuningFrequencies.getNoteFrequency(currentTargetPitchIndex) * tuningFrequencies.halfToneRatio.pow(-1.5f),
    tuningFrequencies.getNoteFrequency(currentTargetPitchIndex) * tuningFrequencies.halfToneRatio.pow(1.5f)
  )

  // the next to variable must both be either frequency based or autocorrelation based
//    private val pitchDetectorTypePrep = PreprocessorThread.PREPROCESS_FREQUENCYBASED
//    private val pitchDetectorTypePost = PostprocessorThread.POSTPROCESS_FREQUENCYBASED
  private val pitchDetectorTypePrep = PreprocessorThread.PREPROCESS_AUTOCORRELATION_BASED
  private val pitchDetectorTypePost = PostprocessorThread.POSTPROCESS_AUTOCORRELATION_BASED

  class UiHandler(private val fragment: WeakReference<TunerFragment>) : Handler() {

    override fun handleMessage(msg: Message) {
      super.handleMessage(msg)
      fragment.get()?.let {
        //Log.v("Tuner", "something finished " + it.i)
//                it.i = it.i + 1

        val obj = msg.obj
        //Log.v("Tuner", "TunerFragment.UiHandler.handleMessage : message num="+msg.what)
        when (msg.what) {
          RecordReaderThread.FINISH_WRITE -> {
            //Log.v("Tuner", "TunerFragment.UiHandler:handleMessage : write finished")
            when (obj) {
              is CircularRecordData.WriteBuffer -> it.doPreprocessing(obj)
            }
          }
          PreprocessorThread.PREPROCESSING_FINISHED -> {
            //Log.v("Tuner", "TunerFragment.UiHandler:handleMessage : preprocessing finished")
            when (obj) {
              is PreprocessorThread.ReadBufferAndProcessingResults -> it.doPostprocessing(
                obj
              )
            }
          }
          PostprocessorThread.POSTPROCESSING_FINISHED -> {
            //Log.v("Tuner", "TunerFragment.UiHandler.handleMessage : postprocessing finished")
            when (obj) {
              is PostprocessorThread.PreprocessingDataAndPostprocessingResults -> it.doVisualize(
                obj
              )
            }
          }
        }
      }
    }
  }

  private val uiHandler = UiHandler(WeakReference(this))

  private val askForRecordPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
    if(result) {
      restartTuner()
    }
    else {
      Toast.makeText(activity, "No audio recording permission is granted", Toast.LENGTH_LONG).show()
      Log.v("Tuner", "TunerFragment.onRequestPermissionsResult: No audio recording permission is granted.")
    }
  }

//  override fun onCreate(savedInstanceState: Bundle?) {
//    super.onCreate(savedInstanceState)
//  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    val view = inflater.inflate(R.layout.diagrams,container, false)

    spectrumPlot = view.findViewById(R.id.spectrum_plot)
    correlationPlot = view.findViewById(R.id.correlation_plot)

    spectrumPlot?.xRange(0f, 1760f, PlotView.NO_REDRAW)
    spectrumPlot?.setXTicks(floatArrayOf(0f, 200f, 400f, 600f, 800f, 1000f, 1200f, 1400f, 1600f), true) {
        i -> getString(R.string.hertz, i)
    }

    if(pitchDetectorTypePost == PostprocessorThread.POSTPROCESS_AUTOCORRELATION_BASED) {
      correlationPlot?.xRange(0f, 1.0f/25f, PlotView.NO_REDRAW)
      correlationPlot?.setXTicks(floatArrayOf(1/1600f, 1/200f, 1/80f, 1/50f, 1/38f, 1/30f), false) {
          i -> getString(R.string.hertz, 1/i)
      }
      correlationPlot?.setYTicks(floatArrayOf(0f), true) {""}
    }
    // spectrumPlot?.setXTickTextFormat { i -> getString(R.string.hertz, i) }
//        frequencyText = findViewById(R.id.frequency_text)

    pitchPlot = view.findViewById(R.id.pitch_plot)
    pitchPlot?.xRange(0f, 1.1f * pitchHistory.size.toFloat(), PlotView.NO_REDRAW)
    //pitchPlot?.yRange(200f, 900f, PlotView.NO_REDRAW)
    pitchPlot?.yRange(currentPitchPlotRange[0], currentPitchPlotRange[1], PlotView.NO_REDRAW)
    //pitchPlot?.setYTickTextFormat { i -> getString(R.string.hertz, i) }
    //pitchPlot?.setYMarkTextFormat { i -> getString(R.string.hertz, i) }
    val noteFrequencies = FloatArray(100) { i -> tuningFrequencies.getNoteFrequency(i - 50) }
    pitchPlot?.setYTicks(noteFrequencies, false) { frequency -> tuningFrequencies.getNoteName(frequency) }
    //pitchPlot?.setYTicks(floatArrayOf(0f,200f, 400f, 600f, 800f, 1000f,1200f, 1400f, 1600f), false) { i -> getString(R.string.hertz, i) }
    //pitchPlot?.setYMarks(floatArrayOf(440f))
    pitchPlot?.setYMarks(floatArrayOf(tuningFrequencies.getNoteFrequency(currentTargetPitchIndex))) { i ->
      tuningFrequencies.getNoteName(
        i
      )
    }
    pitchPlot?.plot(pitchHistory.getHistory())

    return view
  }

  override fun onResume() {
    super.onResume()

    askForRecordPermission(Manifest.permission.RECORD_AUDIO)
  }

  private fun restartTuner() {
    Log.v("Tuner", "Tuner.restartTuner")

    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    val windowType = when(preferences.getString("windowing", "no_window")) {
      "window_hamming" -> RealFFT.HAMMING_WINDOW
      else -> RealFFT.NO_WINDOW
    }

    stopThreadsAndRecord()

    preprocessor = PreprocessorThread(
      processingBufferSize,
      1.0f / sampleRate,
      minimumFrequency,
      maximumFrequency,
      uiHandler,
      windowType
    )
    preprocessingResults = ProcessingResultBuffer(3 + numBufferForPostprocessing) {
      PreprocessorThread.PreprocessingResults()
    }
    preprocessor?.start()

    postprocessor = PostprocessorThread(processingBufferSize, 1.0f / sampleRate, getProcessingInterval(), uiHandler)
    postprocessingResults = ProcessingResultBuffer(3) { PostprocessorThread.PostprocessingResults() }
    postprocessor?.start()

    recordReader = RecordReaderThread(uiHandler)
    recordReader?.start()

    val processingInterval = getProcessingInterval()

    //val sampleRate = AudioFormat.SAMPLE_RATE_UNSPECIFIED
    val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT)

    // Times four because size is given in bytes, but we are reading floats
    val audioRecordBufferSize = kotlin.math.max(2 * processingInterval * 4, minBufferSize)

    // it is possible, that audiorecordbuffersize should be always minBufferSize
    val localRecord = AudioRecord(
      MediaRecorder.AudioSource.MIC,
      sampleRate,
      AudioFormat.CHANNEL_IN_MONO,
      AudioFormat.ENCODING_PCM_FLOAT,
      audioRecordBufferSize
    )
    if (localRecord.state == AudioRecord.STATE_UNINITIALIZED) {
      Log.v("Tuner", "TunerFragment.startAudioRecorder: Not able to acquire audio resource")
      Toast.makeText(activity, "Not able to acquire audio resource", Toast.LENGTH_LONG).show()
    }

    var circularBufferSize = 3 * kotlin.math.max(
      processingBufferSize,
      processingInterval
    )  // Smaller size might be enough?
    if (circularBufferSize % processingInterval > 0)  // Make sure, the circularBufferSize is a multiple of the processingInterval
      circularBufferSize =
        (circularBufferSize / processingInterval + 1) * processingInterval

    audioData = CircularRecordData(circularBufferSize)

    val posNotificationState = localRecord.setPositionNotificationPeriod(processingInterval)

    if (posNotificationState != AudioRecord.SUCCESS) {
      Log.v("Tuner", "TunerFragment.startAudioRecord: Not able to set position notification period"
      )
      Toast.makeText(activity, "Not able to set position notification period", Toast.LENGTH_LONG).show()
    }

    Log.v("Tuner", "TunerFragment.startAudioRecorder: minBufferSize = $minBufferSize")
    Log.v("Tuner", "TunerFragment.startAudioRecorder: circularBufferSize = $circularBufferSize")
    Log.v("Tuner", "TunerFragment.startAudioRecorder: processingInterval = $processingInterval")
    Log.v("Tuner", "TunerFragment.startAudioRecorder: audioRecordBufferSize = $audioRecordBufferSize")

    localRecord.setRecordPositionUpdateListener(object :
      AudioRecord.OnRecordPositionUpdateListener {
      override fun onMarkerReached(recorder: AudioRecord?) {
        Log.v("Tuner", "TunerFragment.onMarkerReached")
      }

      override fun onPeriodicNotification(recorder: AudioRecord?) {
        //Log.v("Tuner", "TunerFragment.onPeriodicNotification")
        //recorder?.getTimestamp(audioTimestamp, TIMEBASE_MONOTONIC)
        //Log.v("Tuner", "TunerFragment.onPeriodicNotification : timestamp=" +audioTimestamp.framePosition)
        readAudioData(recorder)
      }
    })

    record = localRecord
//            if(record?.recordingState == AudioRecord.RECORDSTATE_RECORDING)
//                Log.v("Tuner", "TunerFragment.startRecorder:recordingState = recording")

    record?.startRecording()
  }

  override fun onPause() {

    stopThreadsAndRecord()

    super.onPause()
  }

  override fun onDestroy() {
    record?.release()
    super.onDestroy()
  }

  private fun readAudioData(recorder: AudioRecord?) {
    if(context == null)
      return

    //Log.v("Tuner", "TunerFragment.readAudioData")
    if (recorder != null) {
      if (recorder.state == AudioRecord.STATE_UNINITIALIZED) {
        return
      }

      val writeBuffer = audioData?.lockWrite(recorder.positionNotificationPeriod)

      var readToDummy = false

      if (audioData == null) {
        Log.v("Tuner", "TunerFragment.onPeriodicNotification: recordBuffer does not exist")
        readToDummy = true
      } else if (writeBuffer == null) {
        Log.v("Tuner", "TunerFragment.onPeriodicNotification: cannot acquire write buffer")
        readToDummy = true
      } else {
        val recordAndData = RecordReaderThread.RecordAndData(recorder, writeBuffer)
        val handler = recordReader?.handler
        val message = handler?.obtainMessage(RecordReaderThread.READ_DATA, recordAndData)
        if (message != null) {
          handler.sendMessage(message)
        } else {
          audioData?.unlockWrite(writeBuffer)
        }
      }

      if (readToDummy)
        recorder.read(dummyAudioBuffer, 0, recorder.positionNotificationPeriod, AudioRecord.READ_BLOCKING)
    }
  }

  /// Inhere we trigger the processing of the audio data we just read
  private fun doPreprocessing(writeBuffer: CircularRecordData.WriteBuffer) {
    if(context == null)
      return

    // Log.v("Tuner", "TunerFragment.doPreprocessing")
    val startWrite = writeBuffer.startWrite
    val endWrite = startWrite + writeBuffer.size

    audioData?.unlockWrite(writeBuffer)

    val processingInterval = getProcessingInterval()
    // Log.v("Tuner", "TunerFragment.onFinishedReadRecordData " + startWrite + " " + endWrite)

    // We might want to process audio on more than one thread in future
    for (i in startWrite + processingInterval..endWrite step processingInterval) {
      //Log.v("Tuner", "TunerFragment.onFinishReadRecordData:loop: " + i)
      val startProcessingIndex = i - processingBufferSize

      if (startProcessingIndex >= 0) {

        val readBuffer = audioData?.lockRead(startProcessingIndex, processingBufferSize)
        if (readBuffer == null) {
          Log.v("Tuner", "TunerFragment.doPreprocessing: Not able to get read access to recordBuffer"
          )
        } else {
          val results = preprocessingResults?.lockWrite()
          if (results == null) {
            Log.v("Tuner", "TunerFragment.doPreprocessing: Not able to get write access to the preprocessingResults"
            )
            audioData?.unlockRead(readBuffer)
          } else {
            Log.v("Tuner", "TunerFragment.doPreprocessing: Sending data to preprocessing thread")

            val sendObject =
              PreprocessorThread.ReadBufferAndProcessingResults(readBuffer, results)
            val handler = preprocessor?.handler
            val message = handler?.obtainMessage(pitchDetectorTypePrep, sendObject)
            if (message != null) {
              handler.sendMessage(message)
            } else {
              audioData?.unlockRead(readBuffer)
              preprocessingResults?.unlockWrite(results)
            }
          }
        }
      } else {
        Log.v("Tuner", "TunerFragment.doPreprocessing: Not enough data yet to preprocess")
      }
    }
  }

  private fun doPostprocessing(readBufferAndProcessingResults: PreprocessorThread.ReadBufferAndProcessingResults) {
    Log.v("Tuner", "TunerFragment.doPostprocessing")
    if(context == null)
      return

    audioData?.unlockRead(readBufferAndProcessingResults.readBuffer)
    val numUnlocked =
      preprocessingResults?.unlockWrite(readBufferAndProcessingResults.preprocessingResults)
        ?: 0

    for (j in numUnlocked downTo 1) {
      val prepArray =
        Array(numBufferForPostprocessing) { i -> preprocessingResults?.lockRead(i - numBufferForPostprocessing + 1 - j) }

      if (prepArray.contains(null)) {
        for (pA in prepArray)
          preprocessingResults?.unlockRead(pA)
        continue
      }

      val post = postprocessingResults?.lockWrite()

      // Make sure, that prepArray has only non-null entries and post is not null, otherwise unlock and go on
      if (post == null) {
        for (pA in prepArray)
          preprocessingResults?.unlockRead(pA)
        continue
      }
      //Log.v("Tuner", "TunerFragment.doPostprocessing : sending data to postprocessing thread")
      val sendObject =
        PostprocessorThread.PreprocessingDataAndPostprocessingResults(prepArray, post)
      val handler = postprocessor?.handler
      val message = handler?.obtainMessage(pitchDetectorTypePost, sendObject)
      if (message != null) {
        handler.sendMessage(message)
      } else {
        for (pA in prepArray)
          preprocessingResults?.unlockRead(pA)
        postprocessingResults?.unlockWrite(post)
      }
    }
  }

  private fun doVisualize(preprocessingDataAndPostprocessingResults: PostprocessorThread.PreprocessingDataAndPostprocessingResults) {
    Log.v("Tuner", "TunerFragment.doVisualize")
    if(context == null)
      return

    val prepArray = preprocessingDataAndPostprocessingResults.preprocessingResults
    val post = preprocessingDataAndPostprocessingResults.postprocessingResults

    val numUnlocked = postprocessingResults?.unlockWrite(post) ?: 0
    if (numUnlocked == 0) {
      for (pA in prepArray)
        preprocessingResults?.unlockRead(pA)
      return
    }

    val postResults = postprocessingResults?.lockRead(-1)

    if(pitchDetectorTypePrep == PreprocessorThread.PREPROCESS_FREQUENCY_BASED) {
      prepArray.last()?.frequencyBasedResults?.let { result ->
        //Log.v("Tuner", "Max level: " + result.maxValue)
//            volumeMeter?.let {
//                val minAllowedVal = 10.0f.pow(it.minValue)
//                val value = kotlin.math.max(minAllowedVal, result.maxValue)
//                val spl = kotlin.math.log10(value)
//                //Log.v("Tuner", "spl: " + spl)
//                it.volume = spl
//
//                val freq = RealFFT.getFrequency(result.idxMaxFreq, result.spectrum.size, 1.0f / sampleRate)
//                val pitchFreq = RealFFT.getFrequency(result.idxMaxPitch, result.spectrum.size, 1.0f / sampleRate)
//                Log.v("Tuner", "freq=" + freq + "   pitch=" + pitchFreq)
//            }
        val dt = 1.0f / sampleRate

        for (i in 0 until result.numLocalMaxima)
          spectrumPlotXMarks[i] = RealFFT.getFrequency(result.localMaxima[i], result.spectrum.size-2, dt)
        spectrumPlot?.setXMarks(spectrumPlotXMarks, result.numLocalMaxima, false) { i ->
          getString(R.string.hertz, i)
        }
        //spectrumPlotXMarks[0] = postResults?.frequency ?: 0f
        //spectrumPlot?.setXMarks(spectrumPlotXMarks, 1, false)
        spectrumPlot?.plot(result.frequencies, result.ampSpec)
      }
    }
    else if(pitchDetectorTypePrep == PreprocessorThread.PREPROCESS_AUTOCORRELATION_BASED) {
      prepArray.last()?.autocorrelationBasedResults?.let{ result ->
        val dt = 1.0f / sampleRate
        correlationPlotXMarks[0] = result.idxMaxPitch * dt
        correlationPlot?.setXMarks(correlationPlotXMarks, 1, false) {i ->
          getString(R.string.hertz, 1.0/i)
        }
        spectrumPlotXMarks[0] = 1.0f / correlationPlotXMarks[0]
        spectrumPlot?.setXMarks(spectrumPlotXMarks, 1, false) {i ->
          getString(R.string.hertz, i)
        }
        correlationPlot?.plot(result.times, result.correlation)
        spectrumPlot?.plot(result.frequencies, result.ampSpec)
      }
    }

    for (pA in prepArray)
      preprocessingResults?.unlockRead(pA)

    var newFrequency = 0f
    if(pitchDetectorTypePost == PostprocessorThread.POSTPROCESS_FREQUENCY_BASED) {
      postResults?.frequencyBasedResults?.let {
        newFrequency = it.frequency
      }
    }
    else if(pitchDetectorTypePost == PostprocessorThread.POSTPROCESS_AUTOCORRELATION_BASED) {
      postResults?.autocorrelationBasedResults?.let {
        newFrequency = it.frequency
      }
    }

    postprocessingResults?.unlockRead(postResults)

    if (newFrequency in minimumFrequency..maximumFrequency) {
//            Log.v("Tuner", "freqcorr=$newFrequency")
//            frequencyText?.text = "frequency: " + newFrequency + "Hz"
      val newEstimatedPitch = pitchHistory.addValue(newFrequency)

      if (currentTargetPitchIndex != newEstimatedPitch) {
        currentTargetPitchIndex = newEstimatedPitch

        // highlight new target pitch
        pitchPlot?.setYMarks(
          floatArrayOf(
            tuningFrequencies.getNoteFrequency(
              currentTargetPitchIndex
            )
          )
        ) { i -> tuningFrequencies.getNoteName(i) }

        val pitchTargetFrequency = tuningFrequencies.getNoteFrequency(currentTargetPitchIndex)
        currentPitchPlotRange[0] = pitchTargetFrequency * tuningFrequencies.halfToneRatio.pow(-1.5f)
        currentPitchPlotRange[1] = pitchTargetFrequency * tuningFrequencies.halfToneRatio.pow(1.5f)
        pitchPlot?.yRange(currentPitchPlotRange[0], currentPitchPlotRange[1], 600)
      }

      currentPitchPoint[0] = (pitchHistory.size - 1).toFloat()
      currentPitchPoint[1] = pitchHistory.getCurrentValue()
      pitchPlot?.setPoints(currentPitchPoint)
      pitchPlot?.plot(pitchHistory.getHistory())
    }
  }

  private fun getProcessingInterval(): Int {
    // overlapFraction=1 -> no overlap, overlapFraction=2 -> 50% overlap, overlapFraction=4 -> 25% overlap, ...
    return processingBufferSize / overlapFraction
  }

  private fun stopThreadsAndRecord() {
    record?.stop()
    record?.release()
    record = null

    preprocessor?.handler?.removeCallbacksAndMessages(null)
    preprocessor?.quit()
    preprocessor = null

    postprocessor?.handler?.removeCallbacksAndMessages(null)
    postprocessor?.quit()
    postprocessor = null

    recordReader?.handler?.removeCallbacksAndMessages(null)
    recordReader?.quit()
    recordReader = null
  }
}