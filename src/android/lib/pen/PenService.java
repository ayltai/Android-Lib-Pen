package android.lib.pen;

import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.pen.Spen;
import com.samsung.android.sdk.pen.SpenSettingEraserInfo;
import com.samsung.android.sdk.pen.SpenSettingPenInfo;
import com.samsung.android.sdk.pen.SpenSettingViewInterface;
import com.samsung.android.sdk.pen.document.SpenInvalidPasswordException;
import com.samsung.android.sdk.pen.document.SpenNoteDoc;
import com.samsung.android.sdk.pen.document.SpenNoteFile;
import com.samsung.android.sdk.pen.document.SpenPageDoc;
import com.samsung.android.sdk.pen.document.SpenUnsupportedTypeException;
import com.samsung.android.sdk.pen.document.SpenUnsupportedVersionException;
import com.samsung.android.sdk.pen.engine.SpenColorPickerListener;
import com.samsung.android.sdk.pen.engine.SpenEraserChangeListener;
import com.samsung.android.sdk.pen.engine.SpenPenChangeListener;
import com.samsung.android.sdk.pen.engine.SpenPenDetachmentListener;
import com.samsung.android.sdk.pen.engine.SpenReplayListener;
import com.samsung.android.sdk.pen.engine.SpenSurfaceView;
import com.samsung.android.sdk.pen.engine.SpenTouchListener;
import com.samsung.android.sdk.pen.engine.SpenZoomListener;
import com.samsung.android.sdk.pen.settingui.SpenSettingEraserLayout;
import com.samsung.android.sdk.pen.settingui.SpenSettingPenLayout;

/**
 * Provides common operations for using Samsung S-Pen.
 */
public class PenService implements View.OnClickListener, SpenColorPickerListener, SpenSettingEraserLayout.EventListener, SpenPageDoc.HistoryListener, SpenReplayListener, SpenZoomListener, SpenPenChangeListener, SpenEraserChangeListener, SpenPenDetachmentListener, SpenTouchListener {
    /**
     * The pen button for selecting the pen tool and showing the {@link SpenSettingPenLayout pen setting}.
     */
    public static final int BUTTON_PEN = 0;

    /**
     * The eraser button for selecting the eraser tool and showing the {@link SpenSettingEraserLayout eraser setting}.
     */
    public static final int BUTTON_ERASER = 1;

    /**
     * The undo button used to undo in history.
     */
    public static final int BUTTON_UNDO = 2;

    /**
     * The redo button used to redo in history.
     */
    public static final int BUTTON_REDO = 3;

    /**
     * The zoom button used to zoom the canvas in 1.5x.
     */
    public static final int BUTTON_ZOOM = 4;

    /**
     * Positions a background image at the center of the {@link SpenSurfaceView canvas}.
     */
    public static final int MODE_CENTER = SpenPageDoc.BACKGROUND_IMAGE_MODE_CENTER;

    /**
     * Fits a background image at the center of the {@link SpenSurfaceView canvas} and keep its aspect ratio.
     */
    public static final int MODE_FIT = SpenPageDoc.BACKGROUND_IMAGE_MODE_FIT;

    /**
     * Tiles a background image with the {@link SpenSurfaceView canvas}.
     */
    public static final int MODE_TILE = SpenPageDoc.BACKGROUND_IMAGE_MODE_TILE;

    /**
     * Stretches a background image to fill the {@link SpenSurfaceView canvas}.
     */
    public static final int MODE_STRETCH = SpenPageDoc.BACKGROUND_IMAGE_MODE_STRETCH;

    /**
     * Replay is paused.
     * @see #getReplayState()
     */
    public static final int REPLAY_STATE_PAUSED = SpenSurfaceView.REPLAY_STATE_PAUSED;

    /**
     * Replay is playing.
     * @see #getReplayState()
     */
    public static final int REPLAY_STATE_PLAYING = SpenSurfaceView.REPLAY_STATE_PLAYING;

    /**
     * Replay is stopped.
     * @see #getReplayState()
     */
    public static final int REPLAY_STATE_STOPPED = SpenSurfaceView.REPLAY_STATE_STOPPED;

    /**
     * Replays animation at a slow speed.
     */
    public static final int SPEED_SLOW = 0;

    /**
     * Replays animation at normal speed.
     */
    public static final int SPEED_NORMAL = 1;

    /**
     * Replays animation at a fast speed.
     */
    public static final int SPEED_FAST = 2;

    private static final String NULL = new String();

    private static final Uri SPEN_SDK_MARKET_URI = Uri.parse("market://details?id=" + Spen.SPEN_NATIVE_PACKAGE_NAME); //$NON-NLS-1$

    private static final int RESPONSE_TIME = 500;

    private static final float ZOOM_RATIO = 1.5f;

    private final Activity activity;
    private final View     rootLayout;

    private SpenSurfaceView surfaceView;
    private SpenNoteDoc     noteDoc;

    private SpenSettingPenLayout    penSetting;
    private SpenSettingEraserLayout eraserSetting;

    private ToggleButton penButton;
    private ToggleButton eraserButton;
    private ToggleButton undoButton;
    private ToggleButton redoButton;
    private ToggleButton zoomButton;

    private boolean penEnabled;
    private boolean penSettingEnabled    = true;
    private boolean eraserSettingEnabled = true;
    private int     toolType             = SpenSettingViewInterface.TOOL_SPEN;
    private int     currentPage;
    private int     canvasWidth;
    private boolean dirty;
    private boolean isZoomed;

    /**
     * Determines whether a SPD file is password protected.
     * @param path the absolute path of a SPD file.
     * @return <code>true</code> if the file is password protected; otherwise, <code>false</code>.
     */
    public static boolean isLocked(final String path) {
        return SpenNoteFile.isLocked(path);
    }

    /**
     * Protects a SPD file with the specified <code>password</code>.
     * @param context an application context.
     * @param path the absolute path of a SPD file.
     * @param password the password to protect a SPD file.
     * @throws IOException thrown if the specified <code>path</code> is not found, or if a temporary directory cannot be created.
     * @throws SpenUnsupportedTypeException thrown if the specified <code>path</code> is not a SPD file.
     */
    public static void lock(final Context context, final String path, final String password) throws IOException, SpenUnsupportedTypeException {
        SpenNoteFile.lock(context, path, password);
    }

    /**
     * Unprotects a SPD file using the given <code>password</code>.
     * @param context an application context.
     * @param path the absolute path of a SPD file.
     * @param password the password to unprotect a SPD file.
     * @throws IOException  thrown if the specified <code>path</code> is not found, or if a temporary directory cannot be created.
     * @throws SpenUnsupportedTypeException thrown if the specified <code>path</code> is not a SPD file.
     * @throws SpenInvalidPasswordException thrown if the given <code>password</code> is incorrect.
     */
    public static void unlock(final Context context, final String path, final String password) throws IOException, SpenInvalidPasswordException, SpenUnsupportedTypeException {
        SpenNoteFile.unlock(context, path, password);
    }

    /**
     * Initializes tool buttons ({@link #BUTTON_PEN}, {@link #BUTTON_ERASER}, {@link #BUTTON_UNDO} and {@link #BUTTON_REDO})
     * and settings ({@link SpenSettingPenLayout pen setting} and {@link SpenSettingEraserLayout eraser setting}.
     * @param activity the {@link Activity} that hosts a {@link RelativeLayout}, where a {@link SpenSurfaceView canvas}
     * will be created.
     * @param rootLayout the {@link View} that contains {@link R.id.pen_container} and {@link R.id.pen_canvas}.
     */
    public PenService(final Activity activity, final View rootLayout) {
        this.activity   = activity;
        this.rootLayout = rootLayout;

        final ViewGroup penButtons = (ViewGroup)rootLayout.findViewById(R.id.pen_buttons);

        if (penButtons != null) {
            this.penButton    = (ToggleButton)penButtons.findViewById(R.id.pen_pen_button);
            this.eraserButton = (ToggleButton)penButtons.findViewById(R.id.pen_eraser_button);
            this.undoButton   = (ToggleButton)penButtons.findViewById(R.id.pen_undo_button);
            this.redoButton   = (ToggleButton)penButtons.findViewById(R.id.pen_redo_button);
            this.zoomButton   = (ToggleButton)penButtons.findViewById(R.id.pen_zoom_button);
        }
    }

    /**
     * Cleans up any resources used by the Pen package.
     */
    public void onDestroy() {
        if (this.noteDoc != null) {
            for (int i = this.noteDoc.getPageCount(); --i >= 0;) {
                final SpenPageDoc pageDoc = this.noteDoc.getPage(i);

                if (pageDoc.isRecording()) {
                    pageDoc.stopRecord();
                }
            }
        }

        if (this.penSetting != null) {
            this.penSetting.close();
        }

        if (this.eraserSetting != null) {
            this.eraserSetting.close();
        }

        if (this.surfaceView != null) {
            this.surfaceView.close();
        }

        if (this.noteDoc != null) {
            try {
                this.noteDoc.close();
            } catch (final IOException e) {
                Log.e(this.getClass().getName(), e.getMessage(), e);
            }
        }
    }

    /**
     * Called when any one of the following buttons is clicked.
     * <p>{@link R.id.pen_pen_button}, {@link R.id.pen_earser_button}, {@link R.id.pen_undo_button}, {@link R.id.pen_redo_button}</p>
     * @param view the button clicked.
     */
    @Override
    public void onClick(final View view) {
        final int id = view.getId();

        if (id == R.id.pen_pen_button) {
            this.selectButton(PenService.BUTTON_PEN);
        } else if (id == R.id.pen_eraser_button) {
            this.selectButton(PenService.BUTTON_ERASER);
        } else if (id == R.id.pen_undo_button) {
            this.undo();
        } else if (id == R.id.pen_redo_button) {
            this.redo();
        } else if (id == R.id.pen_zoom_button) {
            this.zoom();
        }
    }

    @Override
    public boolean onTouch(final View view, final MotionEvent event) {
        return false;
    }

    /**
     * Called when a color is picked.
     * @param color the color that is picked.
     * @param x the x coordinate in pixels.
     * @param y the y coordinate in pixels.
     */
    @Override
    public void onChanged(final int color, final int x, final int y) {
        if (this.penSettingEnabled) {
            if (this.surfaceView == null) {
                throw new IllegalStateException();
            }

            final SpenSettingPenInfo info = this.penSetting.getInfo();

            info.color = color;

            this.surfaceView.setPenSettingInfo(info);
            this.penSetting.setInfo(info);
        }
    }

    /**
     * Called when a {@link SpenSettingPenInfo pen setting} is changed.
     * @param info the changed {@link SpenSettingPenInfo pen setting}.
     */
    @Override
    public void onChanged(final SpenSettingPenInfo info) {
    }

    /**
     * Called when a {@link SpenSettingEraserInfo eraser setting} is changed.
     * @param info the changed {@link SpenSettingEraserInfo eraser setting}.
     */
    @Override
    public void onChanged(final SpenSettingEraserInfo info) {
    }

    /**
     * Called when the user requests to clear all objects on the {@link SpenSurfaceView canvas}.
     */
    @Override
    public void onClearAll() {
        if (this.surfaceView == null) {
            throw new IllegalStateException();
        }

        if (this.noteDoc != null) {
            this.noteDoc.getPage(this.currentPage).removeAllObject();
        }

        this.surfaceView.update();
    }

    /**
     * Called when a new history event is committed.
     * @param the current {@link SpenPageDoc page} that commits a new history.
     */
    @Override
    public void onCommit(final SpenPageDoc doc) {
        this.dirty = true;
    }

    /**
     * Called when the undoable state is changed.
     * @param doc the current {@link SpenPageDoc page} with changed undoable state.
     * @param undoable the current undoable state.
     */
    @Override
    public void onUndoable(final SpenPageDoc doc, final boolean undoable) {
        if (this.undoButton != null) {
            this.undoButton.setEnabled(undoable);
        }
    }

    /**
     * Called when the redoable state is changed.
     * @param doc the current {@link SpenPageDoc page} with changed redoable state.
     * @param redoable the current redoable state.
     */
    @Override
    public void onRedoable(final SpenPageDoc doc, final boolean redoable) {
        if (this.redoButton != null) {
            this.redoButton.setEnabled(redoable);
        }
    }

    /**
     * Called when a replay is completed.
     */
    @Override
    public void onCompleted() {
    }

    /**
     * Called when a replay is playing.
     * @param progress the progress indicator ranging from 0 to 100.
     * @param id the object ID.
     */
    @Override
    public void onProgressChanged(final int progress, final int id) {
    }

    /**
     * Called when a canvas zoom is completed.
     * @param panX the x coordinate in pixels.
     * @param panY the y coordinate in pixels.
     * @param ratio the zoom ratio.
     */
    @Override
    public void onZoom(final float panX, final float panY, final float ratio) {
    }

    /**
     * Called when S-Pen is detached from or attached to a device.
     * @param detached <code>true</code> if S-Pen is detached; otherwise, <code>false</code>.
     */
    @Override
    public void onDetached(final boolean detached) {
    }

    /**
     * Determines if the {@link SpenNoteDoc document} is modified.
     * @return <code>true</code> if the {@link SpenNoteDoc document} is modified; otherwise, <code>false</code>.
     */
    public boolean isDirty() {
        if (this.noteDoc == null) {
            return this.dirty;
        }

        if (this.noteDoc.isChanged()) {
            return true;
        }

        return this.dirty;
    }

    /**
     * Determines whether a pen is available on the device.
     * @return <code>true</code> if a pen is available; otherwise, <code>false</code>.
     */
    public boolean isPenEnabled() {
        return this.penEnabled;
    }

    /**
     * Gets the current displayed page index.
     * @return the current displayed page index.
     */
    public int getCurrentPage() {
        return this.noteDoc == null ? -1 : this.currentPage;
    }

    /**
     * Sets the current page to display.
     * @param page the page index to display.
     */
    public void setCurrentPage(final int page) {
        if (this.surfaceView == null) {
            throw new IllegalStateException();
        }

        if (this.noteDoc != null) {
            if (this.currentPage != page) {
                this.currentPage = page;

                this.surfaceView.setPageDoc(this.noteDoc.getPage(this.currentPage), true);
            }
        }
    }

    /**
     * Sets the background color of a page.
     * @param page the index of the page to set color to.
     * @param color the color to set.
     */
    public void setBackgroundColor(final int page, final int color) {
        if (this.surfaceView == null) {
            throw new IllegalStateException();
        }

        if (this.noteDoc != null) {
            this.noteDoc.getPage(page).setBackgroundColor(color);
        }
    }

    /**
     * Sets the background image of a page.
     * @param page the index of the page to set color to.
     * @param imagePath the path of an image file.
     */
    public void setBackgroundImage(final int page, final String imagePath) {
        if (this.surfaceView == null) {
            throw new IllegalStateException();
        }

        if (this.noteDoc != null) {
            this.noteDoc.getPage(page).setBackgroundImage(imagePath);
        }
    }

    /**
     * Sets the visibility of any one of the following buttons:
     * {@link #BUTTON_PEN} {@link #BUTTON_ERASER}, {@link #BUTTON_UNDO} and {@link #BUTTON_REDO}.
     * @param button one of {@link #BUTTON_PEN} {@link #BUTTON_ERASER}, {@link #BUTTON_UNDO} and {@link #BUTTON_REDO}.
     * @param visibility one of {@link View#VISIBLE}, {@link View#INVISIBLE} and {@link View#GONE}.
     */
    public void setButtonVisibility(final int button, final int visibility) {
        View view = null;

        switch (button) {
            case BUTTON_PEN:    view = this.penButton;    break;
            case BUTTON_ERASER: view = this.eraserButton; break;
            case BUTTON_UNDO:   view = this.undoButton;   break;
            case BUTTON_REDO:   view = this.redoButton;   break;
            case BUTTON_ZOOM:   view = this.zoomButton;   break;
        }

        if (view != null) {
            view.setVisibility(visibility);
        }
    }

    /**
     * Sets a drawable resource for a button.
     * @param button one of {@link #BUTTON_PEN} {@link #BUTTON_ERASER}, {@link #BUTTON_UNDO} and {@link #BUTTON_REDO}.
     * @param resource a drawable resource to set.
     */
    public void setButtonResource(final int button, final int resource) {
        this.setButtonDrawable(button, this.activity.getResources().getDrawable(resource));
    }

    /**
     * Sets a drawable for a button.
     * @param button one of {@link #BUTTON_PEN} {@link #BUTTON_ERASER}, {@link #BUTTON_UNDO} and {@link #BUTTON_REDO}.
     * @param drawable a drawable to set.
     */
    public void setButtonDrawable(final int button, final Drawable drawable) {
        ToggleButton view = null;

        switch (button) {
            case BUTTON_PEN:
                if (this.penButton != null) {
                    view = this.penButton;
                }

                break;

            case BUTTON_ERASER:
                if (this.eraserButton != null) {
                    view = this.eraserButton;
                }

                break;

            case BUTTON_UNDO:
                if (this.undoButton != null) {
                    view = this.undoButton;
                }

                break;

            case BUTTON_REDO:
                if (this.redoButton != null) {
                    view = this.redoButton;
                }

                break;

            case BUTTON_ZOOM:
                if (this.zoomButton != null) {
                    view = this.zoomButton;
                }

                break;
        }

        if (view != null) {
            view.setCompoundDrawables(null, drawable, null, null);
        }
    }

    /**
     * Determines whether {@link SpenSettingPenLayout pen setting} is enabled.
     * @return <code>true</code> if {@link SpenSettingPenLayout pen setting} is enabled; otherwise, <code>false</code>.
     */
    public boolean isPenSettingEnabled() {
        return this.penSettingEnabled;
    }

    /**
     * Enables or disables {@link SpenSettingPenLayout pen setting} when {@link #BUTTON_PEN} is clicked.
     * @param enabled <code>true</code> to enable {@link SpenSettingPenLayout pen setting}; otherwise, <code>false</code>.
     */
    public void setPenSettingEnabled(final boolean enabled) {
        this.penSettingEnabled = enabled;
    }

    /**
     * Determines whether {@link SpenSettingEraserLayout eraser layout} is enabled.
     * @return <code>true</code> if {@link SpenSettingEraserLayout eraser layout} is enabled; otherwise, <code>false</code>.
     */
    public boolean isEraserSettingEnabled() {
        return this.eraserSettingEnabled;
    }

    /**
     * Enables or disables {@link SpenSettingEraserLayout eraser setting} when {@link #BUTTON_ERASER} is clicked.
     * @param enabled <code>true</code> to enable {@link SpenSettingEraserLayout eraser setting}; otherwise, <code>false</code>.
     */
    public void setEraserSettingEnabled(final boolean enabled) {
        this.eraserSettingEnabled = enabled;
    }

    /**
     * Determines whether scroll feature is enabled.
     * @return <code>true</code> if scroll feature is enabled; otherwise, <code>false</code>.
     */
    public boolean isScrollEnabled() {
        if (this.surfaceView == null) {
            throw new IllegalStateException();
        }

        return this.surfaceView.isHorizontalSmartScrollEnabled() || this.surfaceView.isVerticalSmartScrollEnabled();
    }

    /**
     * Enables or disables "Smart Scroll" feature.
     * <p>If enabled, the {@link SpenSurfaceView canvas} automatically scrolls when a S-Pen hovers over the edge of it.</p>
     * <p>To enable scroll feature, call this method after {@link #init(int, int, int, String, int, int, int, int)}
     * to ensure that the {@link SpenSurfaaceView canvas} layout is ready.</p>
     * @param enable <code>true</code> to enable scroll feature, or <code>false</code> to disable it. Default is <code>false</code>.
     * @param edgeSize the size of each of the 4 edges in pixels
     * @param velocity the scroll velocity in pixels.
     */
    public void setScrollEnabled(final boolean enable, final int edgeSize, final int velocity) {
        if (this.surfaceView == null) {
            throw new IllegalStateException();
        }

        final int width  = this.surfaceView.getWidth();
        final int height = this.surfaceView.getHeight();

        this.surfaceView.setHorizontalSmartScrollEnabled(enable, new Rect(0, 0, edgeSize, height), new Rect(width, 0, width, height), PenService.RESPONSE_TIME, velocity);
        this.surfaceView.setVerticalSmartScrollEnabled(enable, new Rect(0, 0, width, 0), new Rect(0, height, width, height), PenService.RESPONSE_TIME, velocity);
    }

    /**
     * Determines whether zoom feature is enabled.
     * @return <code>true</code> if zoom feature is enabled; otherwise, <code>false</code>.
     */
    public boolean isZoomEnabled() {
        if (this.surfaceView == null) {
            throw new IllegalStateException();
        }

        return this.surfaceView.isSmartScaleEnabled();
    }

    /**
     * Enables or disables "Smart Zoom" feature.
     * <p>If enabled, the {@link SpenSurfaceView canvas} automatically zooms when a S-Pen hovers over it.</p>
     * <p>To enable zoom feature, call this method after {@link #init(int, int, int, String, int, int, int, int)}
     * to ensure that the {@link SpenSurfaceView canvas} layout is ready.</p>
     * <p>Note: This may not work on Android 4.0 (API level 14) devices.</p>
     * @param enable <code>true</code> to enable zoom feature, or <code>false</code> to disable it. Default is <code>false</code>.
     * @param zoomRatio the zoom ratio.
     */
    public void setZoomEnabled(final boolean enable, final float zoomRatio) {
        if (this.surfaceView == null) {
            throw new IllegalStateException();
        }

        this.surfaceView.setSmartScaleEnabled(enable, new Rect(0, 0, this.surfaceView.getWidth(), this.surfaceView.getHeight()), 8, PenService.RESPONSE_TIME, zoomRatio);
    }

    /**
     * Select a tool button to use.
     * @param button one of {@link #BUTTON_PEN} {@link #BUTTON_ERASER}, {@link #BUTTON_UNDO} and {@link #BUTTON_REDO}.
     */
    public void selectButton(final int button) {
        if (this.surfaceView != null) {
            switch (button) {
                case BUTTON_PEN:
                    if (this.penButton != null) {
                        this.eraserSetting.setVisibility(View.GONE);

                        this.surfaceView.setToolTypeAction(this.toolType, SpenSettingViewInterface.ACTION_STROKE);

                        if (this.eraserButton != null) {
                            this.eraserButton.setChecked(false);
                        }

                        if (!this.penButton.isChecked()) {
                            this.penButton.setChecked(true);
                        }

                        this.onPenSettingClick();
                    }

                    break;

                case BUTTON_ERASER:
                    if (this.eraserButton != null) {
                        this.penSetting.setVisibility(View.GONE);

                        if (this.penButton != null) {
                            this.penButton.setChecked(false);
                        }

                        if (!this.eraserButton.isChecked()) {
                            this.eraserButton.setChecked(true);
                        }

                        this.onEraserSettingClick();
                    }

                    break;
            }
        }
    }

    /**
     * Gets the number of pages.
     * @return the number of pages.
     */
    public int getPageCount() {
        if (this.noteDoc == null) {
            return 0;
        }

        return this.noteDoc.getPageCount();
    }

    /**
     * Appends a new {@link SpenPageDoc page} with the specified background color/image.
     * @param backgroundColor the background color of the {@link SpenPageDoc page}.
     * <p>The default color will be used if <code>backgroundColor</code> is negative.</p>
     * @param backgroundImagePath the absolute path of the background image file.
     * <p>An empty background will be used if <code>backgroundImagePath</code> is empty or <code>null</code>.</p>
     * @param backgroundImageMode either {@link #MODE_CENTER}, {@link #MODE_FIT}, {@link #MODE_TILE} or {@link #MODE_STRETCH}.
     */
    public void appendPage(final int backgroundColor, final String backgroundImagePath, final int backgroundImageMode) {
        if (this.noteDoc != null) {
            final SpenPageDoc pageDoc = this.noteDoc.appendPage();
            pageDoc.setBackgroundColor(backgroundColor);

            if (!TextUtils.isEmpty(backgroundImagePath)) {
                pageDoc.setBackgroundImage(backgroundImagePath);
            }

            if (backgroundImageMode == PenService.MODE_CENTER || backgroundImageMode == PenService.MODE_FIT || backgroundImageMode == PenService.MODE_TILE || backgroundImageMode == PenService.MODE_STRETCH) {
                pageDoc.setBackgroundImageMode(backgroundImageMode);
            }

            pageDoc.clearHistory();
            pageDoc.setHistoryListener(this);
        }
    }

    /**
     * Inserts a new {@link SpenPageDoc page} at the specified page index with the specified background color/image.
     * @param backgroundColor the background color of the {@link SpenPageDoc page}.
     * <p>The default color will be used if <code>backgroundColor</code> is negative.</p>
     * @param backgroundImagePath the absolute path of the background image file.
     * <p>An empty background will be used if <code>backgroundImagePath</code> is empty or <code>null</code>.</p>
     * @param backgroundImageMode either {@link #MODE_CENTER}, {@link #MODE_FIT}, {@link #MODE_TILE} or {@link #MODE_STRETCH}.
     */
    public void insertPage(final int pageIndex, final int backgroundColor, final String backgroundImagePath, final int backgroundImageMode) {
        if (this.noteDoc != null) {
            final SpenPageDoc pageDoc = this.noteDoc.insertPage(pageIndex);
            pageDoc.setBackgroundColor(backgroundColor);

            if (!TextUtils.isEmpty(backgroundImagePath)) {
                pageDoc.setBackgroundImage(backgroundImagePath);
            }

            if (backgroundImageMode == PenService.MODE_CENTER || backgroundImageMode == PenService.MODE_FIT || backgroundImageMode == PenService.MODE_TILE || backgroundImageMode == PenService.MODE_STRETCH) {
                pageDoc.setBackgroundImageMode(backgroundImageMode);
            }

            pageDoc.clearHistory();
            pageDoc.setHistoryListener(this);
        }
    }

    /**
     * Removes a {@link SpenPageDoc page} at the specified page index.
     * @param the page index to remove.
     */
    public void removePage(final int pageIndex) {
        if (this.noteDoc != null) {
            this.noteDoc.removePage(pageIndex);
        }
    }

    /**
     * Moves a page at the specified <code>pageIndex</code> to a new index.
     * @param pageIndex the page index to move from.
     * @param step the number of page index to move.
     * <p>Moves a page forward by specifying a positive step, or backward by specifying a negative step.</p>
     */
    public void movePage(final int pageIndex, final int step) {
        if (this.noteDoc != null) {
            this.noteDoc.movePageIndex(this.noteDoc.getPage(pageIndex), step);
        }
    }

    /**
     * Undo the previous action, if any.
     */
    public void undo() {
        if (this.surfaceView == null) {
            throw new IllegalStateException();
        }

        if (this.noteDoc != null) {
            final SpenPageDoc pageDoc = this.noteDoc.getPage(this.currentPage);

            if (pageDoc.isUndoable()) {
                this.surfaceView.updateUndo(pageDoc.undo());
            }
        }
    }

    /**
     * Redo the next action, if any.
     */
    public void redo() {
        if (this.surfaceView == null) {
            throw new IllegalStateException();
        }

        if (this.noteDoc != null) {
            final SpenPageDoc pageDoc = this.noteDoc.getPage(this.currentPage);

            if (pageDoc.isRedoable()) {
                this.surfaceView.updateRedo(pageDoc.redo());
            }
        }
    }

    private void zoom() {
        this.isZoomed = !this.isZoomed;

        this.setZoomEnabled(this.isZoomed, PenService.ZOOM_RATIO);

        this.zoomButton.setChecked(!this.zoomButton.isChecked());
    }

    /**
     * Starts recording changes.
     * <p>By default, the {@link SpenPageDoc document) internally records any changes.</p>
     */
    public void startRecord() {
        if (this.noteDoc != null) {
            final SpenPageDoc pageDoc = this.noteDoc.getPage(this.currentPage);

            if (!pageDoc.isRecording()) {
                pageDoc.startRecord();
            }
        }
    }

    /**
     * Stops recording changes.
     * <p>This is a no-op if recording is not previously started.</p>
     */
    public void stopRecord() {
        if (this.noteDoc != null) {
            final SpenPageDoc pageDoc = this.noteDoc.getPage(this.currentPage);

            if (pageDoc.isRecording()) {
                pageDoc.stopRecord();
            }
        }
    }

    /**
     * Starts replaying the objects drawn on the {@link SpenSurfaceView canvas}, if any.
     * <p>You can replay animation by calling {@link #startRecord()}, {@link #stopRecord()},
     * {@link #startReplay()}, {@link #pauseReplay()}, {@link #resumeReplay()} and {@link #stopReplay()} methods.</p>
     * <p>It is an no-op if {@link #startRecord()} there is no object on the {@link SpenSurfaceView canvas},
     * or {@link #startRecord()} is not called before.</p>
     * @see #startRecord()
     * @see #stopRecord()
     * @see #stopReplay()
     * @see #pauseReplay()
     * @see #resumeReplay()
     */
    public void startReplay() {
        if (this.surfaceView == null) {
            throw new IllegalStateException();
        }

        this.surfaceView.startReplay();
    }

    /**
     * Stops replaying the objects drawn on the {@link SpenSurfaceView canvas}, if any.
     * <p>You can replay animation by calling {@link #startRecord()}, {@link #stopRecord()},
     * {@link #startReplay()}, {@link #pauseReplay()}, {@link #resumeReplay()} and {@link #stopReplay()} methods.</p>
     * <p>It is an no-op if {@link #startReplay()} is not called before.</p>
     * @see #startRecord()
     * @see #stopRecord()
     * @see #startReplay()
     * @see #pauseReplay()
     * @see #resumeReplay()
     */
    public void stopReplay() {
        if (this.surfaceView == null) {
            //throw new IllegalStateException();
        } else {
            if (this.surfaceView.getReplayState() == SpenSurfaceView.REPLAY_STATE_PLAYING) {
                this.surfaceView.stopReplay();
            }
        }
    }

    /**
     * Pauses replaying the objects drawn on the {@link SpenSurfaceView canvas}, if any.
     * <p>You can replay animation by calling {@link #startRecord()}, {@link #stopRecord()},
     * {@link #startReplay()}, {@link #pauseReplay()}, {@link #resumeReplay()} and {@link #stopReplay()} methods.</p>
     * <p>It is an no-op if {@link #startReplay()} is not called before.</p>
     * @see #startRecord()
     * @see #stopRecord()
     * @see #startReplay()
     * @see #stopReplay()
     * @see #resumeReplay()
     */
    public void pauseReplay() {
        if (this.surfaceView == null) {
            throw new IllegalStateException();
        }

        this.surfaceView.pauseReplay();
    }

    /**
     * Resumes replaying the objects drawn on the {@link SpenSurfaceView canvas}, if any.
     * <p>You can replay animation by calling {@link #startRecord()}, {@link #stopRecord()},
     * {@link #startReplay()}, {@link #pauseReplay()}, {@link #resumeReplay()} and {@link #stopReplay()} methods.</p>
     * <p>It is an no-op if {@link #pauseReplay()} is not called before.</p>
     * @see #startRecord()
     * @see #stopRecord()
     * @see #startReplay()
     * @see #stopReplay()
     * @see #pauseReplay()
     */
    public void resumeReplay() {
        if (this.surfaceView == null) {
            throw new IllegalStateException();
        }

        this.surfaceView.resumeReplay();
    }

    /**
     * Gets the animation replay state.
     * @return the current replay state.
     * <p>Could be one of these values: {@link #REPLAY_STATE_PAUSED}, {@link #REPLAY_STATE_PLAYING} or {@link #REPLAY_STATE_STOPPED}.</p>
     */
    public int getReplayState() {
        if (this.surfaceView == null) {
            throw new IllegalStateException();
        }

        return this.surfaceView.getReplayState();
    }

    /**
     * Sets the speed for replaying objects drawn on the {@link SpenSurfaceView canvas}.
     * @param speed the replay speed.
     * <p>The valid values are {@link #SPEED_SLOW}, {@link #SPEED_NORMAL} and {@link #SPEED_FAST}.</p>
     */
    public void setReplaySpeed(final int speed) {
        if (this.surfaceView == null) {
            throw new IllegalStateException();
        }

        this.surfaceView.setReplaySpeed(speed);
    }

    /**
     * Creates a {@link Bitmap} from the current {@link SpenPageDoc page}.
     * @param scale the scale to resize the generated {@link Bitmap}.
     * @return the {@link Bitmap} captured from the current {@link SpenPageDoc page}.
     */
    public Bitmap generateThumbnail(final float scale) {
        if (this.surfaceView == null) {
            throw new IllegalStateException();
        }

        return this.surfaceView.capturePage(scale);
    }

    /**
     * Saves the {@link SpenNoteDoc document} to a SPD file at the specified <code>path</code>.
     * @param path the absolute path to save a SPD file to.
     * @throws IOException if the operation fails.
     */
    public void save(final String path) throws IOException {
        if (this.dirty && this.noteDoc != null) {
            this.noteDoc.save(path);
        }
    }

    /**
     * Loads a SPD file into the current {@link SpenNoteDoc document}.
     * @param path the absolute path of a SPD file to load.
     * @param writable <code>true</code> if the SPD file should be writable; otherwise, <code>false</code>.
     * @throws IOException thrown if the specified <code>path</code> is not found or a cache directory cannot be generated.
     * @throws SpenUnsupportedTypeException thrown if the file to load is not in SPD format.
     * @throws SpenUnsupportedVersionException thrown if the Pen package installed on the device is incompatible.
     * @throws SpenInvalidPasswordException thrown if the file is password protected.
     */
    public void load(final String path, final boolean writable) throws SpenInvalidPasswordException, SpenUnsupportedTypeException, SpenUnsupportedVersionException, IOException {
        if (this.surfaceView == null) {
            throw new IllegalStateException();
        }

        final SpenNoteDoc noteDoc = new SpenNoteDoc(this.activity, path, this.canvasWidth, writable ? SpenNoteDoc.MODE_WRITABLE : SpenNoteDoc.MODE_READ_ONLY);

        if (this.noteDoc != null) {
            this.noteDoc.close();
        }

        this.noteDoc = noteDoc;

        if (this.noteDoc.getPageCount() > 0) {
            this.currentPage = 0;

            final SpenPageDoc page = this.noteDoc.getPage(0);
            page.setHistoryListener(this);

            this.surfaceView.setPageDoc(page, true);
            this.surfaceView.update();
        }

        if (this.noteDoc.getPageCount() > 1) {
            this.noteDoc.getPage(1).setHistoryListener(this);
        }
    }

    /**
     * Loads a SPD file into the current {@link SpenNoteDoc document}.
     * @param path the absolute path of a SPD file to load.
     * @param writable <code>true</code> if the SPD file should be writable; otherwise, <code>false</code>.
     * @throws IOException thrown if the specified <code>path</code> is not found or a cache directory cannot be generated.
     * @throws SpenUnsupportedTypeException thrown if the file to load is not in SPD format.
     * @throws SpenUnsupportedVersionException thrown if the Pen package installed on the device is incompatible.
     * @throws SpenInvalidPasswordException thrown if the specified <code>password</code> is incorrect.
     */
    public void load(final String path, final String password, final boolean writable) throws SpenInvalidPasswordException, SpenUnsupportedTypeException, SpenUnsupportedVersionException, IOException {
        if (this.surfaceView == null) {
            throw new IllegalStateException();
        }

        final SpenNoteDoc noteDoc = new SpenNoteDoc(this.activity, path, password, this.canvasWidth, writable ? SpenNoteDoc.MODE_WRITABLE : SpenNoteDoc.MODE_READ_ONLY, !writable);

        if (this.noteDoc != null) {
            this.noteDoc.close();
        }

        this.noteDoc = noteDoc;

        if (this.noteDoc.getPageCount() > 0) {
            this.currentPage = 0;

            final SpenPageDoc page = this.noteDoc.getPage(0);
            page.setHistoryListener(this);

            this.surfaceView.setPageDoc(page, true);
            this.surfaceView.update();
        }

        if (this.noteDoc.getPageCount() > 1) {
            this.noteDoc.getPage(1).setHistoryListener(this);
        }
    }

    /**
     * Initialize S-Pen related objects.
     * <p>This will create a {@link SpenSurfaceView canvas} and a {@link SpenNoteDoc document} objects,
     * and append a {@link SpenPageDoc page} to it.</p>
     * <p>If S-Pen is not supported on the device, a message will be displayed and {@link Activity#finish()} will be called.</p>
     * @param canvasWidth the width of the {@link SpenSurfaceView canvas} to put a {@link SpenPageDoc page} in it.
     * @param canvasHeight the height of the {@link SpenSurfaceView canvas} to put a {@link SpenPageDoc page} in it.
     * @param backgroundColor the background color of the {@link SpenPageDoc page}.
     * <p>The default color will be used if <code>backgroundColor</code> is negative.</p>
     * @param backgroundImagePath the absolute path of the background image file.
     * <p>An empty background will be used if <code>backgroundImagePath</code> is empty or <code>null</code>.</p>
     * @param backgroundImageMode either {@link #MODE_CENTER}, {@link #MODE_FIT}, {@link #MODE_TILE} or {@link #MODE_STRETCH}.
     * @param penColor the initial pen color to use.
     * @param penSize the initial pen size in pixels to use.
     * @param eraserSize the initial eraser size in pixels to use.
     * @return <code>true</code> if Spen SDK is initialized successfully; otherwise, <code>false</code>.
     */
    public boolean init(final int canvasWidth, final int canvasHeight, final int backgroundColor, final String backgroundImagePath, final int backgroundImageMode, final int penColor, final int penSize, final int eraserSize) {
        if (this.initSpen()) {
            final RelativeLayout canvas = (RelativeLayout)this.rootLayout.findViewById(R.id.pen_canvas);

            if (this.penSettingEnabled) {
                this.penSetting = new SpenSettingPenLayout(this.activity, PenService.NULL, canvas);
            }

            if (this.eraserSettingEnabled) {
                this.eraserSetting = new SpenSettingEraserLayout(this.activity, PenService.NULL, canvas);
            }

            this.initCanvas(canvasWidth, canvasHeight, backgroundColor, backgroundImagePath, backgroundImageMode);
            this.initPenInfo(penColor, penSize);
            this.initEraserInfo(eraserSize);

            if (this.surfaceView != null) {
                this.surfaceView.setColorPickerListener(this);
            }

            if (this.eraserSetting != null) {
                this.eraserSetting.setEraserListener(this);
            }

            if (this.penButton != null) {
                this.penButton.setOnClickListener(this);
            }

            if (this.eraserButton != null) {
                this.eraserButton.setOnClickListener(this);
            }

            if (this.undoButton != null) {
                this.undoButton.setOnClickListener(this);
            }

            if (this.redoButton != null) {
                this.redoButton.setOnClickListener(this);
            }

            if (this.zoomButton != null) {
                this.zoomButton.setOnClickListener(this);
            }

            final SpenPageDoc pageDoc = this.noteDoc.getPage(this.currentPage);

            if (this.undoButton != null) {
                this.undoButton.setEnabled(pageDoc.isUndoable());
            }

            if (this.redoButton != null) {
                this.redoButton.setEnabled(pageDoc.isRedoable());
            }

            pageDoc.setHistoryListener(this);

            this.selectButton(PenService.BUTTON_PEN);
            this.onPenSettingClick();

            return true;
        }

        return false;
    }

    private boolean initSpen() {
        final Spen spen = new Spen();

        try {
            spen.initialize(this.activity);

            this.penEnabled = spen.isFeatureEnabled(Spen.DEVICE_PEN);

            return true;
        } catch (final SsdkUnsupportedException e) {
            Log.i(this.getClass().getName(), e.getMessage(), e);

            this.handleUnsupportedException(e);
        } catch (final Exception e) {
            Toast.makeText(this.activity, R.string.message_spen_not_initialized, Toast.LENGTH_SHORT).show();

            this.activity.finish();
        }

        return false;
    }

    private void initCanvas(final int canvasWidth, final int canvasHeight, final int backgroundColor, final String backgroundImagePath, final int backgroundImageMode) {
        final ViewGroup canvas    = (ViewGroup)this.rootLayout.findViewById(R.id.pen_canvas);
        final ViewGroup container = (ViewGroup)this.rootLayout.findViewById(R.id.pen_container);

        this.canvasWidth = canvasWidth;
        this.surfaceView = new SpenSurfaceView(this.activity);

        canvas.addView(this.surfaceView);

        try {
            this.noteDoc = new SpenNoteDoc(this.activity, canvasWidth, canvasHeight);
        } catch (final IOException e) {
            Toast.makeText(this.activity, R.string.message_spen_not_initialized, Toast.LENGTH_SHORT).show();

            this.activity.finish();
        } catch (final Exception e) {
            Toast.makeText(this.activity, R.string.message_spen_not_initialized, Toast.LENGTH_SHORT).show();

            this.activity.finish();
        }

        if (this.noteDoc != null) {
            final SpenPageDoc pageDoc = this.noteDoc.appendPage();
            pageDoc.setBackgroundColor(backgroundColor);

            if (!TextUtils.isEmpty(backgroundImagePath)) {
                pageDoc.setBackgroundImage(backgroundImagePath);
            }

            if (backgroundImageMode == PenService.MODE_CENTER || backgroundImageMode == PenService.MODE_FIT || backgroundImageMode == PenService.MODE_TILE || backgroundImageMode == PenService.MODE_STRETCH) {
                pageDoc.setBackgroundImageMode(backgroundImageMode);
            }

            pageDoc.clearHistory();
            pageDoc.setHistoryListener(this);

            this.surfaceView.setPageDoc(pageDoc, true);

            if (this.penEnabled) {
                this.surfaceView.setToolTypeAction(SpenSettingViewInterface.TOOL_FINGER, SpenSettingViewInterface.ACTION_NONE);

                this.toolType = SpenSettingViewInterface.TOOL_SPEN;
            } else {
                this.surfaceView.setToolTypeAction(SpenSettingViewInterface.TOOL_FINGER, SpenSettingViewInterface.ACTION_STROKE);

                this.toolType = SpenSettingViewInterface.TOOL_FINGER;

                Toast.makeText(this.activity, R.string.message_finger_only, Toast.LENGTH_SHORT).show();
            }
        }

        if (this.penSettingEnabled) {
            container.addView(this.penSetting);
            this.penSetting.setCanvasView(this.surfaceView);
        }

        if (this.eraserSettingEnabled) {
            container.addView(this.eraserSetting);
            this.eraserSetting.setCanvasView(this.surfaceView);
        }

        this.surfaceView.setReplayListener(this);
        this.surfaceView.setZoomListener(this);
        this.surfaceView.setPenChangeListener(this);
        this.surfaceView.setEraserChangeListener(this);
        this.surfaceView.setPenDetachmentListener(this);
        this.surfaceView.setTouchListener(this);
    }

    private void initPenInfo(final int penColor, final int penSize) {
        if (this.penSettingEnabled) {
            if (this.surfaceView == null) {
                throw new IllegalStateException();
            }

            final SpenSettingPenInfo info = new SpenSettingPenInfo();

            info.color = penColor;
            info.size  = penSize;

            this.surfaceView.setPenSettingInfo(info);
            this.penSetting.setInfo(info);
        }
    }

    private void initEraserInfo(final int eraserSize) {
        if (this.eraserSettingEnabled) {
            if (this.surfaceView == null) {
                throw new IllegalStateException();
            }

            final SpenSettingEraserInfo info = new SpenSettingEraserInfo();

            info.size = eraserSize;

            this.surfaceView.setEraserSettingInfo(info);
            this.eraserSetting.setInfo(info);
        }
    }

    private void onPenSettingClick() {
        if (this.penSettingEnabled) {
            if (this.penSetting.isShown()) {
                this.penSetting.setVisibility(View.GONE);
            } else {
                this.penSetting.setViewMode(SpenSettingPenLayout.VIEW_MODE_EXTENSION);

                final View                     toolsLayout = (View)this.rootLayout.findViewById(R.id.pen_buttons).getParent();
                final FrameLayout.LayoutParams params      = (FrameLayout.LayoutParams)this.penSetting.getLayoutParams();
                params.leftMargin = (int)toolsLayout.getX() + this.penButton.getLeft();
                params.topMargin  = (int)toolsLayout.getY() + this.penButton.getTop() + this.penButton.getHeight();

                this.penSetting.setLayoutParams(params);
                this.penSetting.setVisibility(View.VISIBLE);
            }
        }
    }

    private void onEraserSettingClick() {
        if (this.eraserSettingEnabled) {
            if (this.surfaceView.getToolTypeAction(this.toolType) == SpenSettingViewInterface.ACTION_ERASER) {
                if (this.eraserSetting.isShown()) {
                    this.eraserSetting.setVisibility(View.GONE);
                } else {
                    this.eraserSetting.setViewMode(SpenSettingEraserLayout.VIEW_MODE_NORMAL);

                    final View                     toolsLayout = (View)this.rootLayout.findViewById(R.id.pen_buttons).getParent();
                    final FrameLayout.LayoutParams params      = (FrameLayout.LayoutParams)this.eraserSetting.getLayoutParams();
                    params.leftMargin = (int)toolsLayout.getX() + this.eraserButton.getLeft();
                    params.topMargin  = (int)toolsLayout.getY() + this.eraserButton.getTop() + this.eraserButton.getHeight();

                    this.eraserSetting.setLayoutParams(params);
                    this.eraserSetting.setVisibility(View.VISIBLE);
                }
            } else {
                this.surfaceView.setToolTypeAction(this.toolType, SpenSettingViewInterface.ACTION_ERASER);
            }
        }
    }

    private void handleUnsupportedException(final SsdkUnsupportedException e) {
        int     messageId = R.string.message_spen_not_initialized;
        boolean isFatal   = false;

        switch (e.getType()) {
            case SsdkUnsupportedException.VENDOR_NOT_SUPPORTED:
                messageId = R.string.message_non_samsung_device;
                isFatal   = true;

                break;

            case SsdkUnsupportedException.DEVICE_NOT_SUPPORTED:
                messageId = R.string.message_spen_not_found;
                isFatal   = true;

                break;

            case SsdkUnsupportedException.LIBRARY_NOT_INSTALLED:
                messageId = R.string.message_spen_library_not_found;

                break;

            case SsdkUnsupportedException.LIBRARY_UPDATE_IS_REQUIRED:
            case SsdkUnsupportedException.LIBRARY_UPDATE_IS_RECOMMENDED:
                messageId = R.string.message_spen_library_update_required;

                break;
        }

        if (isFatal) {
            Toast.makeText(this.activity, messageId, Toast.LENGTH_SHORT).show();

            this.activity.finish();
        } else {
            new AlertDialog.Builder(this.activity).setIcon(android.R.drawable.ic_dialog_alert).setTitle(R.string.message_update).setMessage(messageId).setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @SuppressWarnings("synthetic-access")
                @Override
                public void onClick(final DialogInterface dialog, final int which) {
                    PenService.this.activity.startActivity(new Intent(Intent.ACTION_VIEW, PenService.SPEN_SDK_MARKET_URI).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));

                    dialog.dismiss();

                    PenService.this.activity.finish();
                }
            }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                @SuppressWarnings({"synthetic-access"})
                @Override
                public void onClick(final DialogInterface dialog, final int which) {
                    dialog.dismiss();

                    PenService.this.activity.finish();
                }
            }).show();
        }
    }
}
