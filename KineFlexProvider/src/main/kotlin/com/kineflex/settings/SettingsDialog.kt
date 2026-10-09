package com.kineflex.settings

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.InputType
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kineflex.BuildConfig
import com.kineflex.kineflex.KineFlexApi
import com.kineflex.tmdb.TmdbApi
import com.lagradost.cloudstream3.plugins.Plugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Settings dialog for configuring KineFlex Bearer API Key and TMDB credentials.
 * Implements show/hide toggles, connection testing, persistent saving, and key clearing.
 */
class SettingsDialog(private val plugin: Plugin) : BottomSheetDialogFragment() {

    private var isKineFlexVisible = false
    private var isTmdbVisible = false

    @SuppressLint("DiscouragedApi")
    private fun <T : View> View.findViewByName(name: String): T? {
        val id = plugin.resources?.getIdentifier(name, "id", BuildConfig.LIBRARY_PACKAGE_NAME)
            ?: return null
        return findViewById(id)
    }

    @SuppressLint("DiscouragedApi")
    private fun getStringRes(name: String): String? {
        val id = plugin.resources?.getIdentifier(name, "string", BuildConfig.LIBRARY_PACKAGE_NAME)
            ?: return null
        return plugin.resources?.getString(id)
    }

    @SuppressLint("DiscouragedApi")
    private fun getDrawableRes(name: String): Drawable? {
        val id = plugin.resources?.getIdentifier(name, "drawable", BuildConfig.LIBRARY_PACKAGE_NAME)
            ?: return null
        return plugin.resources?.let { ResourcesCompat.getDrawable(it, id, null) }
    }

    @SuppressLint("DiscouragedApi")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layoutId = plugin.resources?.getIdentifier(
            "dialog_settings",
            "layout",
            BuildConfig.LIBRARY_PACKAGE_NAME
        ) ?: return null

        return plugin.resources?.getLayout(layoutId)?.let {
            inflater.inflate(it, container, false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val editKineFlex = view.findViewByName<EditText>("edit_kineflex_key")
        val editTmdb = view.findViewByName<EditText>("edit_tmdb_key")
        val toggleKineFlex = view.findViewByName<ImageView>("btn_toggle_kineflex_visibility")
        val toggleTmdb = view.findViewByName<ImageView>("btn_toggle_tmdb_visibility")
        val textKineFlexStatus = view.findViewByName<TextView>("text_kineflex_status")
        val textTmdbStatus = view.findViewByName<TextView>("text_tmdb_status")
        val textTestResult = view.findViewByName<TextView>("text_test_result")
        val btnTest = view.findViewByName<Button>("btn_test")
        val btnClear = view.findViewByName<Button>("btn_clear")
        val btnCancel = view.findViewByName<Button>("btn_cancel")
        val btnSave = view.findViewByName<Button>("btn_save")

        // Load current values
        val currentKineFlexKey = KineFlexSettings.getKineFlexApiKey(requireContext())
        val currentTmdbKey = KineFlexSettings.getTmdbCredential(requireContext())

        editKineFlex?.setText(currentKineFlexKey)
        editTmdb?.setText(currentTmdbKey)

        updateStatusLabels(currentKineFlexKey, currentTmdbKey, textKineFlexStatus, textTmdbStatus)

        // Toggle KineFlex password visibility
        toggleKineFlex?.setOnClickListener {
            isKineFlexVisible = !isKineFlexVisible
            if (isKineFlexVisible) {
                editKineFlex?.transformationMethod = HideReturnsTransformationMethod.getInstance()
                toggleKineFlex.setImageDrawable(getDrawableRes("ic_visibility_24"))
            } else {
                editKineFlex?.transformationMethod = PasswordTransformationMethod.getInstance()
                toggleKineFlex.setImageDrawable(getDrawableRes("ic_visibility_off_24"))
            }
            editKineFlex?.setSelection(editKineFlex.text?.length ?: 0)
        }

        // Toggle TMDB credential visibility
        toggleTmdb?.setOnClickListener {
            isTmdbVisible = !isTmdbVisible
            if (isTmdbVisible) {
                editTmdb?.transformationMethod = HideReturnsTransformationMethod.getInstance()
                toggleTmdb.setImageDrawable(getDrawableRes("ic_visibility_24"))
            } else {
                editTmdb?.transformationMethod = PasswordTransformationMethod.getInstance()
                toggleTmdb.setImageDrawable(getDrawableRes("ic_visibility_off_24"))
            }
            editTmdb?.setSelection(editTmdb.text?.length ?: 0)
        }

        // Test connection action
        btnTest?.setOnClickListener {
            val kKey = editKineFlex?.text?.toString()?.trim().orEmpty()
            val tKey = editTmdb?.text?.toString()?.trim().orEmpty()

            textTestResult?.visibility = View.VISIBLE
            textTestResult?.text = "Testing connection..."
            textTestResult?.setTextColor(Color.parseColor("#B0B0B0"))

            CoroutineScope(Dispatchers.IO).launch {
                val sb = StringBuilder()

                if (kKey.isNotEmpty()) {
                    val kResult = KineFlexApi.testConnection(kKey)
                    kResult.onSuccess { sb.append("• KineFlex: $it\n") }
                    kResult.onFailure { sb.append("• KineFlex Error: ${it.message}\n") }
                } else {
                    sb.append("• KineFlex: Key not entered\n")
                }

                if (tKey.isNotEmpty()) {
                    val tResult = TmdbApi.testCredential(tKey)
                    tResult.onSuccess { sb.append("• TMDB: $it\n") }
                    tResult.onFailure { sb.append("• TMDB Error: ${it.message}\n") }
                } else {
                    sb.append("• TMDB: Key not entered\n")
                }

                withContext(Dispatchers.Main) {
                    textTestResult?.text = sb.toString().trimEnd()
                }
            }
        }

        // Clear keys action
        btnClear?.setOnClickListener {
            editKineFlex?.setText("")
            editTmdb?.setText("")
            KineFlexSettings.clearAll(requireContext())
            updateStatusLabels("", "", textKineFlexStatus, textTmdbStatus)
            textTestResult?.visibility = View.GONE
            Toast.makeText(context, getStringRes("toast_cleared") ?: "Credentials cleared", Toast.LENGTH_SHORT).show()
        }

        // Cancel action
        btnCancel?.setOnClickListener {
            dismiss()
        }

        // Save action
        btnSave?.setOnClickListener {
            val newKineFlexKey = editKineFlex?.text?.toString()?.trim().orEmpty()
            val newTmdbKey = editTmdb?.text?.toString()?.trim().orEmpty()

            KineFlexSettings.setKineFlexApiKey(requireContext(), newKineFlexKey)
            KineFlexSettings.setTmdbCredential(requireContext(), newTmdbKey)

            updateStatusLabels(newKineFlexKey, newTmdbKey, textKineFlexStatus, textTmdbStatus)

            Toast.makeText(context, getStringRes("toast_saved") ?: "Credentials saved", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    private fun updateStatusLabels(
        kineFlexKey: String,
        tmdbKey: String,
        textKineFlex: TextView?,
        textTmdb: TextView?
    ) {
        if (kineFlexKey.isNotEmpty()) {
            textKineFlex?.text = "KineFlex: Configured"
            textKineFlex?.setTextColor(Color.parseColor("#4CAF50"))
        } else {
            textKineFlex?.text = "KineFlex: Not Configured"
            textKineFlex?.setTextColor(Color.parseColor("#CF6679"))
        }

        if (tmdbKey.isNotEmpty()) {
            textTmdb?.text = "TMDB: Configured"
            textTmdb?.setTextColor(Color.parseColor("#4CAF50"))
        } else {
            textTmdb?.text = "TMDB: Not Configured"
            textTmdb?.setTextColor(Color.parseColor("#CF6679"))
        }
    }
}
