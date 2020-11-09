package com.night.dmcscrapped.units

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import android.widget.Toast
import java.io.File

object ImageUnit {

    fun getBitmap(
        context: Context,
        filename: String,
        screenWidth: Int,
        screenHeight: Int,
        displayDegree: Int
    ): Bitmap? {

        val bitmapOption = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        val file = File(context.filesDir, filename)
        var bitmap = BitmapFactory.decodeFile(file.path)
        bitmapOption.inJustDecodeBounds = false
        bitmapOption.inSampleSize = calculateInSampleSize(bitmapOption, screenWidth, screenHeight)
        bitmap = BitmapFactory.decodeFile(file.path, bitmapOption)
        val matrix = Matrix().apply {
            setRotate(displayDegree.toFloat())
        }
        bitmap =  Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)

        val bitmapHeight = bitmap.height.toFloat()
        val bitmapWidth = bitmap.width.toFloat()
        if (bitmapHeight <= 0 || bitmapWidth <= 0) {
            Toast.makeText(context, "width and height must be >0", Toast.LENGTH_SHORT).show()
            return null
        }
        var scal = screenHeight.toFloat() / bitmapHeight
        if(bitmapWidth/bitmapHeight >= 1.5f){
            scal = screenWidth /bitmapWidth
            Log.d("@@", "----------")
            Log.d("@@" + " sendPicture", (bitmapWidth / bitmapHeight).toString() + ">= 1.5 & scal:" + scal)
            Log.d("@@", "----------")
        }
        val matrixx = Matrix().apply {
            postScale(scal,scal)
        }
        bitmap = Bitmap.createBitmap(bitmap,0,0,bitmap.width,bitmap.height,matrixx,false)
        return bitmap

    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize > reqHeight && halfWidth / inSampleSize > reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}