package android.lib.pen.demo;

import java.util.HashMap;
import java.util.Map;

import android.view.DragEvent;
import android.view.View;

/**
 * A OnDragListener that supports dragging of multiple objects by keeping track of them in a Map.
 */
final class MultiDragListener implements View.OnDragListener {
    private final Map<View, View.OnDragListener> listeners = new HashMap<View, View.OnDragListener>();

    public MultiDragListener() {
    }

    public void addListener(final View view, final View.OnDragListener listener) {
        this.listeners.put(view, listener);
    }

    @Override
    public boolean onDrag(final View view, final DragEvent event) {
        final View.OnDragListener listener = this.listeners.get(event.getLocalState());

        if (listener == null) {
            return false;
        }

        return listener.onDrag(view, event);
    }
}
