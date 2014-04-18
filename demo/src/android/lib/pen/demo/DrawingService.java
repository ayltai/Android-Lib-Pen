package android.lib.pen.demo;

import android.app.Activity;
import android.lib.pen.PenService;
import android.view.MotionEvent;
import android.view.View;

import com.samsung.android.sdk.pen.document.SpenPageDoc;

final class DrawingService extends PenService {
    private View.OnTouchListener      onTouchListener;
    private OnPageUpdatedListener     onPageUpdatedListener;
    private OnReplayCompletedListener onReplayCompletedListener;

    public DrawingService(final Activity activity, final View rootLayout) {
        super(activity, rootLayout);
    }

    @Override
    public void onCommit(final SpenPageDoc doc) {
        super.onCommit(doc);

        // The page is changed, so update the thumbnail (if any).
        if (this.onPageUpdatedListener != null) {
            this.onPageUpdatedListener.onPageUpdated();
        }
    }

    @Override
    public void undo() {
        super.undo();

        // The page is changed, so update the thumbnail (if any).
        if (this.onPageUpdatedListener != null) {
            this.onPageUpdatedListener.onPageUpdated();
        }
    }

    @Override
    public void redo() {
        super.redo();

        // The page is changed, so update the thumbnail (if any).
        if (this.onPageUpdatedListener != null) {
            this.onPageUpdatedListener.onPageUpdated();
        }
    }

    public void setOnTouchListener(final View.OnTouchListener listener) {
        this.onTouchListener = listener;
    }

    public void setOnPageUpdatedListener(final OnPageUpdatedListener listener) {
        this.onPageUpdatedListener = listener;
    }

    public void setOnReplayCompletedListener(final OnReplayCompletedListener listener) {
        this.onReplayCompletedListener = listener;
    }

    @Override
    public boolean onTouch(final View view, final MotionEvent event) {
        if (this.onTouchListener == null) {
            return super.onTouch(view, event);
        }

        return this.onTouchListener.onTouch(view, event);
    }

    @Override
    public void onCompleted() {
        if (this.onReplayCompletedListener != null) {
            this.onReplayCompletedListener.onReplayCompleted();
        }
    }
}
