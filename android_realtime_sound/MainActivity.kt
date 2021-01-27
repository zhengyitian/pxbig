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
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock

var recordSize = 1024 * 2
var playSize = 1024 * 2
var slag = 0
var clag = 0
var sleepTime = 0

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
    var writeL = (kuai * playSize).toInt()
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
        writeL = (kuai * playSize * con_splitSIze).toInt()

        var buffersize = playSize * 2 * kuai.toInt() * con_splitSIze
        var minsize = AudioTrack.getMinBufferSize(
            (playFre).toInt(),
            outFormat,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (buffersize < minsize)
            buffersize = minsize

        audio = AudioTrack(
            AudioManager.STREAM_MUSIC,
            (playFre).toInt(), //sample rate
            outFormat, //2 channel
            AudioFormat.ENCODING_PCM_16BIT, // 16-bit
            buffersize,
            AudioTrack.MODE_STREAM
        )

        audio.play()
    }

    fun fillData(soundData: ShortArray) {
        var si = soundData.size

        if (!isSplit) {
            for (i in 0 until si) {
                var ab = abs(soundData[i].toInt()) % 16
                var meanV = (soundData[i] * g_cha[ab] + g_cha2[ab]) * cheng + jia
                buf[oriPos] = double2short(meanV)
                oriPos += 1
            }
            return
        }


        if (outFormat == AudioFormat.CHANNEL_OUT_MONO) {
            for (i in 0 until si) {
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
            for (i in 0 until si) {
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
        if (kuai > 0.99999 && kuai < 1.00001) {
            oriPos = 0
            fillData(soundData)
            var dd = buf.sliceArray(0 until writeL)
            audio.write(util.short2byte(dd), 0, writeL * 2)
        } else if (kuai >= 1) {
            if (oriPos > totalLen) {
                oriPos = 0
                playPos = 0
            }
            fillData(soundData)
            if (oriPos >= aheadLen && playPos < totalLen) {
                var dd = buf.sliceArray(playPos until playPos + writeL)
                playPos += writeL
                var re = audio.write(util.short2byte(dd), 0, writeL * 2)

            }
        } else {
            if (playPos > totalLen) {
                oriPos = 0
                playPos = 0
            }
            if (oriPos < totalLen) {
                fillData(soundData)
            }
            var dd = buf.sliceArray(playPos until playPos + writeL)
            playPos += writeL
            var re = audio.write(util.short2byte(dd), 0, writeL * 2)

        }
    }

    fun close() {
        audio.stop()
        audio.release()
    }
}

class thr5 ( var upper: MainActivity): Thread() {
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
            .setBufferSizeInBytes(recordSize * 2)
            .build();
        recorder.startRecording();
        var pp = pl(cheng, jia, kuai, onePieceTime, AudioFormat.CHANNEL_OUT_MONO)
        var soundData = ShortArray(recordSize)
        while (!g_stop) {
            var re = recorder.read(soundData, 0, recordSize)
            pp.inData(soundData)
        }
        pp.close()
        recorder.stop()
        recorder.release()
        upper.runOnUiThread {
            upper.stopback()
        }
    }
}

var cacheCount = AtomicInteger(0)

class backT(var pp: pl, var port: Int, var upper: MainActivity) : Thread() {
    var maxLag = clag * 44100 * 4
    var onesize = sleepTime * 44100 * 4 / 1000
    var b = ByteBuffer.allocate(playSize * 2).order(ByteOrder.LITTLE_ENDIAN)

    fun one(s: SocketChannel): Boolean {
        var co = 0
        while (co < onesize) {
            var re = s.read(b)
            if (re < 0)
                return false

            cacheCount.getAndAdd(re * -1)
            co += re
            if (b.position() != playSize * 2)
                continue
            b.flip()
            var bb = ShortArray(playSize)
            for (i in 0 until playSize)
                bb[i] = b.getShort()
            pp.inData(bb)
            b.clear()
        }
        return true
    }

    override fun run() {
        var s = SocketChannel.open()
        var add = InetSocketAddress("127.0.0.1", port)
        s.connect(add)

        s.configureBlocking(false)
        while (true) {
            try {
                if (cacheCount.get() < 0)
                    break
                if (cacheCount.get() > onesize) {
                    if (!one(s))
                        break
                } else {
                    Thread.sleep(sleepTime.toLong())
                }

                if (cacheCount.get() > 0.5 * 44100 * 4 + maxLag) {
                    var ss = cacheCount.get() - maxLag
                    if (ss % 2 != 0)
                        ss -= 1
                    var bb = ByteBuffer.allocate(ss)
                    while (true) {
                        var re = s.read(bb)
                        if (re < 0)
                            break

                        cacheCount.getAndAdd(re * -1)
                        if (bb.position() == ss)
                            break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                break
            }
        }
        println("backT over")
        s.close()
        pp.close()
        upper.runOnUiThread {
            upper.stopback()
        }
    }
}

class thr4(var i: Int, var upper: MainActivity) : Thread() {
    var ip = ""
    var cheng = 0.0
    var jia = 0.0
    var kuai = 1.0
    var onePieceTime = 6.0

    override fun run() {
        cacheCount.set(0)
        var so = SocketChannel.open()
        var add = InetSocketAddress(ip, 18799)
        so.connect(add)
        var bf = ByteBuffer.allocate(10).order(ByteOrder.LITTLE_ENDIAN)
        bf.putInt(i)
        bf.putInt(slag)
        bf.flip()
        so.write(bf)
        var pp = pl(cheng, jia, kuai, onePieceTime, AudioFormat.CHANNEL_OUT_STEREO)

        var s2 = ServerSocketChannel.open()
        var add2 = InetSocketAddress("0.0.0.0", 27788)
        s2.socket().bind(add2)
        var backThr = backT(pp, 27788, upper)
        backThr.isDaemon = true
        backThr.start()
        var cli2 = s2.accept()
        s2.close()
        so.configureBlocking(false)
        cli2.configureBlocking(false)
        var l = LinkedList<ByteArray>()
        try {
            while (!g_stop) {
                doone(cli2, so, l)
            }
        } catch (e: Exception) {
        }
        so.close()
        cli2.close()
        cacheCount.set(-1)
    }

    fun doone(cli: SocketChannel, cli2: SocketChannel, l: LinkedList<ByteArray>) {
        var se = Selector.open()
        if (l.isNotEmpty()) {
            cli.register(se, SelectionKey.OP_WRITE)
        }
        cli2.register(se, SelectionKey.OP_READ)

        var re = se.select(1000)

        var hasr = false
        var hasw = false

        for (k in se.selectedKeys()) {
            if (k.channel() == cli)
                hasw = true
            if (k.channel() == cli2)
                hasr = true
        }
        se.close()

        if (hasr) {
            while (true) {
                var bb = ByteBuffer.allocate(65536)
                var r = cli2.read(bb)
                if (r == 0)
                    break
                if (r == -1) {
                    Thread.sleep(10)
                    return
                }

                cacheCount.getAndAdd(r)

                bb.flip()
                var jj = bb.remaining()
                var b = ByteArray(jj)
                bb.get(b)
                l.add(b)
            }

        }

        if (hasw) {
            while (l.isNotEmpty()) {
                var one = l.pop()
                var b = ByteBuffer.wrap(one)
                var x = cli.write(b)
                if (b.remaining() == 0)
                    continue
                var bb = ByteArray(b.remaining())
                b.get(bb)
                l.addFirst(bb)
                break
            }
        }
    }
}

var orimode = 0
var orispeaker = false

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

    fun stFunc() {
        saveText()
        g_stop = false
        slag = findViewById<EditText>(R.id.text_slag).text.toString().toInt()
        clag = findViewById<EditText>(R.id.text_clag).text.toString().toInt()
        sleepTime = findViewById<EditText>(R.id.text_sleep).text.toString().toInt()
        findViewById<Button>(R.id.btn_in).isEnabled = false
        findViewById<Button>(R.id.btn_out).isEnabled = false
        findViewById<Button>(R.id.btn_self).isEnabled = false
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
            val m_amAudioManager =
                getSystemService(Context.AUDIO_SERVICE) as AudioManager
            g_outCheck = isChecked
            if (g_outCheck) {
                orimode = m_amAudioManager.mode
                orispeaker = m_amAudioManager.isSpeakerphoneOn
                m_amAudioManager.setMode(AudioManager.MODE_RINGTONE or AudioManager.MODE_IN_CALL)
                m_amAudioManager.setSpeakerphoneOn(true)
            } else {
                m_amAudioManager.setMode(orimode)
                m_amAudioManager.setSpeakerphoneOn(orispeaker)
            }
        }

        findViewById<Button>(R.id.btn_self).setOnClickListener {
            saveText()
            stFunc()
            var t = thr5(this)
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
            stFunc()
            var t = thr4(1, this)
            t.ip = findViewById<EditText>(R.id.text_ip).text.toString()
            t.cheng = findViewById<EditText>(R.id.text_cheng).text.toString().toDouble()
            t.jia = findViewById<EditText>(R.id.text_jia).text.toString().toDouble()
            t.kuai = findViewById<EditText>(R.id.text_kuai).text.toString().toDouble()
            t.onePieceTime = findViewById<EditText>(R.id.text_len).text.toString().toDouble()
            t.start()
        }
        findViewById<Button>(R.id.btn_out).setOnClickListener {
            stFunc()
            var t = thr4(2, this)
            t.ip = findViewById<EditText>(R.id.text_ip).text.toString()
            t.cheng = findViewById<EditText>(R.id.text_cheng).text.toString().toDouble()
            t.jia = findViewById<EditText>(R.id.text_jia).text.toString().toDouble()
            t.kuai = findViewById<EditText>(R.id.text_kuai).text.toString().toDouble()
            t.onePieceTime = findViewById<EditText>(R.id.text_len).text.toString().toDouble()
            t.start()
        }
        findViewById<Button>(R.id.btn_stop).setOnClickListener {
            g_stop = true

        }
        findViewById<Button>(R.id.btn_q).setOnClickListener {
            System.exit(0)
        }
    }

    fun stopback() {
        findViewById<Button>(R.id.btn_in).isEnabled = true
        findViewById<Button>(R.id.btn_out).isEnabled = true
        findViewById<Button>(R.id.btn_self).isEnabled = true
    }
}

