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
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import kotlin.experimental.and

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

var con_width = 2244
var con_height = 2244

lateinit var g_med: MediaProjection

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
            mScreenWidth, mScreenWidth,
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
//iniAudio()
        var ser = ServerSocketChannel.open()
        var add = InetSocketAddress("0.0.0.0", 8899)
        ser.bind(add)
        var maxWaitTime = 5000
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
                if (rr != 16) {
                    cli.close()
                    continue
                }
                jj.flip()
                jj.order(ByteOrder.nativeOrder())
                var xbegin = jj.getInt()
                var ybegin = jj.getInt()
                var xlen = jj.getInt()
                var ylen = jj.getInt()


                var bitmap = startCapture()
                upper.runOnUiThread({ upper.showImg(bitmap) })
                val imageSize = bitmap.rowBytes * bitmap.height
                val uncompressedBuffer =
                    ByteBuffer.allocateDirect(imageSize)
                bitmap.copyPixelsToBuffer(uncompressedBuffer)
                uncompressedBuffer.position(0)
                var bb = ByteBuffer.allocate(xlen * ylen * 4)
                for (j in ybegin until ybegin + ylen) {
                    var startPos = xbegin + j * 2244
                    var endPos = xbegin + j * 2244 + xlen
                    bb.put(
                        uncompressedBuffer.array().slice(startPos * 4 until endPos * 4)
                            .toByteArray()
                    )
                }
                bb.flip()

                //      var bb=ByteBuffer.wrap(ByteArray(xlen*ylen*4))
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


}

class MainActivity : AppCompatActivity() {
    lateinit var ii: Intent
    var worker = thr()
    lateinit var recorder: AudioRecord
    var RECORDER_SAMPLERATE = 44100;//44.1k
    var RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
    var RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    var BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    var BytesPerElement = 2; // 2 bytes in 16bit format

    var soundData = ShortArray(1024 * 1024 * 6)
    var dataLen = 0
    var soundData_b = ShortArray(1024 * 1024 * 6)
    var dataLen_b = 0
    lateinit var audio: AudioTrack
    var hasStop = false
    var fixed = false
    var fixV = 0
    var cat = true
    var firstCat = true
    var catB = 0
    var catE = 0

    fun writeAudioDataToFile(i: Int) {
        dataLen = 0
        while (true) {
            if (hasStop) {
                hasStop = false
                break
            }
            recorder.read(soundData, dataLen, BufferElements2Rec)
            dataLen += BufferElements2Rec

            if (dataLen > 1024 * 1024 * 5)
                break
        }

        recorder.stop()
        recorder.release()
        this.runOnUiThread {
            recordOver()
        }
    }

    fun iniAudio(i: Int) {
        try {
            var con = AudioPlaybackCaptureConfiguration.Builder(g_med)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .build();
            if (i == 1) {
                recorder = AudioRecord.Builder().setAudioPlaybackCaptureConfig(con).setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(RECORDER_SAMPLERATE)
                        .setChannelMask(RECORDER_CHANNELS)
                        .build()
                )
                    .setBufferSizeInBytes(BufferElements2Rec * BytesPerElement)
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
                    .setBufferSizeInBytes(BufferElements2Rec * BytesPerElement)
                    .build();
            }

            recorder.startRecording();
            var tt = Thread(Runnable { writeAudioDataToFile(i); })
            tt.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun doPlay(j: Float) {
        var dataLen2 = dataLen
        var soundData2 = soundData.slice(0 until dataLen).toShortArray()
        var co = (j * dataLen2).toInt()
        var writeLen = 1024
        while (true) {
            if (hasStop) {
                hasStop = false
                break
            }
            if (co + writeLen > dataLen2)
                break
            var bb = soundData2.slice(co until co + writeLen).toShortArray()
//            for (i in bb)
//            {
//                if(i>1000.toShort())
//                {
//                  hasStop = false
//                }
//            }
            var d = util.short2byte(bb)
            audio.write(d, 0, writeLen * 2)
            co += writeLen
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

        var bufsize = AudioTrack.getMinBufferSize(
            (44100 * i).toInt(),
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        );

        audio = AudioTrack(
            AudioManager.STREAM_MUSIC,
            (44100 * i).toInt(), //sample rate
            AudioFormat.CHANNEL_OUT_STEREO, //2 channel
            AudioFormat.ENCODING_PCM_16BIT, // 16-bit
            bufsize,
            AudioTrack.MODE_STREAM
        );
        audio.play()
        var j =
            findViewById<SeekBar>(R.id.seekBar2).progress.toFloat() / findViewById<SeekBar>(R.id.seekBar2).max.toFloat()
        var tt = Thread(Runnable { doPlay(j); })
        tt.start()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("ServiceCast", "WrongConstant")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 1) {
            return
        }
        if (resultCode != Activity.RESULT_OK) {
            System.exit(0)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || data == null) {
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
            worker.mResultData = data.clone() as Intent
            worker.isDaemon = true
            worker.start()

        }
    }

    fun q() {
        worker.q()
        stopService(ii)
        System.exit(0)
    }

    fun showCha() {
        findViewById<TextView>(R.id.textViewcha).setText(soundData[findViewById<SeekBar>(R.id.seekBar3).progress].toString())
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        this.getSupportActionBar()?.hide();
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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
            if(firstCat)
            {
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
        }
        findViewById<Button>(R.id.btnc2).setOnClickListener {
            if (cat) {
                soundData[findViewById<SeekBar>(R.id.seekBar3).progress] =
                    (soundData[findViewById<SeekBar>(R.id.seekBar3).progress] * 2).toShort()
                showCha()
                return@setOnClickListener
            }

            for (i in catB until catE) {
                if (soundData[i] * 2 > Short.MAX_VALUE)
                    soundData[i] = Short.MAX_VALUE
                else if (soundData[i] * 2 < Short.MIN_VALUE)
                    soundData[i] = Short.MIN_VALUE
                else
                    soundData[i] = (soundData[i] * 2).toShort()
            }
            showCha()
        }
        findViewById<Button>(R.id.btnchu2).setOnClickListener {
            if (cat) {
                soundData[findViewById<SeekBar>(R.id.seekBar3).progress] =
                    (soundData[findViewById<SeekBar>(R.id.seekBar3).progress] / 2).toShort()
                showCha()
                return@setOnClickListener
            }

            for (i in catB until catE) {
                soundData[i] = (soundData[i] / 2).toShort()
            }
            showCha()
        }

        findViewById<Button>(R.id.button).setOnClickListener { q() }

        findViewById<Button>(R.id.jianyi).setOnTouchListener(
            RepeatListener(400, 30,
                object : View.OnClickListener {
                    override fun onClick(view: View?) {
                        if (cat) {
                            soundData[findViewById<SeekBar>(R.id.seekBar3).progress] =
                                (soundData[findViewById<SeekBar>(R.id.seekBar3).progress] - 1).toShort()
                            showCha()
                            return
                        }
                        for (i in catB until catE) {

                            if (soundData[i] - 1 < Short.MIN_VALUE)
                                soundData[i] = Short.MIN_VALUE
                            else
                                soundData[i] = (soundData[i] - 1).toShort()
                        }
                        showCha()
                    }
                })
        )

        findViewById<Button>(R.id.jiayi).setOnTouchListener(
            RepeatListener(400, 30,
                object : View.OnClickListener {
                    override fun onClick(view: View?) {
                        if (cat) {
                            soundData[findViewById<SeekBar>(R.id.seekBar3).progress] =
                                (soundData[findViewById<SeekBar>(R.id.seekBar3).progress] + 1).toShort()
                            showCha()
                            return
                        }
                        for (i in catB until catE) {
                            if (soundData[i] + 1 > Short.MAX_VALUE)
                                soundData[i] = Short.MAX_VALUE
                            else
                                soundData[i] = (soundData[i] + 1).toShort()
                        }
                        showCha()
                    }
                })
        )

        findViewById<Button>(R.id.stopBtn).setOnClickListener { hasStop = true }

        findViewById<Button>(R.id.playBtn).setOnClickListener {
            findViewById<Button>(R.id.playBtn).isEnabled = false
            play()
        }
        findViewById<Button>(R.id.startBtn).setOnClickListener {
            findViewById<Button>(R.id.startBtn).isEnabled = false
            findViewById<Button>(R.id.startoutBtn).isEnabled = false
            iniAudio(1)
        }

        findViewById<Button>(R.id.startoutBtn).setOnClickListener {
            findViewById<Button>(R.id.startoutBtn).isEnabled = false
            findViewById<Button>(R.id.startBtn).isEnabled = false
            iniAudio(2)
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
                if(fromUser)
                {
                    fixed = false
                    findViewById<Button>(R.id.fixbtn).setText("n")
                }
                findViewById<TextView>(R.id.seekBarText3).setText(progress.toString())
                var st = ""

                for (i in 0..80) {

                    var ss = soundData[progress + i].toString()
                    if( soundData[progress + i]>0)
                    {
                        st +=  "<font color=#ff0000 >${ss}&nbsp&nbsp&nbsp </font>"
                    }
                    else
                    {
                        st +=  "<font color=#00ff00 >${ss}&nbsp&nbsp&nbsp </font>"
                    }
                }
                findViewById<TextView>(R.id.textView).setText(Html.fromHtml(st))
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

        var projectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(projectionManager.createScreenCaptureIntent(), 1)
    }

    fun recordOver() {
        findViewById<Button>(R.id.startBtn).isEnabled = true
        findViewById<Button>(R.id.startoutBtn).isEnabled = true
        soundData_b = soundData
        dataLen_b = dataLen
        findViewById<SeekBar>(R.id.seekBar3).max = dataLen
    }

    fun showImg(b: Bitmap) {
        findViewById<ImageView>(R.id.imageView).setImageBitmap(b)
    }
}
