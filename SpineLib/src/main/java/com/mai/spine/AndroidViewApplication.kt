package com.mai.spine

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import com.badlogic.gdx.*
import com.badlogic.gdx.Application.ApplicationType
import com.badlogic.gdx.backends.android.*
import com.badlogic.gdx.backends.android.surfaceview.FillResolutionStrategy
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Clipboard
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.SnapshotArray

/**
 * View Application
 */
open class AndroidViewApplication : View, AndroidApplicationBase {
    private var mActivity: Activity
    private lateinit var graphics: AndroidGraphics
    private lateinit var mInput: AndroidInput
    protected lateinit var audio: AndroidAudio
    private lateinit var files: AndroidFiles
    private lateinit var net: AndroidNet
    private lateinit var clipboard: AndroidClipboard
    private lateinit var listener: ApplicationListener
    lateinit var mHandler: Handler
    private val mRunnableArray = Array<Runnable>()
    private val mExecutedRunnableArray = Array<Runnable>()
    private val mLifecycleListeners =
        SnapshotArray<LifecycleListener>(LifecycleListener::class.java)

    private var mLogLevel = Application.LOG_INFO
    private lateinit var mApplicationLogger: ApplicationLogger

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        this.mActivity = context as Activity
    }

    /**
     * This method has to be called in the { Activity#onCreate(Bundle)} method. It sets up all the things necessary to get
     * input, render via OpenGL and so on. Uses a default [AndroidApplicationConfiguration].
     *
     *
     * Note: you have to add the returned view to your layout!
     *
     * @param listener the [ApplicationListener] implementing the program logic
     * @return the GLSurfaceView of the application
     */
    open fun initializeForView(listener: ApplicationListener): View {
        val config = AndroidApplicationConfiguration()
        return initializeForView(listener, config)
    }

    /**
     * This method has to be called in the { Activity#onCreate(Bundle)} method. It sets up all the things necessary to get
     * input, render via OpenGL and so on. You can configure other aspects of the application with the rest of the fields in the
     * [AndroidApplicationConfiguration] instance.
     *
     *
     * Note: you have to add the returned view to your layout!
     *
     * @param listener the [ApplicationListener] implementing the program logic
     * @param config   the [AndroidApplicationConfiguration], defining various settings of the application (use accelerometer,
     * etc.).
     * @return the GLSurfaceView of the application
     */
    fun initializeForView(
        listener: ApplicationListener,
        config: AndroidApplicationConfiguration
    ): View {
        init(listener, config)
        return graphics.view
    }

    private fun init(listener: ApplicationListener, config: AndroidApplicationConfiguration) {
        if (this.version < AndroidApplicationBase.MINIMUM_SDK) {
            throw GdxRuntimeException("LibGDX requires Android API Level " + AndroidApplicationBase.MINIMUM_SDK + " or later.")
        }
        applicationLogger = AndroidApplicationLogger()
        graphics = AndroidGraphics(
            this,
            config,
            if (config.resolutionStrategy == null)
                FillResolutionStrategy()
            else
                config.resolutionStrategy
        )
        mInput = AndroidInputFactory.newAndroidInput(this, mActivity, graphics.view, config)
        audio = AndroidAudio(mActivity, config)
        mActivity.filesDir // workaround for Android bug #10515463
        files = AndroidFiles(mActivity.assets, mActivity.filesDir.absolutePath)
        net = AndroidNet(this, config)
        this.listener = listener
        mHandler = Handler(Looper.getMainLooper())
        clipboard = AndroidClipboard(mActivity)

        // Add a specialized audio lifecycle listener
        addLifecycleListener(object : LifecycleListener {
            override fun resume() {
                // No need to resume audio here
            }

            override fun pause() {
                // audio.pause();
            }

            override fun dispose() {
                audio.dispose()
            }
        })
        Gdx.app = this
        Gdx.input = input
        Gdx.audio = audio
        Gdx.files = files
        Gdx.graphics = graphics
        Gdx.net = net
    }

    override fun getRunnables(): Array<Runnable> {
        return mRunnableArray
    }

    override fun getExecutedRunnables(): Array<Runnable> {
        return mExecutedRunnableArray
    }

    override fun runOnUiThread(runnable: Runnable) {}
    override fun startActivity(intent: Intent) {}
    override fun getApplicationListener(): ApplicationListener {
        return listener
    }

    override fun getGraphics(): Graphics {
        return graphics
    }

    override fun getAudio(): Audio {
        return audio
    }

    override fun getInput(): AndroidInput {
        return mInput
    }

    override fun getFiles(): Files {
        return files
    }

    override fun getNet(): Net {
        return net
    }

    override fun log(tag: String, message: String) {
        if (mLogLevel >= Application.LOG_INFO)
            applicationLogger.log(tag, message)
    }

    override fun log(tag: String, message: String, exception: Throwable) {
        if (mLogLevel >= Application.LOG_INFO)
            applicationLogger.log(tag, message, exception)
    }

    override fun error(tag: String, message: String) {
        if (mLogLevel >= Application.LOG_ERROR)
            applicationLogger.error(tag, message)
    }

    override fun error(tag: String, message: String, exception: Throwable) {
        if (mLogLevel >= Application.LOG_ERROR)
            applicationLogger.error(
                tag,
                message,
                exception
            )
    }

    override fun debug(tag: String, message: String) {
        if (mLogLevel >= Application.LOG_DEBUG)
            applicationLogger.debug(tag, message)
    }

    override fun debug(tag: String, message: String, exception: Throwable) {
        if (mLogLevel >= Application.LOG_DEBUG)
            applicationLogger.debug(
                tag,
                message,
                exception
            )
    }

    override fun setLogLevel(logLevel: Int) {
        this.mLogLevel = logLevel
    }

    override fun getLogLevel(): Int {
        return mLogLevel
    }

    override fun setApplicationLogger(applicationLogger: ApplicationLogger) {
        this.mApplicationLogger = applicationLogger
    }

    override fun getApplicationLogger(): ApplicationLogger {
        return mApplicationLogger
    }

    override fun getType(): ApplicationType {
        return ApplicationType.Android
    }

    override fun getVersion(): Int {
        return Build.VERSION.SDK_INT
    }

    override fun getJavaHeap(): Long {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
    }

    override fun getNativeHeap(): Long {
        return Debug.getNativeHeapAllocatedSize()
    }

    override fun getPreferences(name: String): Preferences {
        return AndroidPreferences(context.getSharedPreferences(name, Context.MODE_PRIVATE))
    }

    override fun getClipboard(): Clipboard {
        return clipboard
    }

    override fun postRunnable(runnable: Runnable) {
        synchronized(runnables) {
            runnables.add(runnable)
            Gdx.graphics.requestRendering()
        }
    }

    override fun exit() {
        mHandler.post {
            (parent as ViewGroup).removeView(this@AndroidViewApplication)
        }
    }

    override fun addLifecycleListener(listener: LifecycleListener) {
        synchronized(mLifecycleListeners) {
            mLifecycleListeners.add(listener)
        }
    }

    override fun removeLifecycleListener(listener: LifecycleListener) {
        synchronized(mLifecycleListeners) {
            mLifecycleListeners.removeValue(listener, true)
        }
    }

    override fun getLifecycleListeners(): SnapshotArray<LifecycleListener> {
        return mLifecycleListeners
    }

    override fun getApplicationWindow(): Window {
        return mActivity.window
    }

    @TargetApi(19)
    override fun getWindowManager(): WindowManager {
        return mActivity.windowManager
    }

    override fun useImmersiveMode(use: Boolean) {}
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        graphics.clearManagedCaches()
    }

    override fun getHandler(): Handler? {
        return this.mHandler
    }
}