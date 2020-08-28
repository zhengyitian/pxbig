package com.example.server

import android.R.attr.button
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SocketChannel


var xs = 0
var ys = 0
var xlen = 100
var xCapLen = 2244
var yCapLen = 2244
var g_reso = 0
var maxX = 800
var maxY = 800

class RepeatListener(
    initialInterval: Int, normalInterval: Int,
    clickListener: View.OnClickListener?
) : OnTouchListener {
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


class thrC : Thread() {
    lateinit var upper: MainActivity
    lateinit var ip: String
    var reso = 0;

    fun drawOne(ty: Int, v: Int, i: Int, j: Int, im: Bitmap) {
        var redCount = v * reso * reso / 3 / 255;
        var redCo = 0;
        var gap = reso / 3 * ty;

        for (ii in i * reso + gap until gap + i * reso + reso / 3) {
            for (jj in j * reso until (j + 1) * reso) {
                if (redCo >= redCount) {
                    im.setPixel(ii, jj, Color.rgb(0, 0, 0))
                    continue
                }
                if (ty == 0) {
                    im.setPixel(ii, jj, Color.rgb(255, 0, 0))

                } else if (ty == 1) {

                    im.setPixel(ii, jj, Color.rgb(0, 255, 0))

                } else if (ty == 2) {
                    im.setPixel(ii, jj, Color.rgb(0, 0, 255))
                }
                redCo += 1;
            }
        }
    }

    fun draw(ss: ByteBuffer, xl: Int, yl: Int): Bitmap {
        var pa = maxX;
        if (xl * reso < maxX)
            pa = xl * reso;
        var pb = maxY;
        if (yl * reso < maxY)
            pb = yl * reso;
        var bb = Bitmap.createBitmap(pa, pb, Bitmap.Config.ARGB_8888)

        for (j in 0 until yl) {
            for (i in 0 until xl) {

                if ((i + 1) * reso > maxX || (j + 1) * reso > maxY) {
                    ss.getInt()
                } else {
                    var r = ss.get().toInt()
                    var g = ss.get().toInt()
                    var b = ss.get().toInt()
                    ss.get()
                    if (r < 0)
                        r += 256
                    if (g < 0)
                        g += 256
                    if (b < 0)
                        b += 256
                    drawOne(0, r, i, j, bb);
                    drawOne(1, g, i, j, bb);
                    drawOne(2, b, i, j, bb);
                }
            }
        }
        return bb
    }

    override fun run() {
        while (true) {
            reso = g_reso
            var xs2 = xs
            var ys2 = ys
            var xl2 = xlen
            var xl = xl2
            var yl = xl2
            if (xl + xs2 > xCapLen) {
                xl = xCapLen - xs2
            }
            if (yl + ys2 > yCapLen) {
                yl = yCapLen - ys2
            }
            if (xl == 0 || yl == 0) {
                Thread.sleep(10)
                continue
            }
            var ss = w(xs2, ys2, xl, yl)
            if (ss.array().size == 0)
                continue

            if (reso != 0) {
                var bb = draw(ss, xl, yl)
                upper.runOnUiThread({ upper.show(bb) })
                continue
            }

            var bb = Bitmap.createBitmap(xl, yl, Bitmap.Config.ARGB_8888)
            for (j in 0 until yl) {
                for (i in 0 until xl) {
                    var r = ss.get().toInt()
                    var g = ss.get().toInt()
                    var b = ss.get().toInt()
                    ss.get()
                    if (r < 0)
                        r += 256
                    if (g < 0)
                        g += 256
                    if (b < 0)
                        b += 256
                    bb.setPixel(i, j, Color.rgb(r, g, b))
                }
            }
            upper.runOnUiThread({ upper.show(bb) })
        }
    }

    fun w(xs2: Int, ys2: Int, xl: Int, yl: Int): ByteBuffer {
        var s = SocketChannel.open()
        var add = InetSocketAddress(ip, 8899)
        try {
            s.connect(add)
            var b = ByteBuffer.allocate(16)
            b.order(ByteOrder.LITTLE_ENDIAN)

            b.putInt(xs2)
            b.putInt(ys2)

            b.putInt(xl)
            b.putInt(yl)
            b.flip()
            s.write(b)
            var ss = ByteBuffer.allocate(xl * yl * 4)
            var re = 0
            while (re != xl * yl * 4) {
                var x = s.read(ss)
                if (x == 0 && re != xl * yl * 4) {
                    s.close()
                    return return ByteBuffer.allocate(0)
                }
                re += x
            }
            s.close()
            ss.flip()
            return ss

        } catch (e: Exception) {
            e.printStackTrace()
            s.close()
            return return ByteBuffer.allocate(0)
        }
    }
}

class MainActivity : AppCompatActivity() {
    var hasStart = false

    override fun onCreate(savedInstanceState: Bundle?) {
        this.getSupportActionBar()?.hide();
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var inte = 30
        findViewById<Button>(R.id.seekBar1b1).setOnTouchListener(
            RepeatListener(400, inte,
                object : View.OnClickListener {
                    override fun onClick(view: View?) {
                        findViewById<SeekBar>(R.id.seekBar1).setProgress(findViewById<SeekBar>(R.id.seekBar1).progress - 1)
                    }
                })
        )

        findViewById<Button>(R.id.seekBar1b2).setOnTouchListener(
            RepeatListener(400, inte,
                object : View.OnClickListener {
                    override fun onClick(view: View?) {
                        findViewById<SeekBar>(R.id.seekBar1).setProgress(findViewById<SeekBar>(R.id.seekBar1).progress + 1)
                    }
                })
        )

        findViewById<Button>(R.id.seekBar2b1).setOnTouchListener(
            RepeatListener(400, inte,
                object : View.OnClickListener {
                    override fun onClick(view: View?) {
                        findViewById<SeekBar>(R.id.seekBar2).setProgress(findViewById<SeekBar>(R.id.seekBar2).progress - 1)
                    }
                })
        )

        findViewById<Button>(R.id.seekBar2b2).setOnTouchListener(
            RepeatListener(400, inte,
                object : View.OnClickListener {
                    override fun onClick(view: View?) {
                        findViewById<SeekBar>(R.id.seekBar2).setProgress(findViewById<SeekBar>(R.id.seekBar2).progress + 1)
                    }
                })
        )

        findViewById<Button>(R.id.seekBar3b1).setOnTouchListener(
            RepeatListener(400, inte,
                object : View.OnClickListener {
                    override fun onClick(view: View?) {
                        findViewById<SeekBar>(R.id.seekBar3).setProgress(findViewById<SeekBar>(R.id.seekBar3).progress - 1)
                    }
                })
        )
        findViewById<Button>(R.id.seekBar3b2).setOnTouchListener(
            RepeatListener(400, inte,
                object : View.OnClickListener {
                    override fun onClick(view: View?) {
                        findViewById<SeekBar>(R.id.seekBar3).setProgress(findViewById<SeekBar>(R.id.seekBar3).progress + 1)
                    }
                })
        )
        findViewById<Button>(R.id.seekBar4b1).setOnTouchListener(
            RepeatListener(400, inte,
                object : View.OnClickListener {
                    override fun onClick(view: View?) {
                        findViewById<SeekBar>(R.id.seekBar4).setProgress(findViewById<SeekBar>(R.id.seekBar4).progress - 1)
                    }
                })
        )
        findViewById<Button>(R.id.seekBar4b2).setOnTouchListener(
            RepeatListener(400, inte,
                object : View.OnClickListener {
                    override fun onClick(view: View?) {
                        findViewById<SeekBar>(R.id.seekBar4).setProgress(findViewById<SeekBar>(R.id.seekBar4).progress + 1)
                    }
                })
        )


        findViewById<SeekBar>(R.id.seekBar1).setOnSeekBarChangeListener(object :
            OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar,
                progress: Int,
                fromUser: Boolean
            ) {
                findViewById<TextView>(R.id.seekBar1t).setText(progress.toString())
                xs = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        findViewById<SeekBar>(R.id.seekBar2).setOnSeekBarChangeListener(object :
            OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar,
                progress: Int,
                fromUser: Boolean
            ) {
                ys = progress
                findViewById<TextView>(R.id.seekBar2t).setText(progress.toString())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        findViewById<SeekBar>(R.id.seekBar3).setOnSeekBarChangeListener(object :
            OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar,
                progress: Int,
                fromUser: Boolean
            ) {
                xlen = progress
                findViewById<TextView>(R.id.seekBar3t).setText(progress.toString())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        findViewById<SeekBar>(R.id.seekBar4).setOnSeekBarChangeListener(object :
            OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar,
                progress: Int,
                fromUser: Boolean
            ) {
                findViewById<TextView>(R.id.seekBar4t).setText(progress.toString())
                g_reso = 3 * progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        findViewById<Button>(R.id.button).setOnClickListener {
            if (hasStart) {
                System.exit(0)
            }
            xCapLen = findViewById<EditText>(R.id.xCapLen).text.toString().toInt()
            yCapLen = findViewById<EditText>(R.id.yCapLen).text.toString().toInt()
            findViewById<Button>(R.id.button).setText("stop")
            hasStart = true
            var t = thrC()
            t.ip = findViewById<EditText>(R.id.ip).text.toString()
            t.upper = this
            t.start()
            findViewById<SeekBar>(R.id.seekBar1).max = xCapLen
            findViewById<SeekBar>(R.id.seekBar2).max = yCapLen
            findViewById<SeekBar>(R.id.seekBar3).max = 200
            findViewById<SeekBar>(R.id.seekBar4).max = 20
        }

    }

    fun show(bitmap: Bitmap) {
        findViewById<ImageView>(R.id.imageView).setImageBitmap(bitmap)
    }
}