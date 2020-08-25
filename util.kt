package com.example.cap_scr

import android.graphics.Bitmap
import android.media.Image
import android.os.Build
import androidx.annotation.RequiresApi


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
