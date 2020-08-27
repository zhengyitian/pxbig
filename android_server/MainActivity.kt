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
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel

var con_width = 2244
var con_height = 2244


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
                selector.select(maxWaitTime+1000.toLong())
                selector.close()
                if (System.currentTimeMillis()-beginTime>maxWaitTime)
                {
                    cli.close()
                    continue
                }
                var jj = ByteBuffer.allocate(16)
                var rr = cli.read(jj)
                if(rr!=16)
                {
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
                    bb.put( uncompressedBuffer.array().slice(startPos * 4 until endPos * 4).toByteArray()
                    )
                }
                bb.flip()
                var si = bb.array().size
                var co=0
                selector = Selector.open()
                while (co!=si&&System.currentTimeMillis()-beginTime<maxWaitTime)
                {
                    cli.register(selector,SelectionKey.OP_WRITE)
                    selector.select(maxWaitTime+1000.toLong())
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

    @SuppressLint("ServiceCast")
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

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.button).setOnClickListener { q() }
        var projectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(projectionManager.createScreenCaptureIntent(), 1)
    }

    fun showImg(b: Bitmap) {
        findViewById<ImageView>(R.id.imageView).setImageBitmap(b)
    }
}
