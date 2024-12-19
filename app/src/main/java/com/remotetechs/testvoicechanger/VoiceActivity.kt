package com.remotetechs.testvoicechanger

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arthenica.mobileffmpeg.FFmpeg
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class VoiceActivity : AppCompatActivity() {

    private lateinit var btnPlay: Button
    private lateinit var btnTest: Button
    private lateinit var seekBar: SeekBar
    private lateinit var tvStartTime: TextView
    private lateinit var tvEndTime: TextView
    private var mediaPlayer: MediaPlayer? = null
    private var audioPath: String = ""
    private lateinit var recyclerView: RecyclerView
    private lateinit var effectsAdapter: EffectsAdapter

    private val handler = Handler()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice)
        val effects = listOf(
            "Default",
            "Girl",
            "Children",
            "Boy",
            "Robot",
            "Rain"
        )
        btnPlay = findViewById(R.id.btnPlay)
        seekBar = findViewById(R.id.seekBar)
        tvStartTime = findViewById(R.id.tvStartTime)
        tvEndTime = findViewById(R.id.tvEndTime)
        btnTest = findViewById(R.id.btnTest)
        recyclerView = findViewById(R.id.rcy_affects)
        recyclerView.layoutManager = GridLayoutManager(this, 3) // Or another layout manager


        // Get the audio file path from the intent
        audioPath = intent.getStringExtra("audioPath") ?: ""
        Log.d("audio","$audioPath")

        // Initialize MediaPlayer
        mediaPlayer = MediaPlayer()
        try {
            mediaPlayer?.setDataSource(audioPath)
            mediaPlayer?.prepare()
            val duration = mediaPlayer?.duration ?: 0
            tvEndTime.text = formatTime(duration) // Set total duration in TextView
            seekBar.max = duration
        } catch (e: IOException) {
            e.printStackTrace()
        }

        btnTest.setOnClickListener {

        }

        // Adapter setup after MediaPlayer is initialized
        effectsAdapter = EffectsAdapter(effects, mediaPlayer) { selectedEffect ->
            applyEffect(selectedEffect)
        }
        recyclerView.adapter = effectsAdapter

        // Handle Play/Pause Button
        btnPlay.setOnClickListener {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                btnPlay.text = "Play"
            } else {
                mediaPlayer?.start()
                btnPlay.text = "Pause"
                startSeekBarUpdate()
            }
        }

        // Handle playback completion
        mediaPlayer?.setOnCompletionListener {
            seekBar.progress = 0
            btnPlay.text = "Play"
        }

    }

    private fun applyEffect(effect: String) {
        val inputFile = audioPath
        val outputFile = "${externalCacheDir?.absolutePath}/output_effect.mp3"

        // Ensure the output file is deleted if it exists
        val file = File(outputFile)
        if (file.exists()) {
            file.delete()
        }

        // Copy rain sound file to external cache directory
        val rainSoundPath = "${externalCacheDir?.absolutePath}/rain.mp3"
        copyRawResourceToFile(R.raw.rain, rainSoundPath)

        // Generate FFmpeg command
        val command = when (effect) {
            "Boy" -> "-y -i $inputFile -filter:a \"asetrate=44100*0.8,atempo=1.1\" $outputFile"
            "Girl" -> "-y -i $inputFile -filter:a \"asetrate=44100*1.4,atempo=0.95\" $outputFile"
            "Children" -> "-y -i $inputFile -filter:a \"asetrate=44100*1.6,atempo=0.85\" $outputFile"
            "Robot" -> "-y -i $inputFile -filter:a \"asetrate=44100*5.1,atempo=1.5,afftdn\" $outputFile"
            "Rain" -> "-y -i $inputFile -i $rainSoundPath -filter_complex \"[0:a][1:a]amix=inputs=2:duration=longest:dropout_transition=2\" $outputFile"
            "Default" -> "-y -i $inputFile -c copy $outputFile"
            else -> null
        }

        if (command != null) {
            FFmpeg.executeAsync(command) { executionId, returnCode ->
                if (returnCode == 0) {
                    Log.d("FFmpeg", "Voice effect applied successfully.")
                    try {
                        mediaPlayer?.let { player ->
                            if (player.isPlaying) {
                                player.stop()
                            }
                            player.reset()
                            player.setDataSource(outputFile)
                            player.prepare()
                            player.start()
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Log.e("FFmpeg", "Error while playing the modified file: ${e.message}")
                    }
                } else {
                    Log.e("FFmpeg", "Error applying effect. Return code: $returnCode")
                }
            }
        } else {
            Log.e("FFmpeg", "Invalid FFmpeg command!")
        }
    }

    // Utility function to copy raw resource to file
    private fun copyRawResourceToFile(rawResourceId: Int, outputPath: String) {
        val outputFile = File(outputPath)
        if (outputFile.exists()) return // Skip copying if the file already exists

        val inputStream = resources.openRawResource(rawResourceId)
        val outputStream = FileOutputStream(outputFile)

        try {
            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            inputStream.close()
            outputStream.close()
        }
    }


    // Update seek bar progress during playback
    private fun startSeekBarUpdate() {
        Thread {
            while (mediaPlayer?.isPlaying == true) {
                runOnUiThread {
                    val currentPosition = mediaPlayer?.currentPosition ?: 0
                    seekBar.progress = currentPosition
                    tvStartTime.text = formatTime(currentPosition) // Update the start time

                    // Continuously update the seek bar
                }
                Thread.sleep(100)
            }
        }.start()
    }

    // Format the time in mm:ss format
    private fun formatTime(milliseconds: Int): String {
        val seconds = (milliseconds / 1000) % 60
        val minutes = (milliseconds / 1000) / 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release the media player when done
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
