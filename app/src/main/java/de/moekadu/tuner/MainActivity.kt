package de.moekadu.tuner

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    val REQUEST_AUDIO_RECORD_PERMISSION = 1001
    var record : AudioRecord ?= null
    // Next steps:
    // - Create a service which records data
    // - Callback, when a specific bunch of data is ready
    // - Lock mechanism, not to override data, which is currently copied?

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
                }
            }
        }
    }

    fun startAudioRecorder() {
        if(record == null) {
            val sampleRate = 44100
            //val sampleRate = AudioFormat.SAMPLE_RATE_UNSPECIFIED
            val bufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_FLOAT
            );
            val localRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_FLOAT,
                bufferSize
            )
            if (localRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
                Toast.makeText(this, "Not able to aquire audio resource", Toast.LENGTH_LONG).show()
            }

            val numSamples = 256
            val data = FloatArray(numSamples)
            localRecord.setPositionNotificationPeriod(numSamples)

            localRecord.setRecordPositionUpdateListener(object :
                AudioRecord.OnRecordPositionUpdateListener {
                override fun onMarkerReached(recorder: AudioRecord?) {

                }

                override fun onPeriodicNotification(recorder: AudioRecord?) {
                    // instead better call a thread which handels the reading
                    recorder?.read(data, 0, numSamples, AudioRecord.READ_BLOCKING)
                }

            })
            localRecord.startRecording()
            record = localRecord
        }
    }

    override fun onStop() {
        record?.stop()
        record?.release()
        record = null
        super.onStop()
    }
}
