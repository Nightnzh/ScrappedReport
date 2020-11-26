package com.night.dmcscrapped.units

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import android.widget.Toast
import com.night.dmcscrapped.data.model.PlateInfo
import java.io.File

object ImageUnit {

    fun getBitmap(
        context: Context,
        filename: String,
        screenWidth: Int,
        screenHeight: Int,
        topBtm : Int,
        displayDegree: Int,
        plateInfo: PlateInfo
    ): Bitmap? {

        val bitmapOption = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        val file = File(context.filesDir, filename)
        var bitmap = BitmapFactory.decodeFile(file.path)
        bitmapOption.inJustDecodeBounds = false
        bitmapOption.inSampleSize = calculateInSampleSize(bitmapOption, screenWidth, screenHeight)

        bitmap = BitmapFactory.decodeFile(file.path, bitmapOption)
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height,null, false)


        val x = (bitmap.width * plateInfo.scale_left.toFloat()).toInt()
        val y = (bitmap.height * plateInfo.scale_top.toFloat()).toInt()
        val w = (bitmap.width * plateInfo.scale_right.toFloat()).toInt()
        val h = (bitmap.height * plateInfo.scale_bottom.toFloat()).toInt()

        Log.d("@@@tqt","${bitmap.width} ${bitmap.height}" )
        Log.d("@@@tqt", "x : $x , y : $y , w: $w , h : $h")


        when(topBtm){
            0,1 ->{
                bitmap = Bitmap.createBitmap(
                    bitmap, x , y , bitmap.width - x - w -1, bitmap.height - y - h ,
                    null, false
                )
            }
            2->{
                bitmap = Bitmap.createBitmap(
                    bitmap, w - 1, y -1, bitmap.width - w - x  -1, bitmap.height - y - h -1,
                    null, false
                )
            }
        }


        //原
        val matrix = Matrix().apply {
            setRotate(displayDegree.toFloat())
        }

        bitmap = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height,
            matrix, false
        )



        val bitmapHeight = bitmap.height.toFloat()
        val bitmapWidth = bitmap.width.toFloat()
        if (bitmapHeight <= 0 || bitmapWidth <= 0) {
            Toast.makeText(context, "width and height must be >0", Toast.LENGTH_SHORT).show()
            return null
        }
        var scal = screenHeight.toFloat() / bitmapHeight
        if (bitmapWidth / bitmapHeight >= 1.5f) {
            scal = screenWidth / bitmapWidth
            Log.d("@@", "----------")
            Log.d(
                "@@" + " sendPicture",
                (bitmapWidth / bitmapHeight).toString() + ">= 1.5 & scal:" + scal
            )
            Log.d("@@", "----------")
        }
        val matrixx = Matrix().apply {
            Log.d("@@@testMatrix", "$this")
            postScale(scal, scal)
        }
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrixx, false)

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