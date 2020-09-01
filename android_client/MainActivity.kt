package com.example.server

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
import java.io.*
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel


var xs = 0
var ys = 0
var xlen = 100
var xCapLen = 2244
var yCapLen = 2244
var g_reso = 0
var maxX = 1000
var maxY = 1000
var pause = false

class pointItem(var x: Int, var y: Int) {
}

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

class recvThr : Thread() {
    var ip = ""
    lateinit var path: File
    fun w() {
        var add = InetSocketAddress(ip, 8898)
        var so = SocketChannel.open()
        so.connect(add)
        var b = ByteBuffer.allocate(4)
        so.read(b)
        b.flip()
        b.order(ByteOrder.LITTLE_ENDIAN)
        var i = b.getInt()
        b = ByteBuffer.allocate(300 * 100)
        var to = 0
        while (true) {
            var re = so.read(b)
            if (re <= 0)
                break
            to += re
        }
        var file: File = File(path, i.toString())
        var f = FileOutputStream(file)
        f.write(b.array(), 0, to)
        f.close()
    }

    override fun run() {
        while (true) {
            try {
                w()
            } catch (e: java.lang.Exception) {
                Thread.sleep(1000)
            }
        }
    }
}

class thrC : Thread() {
    lateinit var upper: MainActivity
    lateinit var ip: String
    lateinit var path: File

    var reso = 0;
    var drawInfo = HashMap<Int, ArrayList<pointItem>>()
var maxWaitTime = 5*1000.toLong()

    fun iniOne(i: Int) {
        var l = ArrayList<pointItem>()
        var file: File = File(path, i.toString())
        var br = BufferedReader(FileReader(file))
        while (true) {
            var line = br.readLine()
            if (line == null)
                break
            var a= line.split(" ")
            l.add(pointItem(a[0].toInt()-1,a[1].toInt()-1))
        }
        drawInfo[i] = l
    }

    fun iniD() {
        for(i in 0..300)
        {
            try {
                iniOne(i)
            }
            catch (e:Exception)
            {}
        }
    }

    fun drawOne(ty: Int, v: Int, i: Int, j: Int, col: Int, im: Bitmap) {
        var redCount = v * reso * reso / 3 / 255;
        var gap = reso / 3 * ty;

        if (reso == 30 && drawInfo.contains(redCount)) {
            var di = i * reso + gap;
            var dy = j * reso;
            for (ii in i * reso + gap until gap + i * reso + reso / 3) {
                for (jj in j * reso until (j + 1) * reso) {
                    im.setPixel(ii, jj, Color.BLACK)
                }
            }
            for (p in drawInfo[redCount]!!) {
                im.setPixel(di + p.x, dy + p.y, col)
            }
            return;
        }

        var redCo = 0;
        for (ii in i * reso + gap until gap + i * reso + reso / 3) {
            for (jj in j * reso until (j + 1) * reso) {
                if (redCo >= redCount) {
                    im.setPixel(ii, jj, Color.BLACK)
                    continue
                }
                im.setPixel(ii, jj, col)
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
                    drawOne(0, r, i, j, Color.RED, bb);
                    drawOne(1, g, i, j, Color.GREEN, bb);
                    drawOne(2, b, i, j, Color.BLUE, bb);
                }
            }
        }
        return bb
    }

    override fun run() {
        while (true) {
            if (pause) {
                Thread.sleep(10)
                continue
            }
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
        var sT = System.currentTimeMillis()
        try {
            s.connect(add)
            s.configureBlocking(false)
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
         var   se = Selector.open()
            s.register(se,SelectionKey.OP_READ)
            while (re != xl * yl * 4) {
                se.select(maxWaitTime+100)
                if(System.currentTimeMillis()-sT>maxWaitTime)
                {
                    s.close()
                    se.close()
                    return ByteBuffer.allocate(0)
                }
                var x = s.read(ss)
                if (x == 0 && re != xl * yl * 4) {
                    s.close()
                    return ByteBuffer.allocate(0)
                }
                re += x
            }
            se.close()
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
    fun saveText() {
        var path: File = baseContext.filesDir
        var file: File = File(path, "a.txt")
        var f = FileOutputStream(file)
        var bb = ByteBuffer.allocate(100)
        var b = findViewById<EditText>(R.id.ip).text.toString().toByteArray()
        bb.putInt(b.size)
        bb.put(b)
        b = findViewById<EditText>(R.id.xCapLen).text.toString().toByteArray()
        bb.putInt(b.size)
        bb.put(b)
        b = findViewById<EditText>(R.id.yCapLen).text.toString().toByteArray()
        bb.putInt(b.size)
        bb.put(b)
        f.write(bb.array())
        f.close()
    }

    fun iniText() {
        var path: File = baseContext.filesDir
        var file: File = File(path, "a.txt")
        val f = FileInputStream(file)
        val b = ByteArray(100)
        f.read(b, 0, 100)
        f.close()

        var bb = ByteBuffer.wrap(b)
        var l = bb.getInt()
        var stb = ByteArray(l)
        bb.get(stb)
        var st = String(stb)
        findViewById<EditText>(R.id.ip).setText(st)
        l = bb.getInt()
        stb = ByteArray(l)
        bb.get(stb)
        st = String(stb)
        findViewById<EditText>(R.id.xCapLen).setText(st)
        l = bb.getInt()
        stb = ByteArray(l)
        bb.get(stb)
        st = String(stb)
        findViewById<EditText>(R.id.yCapLen).setText(st)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        this.getSupportActionBar()?.hide();
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var inte = 30

        try {
            iniText()
        } catch (e: Exception) {
        }
        var tt = recvThr()
        tt.ip = findViewById<EditText>(R.id.ip).text.toString()
        tt.path = baseContext.filesDir
        tt.start()
        findViewById<Button>(R.id.pauseBtn).setOnClickListener {
            pause = !pause
            if (pause) {
                findViewById<Button>(R.id.pauseBtn).setText("->")
            } else {
                findViewById<Button>(R.id.pauseBtn).setText("||")
            }
        }

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
            try {
                saveText()
            } catch (e: Exception) {
            }
            xCapLen = findViewById<EditText>(R.id.xCapLen).text.toString().toInt()
            yCapLen = findViewById<EditText>(R.id.yCapLen).text.toString().toInt()
            findViewById<Button>(R.id.button).setText("stop")
            hasStart = true
            var t = thrC()
            t.ip = findViewById<EditText>(R.id.ip).text.toString()
            t.upper = this
            t.path = baseContext.filesDir
            t.iniD()
            t.start()
            findViewById<SeekBar>(R.id.seekBar1).max = xCapLen
            findViewById<SeekBar>(R.id.seekBar2).max = yCapLen
            findViewById<SeekBar>(R.id.seekBar3).max = 200
            findViewById<SeekBar>(R.id.seekBar4).max = 20
        }

    }

    fun show(bitmap: Bitmap) {
        if (pause)
            return
        findViewById<ImageView>(R.id.imageView).setImageBitmap(bitmap)
    }
}