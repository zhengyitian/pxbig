package com.example.sound_stream


import android.content.Context
import android.media.*
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.Math.abs
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SocketChannel


var g_stop = false
var g_cha = DoubleArray(16) { 1.0 }
var g_cha2 = DoubleArray(16) { 0.0 }
var g_inCheck = false
var g_outCheck = false

var d1_cheng = 1.0
var d1_jia = 0.0
var d2_cheng = 1.0
var d2_jia = 0.0
var d3_cheng = 1.0
var d3_jia = 0.0
var g_split = false

fun setdd(a1: Double, a2: Double, a3: Double, a4: Double, a5: Double, a6: Double) {
    d1_cheng = a1
    d1_jia = a2
    d2_cheng = a3
    d2_jia = a4
    d3_cheng = a5
    d3_jia = a6
}

fun double2short(aa: Double): Short {
    if (aa >= Short.MAX_VALUE)
        return Short.MAX_VALUE
    else if (aa <= Short.MIN_VALUE)
        return Short.MIN_VALUE
    else
        return aa.toShort()
}

var con_splitSIze = 3

class pl(
    var cheng: Double,
    var jia: Double,
    var kuai: Double,
    var onePieceTime: Double,
    var outFormat: Int
) {
    lateinit var audio: AudioTrack
    var playFre = 0.0
    var oriPos = 0
    var playPos = 0
    var totalLen = onePieceTime * playFre
    var aheadLen = (playFre - 44100) * onePieceTime
    var buf = ShortArray(0)
    var writeL = (kuai * 1024).toInt()
    var isSplit = g_split

    init {
        if (kuai > 3 || kuai < 0.3)
            kuai = 1.0
        if (isSplit) con_splitSIze = 3
        else con_splitSIze = 1

        playFre = 44100 * kuai * con_splitSIze
        totalLen = onePieceTime * playFre
        aheadLen = (playFre - 44100 * con_splitSIze) * onePieceTime
        buf = ShortArray(totalLen.toInt() + 1024 * 1024)
        writeL = (kuai * 1024 * 3).toInt()

        var bufsize = AudioTrack.getMinBufferSize(
            (playFre).toInt(),
            outFormat,
            AudioFormat.ENCODING_PCM_16BIT
        );
        audio = AudioTrack(
            AudioManager.STREAM_MUSIC,
            (playFre).toInt(), //sample rate
            outFormat, //2 channel
            AudioFormat.ENCODING_PCM_16BIT, // 16-bit
            bufsize,
            AudioTrack.MODE_STREAM
        );
        audio.play()
    }

    fun fillData(soundData: ShortArray) {
        if (!isSplit) {
            for (i in 0..1023) {
                var ab = abs(soundData[i].toInt()) % 16
                var meanV = (soundData[i] * g_cha[ab] + g_cha2[ab]) * cheng + jia
                buf[oriPos] = double2short(meanV)
                oriPos += 1
            }
            return
        }


        if (outFormat == AudioFormat.CHANNEL_OUT_MONO) {
            for (i in 0..1023) {
                var ab = abs(soundData[i].toInt()) % 16
                var meanV = (soundData[i] * g_cha[ab] + g_cha2[ab]) * cheng + jia
                buf[oriPos] = double2short(meanV * d1_cheng + d1_jia)
                oriPos += 1
                buf[oriPos] = double2short(meanV * d2_cheng + d2_jia)
                oriPos += 1
                buf[oriPos] = double2short(meanV * d3_cheng + d3_jia)
                oriPos += 1
            }
        } else {
            for (i in 0..1023) {
                if (i % 2 != 0) continue
                var ab = abs(soundData[i].toInt()) % 16
                var mean1 = (soundData[i] * g_cha[ab] + g_cha2[ab]) * cheng + jia
                var ab2 = abs(soundData[i + 1].toInt()) % 16
                var mean2 = (soundData[i + 1] * g_cha[ab] + g_cha2[ab]) * cheng + jia
                buf[oriPos] = double2short(mean1 * d1_cheng + d1_jia)
                oriPos += 1
                buf[oriPos] = double2short(mean2 * d1_cheng + d1_jia)
                oriPos += 1
                buf[oriPos] = double2short(mean1 * d2_cheng + d2_jia)
                oriPos += 1
                buf[oriPos] = double2short(mean2 * d2_cheng + d2_jia)
                oriPos += 1
                buf[oriPos] = double2short(mean1 * d3_cheng + d3_jia)
                oriPos += 1
                buf[oriPos] = double2short(mean2 * d3_cheng + d3_jia)
                oriPos += 1
            }
        }
    }

    fun inData(soundData: ShortArray) {
        if (kuai >= 1) {
            if (oriPos > totalLen) {
                oriPos = 0
                playPos = 0
            }
            fillData(soundData)
            if (oriPos >= aheadLen && playPos < totalLen) {
                var dd = buf.slice(playPos until playPos + writeL).toShortArray()
                playPos += writeL
                audio.write(util.short2byte(dd), 0, writeL * 2)
            }
        } else {
            if (playPos > totalLen) {
                oriPos = 0
                playPos = 0
            }
            if (oriPos < totalLen) {
                fillData(soundData)
            }
            var dd = buf.slice(playPos until playPos + writeL).toShortArray()
            playPos += writeL
            audio.write(util.short2byte(dd), 0, writeL * 2)
        }
    }

    fun close() {
        audio.stop()
        audio.release()
    }
}

class thr5 : Thread() {
    var cheng = 0.0
    var jia = 0.0
    var kuai = 1.0
    var onePieceTime = 6.0

    override fun run() {
        var tt = 0
        if (!g_inCheck) {
            tt = MediaRecorder.AudioSource.CAMCORDER
        } else {
            tt = MediaRecorder.AudioSource.MIC
        }


        var recorder = AudioRecord.Builder().setAudioSource(tt)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(44100)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .build()
            )
            .setBufferSizeInBytes(1024 * 2)
            .build();
        recorder.startRecording();
        var pp = pl(cheng, jia, kuai, onePieceTime, AudioFormat.CHANNEL_OUT_MONO)
        var soundData = ShortArray(1024)
        while (!g_stop) {
            recorder.read(soundData, 0, 1024)
            pp.inData(soundData)
        }
        pp.close()
        recorder.stop()
        recorder.release()
    }
}

class thr4 : Thread() {
    var i = 0
    var ip = ""
    var cheng = 0.0
    var jia = 0.0
    var kuai = 1.0
    var onePieceTime = 6.0

    override fun run() {
        var so = SocketChannel.open()
        var add = InetSocketAddress(ip, 18799)
        so.connect(add)
        so.write(ByteBuffer.allocate(i))
        var pp = pl(cheng, jia, kuai, onePieceTime, AudioFormat.CHANNEL_OUT_STEREO)
        var b = ByteBuffer.allocate(2048).order(ByteOrder.LITTLE_ENDIAN)
        var d = ShortArray(1024)
        while (!g_stop) {
            while (b.position() != 2048) {
                so.read(b)
            }
            b.flip()

            for (i in 0..1023) {
                d[i] = b.getShort()
            }
            b.clear()
            pp.inData(d)
        }
        so.close()
        pp.close()
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
        findViewById<Switch>(R.id.switch_in).setOnCheckedChangeListener { _, isChecked ->
            g_inCheck = isChecked
        }
        findViewById<Switch>(R.id.switch_split).setOnCheckedChangeListener { _, isChecked ->
            g_split = isChecked
        }
        findViewById<Switch>(R.id.switch_out).setOnCheckedChangeListener { _, isChecked ->
            g_outCheck = isChecked
            if (g_outCheck) {
                val m_amAudioManager =
                    getSystemService(Context.AUDIO_SERVICE) as AudioManager
                m_amAudioManager.setMode(AudioManager.MODE_RINGTONE or AudioManager.MODE_IN_CALL)
                m_amAudioManager.setSpeakerphoneOn(true)
            }
        }

        findViewById<Button>(R.id.btn_self).setOnClickListener {
            saveText()
            g_stop = false
            findViewById<Button>(R.id.btn_self).isEnabled = false
            var t = thr5()
            t.cheng = findViewById<EditText>(R.id.text_cheng).text.toString().toDouble()
            t.jia = findViewById<EditText>(R.id.text_jia).text.toString().toDouble()
            t.kuai = findViewById<EditText>(R.id.text_kuai).text.toString().toDouble()
            t.onePieceTime = findViewById<EditText>(R.id.text_len).text.toString().toDouble()
            t.start()
        }
        findViewById<Button>(R.id.btn_load).setOnClickListener {
            var bb = g_cha[findViewById<EditText>(R.id.text_index).text.toString().toInt()]
            var bb2 = g_cha2[findViewById<EditText>(R.id.text_index).text.toString().toInt()]
            findViewById<EditText>(R.id.text_cha).setText(bb.toString())
            findViewById<EditText>(R.id.text_cha2).setText(bb2.toString())
        }
        findViewById<Button>(R.id.btn_dd).setOnClickListener {
            setdd(
                findViewById<EditText>(R.id.dd1).text.toString().toDouble(),
                findViewById<EditText>(R.id.dd2).text.toString().toDouble(),
                findViewById<EditText>(R.id.dd3).text.toString().toDouble(),
                findViewById<EditText>(R.id.dd4).text.toString().toDouble(),
                findViewById<EditText>(R.id.dd5).text.toString().toDouble(),
                findViewById<EditText>(R.id.dd6).text.toString().toDouble()
            )
        }
        findViewById<Button>(R.id.btn_save).setOnClickListener {
            g_cha[findViewById<EditText>(R.id.text_index).text.toString().toInt()] =
                findViewById<EditText>(R.id.text_cha).text.toString().toDouble()
            g_cha2[findViewById<EditText>(R.id.text_index).text.toString().toInt()] =
                findViewById<EditText>(R.id.text_cha2).text.toString().toDouble()
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
            t.kuai = findViewById<EditText>(R.id.text_kuai).text.toString().toDouble()
            t.onePieceTime = findViewById<EditText>(R.id.text_len).text.toString().toDouble()
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
            t.kuai = findViewById<EditText>(R.id.text_kuai).text.toString().toDouble()
            t.onePieceTime = findViewById<EditText>(R.id.text_len).text.toString().toDouble()
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
