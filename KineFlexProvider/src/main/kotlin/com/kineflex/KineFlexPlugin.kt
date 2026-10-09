package com.kineflex

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.kineflex.settings.SettingsDialog
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class KineFlexPlugin : Plugin() {
    private var activity: AppCompatActivity? = null

    override fun load(context: Context) {
        activity = context as? AppCompatActivity

        // Register the KineFlex provider
        registerMainAPI(KineFlexProvider())

        // Hook the extension settings dialog
        openSettings = {
            val dialog = SettingsDialog(this)
            activity?.let { act ->
                dialog.show(act.supportFragmentManager, "KineFlexSettings")
            }
        }
    }
}
