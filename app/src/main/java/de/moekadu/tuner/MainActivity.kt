package de.moekadu.tuner

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.lang.ref.WeakReference
import kotlin.math.pow

class MainActivity : AppCompatActivity() {

    companion object {
        const val sampleRate = 44100
        const val REQUEST_AUDIO_RECORD_PERMISSION = 1001
        //const val processingBufferSize = 16384
        const val processingBufferSize = 4096
    }

    // TODO: next steps:
    // - The record processor must compute the spectrum and store it in the passed processing result

    var record : AudioRecord ?= null

    var recordReader : RecordReaderThread ?= null
    var recordBuffer : CircularRecordData ?= null

    var recordProcessor : RecordProcessorThread ?= null
    var processingResults : ResultBuffer ?= null

    //val overlapFraction = 4
    val overlapFraction = 2

    var volumeMeter : VolumeMeter ?= null

    var spectrumPlot : PlotView ?= null

    var i = 0

    class UiHandler(private val activity : WeakReference<MainActivity>) : Handler() {

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            activity.get()?.let {
                //Log.v("Tuner", "something finished " + it.i)
                it.i = it.i + 1

                val obj = msg.obj

                if(msg.what == RecordReaderThread.FINISHWRITE)
                {
                    when(obj) {
                        is CircularRecordData.WriteBuffer -> it.onFinishReadRecordData(obj)
                    }
                }
                else if(msg.what == RecordProcessorThread.PROCESSING_FINISHED)
                {
                    when(obj) {
                        is RecordProcessorThread.ReadBufferAndProcessingResults -> it.onFinishProcessingData(obj)
                    }
                }
            }
        }
    }
    val uiHandler = UiHandler(WeakReference(this))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        volumeMeter = findViewById(R.id.volume_meter)
        //volumeMeter?.startDelay = kotlin.math.max((2 * 1000 * processingBufferSize / overlapFraction / sampleRate).toLong(), 200L)
        spectrumPlot = findViewById(R.id.spectrum_plot)
    }

    override fun onStart() {
        super.onStart()
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_AUDIO_RECORD_PERMISSION)
        }
        else {
            startAudioRecorder()
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode) {
            REQUEST_AUDIO_RECORD_PERMISSION -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startAudioRecorder()
                }
                else {
                    Toast.makeText(this, "No audio recording permission is granted", Toast.LENGTH_LONG).show();
                    Log.v("Tuner", "MainActivity:onRequestPermissionsResult: No audio recording permission is granted.")
                }
            }
        }
    }

    fun startAudioRecorder() {
        Log.v("Tuner", "MainActivity::startAudioRecorder")

        if(recordProcessor == null) {
            recordProcessor = RecordProcessorThread(processingBufferSize, uiHandler)
            processingResults = ResultBuffer(3, processingBufferSize)
            recordProcessor?.start()
        }

        if(recordReader == null) {
            recordReader = RecordReaderThread(uiHandler)
            recordReader?.start()
        }

        if(record == null) {
            val processingInterval = getProcessingInterval()

            //val sampleRate = AudioFormat.SAMPLE_RATE_UNSPECIFIED
            val minBufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_FLOAT
            )

            // Times four because size is given in bytes, but we are reading floats
            val audioRecordBufferSize = kotlin.math.max(2*processingInterval*4, minBufferSize)

            val localRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_FLOAT,
                audioRecordBufferSize
            )
            if (localRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
                Log.v("Tuner", "MainActivity::startAudioRecorder: Not able to aquire audio resource")
                Toast.makeText(this, "Not able to acquire audio resource", Toast.LENGTH_LONG).show()
            }

            var circularBufferSize = 3 * kotlin.math.max(processingBufferSize, processingInterval)  // Smaller size might be enough?
            if(circularBufferSize % processingInterval > 0)  // Make sure, the circularBufferSize is a multiple of the processingInterval
                circularBufferSize = (circularBufferSize / processingInterval + 1) * processingInterval

            recordBuffer = CircularRecordData(circularBufferSize)

            val posNotifState = localRecord.setPositionNotificationPeriod(processingInterval)

            if(posNotifState != AudioRecord.SUCCESS) {
                Log.v(
                    "Tuner",
                    "MainActivity::startAudioRecord: Not able to set position notification period"
                )
                Toast.makeText(this, "Not able to set position notification period", Toast.LENGTH_LONG).show()
            }

            Log.v("Tuner", "MainActivity::startAudioRecorder: minBufferSize = " + minBufferSize)
            Log.v("Tuner", "MainActivity::startAudioRecorder: circularBufferSize = " + circularBufferSize)
            Log.v("Tuner", "MainActivity::startAudioRecorder: processingInterval = " + processingInterval)
            Log.v("Tuner", "MainActivity::startAudioRecorder: audioRecordBufferSize = " + audioRecordBufferSize)

            localRecord.setRecordPositionUpdateListener(object :
                AudioRecord.OnRecordPositionUpdateListener {
                override fun onMarkerReached(recorder: AudioRecord?) {
                    Log.v("Tuner", "MainActivity:onMarkerReached")
                }

                override fun onPeriodicNotification(recorder: AudioRecord?) {
                    //Log.v("Tuner", "MainActivity:onPeriodicNotification")

                    recorder?.let {
                        if(recorder.state == AudioRecord.STATE_UNINITIALIZED)
                            return

                        val writeBuffer = recordBuffer?.lockWrite(recorder.positionNotificationPeriod)

                        if(recordBuffer == null)
                            Log.v("Tuner", "MainActivity::onPeriodicNotification: recordBuffer does not exist")
                        else if(writeBuffer == null)
                            Log.v("Tuner", "MainActivity::onPeriodicNotification: cannot acquire write buffer")

                        writeBuffer?.let {
                            val recordAndData = RecordReaderThread.RecordAndData(recorder, writeBuffer)
                            val handler = recordReader?.handler
                            val message = handler?.obtainMessage(RecordReaderThread.READDATA, recordAndData)
                            message?.let {
                                handler.sendMessage(message)
                            }
                        }
                    }
                }

            })

            record = localRecord
//            if(record?.recordingState == AudioRecord.RECORDSTATE_RECORDING)
//                Log.v("Tuner", "MainActivity:startRecorder:recordingState = recording")
        }

        record?.startRecording()
    }

    override fun onStop() {

        record?.stop()
        record?.release()
        record = null

        recordProcessor?.quit()
        recordProcessor = null

        recordReader?.quit()
        recordReader = null

        super.onStop()
    }

    override fun onDestroy() {
        record?.release()
        super.onDestroy()
    }

    /// Inhere we trigger the processing of the audio data we just read
    private fun onFinishReadRecordData(writeBuffer : CircularRecordData.WriteBuffer) {
        val startWrite = writeBuffer.startWrite
        val endWrite = startWrite + writeBuffer.size

        recordBuffer?.unlockWrite(writeBuffer)

        val processingInterval = getProcessingInterval();
        // Log.v("Tuner", "MainActivity:onFinishedReadRecordData " + startWrite + " " + endWrite)

        // We might want to process audio on more than one thread in future
        for (i in startWrite+processingInterval .. endWrite step processingInterval)
        {
            //Log.v("Tuner", "MainActivity:onFinshReadRecordData:loop: " + j)
            val startProcessingIndex = i - processingBufferSize

            if(startProcessingIndex >= 0) {

                val readBuffer = recordBuffer?.lockRead(startProcessingIndex, processingBufferSize)
                if (readBuffer == null)
                    Log.v("Tuner", "MainActivity:onFinishReadRecordData: Not able to get read access to recordBuffer")

                val results = processingResults?.lockWrite()
                if (results == null)
                    Log.v("Tuner", "MainActivity:onFinishReadRecordData: Not able to get write access to the processingResults")

                readBuffer?.let {
                    results?.let {
                        val sendObject = RecordProcessorThread.ReadBufferAndProcessingResults(readBuffer, results)
                        val handler = recordProcessor?.handler
                        val message =
                            handler?.obtainMessage(RecordProcessorThread.PROCESS_AUDIO, sendObject)
                        message?.let {
                            handler.sendMessage(message)
                        }
                    }
                }
            }
        }
    }

    private fun onFinishProcessingData(readBufferAndProcessingResults: RecordProcessorThread.ReadBufferAndProcessingResults) {
        recordBuffer?.unlockRead(readBufferAndProcessingResults.readBuffer)
        val numUnlocked = processingResults?.unlockWrite(readBufferAndProcessingResults.processingResults) ?: 0

        if(numUnlocked > 0) {
            val result = processingResults?.lockRead(-1)
            result?.let {
                //Log.v("Tuner", "Max level: " + result.maxValue)
                volumeMeter?.let {
                    val minAllowedVal = 10.0f.pow(it.minValue)
                    val value = kotlin.math.max(minAllowedVal, result.maxValue)
                    val spl = kotlin.math.log10(value)
                    //Log.v("Tuner", "spl: " + spl)
                    it.volume = spl

                    val freq = result.idxMaxFreq * sampleRate / result.spectrum.size
                    Log.v("Tuner", "freq=" + freq)
                }

                spectrumPlot?.let {
                    it.plot(result.ampSpec, 0, 100)
                }
            }

            processingResults?.unlockRead(result)
        }
    }

    private fun getProcessingInterval() : Int {
        // overlapFraction=1 -> no overlap, overlapFraction=2 -> 50% overlap, overlapFraction=4 -> 25% overlap, ...
        return processingBufferSize / overlapFraction
    }
}
