package com.example.server

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
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
var reso = 0
var maxX = 600
var maxY = 600

class thrC : Thread() {
    lateinit var upper: MainActivity
    lateinit var ip: String

    fun drawOne(ty: Int, v: Int, i: Int, j: Int, im: Bitmap) {
        var redCount = v * reso * reso / 3 / 255;
        var redCo = 0;
        var gap = reso / 3 * ty;

        for (ii in i * reso + gap until gap + i * reso + reso / 3) {
            for (jj in j * reso until (j + 1) * reso) {
                if (redCo >= redCount) {

                    try {

                    im.setPixel(ii, jj, Color.rgb(0, 0, 0))
                    }
                    catch (e:java.lang.Exception)
                    {
                        e.printStackTrace()
                    }
                    continue
                }
                if (ty == 0) {


                    try {
                    im.setPixel(ii, jj, Color.rgb(255, 0, 0))
                    }
                    catch (e:java.lang.Exception)
                    {
                        e.printStackTrace()
                    }

                } else if (ty == 1) {


                    try {
                    im.setPixel(ii, jj, Color.rgb(0, 255, 0))

                    }
                    catch (e:java.lang.Exception)
                    {
                        e.printStackTrace()
                    }
                } else if (ty == 2) {

try {
    im.setPixel(ii, jj, Color.rgb(0, 0, 255))
}
catch (e:java.lang.Exception)
{
    e.printStackTrace()
}

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

                if ((i +2) * reso > maxX-1 || (j +2) * reso > maxY-1) {
                    ss.getInt()
                    continue
                }
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
        return bb
    }

    override fun run() {
        while (true) {
            var xs2 = xs
            var ys2 = ys
            var xl2 = xlen
            var xl = xl2
            var yl = xl2
            if (xl + xs2 + 1 > xCapLen) {
                xl = xCapLen - 1 - xs2
            }
            if (yl + ys2 + 1 > yCapLen) {
                yl = yCapLen - 1 - ys2
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
            return return ByteBuffer.allocate(0)
        }
    }
}

class MainActivity : AppCompatActivity() {
    var hasStart = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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
                reso = 3 * progress
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
            findViewById<SeekBar>(R.id.seekBar1).max = xCapLen - 1
            findViewById<SeekBar>(R.id.seekBar2).max = yCapLen - 1
            findViewById<SeekBar>(R.id.seekBar3).max = 200
            findViewById<SeekBar>(R.id.seekBar4).max = 20
        }

    }

    fun show(bitmap: Bitmap) {
        findViewById<ImageView>(R.id.imageView).setImageBitmap(bitmap)
    }
}