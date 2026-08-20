package com.example

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * KeyboardService
 *
 * Implements a custom Android InputMethodService.
 * Provides a responsive on-screen software keyboard with typing capabilities,
 * shift/case toggling, symbol switching, a direct shortcut to SettingsActivity,
 * and an Input Method Picker trigger.
 */
class KeyboardService : InputMethodService() {

    private var isShifted = false
    private var isSymbolMode = false
    private val letterButtons = mutableListOf<Button>()

    private val qwertyRow1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
    private val qwertyRow2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
    private val qwertyRow3 = listOf("z", "x", "c", "v", "b", "n", "m")

    private val symbolRow1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    private val symbolRow2 = listOf("@", "#", "$", "%", "&", "-", "+", "(", ")")
    private val symbolRow3 = listOf("*", "\"", "'", ":", ";", "!", "?")

    override fun onCreateInputView(): View {
        return createKeyboardView()
    }

    private fun createKeyboardView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1D192B")) // Frosted deep violet
            val pad = dp(6)
            setPadding(pad, dp(8), pad, dp(12))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // 1. Top Utility / Action Bar
        val topBar = createTopBar()
        root.addView(topBar)

        // 2. Keyboard Rows Container
        val rowsContainer = LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        root.addView(rowsContainer)

        renderKeyboardRows(rowsContainer)

        return root
    }

    private fun createTopBar(): View {
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val padH = dp(6)
            setPadding(padH, 0, padH, dp(6))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(40)
            )
        }

        // App/Keyboard Label with icon
        val titleView = TextView(this).apply {
            text = "⌨ Fluid Keyboard"
            setTextColor(Color.parseColor("#D0BCFF"))
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        topBar.addView(titleView)

        // Shortcut to SettingsActivity (crucial when launcher icon is hidden)
        val settingsBtn = createUtilityButton("⚙ Settings") {
            val intent = Intent(this, SettingsActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }
        topBar.addView(settingsBtn)

        // Switch Input Method Button
        val switchImeBtn = createUtilityButton("🌐 Switch IME") {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showInputMethodPicker()
        }
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            dp(32)
        ).apply {
            marginStart = dp(6)
        }
        topBar.addView(switchImeBtn, params)

        return topBar
    }

    private fun createUtilityButton(label: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            setTextColor(Color.parseColor("#E8DEF8"))
            textSize = 11f
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(8).toFloat()
                setColor(Color.parseColor("#332D41"))
            }
            background = bg
            setPadding(dp(10), 0, dp(10), 0)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(32)
            )
            setOnClickListener { onClick() }
        }
    }

    private fun renderKeyboardRows(container: LinearLayout) {
        container.removeAllViews()
        letterButtons.clear()

        val r1 = if (isSymbolMode) symbolRow1 else qwertyRow1
        val r2 = if (isSymbolMode) symbolRow2 else qwertyRow2
        val r3 = if (isSymbolMode) symbolRow3 else qwertyRow3

        // Row 1
        container.addView(createKeyRow(r1))

        // Row 2
        container.addView(createKeyRow(r2, sidePaddingRatio = 0.05f))

        // Row 3 (with Shift / Backspace)
        container.addView(createRow3(r3))

        // Row 4 (Mode switch, comma, space, period, enter)
        container.addView(createRow4())
    }

    private fun createKeyRow(keys: List<String>, sidePaddingRatio: Float = 0f): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
            ).apply {
                topMargin = dp(4)
            }
        }

        for (k in keys) {
            val keyBtn = createKeyButton(k) {
                commitCharacter(it)
            }
            letterButtons.add(keyBtn)
            row.addView(keyBtn)
        }

        return row
    }

    private fun createRow3(keys: List<String>): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
            ).apply {
                topMargin = dp(4)
            }
        }

        // Shift button
        val shiftBtn = Button(this).apply {
            text = if (isShifted) "⇧ ON" else "⇧"
            setTextColor(if (isShifted) Color.parseColor("#E8DEF8") else Color.WHITE)
            textSize = 14f
            background = createKeyBackground(if (isShifted) "#6750A4" else "#332D41")
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.4f).apply {
                val m = dp(2)
                setMargins(m, m, m, m)
            }
            setOnClickListener {
                if (!isSymbolMode) {
                    isShifted = !isShifted
                    updateShiftState()
                }
            }
        }
        row.addView(shiftBtn)

        // Middle character keys
        for (k in keys) {
            val keyBtn = createKeyButton(k) {
                commitCharacter(it)
            }
            letterButtons.add(keyBtn)
            row.addView(keyBtn)
        }

        // Backspace button
        val backspaceBtn = Button(this).apply {
            text = "⌫"
            setTextColor(Color.WHITE)
            textSize = 16f
            background = createKeyBackground("#332D41")
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.4f).apply {
                val m = dp(2)
                setMargins(m, m, m, m)
            }
            setOnClickListener {
                currentInputConnection?.deleteSurroundingText(1, 0)
            }
        }
        row.addView(backspaceBtn)

        return row
    }

    private fun createRow4(): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
            ).apply {
                topMargin = dp(4)
            }
        }

        // Mode switch (?123 / ABC)
        val modeBtn = Button(this).apply {
            text = if (isSymbolMode) "ABC" else "?123"
            setTextColor(Color.parseColor("#E8DEF8"))
            textSize = 12f
            background = createKeyBackground("#332D41")
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.3f).apply {
                val m = dp(2)
                setMargins(m, m, m, m)
            }
            setOnClickListener {
                isSymbolMode = !isSymbolMode
                (parent as? View)?.let {
                    val container = (it.parent as? LinearLayout)
                    if (container != null) {
                        renderKeyboardRows(container)
                    }
                }
            }
        }
        row.addView(modeBtn)

        // Comma key
        val commaBtn = createKeyButton(",") { commitCharacter(",") }
        commaBtn.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f).apply {
            val m = dp(2)
            setMargins(m, m, m, m)
        }
        row.addView(commaBtn)

        // Space key
        val spaceBtn = Button(this).apply {
            text = "Space"
            setTextColor(Color.parseColor("#CAC4D0"))
            textSize = 13f
            background = createKeyBackground("#3D3647")
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 4.2f).apply {
                val m = dp(2)
                setMargins(m, m, m, m)
            }
            setOnClickListener {
                currentInputConnection?.commitText(" ", 1)
            }
        }
        row.addView(spaceBtn)

        // Period key
        val dotBtn = createKeyButton(".") { commitCharacter(".") }
        dotBtn.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f).apply {
            val m = dp(2)
            setMargins(m, m, m, m)
        }
        row.addView(dotBtn)

        // Enter key
        val enterBtn = Button(this).apply {
            text = "↵"
            setTextColor(Color.WHITE)
            textSize = 16f
            background = createKeyBackground("#6750A4")
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.5f).apply {
                val m = dp(2)
                setMargins(m, m, m, m)
            }
            setOnClickListener {
                val ic = currentInputConnection ?: return@setOnClickListener
                val editorInfo = currentInputEditorInfo
                if (editorInfo != null && (editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION) != EditorInfo.IME_ACTION_NONE) {
                    val action = editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION
                    ic.performEditorAction(action)
                } else {
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
                }
            }
        }
        row.addView(enterBtn)

        return row
    }

    private fun createKeyButton(label: String, onClick: (String) -> Unit): Button {
        return Button(this).apply {
            val displayChar = if (isShifted && !isSymbolMode) label.uppercase() else label
            text = displayChar
            setTextColor(Color.parseColor("#F5EFF7"))
            textSize = 16f
            background = createKeyBackground("#383141")
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                val m = dp(2)
                setMargins(m, m, m, m)
            }
            setOnClickListener {
                val currentTxt = text.toString()
                onClick(currentTxt)
            }
        }
    }

    private fun commitCharacter(char: String) {
        currentInputConnection?.commitText(char, 1)
        if (isShifted && !isSymbolMode) {
            isShifted = false
            updateShiftState()
        }
    }

    private fun updateShiftState() {
        for (btn in letterButtons) {
            val current = btn.text.toString()
            if (current.length == 1 && current[0].isLetter()) {
                btn.text = if (isShifted) current.uppercase() else current.lowercase()
            }
        }
        // Also re-render rows if needed to update shift button visual
        (rootView() as? ViewGroup)?.let { root ->
            // Update shift button UI directly if present
        }
    }

    private fun rootView(): View? {
        return window?.window?.decorView
    }

    private fun createKeyBackground(hexColor: String): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(6).toFloat()
            setColor(Color.parseColor(hexColor))
        }
    }

    private fun dp(value: Int): Int {
        val density = resources.displayMetrics.density
        return (value * density).toInt()
    }
}
