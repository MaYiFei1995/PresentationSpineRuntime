package com.mai.presentationspineruntime

import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import com.badlogic.gdx.utils.GdxNativesLoader
import com.mai.presentationspineruntime.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    companion object {
        init {
            GdxNativesLoader.load()
        }
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.showPresentationBtn.setOnClickListener {
            if (checkDrawOverlays()) {
                if (checkWriteSettings()) {
                    val displayManager: DisplayManager =
                        applicationContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
                    val display = displayManager.displays[0];

                    SpinePresentation(this@MainActivity, display).show()
                } else {
                    Toast.makeText(this@MainActivity, "需要修改系统设置权限", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this@MainActivity, "需要悬浮窗权限", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 检查显示在其他应用上层权限
     */
    private fun checkDrawOverlays(): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                startOverlayPermissionActivity()
                return false
            }
        }
        return true
    }

    /**
     * 检查修改系统设置权限
     */
    private fun checkWriteSettings(): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(this)) {
                startWriteSettingsActivity()
                return false
            }
        }
        return true
    }

    /**
     * 拉起显示在其他应用的上层设置页
     */
    private fun startOverlayPermissionActivity() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        intent.data = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
        startActivity(intent)
    }

    /**
     * 拉起修改系统设置的设置页
     */
    private fun startWriteSettingsActivity() {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
        intent.data = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
        startActivity(intent)
    }
}