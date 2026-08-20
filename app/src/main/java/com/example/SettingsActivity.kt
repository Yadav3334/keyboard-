package com.example

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch

/**
 * SettingsActivity
 *
 * Serves as the main configuration screen for the Input Method app.
 * Allows the user to:
 * 1. Safely toggle the home screen launcher icon visibility using PackageManager
 *    targeting the <activity-alias> (SettingsActivityAlias).
 * 2. Navigate to System Keyboard Settings (Settings.ACTION_INPUT_METHOD_SETTINGS).
 * 3. Open the system Input Method Picker dialog.
 * 4. Test typing with the custom keyboard in real-time.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var switchLauncherIcon: MaterialSwitch
    private lateinit var tvIconStatus: TextView
    private lateinit var viewIconStatusDot: android.view.View
    private lateinit var tvImeStatus: TextView
    private lateinit var tvActiveBadge: TextView
    private lateinit var btnOpenImeSettings: MaterialButton
    private lateinit var btnSwitchIme: MaterialButton

    private val aliasComponentName by lazy {
        ComponentName(this, "com.example.SettingsActivityAlias")
    }

    companion object {
        private const val PREFS_NAME = "keyboard_settings_prefs"
        private const val KEY_LAUNCHER_ICON_VISIBLE = "key_launcher_icon_visible"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        initViews()
        setupListeners()
        updateLauncherIconUiState(isLauncherIconEnabled())
    }

    override fun onResume() {
        super.onResume()
        refreshImeStatus()
        // Sync icon toggle state with system package manager in case it changed
        updateLauncherIconUiState(isLauncherIconEnabled())
    }

    private fun initViews() {
        switchLauncherIcon = findViewById(R.id.switch_launcher_icon)
        tvIconStatus = findViewById(R.id.tv_icon_status)
        viewIconStatusDot = findViewById(R.id.view_icon_status_dot)
        tvImeStatus = findViewById(R.id.tv_ime_status)
        tvActiveBadge = findViewById(R.id.tv_active_badge)
        btnOpenImeSettings = findViewById(R.id.btn_open_ime_settings)
        btnSwitchIme = findViewById(R.id.btn_switch_ime)
    }

    private fun setupListeners() {
        // Switch listener to toggle launcher icon visibility
        switchLauncherIcon.setOnClickListener {
            val isChecked = switchLauncherIcon.isChecked
            if (!isChecked) {
                // Confirm before hiding the icon so user understands how to reopen settings
                showHideIconConfirmationDialog()
            } else {
                setLauncherIconVisibility(true)
            }
        }

        // Button 1: Open system Keyboard Settings
        btnOpenImeSettings.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Unable to open Keyboard Settings", Toast.LENGTH_SHORT).show()
            }
        }

        // Button 2: Trigger Input Method Switcher Picker
        btnSwitchIme.setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            if (imm != null) {
                imm.showInputMethodPicker()
            } else {
                Toast.makeText(this, "Input Method Manager not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Checks whether the <activity-alias> launcher icon is currently enabled.
     * Evaluates both PackageManager component state and SharedPreferences.
     */
    private fun isLauncherIconEnabled(): Boolean {
        val state = packageManager.getComponentEnabledSetting(aliasComponentName)
        return when (state) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> false
            else -> {
                // COMPONENT_ENABLED_STATE_DEFAULT: defaults to true (manifest android:enabled="true")
                prefs.getBoolean(KEY_LAUNCHER_ICON_VISIBLE, true)
            }
        }
    }

    /**
     * Toggles the launcher icon by enabling or disabling the <activity-alias>.
     *
     * Why activity-alias?
     * Disabling the activity-alias removes the MAIN/LAUNCHER entry point from the
     * home screen launcher/app drawer, while keeping SettingsActivity intact.
     * This avoids disabling the entire activity and allows SettingsActivity to be
     * launched from system settings or other shortcuts without errors.
     */
    private fun setLauncherIconVisibility(enable: Boolean) {
        val newState = if (enable) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }

        try {
            packageManager.setComponentEnabledSetting(
                aliasComponentName,
                newState,
                PackageManager.DONT_KILL_APP
            )

            // Save state to SharedPreferences
            prefs.edit().putBoolean(KEY_LAUNCHER_ICON_VISIBLE, enable).apply()

            updateLauncherIconUiState(enable)

            val msg = if (enable) {
                "Launcher icon is now visible on home screen"
            } else {
                "Launcher icon is now hidden from home screen"
            }
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Failed to update icon state: ${e.message}", Toast.LENGTH_SHORT).show()
            updateLauncherIconUiState(isLauncherIconEnabled())
        }
    }

    private fun showHideIconConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.hide_dialog_title)
            .setMessage(R.string.hide_dialog_message)
            .setPositiveButton(R.string.hide_dialog_confirm) { _, _ ->
                setLauncherIconVisibility(false)
            }
            .setNegativeButton(R.string.hide_dialog_cancel) { dialog, _ ->
                // Revert switch to checked state
                switchLauncherIcon.isChecked = true
                dialog.dismiss()
            }
            .setOnCancelListener {
                switchLauncherIcon.isChecked = true
            }
            .show()
    }

    private fun updateLauncherIconUiState(isVisible: Boolean) {
        switchLauncherIcon.isChecked = isVisible

        if (isVisible) {
            tvIconStatus.text = getString(R.string.launcher_icon_visible_status)
            tvIconStatus.setTextColor(Color.parseColor("#0F5132")) // Deep emerald
            viewIconStatusDot.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#10B981"))
        } else {
            tvIconStatus.text = getString(R.string.launcher_icon_hidden_status)
            tvIconStatus.setTextColor(Color.parseColor("#B3261E")) // Soft crimson
            viewIconStatusDot.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#B3261E"))
        }
    }

    /**
     * Checks if KeyboardService is enabled and selected as active input method in the system.
     */
    private fun refreshImeStatus() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val enabledMethods = imm?.enabledInputMethodList ?: emptyList()
        val isServiceEnabled = enabledMethods.any { it.packageName == packageName }

        val defaultIme = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: ""
        val isServiceActive = defaultIme.contains(packageName)

        if (isServiceActive) {
            tvActiveBadge.text = "ACTIVE"
            tvActiveBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#E8DEF8"))
            tvActiveBadge.setTextColor(Color.parseColor("#1D192B"))
            tvImeStatus.text = "Keyboard is active and ready to use"
        } else if (isServiceEnabled) {
            tvActiveBadge.text = "ENABLED"
            tvActiveBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#E8DEF8"))
            tvActiveBadge.setTextColor(Color.parseColor("#1D192B"))
            tvImeStatus.text = "Enabled • Tap 'Switch Input Method' to activate"
        } else {
            tvActiveBadge.text = "SETUP NEEDED"
            tvActiveBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FCE8E6"))
            tvActiveBadge.setTextColor(Color.parseColor("#B3261E"))
            tvImeStatus.text = "Not enabled in Android Settings"
        }
    }
}
