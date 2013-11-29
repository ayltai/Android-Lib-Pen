package android.lib.pen;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ToggleButton;

public final class PenButton extends ToggleButton {
    public PenButton(final Context context) {
        super(context);
    }

    public PenButton(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public PenButton(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void toggle() {
    }
}
