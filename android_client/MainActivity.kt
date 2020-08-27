package com.example.server

import android.R.attr.bitmap
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SocketChannel


class thrC:Thread()
{
    lateinit var upper: MainActivity
    override fun run()
    {
        while (true)
        {
            w()
        }
    }
    fun w()
    {
        var s = SocketChannel.open()
        var add = InetSocketAddress("192.168.43.160",8899)
        try {
            s.connect(add)
            var b = ByteBuffer.allocate(16)
            b.order(ByteOrder.LITTLE_ENDIAN)
            b.putInt(100)
            b.putInt(100)
            b.putInt(100)
            b.putInt(100)
            b.flip()
            s.write(b)
            var ss=ByteBuffer.allocate(100*100*4)
            var re = 0
                while (re!=100*100*4)
                {
                    re+= s.read(ss)
                }
            println(re)
            s.close()
            var bb = Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888)
            ss.flip()
            for(j in 0 until 100)
            {
                for (i in 0 until 100)
                {
                    var r=ss.get().toInt()
                    var g=ss.get().toInt()
                    var b=ss.get().toInt()
                    ss.get()
                    if(r<0)
                        r+=256
                    if(g<0)
                        g+=256
                    if(b<0)
                        b+=256
                    bb.setPixel(i,j,Color.rgb(r,g,b))
                }
            }
upper.runOnUiThread({upper.show(bb)})
        }
        catch (e:Exception)
        {e.printStackTrace()}
    }
}
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
var bb = Bitmap.createBitmap(1000,100,Bitmap.Config.ARGB_8888)
        for(i in 0 until 1000)
        {
            for (j in 0 until 100)
            {
                bb.setPixel(i,j,0)
            }
        }


var t = thrC()
        t.upper=this
        t.start()
    }
    fun show(bitmap:Bitmap)
    {
        val matrix = Matrix()

        val width: Int = bitmap.getWidth()
        val height: Int = bitmap.getHeight()

        val newWidth = 500
        val newHeight = 500
        val scaleWidth = newWidth.toFloat() / width
        val scaleHeight = newHeight.toFloat() / height

        matrix.postScale(scaleWidth, scaleHeight)

   //     matrix.postRotate(90.0f)


     var   newBitmapSize = Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(),
                matrix, true)
        findViewById<ImageView>(R.id.imageView).setImageBitmap(newBitmapSize)
    }
}