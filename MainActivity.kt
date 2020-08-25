package com.example.cap_scr

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import java.lang.Exception
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.ServerSocketChannel
import java.util.concurrent.locks.ReentrantLock

var g_lock = ReentrantLock()
var g_buf = ByteBuffer.allocate(0)

class thrSock : Thread() {
    var bb = ByteBuffer.allocate(0)
    @RequiresApi(Build.VERSION_CODES.N)
    override fun run()
    {
        var ser = ServerSocketChannel.open()
        var add = InetSocketAddress("0.0.0.0",8899)
        ser.bind(add)
        while (true)
        {
            try {
                var cli = ser.accept()
                var jj = ByteBuffer.allocate(16)
                var rr = cli.read(jj)
                jj.flip()
                jj.order(ByteOrder.nativeOrder())
                var xbegin = jj.getInt()
                var ybegin = jj.getInt()
                var xlen = jj.getInt()
                var ylen = jj.getInt()
                g_lock.lock()
                bb = ByteBuffer.allocate(xlen*ylen*4)
                for(j in ybegin until ybegin+ylen)
                {
                    var startPos = xbegin+j*2244
                    var endPos = xbegin+j*2244+xlen
                    bb.put(g_buf.array().slice(startPos*4 until endPos*4).toByteArray())
                }
                g_lock.unlock()
                println(cli)
                bb.flip()
                cli.write(bb)
                cli.close()
            }

            catch (e:Exception)
            {
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
    var mImageReader: ImageReader? = null
    var mScreenWidth = 0
    var mScreenHeight = 0
    var mScreenDensity = 0
    lateinit var bitmap: Bitmap

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
    private fun startCapture() {
        var image: Image? = null
        var co = 0
        while (true) {
            image = mImageReader!!.acquireLatestImage()
            if (image != null) {
                break
            }
            Thread.sleep(10)
            co += 1
        }
        println("count ${co}")

        if (image != null) {
            bitmap = getBitmap(image)
            val imageSize = bitmap.rowBytes * bitmap.height
            val uncompressedBuffer =
                ByteBuffer.allocateDirect(imageSize)
            bitmap.copyPixelsToBuffer(uncompressedBuffer)
            uncompressedBuffer.position(0)
            image.close()
            g_lock.lock()
            g_buf = uncompressedBuffer
            g_lock.unlock()
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun run() {
        createImageReader()
        Thread.sleep(1000)
        startVirtual()
        while (true) {
            startCapture()
            upper.runOnUiThread({upper.showImg(bitmap)})
        }
    }
}


class MainActivity : AppCompatActivity() {
    lateinit var ii: Intent
    @SuppressLint("ServiceCast")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == 1000) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (data != null) {
                    ii = Intent(this@MainActivity, myServ::class.java)
                    startService(ii)
                    var tSo = thrSock()
                    tSo.isDaemon = true
                    tSo.start()
                    var t = thr()
                    t.upper = this
                    var a = getSystemService(Context.WINDOW_SERVICE) as WindowManager
                    val metrics = DisplayMetrics()
                    a.getDefaultDisplay().getMetrics(metrics)
                    t.mScreenDensity = metrics.densityDpi
                    t.mScreenWidth = metrics.widthPixels
                    t.mScreenHeight = metrics.heightPixels
                    t.mScreenHeight = 2244
                    t.mScreenWidth = 2244
                    t.mResultData = data.clone() as Intent
                    t.isDaemon = true
                    t.start()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.button).setOnClickListener { System.exit(0) }
        var projectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(projectionManager.createScreenCaptureIntent(), 1000)
    }

    fun showImg(b: Bitmap) {
        findViewById<ImageView>(R.id.imageView).setImageBitmap(b)
    }
}