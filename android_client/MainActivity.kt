package com.example.server

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


//config before compile
//biggest size of picture shown
//samsung c5 1000
//huaweip20 1000
//redmi 3s 700


var maxX = 700
var maxY = 700

//picture maxSize got from server
var showLen = 700
//end of config

var xs = 0
var ys = 0
var xlen = 100
var xCapLen = 2244
var yCapLen = 2244
var g_reso = 0

var pause = false

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
    var ipR = ""
    var ip2 = "127.0.0.1"

    fun w() {
        var add = InetSocketAddress(ipR, 8898)
        var so = SocketChannel.open()
        try {
            so.socket().connect(add, 2000)
        } catch (e: Exception) {
            so.close()
            throw e
        }

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
        so.close()
        var file: File = File(path, i.toString())
        var f = FileOutputStream(file)
        f.write(b.array(), 0, to)
        f.close()
    }

    override fun run() {
        ipR = ip
        while (true) {
            try {
                w()
            } catch (e: java.lang.Exception) {
                Thread.sleep(1000)
                if (ipR == ip)
                    ipR = ip2
                else
                    ipR = ip
            }
        }
    }
}

class thrC : Thread() {
    lateinit var upper: MainActivity
    lateinit var ip: String
    lateinit var path: File
    var lastCallTime = System.currentTimeMillis()
    var reso = 0;
    var drawInfo = HashMap<Int, ArrayList<pointItem>>()
    var maxWaitTime = 15 * 1000.toLong()

    fun iniOne(i: Int) {
        var l = ArrayList<pointItem>()
        var file: File = File(path, i.toString())
        var br = BufferedReader(FileReader(file))
        while (true) {
            var line = br.readLine()
            if (line == null)
                break
            var a = line.split(" ")
            l.add(pointItem(a[0].toInt() - 1, a[1].toInt() - 1))
        }
        drawInfo[i] = l
    }

    fun iniD() {
        for (i in 0..300) {
            try {
                iniOne(i)
            } catch (e: Exception) {
            }
        }
    }

    fun drawOne(ty: Int, v: Int, i: Int, j: Int, br: ByteArray, xll: Int) {
        var redCount = v * reso * reso / 3 / 255;
        if (reso == 30)
            redCount = v
        var gap = reso / 3 * ty;

        if (reso == 30 && drawInfo.contains(redCount)) {
            var di = i * reso + gap;
            var dy = j * reso;
            for (ii in i * reso + gap until gap + i * reso + reso / 3) {
                for (jj in j * reso until (j + 1) * reso) {
                    var va = (ii + jj * xll) * 4
                    br[va + 3] = -1
                }
            }
            for (p in drawInfo[redCount]!!) {
                var va = ((di + p.x) + (dy + p.y) * xll) * 4
                br[va + 3] = -1
                br[va + ty] = -1
            }
            return;
        }

        var redCo = 0;
        for (ii in i * reso + gap until gap + i * reso + reso / 3) {
            for (jj in j * reso until (j + 1) * reso) {
                var va = (ii + jj * xll) * 4
                br[va + 3] = -1
                if (redCo >= redCount) {
                    continue
                }
                br[va + ty] = -1
                redCo += 1;
            }
        }
    }

    fun draw(ss: ByteBuffer, xl: Int, yl: Int, ret: imageInfo) {
        var xx = maxX / reso
        var yy = maxY / reso

        var pa = xx * reso;
        if (xl * reso < pa) {
            pa = xl * reso;
            xx = xl
        }

        var pb = yy * reso;
        if (yl * reso < pb) {
            pb = yl * reso;
            yy = yl
        }
        ret.xlen = xx
        ret.ylen = yy
        ret.b = IntArray(xx * yy * 3)
        var bb = Bitmap.createBitmap(pa, pb, Bitmap.Config.ARGB_8888)
        var br = ByteArray(pa * pb * 4)
        for (j in 0 until yl) {
            for (i in 0 until xl) {
                if ((i + 1) > xx || (j + 1) > yy) {
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
                    ret.b[(i + j * xx) * 3] = r
                    ret.b[(i + j * xx) * 3 + 1] = g
                    ret.b[(i + j * xx) * 3 + 2] = b
                    drawOne(0, r, i, j, br, pa);
                    drawOne(1, g, i, j, br, pa);
                    drawOne(2, b, i, j, br, pa);
                }
            }
        }
        bb.copyPixelsFromBuffer(ByteBuffer.wrap(br))
        ret.img = bb
    }

    override fun run() {
        if (System.currentTimeMillis() - lastCallTime < 10)
            Thread.sleep(10 + lastCallTime - System.currentTimeMillis())
        lastCallTime = System.currentTimeMillis()
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
            var ret = imageInfo()
            ret.reso = reso
            ret.x = xs2
            ret.y = ys2


            if (reso != 0) {
                var bb = draw(ss, xl, yl, ret)
                upper.runOnUiThread({ upper.show(ret) })
                continue
            }

            ret.xlen = xl
            ret.ylen = yl
            ret.b = IntArray(xl * yl * 3)
            var bb = Bitmap.createBitmap(xl, yl, Bitmap.Config.ARGB_8888)
            var br = ByteArray(xl * yl * 4)
            for (j in 0 until yl) {
                for (i in 0 until xl) {
                    var va = (i + xl * j) * 4
                    var rc = ss.get()
                    var gc = ss.get()
                    var bc = ss.get()
                    ss.get()
                    br[va] = rc
                    br[va + 1] = gc
                    br[va + 2] = bc
                    br[va + 3] = -1
                    var r = rc.toInt()
                    var g = gc.toInt()
                    var b = bc.toInt()
                    if (r < 0)
                        r += 256
                    if (g < 0)
                        g += 256
                    if (b < 0)
                        b += 256
                    ret.b[(i + xl * j) * 3] = r
                    ret.b[(i + xl * j) * 3 + 1] = g
                    ret.b[(i + xl * j) * 3 + 2] = b
                }
            }
            var b = ByteBuffer.wrap(br)
            bb.copyPixelsFromBuffer(b)
            ret.img = bb
            upper.runOnUiThread({ upper.show(ret) })
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
            var se = Selector.open()
            s.register(se, SelectionKey.OP_READ)

            while (re != xl * yl * 4) {
                if (System.currentTimeMillis() - sT > maxWaitTime) {
                    s.close()
                    se.close()
                    return ByteBuffer.allocate(0)
                }
                var r = se.select(maxWaitTime + 100 - (System.currentTimeMillis() - sT))
                if (System.currentTimeMillis() - sT > maxWaitTime) {
                    s.close()
                    se.close()
                    return ByteBuffer.allocate(0)
                }
                var x = s.read(ss)
                if (x <= 0 && re != xl * yl * 4) {
                    s.close()
                    return ByteBuffer.allocate(0)
                }
                re += x
                //      println("got len  ${re}")
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
    var stepMode = false
    var im = imageInfo()
    var lastKeyDown = System.currentTimeMillis()-100000
    var keyDownCause = 0 //0:to 0,1:pause

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode==KeyEvent.KEYCODE_VOLUME_DOWN)
        {
            lastKeyDown = System.currentTimeMillis()-100000
            if (keyDownCause==0)
            {
                findViewById<SeekBar>(R.id.seekBar4).progress = 0
            }
            else
            {
               // onPauseBtnCli()
            }
            keyDownCause = 0
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode==KeyEvent.KEYCODE_VOLUME_DOWN)
        {
            if (System.currentTimeMillis()-lastKeyDown<10000 && keyDownCause==0)
            {
               keyDownCause = 1
                onPauseBtnCli()
            }
            lastKeyDown = System.currentTimeMillis()
            return true
        }
        if (keyCode==KeyEvent.KEYCODE_VOLUME_UP)
        {
            findViewById<SeekBar>(R.id.seekBar4).progress = 10
            return true
        }
       return  super.onKeyDown(keyCode, event)
    }
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

    fun onPauseBtnCli()
    {
        if (stepMode) {
            pause = false
            return
        }
        pause = !pause
        if (pause) {
            findViewById<Button>(R.id.pauseBtn).setText("->")
        } else {
            findViewById<Button>(R.id.pauseBtn).setText("||")
        }
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
        findViewById<Button>(R.id.pauseBtn).setOnLongClickListener {
            pause = true
            stepMode = !stepMode
            if (stepMode)
                findViewById<Button>(R.id.pauseBtn).setText("N")
            else
                findViewById<Button>(R.id.pauseBtn).setText("->")
            true
        }

        findViewById<Button>(R.id.pauseBtn).setOnClickListener {
            onPauseBtnCli()

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
                if (pause)
                    ss()
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
                if (pause)
                    ss()
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
            findViewById<SeekBar>(R.id.seekBar3).max = showLen
            findViewById<SeekBar>(R.id.seekBar4).max = 20
        }

    }

    fun ss() {
        if (im.img == null)
            return
        var x = findViewById<SeekBar>(R.id.seekBar1).progress - im.x
        var y = findViewById<SeekBar>(R.id.seekBar2).progress - im.y
        if (x < 0 || x >= im.xlen || y < 0 || y >= im.ylen)
            return
        var p = (y * im.xlen + x) * 3
        var vv = im.reso
        if (vv < 3)
            vv = 1
        var copyRect = Rect(x * vv, y * vv, im.xlen * vv, im.ylen * vv)
        val subImage = Bitmap.createBitmap(
            copyRect.width(),
            copyRect.height(), Bitmap.Config.ARGB_8888
        )
        val c = Canvas(subImage)
        c.drawBitmap(
            im.img!!, copyRect,  //from   w w w . j a va  2  s. c o m
            Rect(0, 0, copyRect.width(), copyRect.height()), null
        )
        findViewById<ImageView>(R.id.imageView).setImageBitmap(subImage)
        findViewById<EditText>(R.id.ip).setText("${im.b[p]},${im.b[p + 1]},${im.b[p + 2]}")
    }

    fun show(ret: imageInfo) {
        if (pause)
            return
        if (stepMode)
            pause = true
        im = ret
        var p = 0
        findViewById<ImageView>(R.id.imageView).setImageBitmap(ret.img)
        findViewById<EditText>(R.id.ip).setText("${im.b[p]},${im.b[p + 1]},${im.b[p + 2]}")
    }
}
