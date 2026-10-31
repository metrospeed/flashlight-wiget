package com.flashlightwidget

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton

class MainActivity : Activity() {

    private lateinit var toggle: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toggle = findViewById(R.id.toggle)
        toggle.setOnClickListener {
            startForegroundService(FlashlightService.intent(this, FlashlightService.ACTION_TOGGLE))
        }

        val addWidget = findViewById<Button>(R.id.add_widget)
        val widgetManager = AppWidgetManager.getInstance(this)
        if (widgetManager.isRequestPinAppWidgetSupported) {
            addWidget.setOnClickListener {
                widgetManager.requestPinAppWidget(ComponentName(this, FlashlightWidget::class.java), null, null)
            }
        } else {
            addWidget.visibility = View.GONE
        }

        // Lets the "Flashlight is on" notification show; the app works without it.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }
    }

    override fun onResume() {
        super.onResume()
        FlashlightService.listener = ::render
        render()
    }

    override fun onPause() {
        FlashlightService.listener = null
        super.onPause()
    }

    private fun render() {
        toggle.setImageResource(if (FlashlightService.isOn) R.drawable.widget_on else R.drawable.widget_off)
    }
}
