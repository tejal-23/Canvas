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

    // Paints
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
    private var tempShapeRect = RectF()

    //  Callbacks to ViewModel
    var onStrokeCompleted: ((Stroke) -> Unit)? = null
    var onShapeInserted: ((ShapeModel) -> Unit)? = null
    var onTextInserted: ((TextModel) -> Unit)? = null

    // Current authoritative state from ViewModel
    private var strokes: List<Stroke> = emptyList()
    private var shapes: List<ShapeModel> = emptyList()
    private var texts: List<TextModel> = emptyList()

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

    // Public API from toolbar
    fun setColor(colorInt: Int) { currentColor = colorInt }
    fun setStrokeWidth(px: Float) { currentStrokeWidth = px }
    fun insertShape(type: ShapeType) { activeShapeType = type }
    fun insertText(textModel: TextModel) { onTextInserted?.invoke(textModel) }

    // Render state from ViewModel
    fun setState(state: WhiteboardState) {
        strokes = state.strokes
        shapes = state.shapes
        texts = state.texts
        isEraserMode = state.isEraserOn   // eraser mode comes from VM state
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        offscreenBitmap?.eraseColor(Color.TRANSPARENT)

        // Draw strokes
        for (s in strokes) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = s.strokeWidth
                color = s.color.toInt()
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            if (s.isEraser) p.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)

            val path = Path()
            s.points.forEachIndexed { idx, pt ->
                if (idx == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
            }
            offscreenCanvas?.drawPath(path, p)
        }

        // Draw shapes
        for (s in shapes) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = s.color.toInt()
                strokeWidth = s.strokeWidth
            }
            when (s.type) {
                ShapeType.RECTANGLE -> offscreenCanvas?.drawRect(s.x, s.y, s.x + s.width, s.y + s.height, p)
                ShapeType.CIRCLE -> {
                    val cx = s.x + s.width / 2
                    val cy = s.y + s.height / 2
                    val r = minOf(s.width, s.height) / 2
                    offscreenCanvas?.drawCircle(cx, cy, r, p)
                }
                ShapeType.LINE -> offscreenCanvas?.drawLine(s.x, s.y, s.x + s.width, s.y + s.height, p)
                ShapeType.POLYGON -> {
                    if (s.points.size >= 2) {
                        val path = Path().apply {
                            moveTo(s.points.first().x, s.points.first().y)
                            for (pt in s.points.drop(1)) lineTo(pt.x, pt.y)
                            close()
                        }
                        offscreenCanvas?.drawPath(path, p)
                    }
                }
            }
        }

        // Draw texts
        for (t in texts) {
            val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = t.color.toInt()
                textSize = t.fontSizeSp * resources.displayMetrics.scaledDensity
            }
            offscreenCanvas?.drawText(t.text, t.x, t.y, tp)
        }

        // Commit to screen
        offscreenBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }

        // Draw live preview stroke
        if (!currentPath.isEmpty) {
            canvas.drawPath(
                currentPath,
                if (isEraserMode) eraserPaint else drawPaint.apply {
                    color = currentColor
                    strokeWidth = currentStrokeWidth
                }
            )
        }

        // Draw live preview shape
        activeShapeType?.let { type ->
            val previewPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = currentStrokeWidth
                color = currentColor
            }
            when (type) {
                ShapeType.RECTANGLE -> canvas.drawRect(tempShapeRect, previewPaint)
                ShapeType.CIRCLE -> {
                    val cx = tempShapeRect.centerX()
                    val cy = tempShapeRect.centerY()
                    val r = minOf(tempShapeRect.width(), tempShapeRect.height()) / 2
                    canvas.drawCircle(cx, cy, r, previewPaint)
                }
                ShapeType.LINE -> canvas.drawLine(tempShapeRect.left, tempShapeRect.top, tempShapeRect.right, tempShapeRect.bottom, previewPaint)
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
                    tempShapeRect.set(x, y, x, y)
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
                    tempShapeRect.set(
                        minOf(shapeStartX, x), minOf(shapeStartY, y),
                        maxOf(shapeStartX, x), maxOf(shapeStartY, y)
                    )
                    invalidate()
                    return true
                }
                val last = currentPoints.last()
                currentPath.quadTo(last.x, last.y, (last.x + x) / 2, (last.y + y) / 2)
                currentPoints.add(PointF(x, y))
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (activeShapeType != null) {
                    val shape = ShapeModel(
                        id = UUID.randomUUID().toString(),
                        type = activeShapeType!!,
                        x = tempShapeRect.left,
                        y = tempShapeRect.top,
                        width = tempShapeRect.width(),
                        height = tempShapeRect.height(),
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


