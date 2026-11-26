package com.kdt.mcgui;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;

public class MineEditText extends androidx.appcompat.widget.AppCompatEditText {
    public MineEditText(Context ctx) {
        super(ctx);
        init();
    }

    public MineEditText(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        init();
    }

    public MineEditText(Context ctx, AttributeSet attrs, int defStyleAttr) {
        super(ctx, attrs, defStyleAttr);
        init();
    }

    public void init() {
        // Cliorius: jangan menimpa background yang sudah diatur dari layout
        // (cli_input_field). Hanya pasang warna fallback bila belum ada background.
        if (getBackground() == null) {
            setBackgroundColor(Color.parseColor("#1A1D23"));
        }
        int pad = (int) (getResources().getDisplayMetrics().density * 12);
        setPadding(pad, getPaddingTop(), pad, getPaddingBottom());
    }
}
