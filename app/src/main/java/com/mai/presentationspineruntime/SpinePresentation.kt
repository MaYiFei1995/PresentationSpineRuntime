package com.mai.presentationspineruntime

import android.app.Activity
import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import com.mai.presentationspineruntime.databinding.PresentationSpineBinding
import com.mai.presentationspineruntime.databinding.SpineOverlayBinding
import com.mai.spine.OnSpineClickListener
import com.mai.spine.PUSurfaceView
import com.mai.spine.SpineBaseAdapter

class SpinePresentation(private val outerContext: Context, display: Display) :
    Presentation(outerContext, display),
    SpineBaseAdapter.OnSpineCreatedListener {

    private lateinit var binding: PresentationSpineBinding
    private var bool = false
    private var mHandler = object : Handler(Looper.getMainLooper()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = PresentationSpineBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val glSurfaceView = generateBoyGLSurfaceView("Boy1")
        showSpine(glSurfaceView, binding.boyContainer1)

    }

    private fun showSpine(glSurfaceView: View, container: ViewGroup) {
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        glSurfaceView.layoutParams = params

        container.addView(glSurfaceView)
    }

    private fun generateBoyGLSurfaceView(tag: String): View {
        val boyAdapter = BoyAdapter(0, 50, 0, 20)
        boyAdapter.skinName = "default"
        boyAdapter.animationName = "walk"
        boyAdapter.debugMode = tag == "Boy1"
        boyAdapter.tag = tag
        boyAdapter.setOnCreatedListener(this)
        boyAdapter.setOnSpineClickListener(object : OnSpineClickListener {
            override fun onClick() {
                Log.e("SpineTest", "on spine:${boyAdapter.tag} clicked")
            }
        })

        return boyAdapter.create(outerContext as Activity)
    }

    override fun onCreated(tag: String) {
        mHandler.post {
            if (tag == "Boy1") {
                val glSurfaceView = generateBoyGLSurfaceView("Boy2")
                showSpine(glSurfaceView, binding.boyContainer2)
            }
            if (tag == "Boy2") {
                binding.surfaceBtn.setOnClickListener {
                    if (bool)
                        return@setOnClickListener
                    bool = true

                    val overlyRoot = SpineOverlayBinding.inflate(layoutInflater).root

                    val newPU = PUSurfaceView(outerContext)
                    val layoutParams = FrameLayout.LayoutParams(180, 180)
                    layoutParams.gravity = Gravity.CENTER_VERTICAL or Gravity.END
                    newPU.layoutParams = layoutParams
                    newPU.drawImage(R.mipmap.octocat)
                    overlyRoot.addView(newPU)

                    overlyRoot.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    binding.root.addView(overlyRoot)

                    newPU.postDelayed({
                        overlyRoot.removeView(newPU)
                        binding.root.removeView(overlyRoot)
                        bool = false
                    }, 3000)
                }
            }
        }
    }

}