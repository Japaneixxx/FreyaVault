package app.meufin.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View

fun captureView(view: View): Bitmap? {
    if (view.width <= 0 || view.height <= 0) return null

    return Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { bitmap ->
        view.draw(Canvas(bitmap))
    }
}
