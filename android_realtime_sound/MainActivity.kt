package com.example.sound_stream

import android.media.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.Exception
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SocketChannel

var g_stop = false

class thr5 : Thread() {
    var cheng = 0.0
    var jia = 0.0

    override fun run() {
        var bufsize = AudioTrack.getMinBufferSize(
            (44100).toInt(),
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        );

        var audio = AudioTrack(
            AudioManager.STREAM_MUSIC,
            (44100).toInt(), //sample rate
            AudioFormat.CHANNEL_OUT_STEREO, //2 channel
            AudioFormat.ENCODING_PCM_16BIT, // 16-bit
            bufsize,
            AudioTrack.MODE_STREAM
        );
        audio.play()
        var recorder = AudioRecord.Builder().setAudioSource(MediaRecorder.AudioSource.MIC)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(44100)
                    .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(1024 * 2)
            .build();
        recorder.startRecording();
        var soundData = ShortArray(1024)
        var d = ShortArray(1024)
        while (!g_stop) {
            recorder.read(soundData, 0, 1024)
            for (i in 0..1023) {
                var aa = soundData[i] * cheng + jia
                if (aa >= Short.MAX_VALUE)
                    d[i] = Short.MAX_VALUE
                else if (aa <= Short.MIN_VALUE)
                    d[i] = Short.MIN_VALUE
                else
                    d[i] = aa.toShort()
            }
            audio.write(util.short2byte(d), 0, 2048)
        }
        audio.stop()
        audio.release()
        recorder.stop()
        recorder.release()
    }
}

class thr4 : Thread() {
    var i = 0
    var ip = ""
    var cheng = 0.0
    var jia = 0.0

    override fun run() {
        var so = SocketChannel.open()
        var add = InetSocketAddress(ip, 18799)
        so.connect(add)
        so.write(ByteBuffer.allocate(i))
        var bufsize = AudioTrack.getMinBufferSize(
            (44100).toInt(),
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        );

        var audio = AudioTrack(
            AudioManager.STREAM_MUSIC,
            (44100).toInt(), //sample rate
            AudioFormat.CHANNEL_OUT_STEREO, //2 channel
            AudioFormat.ENCODING_PCM_16BIT, // 16-bit
            bufsize,
            AudioTrack.MODE_STREAM
        );
        audio.play()
        var b = ByteBuffer.allocate(2048).order(ByteOrder.LITTLE_ENDIAN)
        var d = ShortArray(1024)
        while (!g_stop) {
            while (b.position() != 2048) {
                so.read(b)
            }
            b.flip()

            for (i in 0..1023) {
                var aa = b.getShort() * cheng + jia
                if (aa >= Short.MAX_VALUE)
                    d[i] = Short.MAX_VALUE
                else if (aa <= Short.MIN_VALUE)
                    d[i] = Short.MIN_VALUE
                else
                    d[i] = aa.toShort()
            }

            b.clear()
            audio.write(util.short2byte(d), 0, 2048)
        }
        so.close()
        audio.stop()
        audio.release()
    }
}

class MainActivity : AppCompatActivity() {
    fun saveText() {
        var path: File = baseContext.filesDir
        var file: File = File(path, "ip.txt")
        var f = FileOutputStream(file)
        var b = findViewById<EditText>(R.id.text_ip).text.toString().toByteArray()
        f.write(b)
        f.close()
    }

    fun iniText() {
        var path: File = baseContext.filesDir
        var file: File = File(path, "ip.txt")
        val f = FileInputStream(file)
        val b = ByteArray(100)
        var rr = f.read(b, 0, 100)
        var ss = b.sliceArray(0 until rr)
        f.close()
        findViewById<EditText>(R.id.text_ip).setText(String(ss))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        this.getSupportActionBar()?.hide();
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        try {
            iniText()
        } catch (e: Exception) {
        }
        findViewById<Button>(R.id.btn_self).setOnClickListener {
            saveText()
            g_stop = false
            findViewById<Button>(R.id.btn_self).isEnabled = false
            var t = thr5()
            t.cheng = findViewById<EditText>(R.id.text_cheng).text.toString().toDouble()
            t.jia = findViewById<EditText>(R.id.text_jia).text.toString().toDouble()
            t.start()
        }
        findViewById<Button>(R.id.btn_in).setOnClickListener {
            saveText()
            g_stop = false
            findViewById<Button>(R.id.btn_in).isEnabled = false
            findViewById<Button>(R.id.btn_out).isEnabled = false
            var t = thr4()
            t.i = 1
            t.ip = findViewById<EditText>(R.id.text_ip).text.toString()
            t.cheng = findViewById<EditText>(R.id.text_cheng).text.toString().toDouble()
            t.jia = findViewById<EditText>(R.id.text_jia).text.toString().toDouble()
            t.start()
        }
        findViewById<Button>(R.id.btn_out).setOnClickListener {
            saveText()
            g_stop = false
            findViewById<Button>(R.id.btn_in).isEnabled = false
            findViewById<Button>(R.id.btn_out).isEnabled = false
            var t = thr4()
            t.i = 2
            t.ip = findViewById<EditText>(R.id.text_ip).text.toString()
            t.cheng = findViewById<EditText>(R.id.text_cheng).text.toString().toDouble()
            t.jia = findViewById<EditText>(R.id.text_jia).text.toString().toDouble()
            t.start()
        }
        findViewById<Button>(R.id.btn_stop).setOnClickListener {
            g_stop = true
            findViewById<Button>(R.id.btn_in).isEnabled = true
            findViewById<Button>(R.id.btn_out).isEnabled = true
            findViewById<Button>(R.id.btn_self).isEnabled = true
        }
        findViewById<Button>(R.id.btn_q).setOnClickListener {
            System.exit(0)
        }
    }
}