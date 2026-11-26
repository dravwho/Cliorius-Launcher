package com.kdt.mcgui

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import net.kdt.pojavlaunch.firefly.R

/**
 * Kartu menu Cliorius: tile ikon + judul + subjudul + chevron.
 * Menggantikan LauncherMenuButton bawaan Pojav agar tampilan konsisten & modern.
 */
class CliMenuCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val iconView: ImageView
    private val titleView: TextView
    private val subtitleView: TextView

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = ContextCompat.getDrawable(context, R.drawable.cli_menu_card)
        isClickable = true
        isFocusable = true
        minimumHeight = dp(64)
        setPadding(dp(12), dp(10), dp(14), dp(10))

        // --- tile ikon ---
        val tile = FrameLayout(context).apply {
            background = ContextCompat.getDrawable(context, R.drawable.cli_icon_tile)
            layoutParams = LayoutParams(dp(40), dp(40))
        }
        iconView = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER)
            setColorFilter(ContextCompat.getColor(context, R.color.primary_text))
        }
        tile.addView(iconView)
        addView(tile)

        // --- teks ---
        val texts = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(14)
                marginEnd = dp(8)
            }
        }
        titleView = TextView(context).apply {
            setTextColor(ContextCompat.getColor(context, R.color.primary_text))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            letterSpacing = 0.01f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        subtitleView = TextView(context).apply {
            setTextColor(ContextCompat.getColor(context, R.color.secondary_text))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setPadding(0, dp(2), 0, 0)
        }
        texts.addView(titleView)
        texts.addView(subtitleView)
        addView(texts)

        // --- chevron ---
        val chevron = ImageView(context).apply {
            setImageResource(R.drawable.cli_chevron)
            layoutParams = LayoutParams(dp(18), dp(18))
            alpha = 0.7f
        }
        addView(chevron)

        // baca atribut custom bila ada
        if (attrs != null) {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.CliMenuCard)
            val icon = ta.getResourceId(R.styleable.CliMenuCard_cliIcon, 0)
            val title = ta.getString(R.styleable.CliMenuCard_cliTitle)
            val subtitle = ta.getString(R.styleable.CliMenuCard_cliSubtitle)
            ta.recycle()
            if (icon != 0) iconView.setImageResource(icon)
            if (title != null) titleView.text = title
            if (subtitle != null) subtitleView.text = subtitle
        }
    }

    fun setIcon(resId: Int) = iconView.setImageResource(resId)
    fun setTitle(text: CharSequence) { titleView.text = text }
    fun setSubtitle(text: CharSequence) {
        subtitleView.text = text
        subtitleView.visibility = if (text.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun dp(v: Int): Int =
        (v * resources.displayMetrics.density).toInt()
}
