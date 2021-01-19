package com.mai.spine

import android.app.Activity
import android.text.TextUtils
import android.view.View
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Files.FileType
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector3
import com.esotericsoftware.spine.*

/**
 * 骨骼动画封装类
 */
abstract class SpineBaseAdapter : ApplicationAdapter {
    private var mAltasFileHandle: FileHandle? = null
    private var mSkeletonFileHandle: FileHandle? = null
    private var hasInit = false

    // Spine参数
    protected lateinit var mCamera: OrthographicCamera
    private lateinit var mBatch: PolygonSpriteBatch
    private lateinit var mRenderer: SkeletonRenderer
    private lateinit var mAtlas: TextureAtlas
    protected var mSkeleton: Skeleton? = null
    protected lateinit var mSkeletonBounds: SkeletonBounds
    protected lateinit var mAnimationState: AnimationState
    private lateinit var mAnimationStateData: AnimationStateData
    private lateinit var mSkeletonJson: SkeletonJson
    private var mSkeletonData: SkeletonData? = null
    private var mSpineClickListener: OnSpineClickListener? = null
    private var mSpineCreatedListener: OnSpineCreatedListener? = null
    private lateinit var mDebugRenderer: SkeletonRendererDebug

    // 缩放适配
    private var paddingStart = -1
    private var paddingTop = -1
    private var paddingEnd = -1
    private var paddingBottom = -1


    var tag = "null"    // 用于onCreated异步回调的tag
    open var skinName = "default"          // 默认皮肤名称
    open var animationName = "animation"   // 默认动画名称

    var debugMode = false               // 默认关闭debug
    var isClickable = true              // view是否可被点击，默认为true

    constructor(padding: Int = 0) : this(padding, padding, padding, padding)

    constructor(paddingStart: Int, paddingTop: Int, paddingEnd: Int, paddingBottom: Int) : super() {
        this.paddingStart = paddingStart
        this.paddingTop = paddingTop
        this.paddingEnd = paddingEnd
        this.paddingBottom = paddingBottom
    }

    /**
     * 动画的点击回调
     */
    fun setOnSpineClickListener(spineClickListener: OnSpineClickListener) {
        mSpineClickListener = spineClickListener
    }

    /**
     * 动画的创建完成回调
     * 多个Spine动画对象需要依次创建，不可以同时创建多个
     * 多以需要设置创建完成的异步回调，并配合tag来进行判断
     */
    fun setOnCreatedListener(onSpineCreatedListener: OnSpineCreatedListener) {
        mSpineCreatedListener = onSpineCreatedListener
    }

    /**
     * 注意：这些周期方法都是在子线程中执行的
     */
    override fun create() {
        try {
            onCreateImpl()
            initialize()
            onCreatedImpl()
            hasInit = true
            if (mSpineCreatedListener != null) {
                mSpineCreatedListener!!.onCreated(tag)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 初始化
     */
    private fun initialize() {
        if (mAltasFileHandle == null || mSkeletonFileHandle == null) {
            throw RuntimeException("请在createImpl中设置altas路径和skeleton路径")
        }
        mCamera = OrthographicCamera()
        mBatch = PolygonSpriteBatch()
        mRenderer = SkeletonRenderer()
        mRenderer.premultipliedAlpha = true

        //debug模式 可以在动画中直观的看见骨骼关系
        if (debugMode)
            mDebugRenderer = SkeletonRendererDebug()

        mAtlas = TextureAtlas(mAltasFileHandle)
        mSkeletonJson = SkeletonJson(mAtlas)
        mSkeletonData = mSkeletonJson.readSkeletonData(mSkeletonFileHandle)

        //适配方案1：等比拉伸，手动配置padding
        val viewHeight = Gdx.graphics.height.toFloat()
        var originHeight = mSkeletonData!!.height
        if (paddingTop != -1) {
            originHeight += paddingTop.toFloat()
        }
        if (paddingBottom != -1) {
            originHeight += paddingBottom.toFloat()
        }
        val scaleY = viewHeight / originHeight
        val viewWidth = Gdx.graphics.width.toFloat()
        var originWidth = mSkeletonData!!.width
        if (paddingStart != -1) {
            originWidth += paddingStart.toFloat()
        }
        if (paddingEnd != -1) {
            originWidth += paddingEnd.toFloat()
        }
//        val scaleX = viewWidth / originWidth
        //        mSkeletonJson.setScale(scaleY);//设置完scale之后要重新读取一下mSkeletonData
//        mSkeletonData = mSkeletonJson.readSkeletonData(mSkeletonFileHandle);
//        Log.e("TAG", "tag:" + tag);
//        Log.e("TAG", "viewWidth:" + viewWidth + " viewHeight" + viewHeight);
//        Log.e("TAG", "jsonWidth:" + originWidth + " jsonHeight" + originHeight);
//        Log.e("TAG", "scaleX:" + scaleX + " scaleY" + scaleY);
//        Log.e("TAG", "padding::" + mPaddingStart + " | " + mPaddingTop + " | " + mPaddingEnd + " | " + mPaddingBottom);
//        Log.e("TAG", "-----------------------------------------");
        mSkeleton = Skeleton(mSkeletonData)
        mSkeleton!!.setScale(scaleY, scaleY)

        //设置骨架在父布局中的位置，需要Spine动画本身的原点在(0,-1)
        mSkeleton!!.setPosition((Gdx.graphics.width / 2).toFloat(), 0f)
        mSkeletonBounds = SkeletonBounds()
        mAnimationStateData = AnimationStateData(mSkeletonData)

        //设置动画切换时的过度时间
        mAnimationStateData.defaultMix = DEFAULT_ANIM_SWITCH_TIME
        mAnimationState = AnimationState(mAnimationStateData)

        Gdx.input.inputProcessor = object : InputAdapter() {
            val point = Vector3()
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                mCamera.unproject(point.set(screenX.toFloat(), screenY.toFloat(), 0f))
                mSkeletonBounds.update(mSkeleton, false)
                if (mSkeletonBounds.aabbContainsPoint(point.x, point.y)) {
                    if (isClickable) {
                        /*doClick回调给Adapter，mSpineClickListener回调给Presentation*/
                        doClick()
                        if (mSpineClickListener != null) {
                            mSpineClickListener!!.onClick()
                            return true
                        }
                    }
                }
                return true
            }
        }
    }

    /**
     * 根据altasPath与skeletonPath初始化之后的回调
     * 用于setSkin、setAnimation、setAttachment
     */
    abstract fun onCreatedImpl()

    /**
     * 点击事件回调
     */
    abstract fun doClick()

    /**
     * 设置Spine的Altas路径
     * @param path 路径
     * @param fileType [FileType]
     */
    fun setAltasPath(path: String, fileType: FileType) {
        mAltasFileHandle = Gdx.files.getFileHandle(path, fileType)
    }

    /**
     * 设置Spine的json文件路径
     */
    fun setSkeletonPath(path: String, fileType: FileType) {
        mSkeletonFileHandle = Gdx.files.getFileHandle(path, fileType)
    }

    /**
     * 设置动画
     */
    fun setAnimation(trackIndex: Int, animationName: String, loop: Boolean) {
        mAnimationState.setAnimation(trackIndex, animationName, loop)
    }

    /**
     * resize回调
     */
    override fun resize(width: Int, height: Int) {
        try {
            onResizeImpl(width, height)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 继续回调
     */
    override fun resume() {
        try {
            onResumeImpl()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 渲染回调
     */
    override fun render() {
        try {
            onRenderImpl()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 暂停回调
     */
    override fun pause() {
        try {
            onPauseImpl()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 销毁回调
     */
    override fun dispose() {
        try {
            onDisposeImpl()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 实例开始创建回调
     * 在这里设置Altas与SkeletonPath
     */
    abstract fun onCreateImpl()

    /**
     * resize回调
     */
    private fun onResizeImpl(width: Int, height: Int) {
        mCamera.setToOrtho(false)
    }

    /**
     * resume回调
     */
    private fun onResumeImpl() {

    }

    /**
     * 渲染回调
     */
    private fun onRenderImpl() {
        synchronized(SpineBaseAdapter::class.java) {
            if (hasInit) {
//            mSkeleton.setX(mSkeleton.getX()+0.1f);
                mAnimationState.update(Gdx.graphics.deltaTime)
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
                Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
                mAnimationState.apply(mSkeleton)
                mSkeleton!!.updateWorldTransform()
                mCamera.update()
                mBatch.projectionMatrix.set(mCamera.combined)
                if (debugMode)
                    mDebugRenderer.shapeRenderer.projectionMatrix = mCamera.combined
                mBatch.begin()
                mRenderer.draw(mBatch, mSkeleton)
                mBatch.end()
                if (debugMode)
                    mDebugRenderer.draw(mSkeleton)
            }
        }
    }

    /**
     * onPause回调
     */
    private fun onPauseImpl() {
        if (!::mAnimationState.isInitialized)
            mAnimationState.clearTrack(0)
    }

    /**
     * 销毁回调
     */
    private fun onDisposeImpl() {
        mAtlas.dispose()
    }

    /**
     * 换装饰
     *
     * @param slotName       插槽名称
     * @param attachmentName 装饰名称
     * @return
     */
    fun setAttachment(slotName: String, attachmentName: String): Boolean {
        if (mSkeleton == null || TextUtils.isEmpty(slotName)) {
            return false
        }
        val slot = mSkeleton!!.findSlot(slotName) ?: return false
        if (TextUtils.isEmpty(attachmentName)) {
            slot.attachment = null
        } else {
            mSkeleton!!.getAttachment(slotName, attachmentName) ?: return false
            mSkeleton!!.setAttachment(slotName, attachmentName)
        }
        return true
    }

    /**
     * 换肤
     *
     * @param skinName 皮肤名称
     * @return
     */
    fun setSkin(skinName: String): Boolean {
        if (mSkeleton == null || mSkeletonData == null || TextUtils.isEmpty(skinName)) {
            return false
        }
        if (mSkeletonData!!.findSkin(skinName) == null) {
            return false
        }
        mSkeleton!!.setSkin(skinName)
        return true
    }

    /**
     * 创建完成的回调
     */
    interface OnSpineCreatedListener {
        fun onCreated(tag: String)
    }

    /**
     * 创建 SurfaceView 对象
     */
    @Synchronized
    fun create(activity: Activity): View {
        val spineBaseAnimView = SpineBaseAnimView(activity)
        spineBaseAnimView.adapter = this
        return spineBaseAnimView.initializeForView(this)
    }

    companion object {
        //动画切换时的过渡时间
        private const val DEFAULT_ANIM_SWITCH_TIME = 0.0f
    }
}