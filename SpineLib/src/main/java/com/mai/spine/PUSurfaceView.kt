package com.mai.spine

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View

/**
 * 替代PopupWindow的SurfaceView
 * 用于显示图片和纯色在其他Spine动画的SurfaceView上层
 */
class PUSurfaceView : SurfaceView, SurfaceHolder.Callback {
    private lateinit var mSurfaceHolder: SurfaceHolder
    private var mDrawType = 0   //绘制类型: -1 未指定; 0 颜色; 1 本地图片
    private var mBGColor = 0    //绘制颜色
    private var mImageResId = 0 //图片资源ID

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs, defStyleAttr)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(
        context, attrs, defStyleAttr, defStyleRes
    ) {
        init(context, attrs, defStyleAttr)
    }

    /**
     * 初始化
     */
    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        // 获取自定义属性
        val typedArray =
            context.obtainStyledAttributes(attrs, R.styleable.PUSurfaceView, defStyleAttr, 0)
        // 绘制类型
        mDrawType = typedArray.getInt(R.styleable.PUSurfaceView_drawType, -1)
        // 背景色
        mBGColor = typedArray.getColor(R.styleable.PUSurfaceView_bgColor, Color.TRANSPARENT)
        // 图片资源id
        mImageResId = typedArray.getResourceId(R.styleable.PUSurfaceView_imageSrc, 0)
        typedArray.recycle()

        // 初始化canvas
        mSurfaceHolder = holder
        mSurfaceHolder.addCallback(this)
        keepScreenOn = true
        // 透明色
        mSurfaceHolder.setFormat(PixelFormat.TRANSLUCENT)
        // Z轴置顶
        setZOrderOnTop(true)
    }

    /**
     * 绘制
     */
    private fun draw() {
        // 没有配置drawType忽略
        if (mDrawType == -1) {
            return
        }

        val mCanvas = mSurfaceHolder.lockCanvas() ?: return
        // 清空canvas
        mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        if (mDrawType == 0) {
            mCanvas.drawColor(mBGColor)
        }
        if (mDrawType == 1) {
            val bitmap = BitmapFactory.decodeResource(context.resources, mImageResId)
            if (bitmap != null) {
                val matrix = Matrix()
                configureDrawMatrix(bitmap, this, matrix)
                mCanvas.drawBitmap(bitmap, matrix, null)
                bitmap.recycle()
            }
        }
        mSurfaceHolder.unlockCanvasAndPost(mCanvas)
    }

    /**
     * 根据传入的color绘制纯色
     */
    fun drawColor(color: Int) {
        mBGColor = color
        mDrawType = 0
        draw()
    }

    /**
     * 根据传入id绘制图片
     */
    fun drawImage(id: Int) {
        mImageResId = id
        mDrawType = 1
        draw()
    }

    /**
     * Surface创建回调，子线程执行绘制任务
     */
    override fun surfaceCreated(holder: SurfaceHolder) {
        Thread { this.draw() }.start()
    }

    /**
     * ignore
     */
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

    }

    /**
     * 销毁回调，释放资源
     */
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        mSurfaceHolder.removeCallback(this)
        val canvas = mSurfaceHolder.lockCanvas() ?: return
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        mSurfaceHolder.unlockCanvasAndPost(canvas)
    }

    /**
     * 根据ScaleType配置绘制bitmap的Matrix
     */
    private fun configureDrawMatrix(bitmap: Bitmap, view: View, mDrawMatrix: Matrix) {
        val srcRect = RectF(
            0f, 0f, bitmap.width.toFloat(), bitmap.height
                .toFloat()
        )
        val dstRect = RectF(0f, 0f, view.width.toFloat(), view.height.toFloat())
        mDrawMatrix.setRectToRect(srcRect, dstRect, Matrix.ScaleToFit.FILL)
    }
}