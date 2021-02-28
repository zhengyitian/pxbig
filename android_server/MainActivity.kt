package com.example.cap_scr

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.*
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.Html
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import java.io.*
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import java.util.zip.Deflater
import java.util.zip.Inflater
import kotlin.collections.ArrayList
import kotlin.experimental.and

//config before compile
//full_version with screen captrued and internal sound record(needs android 10),otherwise only external sound record.
var full_version = true

//screen size to capture
//huawei p20 2244 2244
//redmi8a 1520 1520
//samsung 5c 1920 1920
//redmi note9 2340
//samsung a9 star 2220

var con_width = 1920
var con_height = 1920

//end of config

var recordSize = 1024 * 10
var playSize = 1024 * 10


class RepeatListener(
    initialInterval: Int, normalInterval: Int,
    clickListener: View.OnClickListener?
) : View.OnTouchListener {
    private val handler: Handler = Handler()
    private val initialInterval: Int
    private val normalInterval: Int
    private val clickListener: View.OnClickListener
    private var touchedView: View? = null
    private val handlerRunnable: Runnable = object : Runnable {
        override fun run() {
            if (touchedView?.isEnabled()!!) {
                handler.postDelayed(this, normalInterval.toLong())
                if (clickListener != null) {
                    clickListener.onClick(touchedView)
                }
            } else {
                handler.removeCallbacks(this)
                touchedView!!.setPressed(false)
                touchedView = null
            }
        }
    }

    override fun onTouch(view: View?, motionEvent: MotionEvent): Boolean {
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                handler.removeCallbacks(handlerRunnable)
                handler.postDelayed(handlerRunnable, initialInterval.toLong())
                touchedView = view
                touchedView?.setPressed(true)
                clickListener.onClick(view)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(handlerRunnable)
                touchedView?.setPressed(false)
                touchedView = null
                return true
            }
        }
        return false
    }

    init {
        requireNotNull(clickListener) { "null runnable" }
        require(!(initialInterval < 0 || normalInterval < 0)) { "negative interval" }
        this.initialInterval = initialInterval
        this.normalInterval = normalInterval
        this.clickListener = clickListener
    }
}


lateinit var g_med: MediaProjection
var soundData_b = ShortArray(1024 * 1024 * 6)
var dataLen_b = 0
var isRecording = false

class thr4 : Thread() {
    lateinit var upper: MainActivity
    var ip = ""
    var ty = 0

    override fun run() {
        var so = SocketChannel.open()
        var add = InetSocketAddress(ip, 18899)
        so.connect(add)
        if (ty == 0) {
            var b = ByteArray(1)
            var xx = ByteBuffer.wrap(b)
            so.write(xx)
            so.close()
            return
        }
        if (ty == 1) {
            var b = ByteArray(1)
            b[0] = 1
            var xx = ByteBuffer.wrap(b)
            so.write(xx)
            so.close()
            return
        }

        if (ty == 2) {
            var b = ByteArray(1)
            b[0] = 2
            var xx = ByteBuffer.wrap(b)
            so.write(xx)
            so.close()
        }

        so = SocketChannel.open()
        so.connect(add)
        var co = 0
        var b = ByteBuffer.allocate(6 * 1024 * 1024 * 2)
        var xx = ByteBuffer.wrap(ByteArray(2))
        so.write(xx)
        var to = 0
        while (true) {
            var re = so.read(b)
            if (re <= 0)
                break
            to += re
            co += re
            if (co > 100 * 1024) {
                co = 0
                upper.runOnUiThread {
                    upper.findViewById<Button>(R.id.stop2btn).setText((to / 1204 / 100).toString())
                }
            }
        }
        so.close()
        b.order(ByteOrder.LITTLE_ENDIAN)
        b.flip()
        var ss = ShortArray(6 * 1024 * 1024)
        for (i in 0 until to / 2) {
            ss[i] = b.getShort()
        }

        upper.runOnUiThread {
            upper.findViewById<Button>(R.id.in2btn).isEnabled = true
            upper.findViewById<Button>(R.id.out2btn).isEnabled = true
            upper.findViewById<Button>(R.id.stop2btn).isEnabled = true
            upper.findViewById<Button>(R.id.stop2btn).setText("stop")
            upper.recordOver(ss, to / 2)
        }
    }
}


class backT(var recorder: AudioRecord, var port: Int) : Thread() {
    var bb = ByteArray(recordSize * 2)
    override fun run() {
        var s = SocketChannel.open()
        var add = InetSocketAddress("127.0.0.1", port)
        s.connect(add)
        while (true) {
            try {
                recorder.read(bb, 0, recordSize * 2)
                var b = ByteBuffer.wrap(bb)
                var xx = s.write(b)
            } catch (e: Exception) {
                break
            }
        }
        s.close()
        recorder.stop()
        recorder.release()
    }
}

class workThr : Thread() {
    lateinit var recorder: AudioRecord
    var RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
    var RECORDER_SAMPLERATE = 44100
    lateinit var cli: SocketChannel
    lateinit var backThr: backT
    fun serverOne(cli: SocketChannel) {
        var jj = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
        cli.read(jj)
        jj.flip()
        var i = jj.getInt()
        var soundMaxLagTime = jj.getInt()
        println("i S{i} lag ${soundMaxLagTime}")
        var xx = android.os.Build.VERSION.SDK_INT
        if (i == 1 && full_version && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            var con = AudioPlaybackCaptureConfiguration.Builder(g_med)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .build();
            recorder =
                AudioRecord.Builder().setAudioPlaybackCaptureConfig(con).setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(RECORDER_SAMPLERATE)
                        .setChannelMask(RECORDER_CHANNELS)
                        .build()
                )
                    .setBufferSizeInBytes(recordSize * 4)
                    .build();
        } else {
            recorder = AudioRecord.Builder().setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(RECORDER_SAMPLERATE)
                        .setChannelMask(RECORDER_CHANNELS)
                        .build()
                )
                .setBufferSizeInBytes(recordSize * 4)
                .build();
        }

        var s2 = ServerSocketChannel.open()
        var add2 = InetSocketAddress("0.0.0.0", 0)
        s2.bind(add2)
        var ii = s2.localAddress as InetSocketAddress
        backThr = backT(recorder, ii.port)
        backThr.isDaemon = true
        backThr.start()
        recorder.startRecording();
        var cli2 = s2.accept()
        s2.close()
        cli.configureBlocking(false)
        cli2.configureBlocking(false)
        var l = LinkedList<ByteArray>()

        try {
            while (true) {
                doone(cli, cli2, l)
            }
        } catch (e: Exception) {
        }
        cli2.close()
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

    override fun run() {
        try {
            serverOne(cli)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        cli.close()
    }
}

class thrSoundStream : Thread() {

    override fun run() {
        var ser = ServerSocketChannel.open()
        var add = InetSocketAddress("0.0.0.0", 18799)
        ser.bind(add)

        while (true) {
            var cli = ser.accept()
            var tt = workThr()
            tt.cli = cli
            tt.isDaemon = true
            tt.start()
        }
    }
}

class thr3 : Thread() {
    lateinit var upper: MainActivity
    override fun run() {
        var ser = ServerSocketChannel.open()
        var add = InetSocketAddress("0.0.0.0", 18899)
        ser.bind(add)
        var maxWaitTime = 18000
        while (true) {
            try {
                var cli = ser.accept()
                var beginTime = System.currentTimeMillis()
                cli.configureBlocking(false)
                var selector = Selector.open()
                cli.register(selector, SelectionKey.OP_READ)
                selector.select(maxWaitTime + 1000.toLong())
                selector.close()
                if (System.currentTimeMillis() - beginTime > maxWaitTime) {
                    cli.close()
                    continue
                }

                var jj = ByteBuffer.allocate(16)
                var rr = cli.read(jj)

                if (rr == 2) {
                    var ww = 0
                    while (ww < 50 && isRecording) {
                        Thread.sleep(100)
                        ww += 1
                    }
                    var b = soundData_b.sliceArray(0 until dataLen_b)
                    var bb = ByteBuffer.wrap(util.short2byte(b))
                    var si = bb.array().size
                    var co = 0
                    cli.configureBlocking(false)
                    selector = Selector.open()
                    while (co != si && System.currentTimeMillis() - beginTime < maxWaitTime) {
                        cli.register(selector, SelectionKey.OP_WRITE)
                        selector.select(maxWaitTime + 1000.toLong())
                        co += cli.write(bb)
                    }
                    selector.close()
                    cli.close()
                } else if (rr == 1) {
                    jj.flip()
                    var a = jj.get()
                    cli.close()
                    if (a == 0.toByte())
                        upper.runOnUiThread({ upper.doStartIn() })
                    if (a == 1.toByte())
                        upper.runOnUiThread({ upper.doStartOut() })
                    if (a == 2.toByte())
                        upper.runOnUiThread({ upper.doStop() })


                } else {
                    cli.close()
                    continue
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class thr : Thread() {
    lateinit var upper: MainActivity
    lateinit var mResultData: Intent
    var mMediaProjection: MediaProjection? = null
    var mVirtualDisplay: VirtualDisplay? = null
    lateinit var mImageReader: ImageReader
    var mScreenWidth = con_width
    var mScreenHeight = con_height
    var mScreenDensity = 0


    fun q() {
        stopVirtual()
        tearDownMediaProjection()
    }

    @SuppressLint("WrongConstant")
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun createImageReader() {
        mImageReader = ImageReader.newInstance(
            mScreenWidth,
            mScreenHeight,
            PixelFormat.RGBA_8888,
            2
        )
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun startVirtual() {
        if (mMediaProjection != null) {
            virtualDisplay()
        } else {
            setUpMediaProjection()
            virtualDisplay()
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun setUpMediaProjection() {
        mMediaProjection =
            getMediaProjectionManager().getMediaProjection(Activity.RESULT_OK, mResultData)
        g_med = mMediaProjection!!
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun getMediaProjectionManager(): MediaProjectionManager {
        return upper.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun virtualDisplay() {
        mVirtualDisplay = mMediaProjection!!.createVirtualDisplay(
            "screen-mirror",
            mScreenWidth, mScreenHeight,
            mScreenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mImageReader?.surface,
            null,
            null
        )
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun tearDownMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection!!.stop()
            mMediaProjection = null
        }
    }


    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun stopVirtual() {
        if (mVirtualDisplay == null) {
            return
        }
        mVirtualDisplay!!.release()
        mVirtualDisplay = null
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun getBitmap(image: Image): Bitmap {
        val width = image.width
        val height = image.height
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * width
        var bitmap =
            Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)
        bitmap = Bitmap.createBitmap(bitmap!!, 0, 0, width, height)
        return bitmap
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun startCapture(): Bitmap {
        while (true) {
            var image = mImageReader.acquireLatestImage()
            if (image == null) {
                Thread.sleep(10)
                continue
            }
            var b = getBitmap(image)
            image.close()
            return b;
        }
    }


    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun run() {
        createImageReader()
        Thread.sleep(1000)
        startVirtual()
        var ser = ServerSocketChannel.open()
        var add = InetSocketAddress("0.0.0.0", 8899)
        ser.bind(add)
        var maxWaitTime = 15000
        while (true) {
            try {
                var cli = ser.accept()
                var beginTime = System.currentTimeMillis()

                cli.configureBlocking(false)
                var selector = Selector.open()
                cli.register(selector, SelectionKey.OP_READ)
                selector.select(maxWaitTime + 1000.toLong())
                selector.close()
                if (System.currentTimeMillis() - beginTime > maxWaitTime) {
                    cli.close()
                    continue
                }
                var jj = ByteBuffer.allocate(16)
                var rr = cli.read(jj)

                if (rr == 16) {
                    jj.flip()
                    jj.order(ByteOrder.LITTLE_ENDIAN)
                    var xbegin = jj.getInt()
                    var ybegin = jj.getInt()
                    var xlen = jj.getInt()
                    var ylen = jj.getInt()
                    var bitmap = startCapture()
                    val imageSize = bitmap.rowBytes * bitmap.height
                    val uncompressedBuffer =
                        ByteBuffer.allocateDirect(imageSize)
                    bitmap.copyPixelsToBuffer(uncompressedBuffer)
                    uncompressedBuffer.position(0)
                    var bb = ByteBuffer.allocate(xlen * ylen * 4)
                    for (j in ybegin until ybegin + ylen) {
                        var startPos = xbegin + j * con_width
                        var endPos = xbegin + j * con_width + xlen
                        bb.put(
                            uncompressedBuffer.array().sliceArray(startPos * 4 until endPos * 4)
                        )
                    }
                    bb.flip()
                    var tb = bb.array()
                    var t1 = System.currentTimeMillis()
                    var f = Deflater()
                    f.setInput(tb)
                    f.finish()
                    var tt = ByteArray(tb.size)
                    var re = f.deflate(tt)
                    //   println("${System.currentTimeMillis() - t1} ${tb.size} ${re}")
                    if (f.finished()) {
                        bb = ByteBuffer.allocate(5 + re)
                        bb.order(ByteOrder.BIG_ENDIAN)
                        bb.put(1)
                        bb.putInt(re)
                        bb.put(tt.sliceArray(0 until re))
                        bb.flip()
                    } else {
                        bb = ByteBuffer.allocate(1 + tb.size)
                        bb.put(0)
                        bb.put(tb)
                        bb.flip()
                    }

                    var si = bb.array().size
                    var co = 0
                    cli.configureBlocking(false)
                    selector = Selector.open()
                    while (co != si && System.currentTimeMillis() - beginTime < maxWaitTime) {
                        cli.register(selector, SelectionKey.OP_WRITE)
                        selector.select(maxWaitTime + 1000.toLong())
                        co += cli.write(bb)
                    }
                    selector.close()
                    cli.close()
                } else {
                    cli.close()
                    continue
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

var fftLen = 8 * 1024
fun double2short(aa: Double): Short {
    if (aa >= Short.MAX_VALUE)
        return Short.MAX_VALUE
    else if (aa <= Short.MIN_VALUE)
        return Short.MIN_VALUE
    else
        return aa.toShort()
}

class MainActivity : AppCompatActivity() {
    lateinit var ii: Intent
    var worker = thr()
    lateinit var recorder: AudioRecord
    var RECORDER_SAMPLERATE = 44100;//44.1k
    var RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
    var RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    var soundData = ShortArray(1024 * 1024 * 6)
    var dataLen = 0
    lateinit var audio: AudioTrack
    var hasStop = false
    var fixed = false
    var fixV = 0
    var cat = true
    var firstCat = true
    var catB = 0
    var catE = 0
    var lastUpTime = System.currentTimeMillis()
    var addm = true
    var fftStr = ""

    fun saveText() {
        var path: File = baseContext.filesDir
        var file: File = File(path, "ip.txt")
        var f = FileOutputStream(file)
        var b = findViewById<EditText>(R.id.ip).text.toString().toByteArray()
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
        findViewById<EditText>(R.id.ip).setText(String(ss))
    }

    fun doStartIn() {
        if (!findViewById<Button>(R.id.startBtn).isEnabled)
            return
        hasStop = false
        findViewById<Button>(R.id.startBtn).isEnabled = false
        findViewById<Button>(R.id.startoutBtn).isEnabled = false
        iniAudio(1)
    }

    fun doStartOut() {
        if (!findViewById<Button>(R.id.startoutBtn).isEnabled)
            return
        hasStop = false
        findViewById<Button>(R.id.startoutBtn).isEnabled = false
        findViewById<Button>(R.id.startBtn).isEnabled = false
        iniAudio(2)
    }

    fun doStop() {
        hasStop = true
    }

    fun writeAudioDataToFile() {
        isRecording = true
        dataLen = 0
        while (true) {
            if (hasStop) {
                hasStop = false
                break
            }
            recorder.read(soundData, dataLen, recordSize)
            dataLen += recordSize

            if (dataLen > 1024 * 1024 * 5)
                break
        }

        recorder.stop()
        recorder.release()
        this.runOnUiThread {
            recordOver(soundData, dataLen)
        }

    }

    fun iniAudio(i: Int) {
        try {
            var xx = android.os.Build.VERSION.SDK_INT
            if (i == 1 && full_version && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                var con = AudioPlaybackCaptureConfiguration.Builder(g_med)
                    .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                    .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                    .addMatchingUsage(AudioAttributes.USAGE_GAME)
                    .build();
                recorder = AudioRecord.Builder().setAudioPlaybackCaptureConfig(con).setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(RECORDER_SAMPLERATE)
                        .setChannelMask(RECORDER_CHANNELS)
                        .build()
                )
                    .setBufferSizeInBytes(recordSize * 4)
                    .build();
            } else {
                recorder = AudioRecord.Builder().setAudioSource(MediaRecorder.AudioSource.MIC)
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(RECORDER_SAMPLERATE)
                            .setChannelMask(RECORDER_CHANNELS)
                            .build()
                    )
                    .setBufferSizeInBytes(recordSize * 4)
                    .build();
            }

            recorder.startRecording();
            var tt = Thread(Runnable { writeAudioDataToFile(); })
            tt.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun doPlay(j: Float) {
        var dataLen2 = dataLen
        var soundData2 = soundData.sliceArray(0 until dataLen)
        var co = (j * dataLen2).toInt()
        while (true) {
            if (hasStop) {
                hasStop = false
                break
            }
            if (co + playSize > dataLen2)
                break

            audio.write(soundData2, co, playSize)
            co += playSize
        }
        audio.stop()
        audio.release()
        this.runOnUiThread {
            findViewById<Button>(R.id.playBtn).isEnabled = true
        }
    }

    fun dofft(ii: ShortArray, sm: Int, bi: Int): ShortArray {
        var a = Array<Complex>(ii.size) {
            Complex(ii[it].toDouble(), 0.0)
        }
        var b = FFT.fft(a)
        for (i in 0..fftLen / 2) {
            if (i in sm until bi)
                continue
            b[i] = Complex(0.0, 0.0)
            if (i != 0)
                b[fftLen - i] = Complex(0.0, 0.0)
        }
        var xx = FFT.ifft(b)
        var re = ShortArray(ii.size) { double2short(xx[it].re()) }
        return re
    }

    fun fft(a: ShortArray): ShortArray {
        var ll = fftStr.split("-")
        var le = ShortArray(fftLen)
        var ri = ShortArray(fftLen)
        for (i in 0 until fftLen) {
            le[i] = a[2 * i]
            ri[i] = a[2 * i + 1]
        }
        var fle = dofft(le, ll[0].toInt(), ll[1].toInt())
        var fri = dofft(ri, ll[2].toInt(), ll[3].toInt())
        var re = ShortArray(fftLen * 2)
        for (i in 0 until fftLen) {
            re[i * 2] = fle[i]
            re[i * 2 + 1] = fri[i]
        }
        return re
    }

    fun doPlay2(j: Float) {
        var dataLen2 = dataLen
        var startPos = (j * dataLen2).toInt()
        if (startPos % 2 != 0)
            startPos += 1
        var soundData2 = soundData.sliceArray(startPos until dataLen)
        var pos = 0

        while (true) {
            if (hasStop) {
                hasStop = false
                break
            }
            if (pos + fftLen * 2 > soundData2.size)
                break
            var jj = fft(soundData2.sliceArray(pos until pos + fftLen * 2))
            audio.write(jj, 0, jj.size)
            pos += fftLen * 2
        }
        audio.stop()
        audio.release()
        this.runOnUiThread {
            findViewById<Button>(R.id.playBtn).isEnabled = true
        }
    }

    fun play() {
        var i =
            findViewById<SeekBar>(R.id.seekBar).progress.toFloat() * 3 / findViewById<SeekBar>(R.id.seekBar).max.toFloat()
        i += 0.1.toFloat()

        var minsize = AudioTrack.getMinBufferSize(
            (44100 * i).toInt(),
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        var buffersize = playSize * 4
        if (buffersize < minsize)
            buffersize = minsize

        audio = AudioTrack(
            AudioManager.STREAM_MUSIC,
            (44100 * i).toInt(), //sample rate
            AudioFormat.CHANNEL_OUT_STEREO, //2 channel
            AudioFormat.ENCODING_PCM_16BIT, // 16-bit
            buffersize,
            AudioTrack.MODE_STREAM
        );
        audio.play()
        var j =
            findViewById<SeekBar>(R.id.seekBar2).progress.toFloat() / findViewById<SeekBar>(R.id.seekBar2).max.toFloat()
        if (fftStr.contains("-")) {
            var tt = Thread(Runnable { doPlay2(j); })
            tt.start()
        } else {
            var tt = Thread(Runnable { doPlay(j); })
            tt.start()
        }
    }


    @SuppressLint("ServiceCast", "WrongConstant")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 1) {
            return
        }
        if (resultCode != Activity.RESULT_OK) {
            System.exit(0)
        }
        var xx = Build.VERSION.SDK_INT
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || data == null) {
            System.exit(0)
        }
        if (data != null) {
            ii = Intent(this@MainActivity, myServ::class.java)
            startService(ii)
            worker.upper = this
            var a = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            a.getDefaultDisplay().getMetrics(metrics)
            worker.mScreenDensity = metrics.densityDpi
            //   worker.mScreenWidth = metrics.widthPixels
            // worker.mScreenHeight = metrics.heightPixels
            worker.mResultData = data.clone() as Intent
            worker.isDaemon = true
            worker.start()
        }
    }

    fun q() {
        if (!full_version) {
            System.exit(0)
            return
        }
        worker.q()
        stopService(ii)
        System.exit(0)
    }


    fun floorV(a: Int): Short {
        if (a > Short.MAX_VALUE)
            return Short.MAX_VALUE
        if (a < Short.MIN_VALUE)
            return Short.MIN_VALUE
        return a.toShort()
    }

    fun dealCha(met: (i: Short) -> Int) {
        if (cat) {
            soundData[findViewById<SeekBar>(R.id.seekBar3).progress] =
                floorV(met(soundData[findViewById<SeekBar>(R.id.seekBar3).progress]))
            showVal()
            return
        }

        for (i in catB..catE) {
            soundData[i] = floorV(met(soundData[i]))
        }
        showVal()
    }

    fun showVal() {
        var progress = findViewById<SeekBar>(R.id.seekBar3).progress
        progress = progress - (progress % 6)
        var st = "${progress}<br/>"
        for (i in 0 until 6 * 12) {
            var nn = soundData[progress + i].toInt()
            if (!addm && nn < 0)
                nn = nn * -1
            var ss = nn.toString()
            if (soundData[progress + i] > 0) {
                st += "<font color=#ff0000 >${ss}&nbsp&nbsp&nbsp </font>"
            } else {
                st += "<font color=#00ff00 >${ss}&nbsp&nbsp&nbsp </font>"
            }
            if ((i + 1) % 6 == 0 && i != 6 * 12 - 1)
                st += "<br/>"
        }

        findViewById<TextView>(R.id.textView).setText(Html.fromHtml(st))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        this.getSupportActionBar()?.hide();
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (full_version) {
            var t = thr3()
            t.upper = this
            t.start()
            var tx = thrSoundStream()
            tx.start()
        }
        try {
            iniText()
        } catch (e: Exception) {
        }

        findViewById<TextView>(R.id.textView).setOnTouchListener { view, motionEvent ->
            var m = motionEvent.action
            if (m == MotionEvent.ACTION_UP) {
                if (System.currentTimeMillis() - lastUpTime < 300) {
                    addm = !addm
                    showVal()
                }

                lastUpTime = System.currentTimeMillis()
            }


            true
        }
        findViewById<Button>(R.id.stop2btn).setOnClickListener {
            findViewById<Button>(R.id.stop2btn).isEnabled = false
            var t = thr4()
            t.upper = this
            t.ip = findViewById<EditText>(R.id.ip).text.toString()
            t.ty = 2
            t.start()
        }

        findViewById<Button>(R.id.in2btn).setOnClickListener {
            saveText()
            findViewById<Button>(R.id.in2btn).isEnabled = false
            findViewById<Button>(R.id.out2btn).isEnabled = false
            var t = thr4()
            t.upper = this
            t.ip = findViewById<EditText>(R.id.ip).text.toString()
            t.ty = 0
            t.start()
        }

        findViewById<Button>(R.id.out2btn).setOnClickListener {
            saveText()
            findViewById<Button>(R.id.in2btn).isEnabled = false
            findViewById<Button>(R.id.out2btn).isEnabled = false
            var t = thr4()
            t.upper = this
            t.ip = findViewById<EditText>(R.id.ip).text.toString()
            t.ty = 1
            t.start()
        }

        findViewById<Button>(R.id.fixbtn).setOnClickListener {
            fixed = !fixed
            if (fixed) {
                findViewById<Button>(R.id.fixbtn).setText("f")
                fixV =
                    findViewById<SeekBar>(R.id.seekBar3).progress - findViewById<SeekBar>(R.id.seekBar4).progress
            } else
                findViewById<Button>(R.id.fixbtn).setText("n")
        }
        findViewById<Button>(R.id.catbtn).setOnClickListener {
            cat = !cat
            if (firstCat) {
                firstCat = false
                cat = true
            }
            if (cat) {
                findViewById<Button>(R.id.catbtn).setText("ov")
                catB = findViewById<SeekBar>(R.id.seekBar3).progress
            } else {
                findViewById<Button>(R.id.catbtn).setText("cat")
                catE = findViewById<SeekBar>(R.id.seekBar3).progress
            }
            findViewById<TextView>(R.id.textViewcha).setText("${catB},${catE}")
        }
        findViewById<Button>(R.id.btnc2).setOnClickListener {
            dealCha { it * 2 }
        }
        findViewById<Button>(R.id.btnchu2).setOnClickListener {
            dealCha { it / 2 }
        }

        findViewById<Button>(R.id.button).setOnClickListener { q() }

        findViewById<Button>(R.id.jianyi).setOnTouchListener(
            RepeatListener(400, 30,
                object : View.OnClickListener {
                    override fun onClick(view: View?) {
                        dealCha { it - 1 }
                    }
                })
        )

        findViewById<Button>(R.id.jiayi).setOnTouchListener(
            RepeatListener(400, 30,
                object : View.OnClickListener {
                    override fun onClick(view: View?) {
                        dealCha { it + 1 }
                    }
                })
        )

        findViewById<Button>(R.id.jiaershi).setOnTouchListener(
            RepeatListener(400, 10,
                object : View.OnClickListener {
                    override fun onClick(view: View?) {
                        dealCha { it + 100 }
                    }
                })
        )
        findViewById<Button>(R.id.jianershi).setOnTouchListener(
            RepeatListener(400, 10,
                object : View.OnClickListener {
                    override fun onClick(view: View?) {
                        dealCha { it - 100 }
                    }
                })
        )
        findViewById<Button>(R.id.rebtn).setOnClickListener {
            soundData = soundData_b.copyOf()
            dataLen = dataLen_b
            showVal()
        }
        findViewById<Button>(R.id.stopBtn).setOnClickListener {
            doStop()
        }

        findViewById<Button>(R.id.playBtn).setOnClickListener {
            hasStop = false
            findViewById<Button>(R.id.playBtn).isEnabled = false
            fftStr = findViewById<EditText>(R.id.ip).text.toString()
            play()
        }
        findViewById<Button>(R.id.startBtn).setOnClickListener {
            doStartIn()

        }

        findViewById<Button>(R.id.startoutBtn).setOnClickListener {
            doStartOut()

        }
        findViewById<Button>(R.id.jianBtn).setOnClickListener {
            findViewById<SeekBar>(R.id.seekBar).progress -= 1
        }
        findViewById<Button>(R.id.jiaBtn).setOnClickListener {
            findViewById<SeekBar>(R.id.seekBar).progress += 1
        }
        findViewById<SeekBar>(R.id.seekBar).max = 100
        findViewById<SeekBar>(R.id.seekBar).progress = 30
        findViewById<SeekBar>(R.id.seekBar).setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar,
                progress: Int,
                fromUser: Boolean
            ) {
                findViewById<TextView>(R.id.seekBarText).setText(progress.toString())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        findViewById<SeekBar>(R.id.seekBar2).max = 100
        findViewById<SeekBar>(R.id.seekBar2).setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar,
                progress: Int,
                fromUser: Boolean
            ) {
                findViewById<TextView>(R.id.seekBarText2).setText(progress.toString())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        findViewById<SeekBar>(R.id.seekBar3).setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar,
                progress: Int,
                fromUser: Boolean
            ) {
                if (fromUser) {
                    fixed = false
                    findViewById<Button>(R.id.fixbtn).setText("n")
                }
                findViewById<TextView>(R.id.seekBarText3).setText(progress.toString())
                showVal()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        findViewById<SeekBar>(R.id.seekBar4).max = 2500
        findViewById<SeekBar>(R.id.seekBar4).setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar,
                progress: Int,
                fromUser: Boolean
            ) {
                //     findViewById<TextView>(R.id.seekBarText4).setText(progress.toString())
                if (fixed) {
                    findViewById<SeekBar>(R.id.seekBar3).setProgress(fixV + progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        findViewById<Button>(R.id.bigjianbtn).setOnTouchListener(
            RepeatListener(400, 30,
                object : View.OnClickListener {
                    override fun onClick(view: View?) {
                        findViewById<SeekBar>(R.id.seekBar4).setProgress(findViewById<SeekBar>(R.id.seekBar4).progress - 1)
                    }
                })
        )
        findViewById<Button>(R.id.bigjiabtn).setOnTouchListener(
            RepeatListener(400, 30,
                object : View.OnClickListener {
                    override fun onClick(view: View?) {
                        findViewById<SeekBar>(R.id.seekBar4).setProgress(findViewById<SeekBar>(R.id.seekBar4).progress + 1)
                    }
                })
        )
        if (full_version) {
            var projectionManager =
                getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            startActivityForResult(projectionManager.createScreenCaptureIntent(), 1)
        }

    }

    fun recordOver(d: ShortArray, l: Int) {
        soundData = d
        dataLen = l
        findViewById<Button>(R.id.startBtn).isEnabled = true
        findViewById<Button>(R.id.startoutBtn).isEnabled = true
        soundData_b = soundData.copyOf()
        dataLen_b = dataLen
        findViewById<SeekBar>(R.id.seekBar3).max = dataLen
        isRecording = false
    }

}


