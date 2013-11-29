package android.lib.pen;

import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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
import com.samsung.android.sdk.pen.document.SpenPageDoc;
import com.samsung.android.sdk.pen.document.SpenUnsupportedTypeException;
import com.samsung.android.sdk.pen.document.SpenUnsupportedVersionException;
import com.samsung.android.sdk.pen.engine.SpenColorPickerListener;
import com.samsung.android.sdk.pen.engine.SpenSurfaceView;
import com.samsung.android.sdk.pen.settingui.SpenSettingEraserLayout;
import com.samsung.android.sdk.pen.settingui.SpenSettingPenLayout;

public class PenService implements View.OnClickListener, SpenColorPickerListener, SpenSettingEraserLayout.EventListener, SpenPageDoc.HistoryListener {
    public static final int BUTTON_PEN    = 0;
    public static final int BUTTON_ERASER = 1;
    public static final int BUTTON_UNDO   = 2;
    public static final int BUTTON_REDO   = 3;

    public static final int MODE_CENTER  = SpenPageDoc.BACKGROUND_IMAGE_MODE_CENTER;
    public static final int MODE_FIT     = SpenPageDoc.BACKGROUND_IMAGE_MODE_FIT;
    public static final int MODE_TILE    = SpenPageDoc.BACKGROUND_IMAGE_MODE_TILE;
    public static final int MODE_STRETCH = SpenPageDoc.BACKGROUND_IMAGE_MODE_STRETCH;

    private static final String NULL = new String();

    private static final Uri SPEN_SDK_MARKET_URI = Uri.parse("market://details?id=" + Spen.SPEN_NATIVE_PACKAGE_NAME); //$NON-NLS-1$

    private final Activity activity;

    private SpenSurfaceView surfaceView;
    private SpenNoteDoc     noteDoc;

    private final SpenSettingPenLayout    penSetting;
    private final SpenSettingEraserLayout eraserSetting;

    private ToggleButton penButton;
    private ToggleButton eraserButton;
    private ImageButton  undoButton;
    private ImageButton  redoButton;

    private boolean penEnabled;
    private boolean penSettingEnabled    = true;
    private boolean eraserSettingEnabled = true;
    private int     toolType             = SpenSettingViewInterface.TOOL_SPEN;
    private int     currentPage;
    private int     canvasWidth;
    private boolean dirty;

    public PenService(final Activity activity) {
        this.activity = activity;

        final ViewGroup      penButtons = (ViewGroup)activity.findViewById(R.id.pen_buttons);
        final RelativeLayout canvas     = (RelativeLayout)activity.findViewById(R.id.pen_canvas);

        if (penButtons != null) {
            this.penButton    = (ToggleButton)penButtons.findViewById(R.id.pen_pen_button);
            this.eraserButton = (ToggleButton)penButtons.findViewById(R.id.pen_eraser_button);
            this.undoButton   = (ImageButton)penButtons.findViewById(R.id.pen_undo_button);
            this.redoButton   = (ImageButton)penButtons.findViewById(R.id.pen_redo_button);
        }

        this.penSetting    = new SpenSettingPenLayout(activity, PenService.NULL, canvas);
        this.eraserSetting = new SpenSettingEraserLayout(activity, PenService.NULL, canvas);
    }

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

    @Override
    public void onClick(final View view) {
        switch (view.getId()) {
            case BUTTON_PEN:    this.selectButton(PenService.BUTTON_PEN);    break;
            case BUTTON_ERASER: this.selectButton(PenService.BUTTON_ERASER); break;
            case BUTTON_UNDO:   this.undo();                                 break;
            case BUTTON_REDO:   this.redo();                                 break;
        }
    }

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

    @Override
    public void onCommit(final SpenPageDoc doc) {
        this.dirty = true;
    }

    @Override
    public void onUndoable(final SpenPageDoc doc, final boolean undoable) {
        if (this.undoButton != null) {
            this.undoButton.setEnabled(undoable);
        }
    }

    @Override
    public void onRedoable(final SpenPageDoc doc, final boolean redoable) {
        if (this.redoButton != null) {
            this.redoButton.setEnabled(redoable);
        }
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public boolean isPenEnabled() {
        return this.penEnabled;
    }

    public int getCurrentPage() {
        return this.noteDoc == null ? -1 : this.currentPage;
    }

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

    public void setButtonVisibility(final int button, final int visibility) {
        final View view;

        switch (button) {
            case BUTTON_PEN:    view = this.penButton;    break;
            case BUTTON_ERASER: view = this.eraserButton; break;
            case BUTTON_UNDO:   view = this.undoButton;   break;
            case BUTTON_REDO:   view = this.redoButton;   break;

            default:
                throw new IllegalArgumentException();
        }

        if (view != null) {
            view.setVisibility(visibility);
        }
    }

    public void setButtonResource(final int button, final int resource) {
        this.setButtonDrawable(button, this.activity.getResources().getDrawable(resource));
    }

    public void setButtonDrawable(final int button, final Drawable drawable) {
        switch (button) {
            case BUTTON_PEN:
                if (this.penButton != null) {
                    this.penButton.setCompoundDrawables(null, drawable, null, null);
                }

                break;

            case BUTTON_ERASER:
                if (this.eraserButton != null) {
                    this.eraserButton.setCompoundDrawables(null, drawable, null, null);
                }

                break;

            case BUTTON_UNDO:
                if (this.undoButton != null) {
                    this.undoButton.setImageDrawable(drawable);
                }

                break;

            case BUTTON_REDO:
                if (this.redoButton != null) {
                    this.redoButton.setImageDrawable(drawable);
                }

                break;

            default:
                throw new IllegalArgumentException();
        }
    }

    public boolean isPenSettingEnabled() {
        return this.penSettingEnabled;
    }

    public void setPenSettingEnabled(final boolean enabled) {
        this.penSettingEnabled = enabled;
    }

    public boolean isEraserSettingEnabled() {
        return this.eraserSettingEnabled;
    }

    public void setEraserSettingEnabled(final boolean enabled) {
        this.eraserSettingEnabled = enabled;
    }

    public void selectButton(final int button) {
        if (this.surfaceView == null) {
            throw new IllegalStateException();
        }

        this.penSetting.setVisibility(View.GONE);
        this.eraserSetting.setVisibility(View.GONE);

        switch (button) {
            case BUTTON_PEN:
                if (this.penButton != null) {
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

    public void startRecord() {
        if (this.noteDoc != null) {
            final SpenPageDoc pageDoc = this.noteDoc.getPage(this.currentPage);

            if (!pageDoc.isRecording()) {
                pageDoc.startRecord();
            }
        }
    }

    public void stopRecord() {
        if (this.noteDoc != null) {
            final SpenPageDoc pageDoc = this.noteDoc.getPage(this.currentPage);

            if (pageDoc.isRecording()) {
                pageDoc.stopRecord();
            }
        }
    }

    public Bitmap generateThumbnail(final float scale) {
        if (this.surfaceView == null) {
            throw new IllegalStateException();
        }

        return this.surfaceView.capturePage(scale);
    }

    public void save(final String path) throws IOException {
        if (this.dirty && this.noteDoc != null) {
            this.noteDoc.save(path);
        }
    }

    public void load(final String path) throws SpenInvalidPasswordException, SpenUnsupportedTypeException, SpenUnsupportedVersionException, IOException {
        if (this.surfaceView == null) {
            throw new IllegalStateException();
        }

        final SpenNoteDoc noteDoc = new SpenNoteDoc(this.activity, path, this.canvasWidth, SpenNoteDoc.MODE_WRITABLE);

        if (this.noteDoc != null) {
            this.noteDoc.close();
        }

        this.noteDoc = noteDoc;

        if (this.noteDoc.getPageCount() > 0) {
            this.currentPage = 0;

            this.surfaceView.setPageDoc(this.noteDoc.getPage(0), true);
            this.surfaceView.update();
        }
    }

    public void load(final String path, final String password) throws SpenInvalidPasswordException, SpenUnsupportedTypeException, SpenUnsupportedVersionException, IOException {
        if (this.surfaceView == null) {
            throw new IllegalStateException();
        }

        final SpenNoteDoc noteDoc = new SpenNoteDoc(this.activity, path, password, this.canvasWidth, SpenNoteDoc.MODE_WRITABLE);

        if (this.noteDoc != null) {
            this.noteDoc.close();
        }

        this.noteDoc = noteDoc;

        if (this.noteDoc.getPageCount() > 0) {
            this.currentPage = 0;

            this.surfaceView.setPageDoc(this.noteDoc.getPage(0), true);
            this.surfaceView.update();
        }
    }

    public void init(final int canvasWidth, final int canvasHeight, final int backgroundColor, final String backgroundImagePath, final int backgroundImageMode, final int penColor, final int penSize, final int eraserSize) {
        this.initSpen();

        final ViewGroup container = (ViewGroup)this.activity.findViewById(R.id.pen_container);

        if (this.penSettingEnabled) {
            container.addView(this.penSetting);
        }

        if (this.eraserSettingEnabled) {
            container.addView(this.eraserSetting);
        }

        this.initCanvas(canvasWidth, canvasHeight, backgroundColor, backgroundImagePath, backgroundImageMode);
        this.initPenInfo(penColor, penSize);
        this.initEraserInfo(eraserSize);

        this.surfaceView.setColorPickerListener(this);
        this.eraserSetting.setEraserListener(this);
        this.penButton.setOnClickListener(this);
        this.eraserButton.setOnClickListener(this);
        this.undoButton.setOnClickListener(this);
        this.redoButton.setOnClickListener(this);

        final SpenPageDoc pageDoc = this.noteDoc.getPage(this.currentPage);

        this.undoButton.setEnabled(pageDoc.isUndoable());
        this.redoButton.setEnabled(pageDoc.isRedoable());

        pageDoc.setHistoryListener(this);

        this.selectButton(PenService.BUTTON_PEN);
    }

    private void initSpen() {
        final Spen spen = new Spen();

        try {
            spen.initialize(this.activity);

            this.penEnabled = spen.isFeatureEnabled(Spen.DEVICE_PEN);
        } catch (final SsdkUnsupportedException e) {
            this.handleUnsupportedException(e);
        } catch (final Exception e) {
            Toast.makeText(this.activity, R.string.message_spen_not_initialized, Toast.LENGTH_SHORT).show();

            this.activity.finish();
        }
    }

    private void initCanvas(final int canvasWidth, final int canvasHeight, final int backgroundColor, final String backgroundImagePath, final int backgroundImageMode) {
        final ViewGroup canvas = (ViewGroup)this.activity.findViewById(R.id.pen_canvas);

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
            final SpenPageDoc pageDoc = this.noteDoc.appendPage(backgroundColor, backgroundImagePath, backgroundImageMode);

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
