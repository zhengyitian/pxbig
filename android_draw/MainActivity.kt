package com.example.draw

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.Exception
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.ServerSocketChannel

var coX = 10
var coY = 30

class thrC : Thread() {
    lateinit var upper: MainActivity
    lateinit var path: File
    fun getF(i: Int): ByteArray {
        try {
            var file: File = File(path, i.toString())
            val f = FileInputStream(file)
            var b = ByteArray(10000)
            var r = f.read(b)
            f.close()
            return b.sliceArray(0 until r)
        } catch (e: Exception) {
            return ByteArray(0)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun run() {
        var s = ServerSocketChannel.open()
        var add = InetSocketAddress("0.0.0.0", 8898)
        s.bind(add)
        for (i in 0..coX * coY) {
            var b = getF(i)
            if (b.size == 0)
                continue
            var so = s.accept()
            var aa = ByteBuffer.allocate(4)
            aa.order(ByteOrder.LITTLE_ENDIAN)
            aa.putInt(i)
            aa.flip()
            so.write(aa)
            var bb = ByteBuffer.wrap(b)
            so.write(bb)
            so.close()
        }
        s.close()
        upper.runOnUiThread { upper.serOver() }
    }
}

class MainActivity : AppCompatActivity() {
    var lineLen = 1
    var boxLen = 60

    var oriColor = Color.BLACK
    var lineColor = Color.GRAY
    var drawColor = Color.WHITE
    lateinit var bv: ImageView
    lateinit var invertSw: Switch
    lateinit var drawSw: Switch
    lateinit var clearBtn: Button
    lateinit var invertBtn: Button
    lateinit var setBtn: Button
    lateinit var numText: TextView
    lateinit var loadBtn: Button
    lateinit var loadNum: EditText
    lateinit var serveBtn: Button
    lateinit var serveNum: EditText
    var b = Bitmap.createBitmap(coX * boxLen, coY * boxLen, Bitmap.Config.ARGB_8888)

    var topM = 50
    var coMap = HashMap<String, Long>()
    var coMapSi = HashMap<String, Long>()

    fun addui(a: View, wi: Int, he: Int) {
        var rl = findViewById<RelativeLayout>(R.id.ll);
        var params = RelativeLayout.LayoutParams(wi, he);
        params.leftMargin = 700;
        params.topMargin = topM;
        topM += he + 50
        rl.addView(a, params);
    }

    fun ini() {
        for (i in 0..coX * boxLen - 1) {
            for (j in 0..coY * boxLen - 1)
                b.setPixel(i, j, oriColor)
        }
    }

    fun drawLine() {
        for (i in 1 until coX) {
            for (j in i * boxLen - lineLen..i * boxLen + lineLen) {
                for (k in 0 until coY * boxLen)
                    b.setPixel(j, k, lineColor)
            }
        }
        for (i in 1 until coY) {
            for (j in i * boxLen - lineLen..i * boxLen + lineLen) {
                for (k in 0 until coX * boxLen)
                    b.setPixel(k, j, lineColor)
            }
        }
    }

    fun inv() {
        var m = HashMap<String, Long>()
        for (i in 1..coX) {
            for (j in 1..coY) {
                var k = i.toString() + " " + j.toString()
                if (coMap.containsKey(k)) {
                    for (ii in (i - 1) * boxLen until i * boxLen) {
                        for (jj in (j - 1) * boxLen until j * boxLen) {
                            b.setPixel(ii, jj, oriColor)
                        }
                    }
                } else {
                    m[k] = 1
                    for (ii in (i - 1) * boxLen until i * boxLen) {
                        for (jj in (j - 1) * boxLen until j * boxLen) {
                            b.setPixel(ii, jj, drawColor)
                        }
                    }
                }
            }
        }
        coMap = m
    }

    fun dealFind(i: Int, j: Int) {
        var k = i.toString() + " " + j.toString()
        if (coMapSi.containsKey(k)) {
            if (System.currentTimeMillis() - coMapSi[k]!! < 500)
                return
        }
        coMapSi[k] = System.currentTimeMillis()
        if (invertSw.isChecked) {
            if (coMap.containsKey(k)) {
                coMap.remove(k);
                for (ii in (i - 1) * boxLen until i * boxLen) {
                    for (jj in (j - 1) * boxLen until j * boxLen) {
                        b.setPixel(ii, jj, oriColor)
                    }
                }
            } else {
                coMap[k] = System.currentTimeMillis()
                for (ii in (i - 1) * boxLen until i * boxLen) {
                    for (jj in (j - 1) * boxLen until j * boxLen) {
                        b.setPixel(ii, jj, drawColor)
                    }
                }
            }
            return
        }
        if (drawSw.isChecked) {
            coMap[k] = System.currentTimeMillis()
            for (ii in (i - 1) * boxLen until i * boxLen) {
                for (jj in (j - 1) * boxLen until j * boxLen) {
                    b.setPixel(ii, jj, drawColor)
                }
            }
            return
        }
        if (coMap.containsKey(k)) {
            coMap.remove(k);
            for (ii in (i - 1) * boxLen until i * boxLen) {
                for (jj in (j - 1) * boxLen until j * boxLen) {
                    b.setPixel(ii, jj, oriColor)
                }
            }
        }
    }

    @SuppressLint("WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        this.getSupportActionBar()?.hide();
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ini()
        drawLine()
        var rl = findViewById<RelativeLayout>(R.id.ll);
        bv = ImageView(this);
        var params = RelativeLayout.LayoutParams(coX * boxLen, coY * boxLen);
        params.leftMargin = 0;
        params.topMargin = 0;
        rl.addView(bv, params);
        bv.setImageBitmap(b)

        numText = TextView(this)
        numText.setText("0")
        addui(numText, 200, 50)

        setBtn = Button(this)
        setBtn.setText("save")
        addui(setBtn, 200, 150)
        setBtn.setOnClickListener {
            if (coMap.size==0)
                return@setOnClickListener
            var path: File = baseContext.filesDir
            var file: File = File(path, coMap.size.toString())
            var f = FileOutputStream(file)
            for (i in coMap.keys) {
                var st = i + "\n"
                f.write(st.toByteArray())
            }
            f.close()
        }

        drawSw = Switch(this)
        drawSw.setText("draw")
        addui(drawSw, 200, 150)

        invertSw = Switch(this)
        invertSw.setText("invert")
        addui(invertSw, 200, 150)
        clearBtn = Button(this)
        clearBtn.setText("clear")
        addui(clearBtn, 200, 150)
        invertBtn = Button(this)
        invertBtn.setText("invert")
        addui(invertBtn, 200, 150)
        clearBtn.setOnClickListener {
            coMap.clear()
            ini()
            drawLine()
            numText.setText("0")
            bv.setImageBitmap(b)
        }
        invertBtn.setOnClickListener {
            inv()
            drawLine()
            numText.setText(coMap.size.toString())
            bv.setImageBitmap(b)
        }

        serveBtn = Button(this)
        serveBtn.setText("serve")
        addui(serveBtn, 200, 150)
        serveBtn.setOnClickListener {
            var t = thrC()
            t.upper = this
            t.path = baseContext.filesDir
            t.start()
            serveBtn.isEnabled = false
        }
        bv.setOnTouchListener { view, motionEvent ->
            var x = motionEvent.getX().toInt()
            var y = motionEvent.getY().toInt()
            if (x >= coX * boxLen || y >= coY * boxLen)
                true
            else {
                var found = false
                for (i in 1..coX) {
                    if (found)
                        break
                    for (j in 1..coY) {
                        if (found)
                            break
                        if (x <= boxLen * i && y <= j * boxLen) {
                            found = true
                            dealFind(i, j);
                            numText.setText(coMap.size.toString())
                            drawLine();
                            println("${i},${j}")
                            bv.setImageBitmap(b)
                            break
                        }
                    }
                }
                true
            }
        }
    }

    fun serOver() {
        serveBtn.isEnabled = true
    }
}
