package com.example.server3

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import java.io.*
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.Inflater


var maxX = 1000
var maxY = 1000

//picture maxSize got from server
var showLen = 1000


//end of config

var g_spli = 255
var xs = 0
var ys = 0
var xlen = 100
var xCapLen = 2244
var yCapLen = 2244
var g_reso = 0

var pause = false
var step_g = 1
var lastShowTime = AtomicLong(System.currentTimeMillis())


class imageInfo(
    var reso: Int = 0,
    var x: Int = 0,
    var y: Int = 0,
    var xlen: Int = 0,
    var ylen: Int = 0,
    var img: Bitmap? = null,
    var b: IntArray = IntArray(0)
)

class pointItem(var x: Int, var y: Int) {
}

class thrC : Thread() {
    lateinit var upper: MainActivity
    lateinit var ip: String
    lateinit var path: File
    var lastCallTime = System.currentTimeMillis()
    var reso = 0;
    var drawInfo = HashMap<Int, ArrayList<pointItem>>()
    var maxWaitTime = 15 * 1000.toLong()


    override fun run() {
        while (true) {
            if (step_g == 0) {
                Thread.sleep(10)
                continue
            }
            if (step_g == 1) {
                Thread.sleep(1000)
            }

            var len = 500
            var bb = Bitmap.createBitmap(len, len, Bitmap.Config.ARGB_8888)
            var br = ByteArray(len * len * 4)

            var r = (-120..120).random()
            var g = (-120..120).random()
            var b = (-120..120).random()

            for (i in 0 until len) {
                for (j in 0 until len) {
                    var ind = (i * len + j) * 4
                    br[ind] = r.toByte()
                    br[ind + 1] = g.toByte()
                    br[ind + 2] = b.toByte()
                    br[ind + 3] = -1
                }
            }
            bb.copyPixelsFromBuffer(ByteBuffer.wrap(br))
            upper.runOnUiThread({ upper.show(bb) })

        }
    }
}

class MainActivity : AppCompatActivity() {
    var hasStart = false
    var stepMode = false
    var im = imageInfo()
    var lastKeyDown = System.currentTimeMillis() - 100000
    var keyDownCause = 0 //0:to 0,1:pause

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            lastKeyDown = System.currentTimeMillis() - 100000
            if (keyDownCause == 0) {
                findViewById<SeekBar>(R.id.seekBar4).progress = 0
                if (step_g > 1) {
                    step_g = 1
                } else {
                    step_g = 0
                }
            } else {
                // onPauseBtnCli()
            }
            keyDownCause = 0
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (System.currentTimeMillis() - lastKeyDown < 10000 && keyDownCause == 0) {
                keyDownCause = 1
            }
            lastKeyDown = System.currentTimeMillis()
            return true
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            findViewById<SeekBar>(R.id.seekBar4).progress = 10
            step_g++
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        this.getSupportActionBar()?.hide();
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        var t = thrC()
        t.ip = findViewById<EditText>(R.id.ip).text.toString()
        t.upper = this
        t.path = baseContext.filesDir
        t.start()

        findViewById<Button>(R.id.button).setOnClickListener {
            System.exit(0)
        }

    }


    fun show(ret: Bitmap) {
        findViewById<ImageView>(R.id.imageView).setImageBitmap(ret)
    }
}
