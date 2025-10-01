package com.example.canvas.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.canvas.models.*
import java.util.*
import androidx.core.graphics.createBitmap

class WhiteboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    // Offscreen buffer
    private var offscreenBitmap: Bitmap? = null
    private var offscreenCanvas: Canvas? = null

    // Reusable Paints
    private val drawPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val eraserPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    private val shapePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Reusable objects
    private val tempPath = Path()
    private val tempRect = RectF()

    // Current drawing state
    private var currentPath = Path()
    private val currentPoints = mutableListOf<PointF>()
    private var currentColor = Color.BLACK
    private var currentStrokeWidth = 8f
    private var isEraserMode = false

    // Shape drawing helpers
    private var activeShapeType: ShapeType? = null
    private var shapeStartX = 0f
    private var shapeStartY = 0f

    // Callbacks
    var onStrokeCompleted: ((Stroke) -> Unit)? = null
    var onShapeInserted: ((ShapeModel) -> Unit)? = null
    var onTextInserted: ((TextModel) -> Unit)? = null

    // Authoritative state
    private var strokes: List<Stroke> = emptyList()
    private var shapes: List<ShapeModel> = emptyList()
    private var texts: List<TextModel> = emptyList()

    // Precompute density once
    private val density = resources.displayMetrics.density

    init {
        setBackgroundColor(Color.WHITE)
        isClickable = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return
        offscreenBitmap = createBitmap(w, h)
        offscreenCanvas = Canvas(offscreenBitmap!!)
    }

    // Toolbar API
    fun setColor(colorInt: Int) { currentColor = colorInt }
    fun setStrokeWidth(px: Float) { currentStrokeWidth = px }
    fun insertShape(type: ShapeType) { activeShapeType = type }
    fun insertText(textModel: TextModel) { onTextInserted?.invoke(textModel) }

    // ViewModel state
    fun setState(state: WhiteboardState) {
        strokes = state.strokes
        shapes = state.shapes
        texts = state.texts
        isEraserMode = state.isEraserOn
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        offscreenBitmap?.eraseColor(Color.TRANSPARENT)

        // Draw strokes
        for (s in strokes) {
            tempPath.reset()
            val points = s.points
            for (i in points.indices) {
                val pt = points[i]
                if (i == 0) tempPath.moveTo(pt.x, pt.y)
                else {
                    val prev = points[i - 1]
                    val midX = (prev.x + pt.x) / 2
                    val midY = (prev.y + pt.y) / 2
                    tempPath.quadTo(prev.x, prev.y, midX, midY)
                }
            }

            val paint = if (s.isEraser) eraserPaint else drawPaint
            paint.strokeWidth = s.strokeWidth
            paint.color = s.color.toInt()
            offscreenCanvas?.drawPath(tempPath, paint)
        }

        // Draw shapes
        for (s in shapes) {
            shapePaint.color = s.color.toInt()
            shapePaint.strokeWidth = s.strokeWidth

            when (s.type) {
                ShapeType.RECTANGLE -> offscreenCanvas?.drawRect(s.x, s.y, s.x + s.width, s.y + s.height, shapePaint)
                ShapeType.CIRCLE -> {
                    val cx = s.x + s.width / 2
                    val cy = s.y + s.height / 2
                    val r = minOf(s.width, s.height) / 2
                    offscreenCanvas?.drawCircle(cx, cy, r, shapePaint)
                }
                ShapeType.LINE -> offscreenCanvas?.drawLine(s.x, s.y, s.x + s.width, s.y + s.height, shapePaint)
                ShapeType.POLYGON -> {
                    if (s.points.size >= 2) {
                        tempPath.reset()
                        tempPath.moveTo(s.points.first().x, s.points.first().y)
                        s.points.drop(1).forEach { tempPath.lineTo(it.x, it.y) }
                        tempPath.close()
                        offscreenCanvas?.drawPath(tempPath, shapePaint)
                    }
                }
            }
        }

        // Draw texts
        for (t in texts) {
            textPaint.color = t.color.toInt()
            textPaint.textSize = t.fontSizeSp * density
            offscreenCanvas?.drawText(t.text, t.x, t.y, textPaint)
        }

        // Commit to screen
        offscreenBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }

        // Live preview stroke
        if (!currentPath.isEmpty) {
            val paint = if (isEraserMode) eraserPaint else drawPaint
            paint.color = currentColor
            paint.strokeWidth = currentStrokeWidth
            canvas.drawPath(currentPath, paint)
        }

        // Live preview shape
        activeShapeType?.let { type ->
            shapePaint.color = currentColor
            shapePaint.strokeWidth = currentStrokeWidth
            when (type) {
                ShapeType.RECTANGLE -> canvas.drawRect(tempRect, shapePaint)
                ShapeType.CIRCLE -> canvas.drawCircle(tempRect.centerX(), tempRect.centerY(), minOf(tempRect.width(), tempRect.height()) / 2, shapePaint)
                ShapeType.LINE -> canvas.drawLine(tempRect.left, tempRect.top, tempRect.right, tempRect.bottom, shapePaint)
                ShapeType.POLYGON -> {}
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (activeShapeType != null) {
                    shapeStartX = x; shapeStartY = y
                    tempRect.set(x, y, x, y)
                    return true
                }
                currentPath.reset()
                currentPath.moveTo(x, y)
                currentPoints.clear()
                currentPoints.add(PointF(x, y))
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (activeShapeType != null) {
                    tempRect.set(
                        minOf(shapeStartX, x), minOf(shapeStartY, y),
                        maxOf(shapeStartX, x), maxOf(shapeStartY, y)
                    )
                    invalidate()
                    return true
                }
                val last = currentPoints.last()
                // Use cubic Bézier smoothing
                val midX = (last.x + x) / 2
                val midY = (last.y + y) / 2
                currentPath.quadTo(last.x, last.y, midX, midY)
                currentPoints.add(PointF(x, y))
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (activeShapeType != null) {
                    val shape = ShapeModel(
                        id = UUID.randomUUID().toString(),
                        type = activeShapeType!!,
                        x = tempRect.left,
                        y = tempRect.top,
                        width = tempRect.width(),
                        height = tempRect.height(),
                        color = currentColor.toLong() and 0xFFFFFFFF,
                        strokeWidth = currentStrokeWidth
                    )
                    onShapeInserted?.invoke(shape)
                    activeShapeType = null
                    invalidate()
                    return true
                }

                // Finalize stroke
                val strokeModel = Stroke(
                    id = UUID.randomUUID().toString(),
                    points = currentPoints.map { PointData(it.x, it.y) },
                    color = currentColor.toLong() and 0xFFFFFFFF,
                    strokeWidth = currentStrokeWidth,
                    isEraser = isEraserMode
                )
                onStrokeCompleted?.invoke(strokeModel)

                currentPath.reset()
                currentPoints.clear()
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
