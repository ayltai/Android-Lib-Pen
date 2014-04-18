package android.lib.pen.demo;

import android.content.ClipData;
import android.graphics.Point;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;

final class DragDropUtils {
    private static final CharSequence EMPTY = ""; //$NON-NLS-1$

    private DragDropUtils() {
    }

    /**
     * Standard drag & drop implementation.
     */
    public static void addDragDropSupport(final View dragHandle, final View dragView, final View dropTarget) {
        dragHandle.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(final View view, final MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    dragView.startDrag(ClipData.newPlainText(DragDropUtils.EMPTY, DragDropUtils.EMPTY), new View.DragShadowBuilder(dragView) {
                        @Override
                        public void onProvideShadowMetrics(final Point shadowSize, final Point shadowTouchPoint) {
                            shadowSize.set(this.getView().getWidth(), this.getView().getHeight());
                            shadowTouchPoint.set(30, 30);
                        }
                    }, dragView, 0);

                    dragView.setVisibility(View.INVISIBLE);

                    return true;
                }

                return false;
            }
        });

        MultiDragListener listener = (MultiDragListener)dropTarget.getTag();

        if (listener == null) {
            listener = new MultiDragListener();
        }

        listener.addListener(dragView, new View.OnDragListener() {
            @Override
            public boolean onDrag(final View view, final DragEvent event) {
                switch (event.getAction()) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        return true;

                    case DragEvent.ACTION_DROP:
                        if (dragView.getVisibility() == View.INVISIBLE) {
                            dragView.setX(event.getX() - 30);
                            dragView.setY(event.getY() - 30);

                            dragView.setVisibility(View.VISIBLE);

                            return true;
                        }

                        break;

                    case DragEvent.ACTION_DRAG_ENDED:
                        if (dragView.getVisibility() == View.INVISIBLE) {
                            dragView.setVisibility(View.VISIBLE);

                            return true;
                        }

                        break;
                }

                return false;
            }
        });

        dropTarget.setOnDragListener(listener);
        dropTarget.setTag(listener);
    }
}
