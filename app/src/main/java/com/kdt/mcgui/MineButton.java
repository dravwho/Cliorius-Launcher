package com.kdt.mcgui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.core.content.res.ResourcesCompat;

import net.kdt.pojavlaunch.firefly.R;

public class MineButton extends androidx.appcompat.widget.AppCompatButton {

    public MineButton(Context ctx) {
        this(ctx, null);
    }

    public MineButton(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        init();
    }

    public void init() {
        setTypeface(ResourcesCompat.getFont(getContext(), R.font.noto_sans_bold));
        setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen._14ssp));
        setAllCaps(true);
        // Background diatur dari layout (cli_play_button) agar konsisten
        // Cliorius: animasi tekan halus (scale 0.97) — ringan, tanpa lag
        android.animation.StateListAnimator sla = android.animation.AnimatorInflater.loadStateListAnimator(
                getContext(), R.animator.cli_press_scale);
        setStateListAnimator(sla);
    }
}
