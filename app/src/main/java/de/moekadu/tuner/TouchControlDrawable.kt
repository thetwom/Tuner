package de.moekadu.tuner

import android.content.Context
import android.graphics.*
import androidx.core.content.ContextCompat

class TouchControlDrawable(context: Context, private var tint: Int?, private var backgroundTint: Int?, drawableId: Int) {
    private val drawable = ContextCompat.getDrawable(context, drawableId)?.mutate()

    private val aspectRatio = (drawable?.intrinsicHeight?.toFloat() ?: 1f) / (drawable?.intrinsicWidth?.toFloat() ?: 1f)
    var width = 0f
        private set
    var height = 0f
        private set

    private val paint = Paint().apply {
        val tintTmp = tint
        colorFilter = if (tintTmp != null)
            PorterDuffColorFilter(tintTmp, PorterDuff.Mode.SRC_IN)
        else
            null
    }

    private val backgroundPaint = Paint().apply {
        style = Paint.Style.FILL
        color = backgroundTint ?: Color.BLACK
    }
    private var drawBackground = backgroundTint != null

    private var bitmap: Bitmap? = null

    private val boundingBox = RectF(0f, 0f, 0f ,0f)

    fun setColors(tint: Int? = this.tint, backgroundTint: Int? = this.backgroundTint): Boolean {
        var changed = false
        if (tint != this.tint) {
            val tintTmp = tint
            paint.colorFilter = if (tintTmp != null)
                PorterDuffColorFilter(tintTmp, PorterDuff.Mode.SRC_IN)
            else
                null
            this.tint = tint
            changed = true
        }
        if (backgroundTint != this.backgroundTint) {
            backgroundPaint.color = backgroundTint ?: Color.BLACK
            this.backgroundTint = backgroundTint
            drawBackground = (backgroundTint != null)
            changed = true
        }
        return changed
    }

    fun setSize(width: Float = USE_ASPECT_RATIO, height: Float = USE_ASPECT_RATIO) {
        require(!(width == USE_ASPECT_RATIO && height == USE_ASPECT_RATIO))
        val newWidth = if (width == USE_ASPECT_RATIO) height / aspectRatio else width
        val newHeight = if (height == USE_ASPECT_RATIO) width * aspectRatio else height

        if (newWidth != this.width || newHeight != this.height) {
//            Log.v("Tuner", "PlotDrawable.setSize: newWidth=$newWidth, newHeight=$newHeight")
            val newBitmap = Bitmap.createBitmap(newWidth.toInt(), newHeight.toInt(), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(newBitmap)
            drawable?.setBounds(0 ,0, canvas.width, canvas.height)
            drawable?.draw(canvas)
            this.width = newWidth
            this.height = newHeight
            bitmap?.recycle()
            bitmap = newBitmap
        }
    }

    fun drawToCanvas(xPosition: Float, yPosition: Float, anchor: MarkAnchor, canvas: Canvas?) {
        if (height != 0f && width != 0f) {
            val x = when (anchor) {
                MarkAnchor.West, MarkAnchor.SouthWest, MarkAnchor.NorthWest -> xPosition
                MarkAnchor.Center, MarkAnchor.South, MarkAnchor.North -> xPosition - 0.5f * width
                MarkAnchor.East, MarkAnchor.SouthEast, MarkAnchor.NorthEast -> xPosition - width
            }

            val y = when (anchor) {
                MarkAnchor.North, MarkAnchor.NorthWest, MarkAnchor.NorthEast -> yPosition
                MarkAnchor.Center, MarkAnchor.West, MarkAnchor.East -> yPosition - 0.5f * height
                MarkAnchor.South, MarkAnchor.SouthWest, MarkAnchor.SouthEast -> yPosition - height
            }

            boundingBox.left = x
            boundingBox.top = y
            boundingBox.right = x + width
            boundingBox.bottom = y + height

            bitmap?.let {
                if (drawBackground)
                    canvas?.drawRect(x+1, y+1, x+width-1, y+height-1, backgroundPaint)
                canvas?.drawBitmap(it, x, y, paint)
            }
        }
    }

    fun contains(xPosition: Float, yPosition: Float): Boolean {
        return boundingBox.contains(xPosition, yPosition)
    }

    companion object {
        const val USE_ASPECT_RATIO = Float.MAX_VALUE
    }
}