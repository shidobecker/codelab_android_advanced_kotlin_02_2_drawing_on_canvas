package com.example.android.minipaint

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.content.res.ResourcesCompat
import kotlin.math.abs

class MyCanvasView(context: Context) : View(context) {

    private lateinit var extraCanvas: Canvas
    private lateinit var extraBitmap: Bitmap
    private val drawColor = ResourcesCompat.getColor(resources, R.color.colorPaint, null)

    private var motionTouchEventX = 0f
    private var motionTouchEventY = 0f


    companion object {
        private const val STROKE_WIDTH = 12f // has to be float
    }

    private val backgroundColor = ResourcesCompat.getColor(resources, R.color.colorBackground, null)


    val paint = Paint().apply {
        color = drawColor
        isAntiAlias = true   // Smooths out edges of what is drawn without affecting shape.

        isDither =
            true   // Dithering affects how colors with higher-precision than the device are down-sampled.

        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = STROKE_WIDTH


        /**
         *
         *isAntiAlias defines whether to apply edge smoothing. Setting isAntiAlias to true, smoothes out the edges of what is drawn without affecting the shape.

         *isDither, when true, affects how colors with higher-precision than the device are down-sampled. For example, dithering is the most common means of reducing the color range of images down to the 256 (or fewer) colors.
        style sets the type of painting to be done to a stroke, which is essentially a line. Paint.Style specifies if the primitive being drawn is filled, stroked, or both (in the same color). The default is to fill the object to which the paint is applied. ("Fill" colors the inside of shape, while "stroke" follows its outline.)

         *strokeJoin of Paint.Join specifies how lines and curve segments join on a stroked path. The default is MITER.

         *strokeCap sets the shape of the end of the line to be a cap. Paint.Cap specifies how the beginning and ending of stroked lines and paths. The default is BUTT.

         *strokeWidth specifies the width of the stroke in pixels. The default is hairline width, which is really thin, so it's set to the STROKE_WIDTH constant you defined earlier.
         */
    }

    private var path = Path()


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        /**
         * Looking at onSizeChanged(), a new bitmap and canvas are created every time the function executes.
         * You need a new bitmap, because the size has changed. However, this is a memory leak, leaving the
         * old bitmaps around. To fix this, recycle extraBitmap before creating the next one by adding this
         * code right after the call to super.
         */

        if (::extraBitmap.isInitialized) extraBitmap.recycle()


        extraBitmap = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )//ARGB_8888 stores each color in 4 bytes and is recommended.

        extraCanvas = Canvas(extraBitmap)

        extraCanvas.drawColor(backgroundColor)

        // Calculate a rectangular frame around the picture.
        val inset = 40
        frame = Rect(inset, inset, width - inset, height - inset)

    }

    private lateinit var frame: Rect


    // Path representing the drawing so far
    private val drawing = Path()

    // Path representing what's currently being drawn
    private val curPath = Path()

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.drawBitmap(extraBitmap, 0F, 0F, null)

        canvas?.drawRect(frame, paint)


        // Draw the drawing so far
        canvas?.drawPath(drawing, paint)
        // Draw any current squiggle
        canvas?.drawPath(curPath, paint)
        // Draw a frame around the canvas
        canvas?.drawRect(frame, paint)

    }


    /**
     * In MyCanvasView, override the onTouchEvent() method to cache the x and y coordinates of the passed
     * in event. Then use a when expression to handle motion events for touching down on the screen,
     * moving on the screen, and releasing touch on the screen. These are the events of interest for
     * drawing a line on the screen. For each event type, call a utility method, as shown in the code below.
     * See the MotionEvent class documentation for a full list of touch events.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        motionTouchEventX = event.x
        motionTouchEventY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> touchStart()
            MotionEvent.ACTION_MOVE -> touchMove()
            MotionEvent.ACTION_UP -> touchUp()
        }

        return true
    }


    /**
     * In MyCanvasView, override the onTouchEvent() method to cache the x and y coordinates of the passed in event.
     * Then use a when expression to handle motion events for touching down on the screen, moving on the
     * screen, and releasing touch on the screen. These are the events of interest for drawing a line on
     * the screen. For each event type, call a utility method, as shown in the code below.
     * See the MotionEvent class documentation for a full list of touch events.
     */
    private var currentX = 0f
    private var currentY = 0f

    private fun touchStart() {
        path.reset()

        path.moveTo(motionTouchEventX, motionTouchEventY)
        currentX = motionTouchEventX
        currentY = motionTouchEventY
    }

    /**
     * Using a path, there is no need to draw every pixel and each time request a refresh of the display.
     * Instead, you can (and will) interpolate a path between points for much better performance.

    If the finger has barely moved, there is no need to draw.
    If the finger has moved less than the touchTolerance distance, don't draw.
    scaledTouchSlop returns the distance in pixels a touch can wander before the system thinks the user
    is scrolling.
    Define the touchMove() method. Calculate the traveled distance (dx, dy), create a curve between the
    two points and store it in path, update the running currentX and currentY tally, and draw the path.
    Then call invalidate() to force redrawing of the screen with the updated path.
     */
    private val touchTolerance = ViewConfiguration.get(context).scaledPagingTouchSlop

    private fun touchMove() {
        val dx = abs(motionTouchEventX - currentX)
        val dy = abs(motionTouchEventY - currentY)
        if (dx >= touchTolerance || dy >= touchTolerance) {
            // QuadTo() adds a quadratic bezier from the last point,
            // approaching control point (x1,y1), and ending at (x2,y2).
            path.quadTo(
                currentX,
                currentY,
                (motionTouchEventX + currentX) / 2,
                (motionTouchEventY + currentY) / 2
            )
            currentX = motionTouchEventX
            currentY = motionTouchEventY
            // Draw the path in the extra bitmap to cache it.
            extraCanvas.drawPath(path, paint)
        }
        invalidate()
    }

    private fun touchUp() {
        // Reset the path so it doesn't get drawn again.
       // path.reset()

        // Add the current path to the drawing so far
        drawing.addPath(curPath)
// Rewind the current path for the next touch
        curPath.reset()
    }


}