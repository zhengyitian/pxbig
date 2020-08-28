package com.example.draw

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    var len = 500
    var b = Bitmap.createBitmap(len,len,Bitmap.Config.ARGB_8888)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        for (i in 0..len-1)
        {
            for(j in 0..len-1)
            {
                b.setPixel(i,j,Color.RED)
            }
        }
        findViewById<ImageView>(R.id.imageView).setImageBitmap(b)
        findViewById<ImageView>(R.id.imageView) .setOnTouchListener { view, motionEvent ->
          var x = motionEvent.getX().toInt()
          var y =   motionEvent.getY().toInt()

                for(i in x-10 until x+10)
                {
                    for(j in y-10 until y+10)
                    {
                        if(i>=0&&i<len&&j>=0&&j<len)
                        b.setPixel(i,j,Color.BLACK)
                    }
                }

                findViewById<ImageView>(R.id.imageView).setImageBitmap(b)


                true
        }
    }
}
