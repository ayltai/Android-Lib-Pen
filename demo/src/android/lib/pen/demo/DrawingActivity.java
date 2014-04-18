package android.lib.pen.demo;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.lib.pen.PenService;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.samsung.android.sdk.pen.document.SpenInvalidPasswordException;
import com.samsung.android.sdk.pen.document.SpenUnsupportedTypeException;
import com.samsung.android.sdk.pen.document.SpenUnsupportedVersionException;

public final class DrawingActivity extends Activity implements Runnable, OnPageUpdatedListener, OnReplayCompletedListener {
    private DrawingService service;

    private ImageView imageView;

    private String spdPath;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setResult(Activity.RESULT_CANCELED);

        this.setContentView(R.layout.activity_drawing);

        // Gets the .spd file path
        this.spdPath = this.getIntent().getStringExtra(Constants.EXTRA_SPD_PATH);

        if (this.spdPath != null && !new File(this.spdPath).exists()) {
            Toast.makeText(this, R.string.message_spd_not_found, Toast.LENGTH_SHORT).show();

            this.finish();
        }

        // Creates a PenService. It will use the root layout specified here to get the @id/pen_container and @id/pen_canvas
        this.service = new DrawingService(this, this.findViewById(R.id.root_layout));

        // We will update the canvas thumbnail when it changes
        this.imageView = (ImageView)this.findViewById(R.id.preview);

        // Adds drag & drop support for the tools layout and the thumbnail, so that the user can draw anywhere on the canvas by moving them around
        DragDropUtils.addDragDropSupport(this.findViewById(R.id.drag_tools), this.findViewById(R.id.tools_layout), this.findViewById(R.id.root_layout));
        DragDropUtils.addDragDropSupport(this.findViewById(R.id.drag_preview), this.findViewById(R.id.preview_layout), this.findViewById(R.id.root_layout));

        // It is better to initialize PenService asynchronously because it takes time
        new Handler().post(this);
    }

    @Override
    protected void onDestroy() {
        // Remember to call PenService.onDestroy() in Activity.onDestroy()
        if (this.service != null) {
            this.service.onDestroy();
        }

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        // Asks the user to save the drawing if it is modified
        if (this.service != null && this.service.isDirty()) {
            new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setTitle(R.string.dialog_unsaved)
            .setMessage(R.string.dialog_unsaved_message)
            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialog, final int which) {
                    DrawingActivity.this.save();
                }
            })
            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialog, final int which) {
                    DrawingActivity.super.onBackPressed();
                }
            })
            .show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onPageUpdated() {
        // Updates thumbnail whenever the page is changed
        this.updateThumbnail();
    }

    @Override
    public void onReplayCompleted() {
        if (this.service != null) {
            // Makes sure the replay is stopped
            this.service.stopReplay();

            // (Optional) Records strokes for replay later. Mostly for eye-candy.
            this.service.startRecord();
        }
    }

    @Override
    public void run() {
        if (this.spdPath == null) {
            // Creates a new drawing and prepares it for editing
            if (this.prepareEditNewDrawing()) {
                // (Optional) Records strokes for replay later. Mostly for eye-candy.
                this.service.startRecord();
            }
        } else {
            // Loads an existing drawing and prepares it for editing
            if (this.prepareEditOldDrawing()) {
                // (Optional) Replay strokes. Mostly for eye-candy.
                this.service.startReplay();
            }
        }
    }

    /**
     * Initializes PenService for a blank drawing.
     * @return <code>true</code> if PenService is initialized successfully; otherwise, <code>false</code>.
     */
    private boolean prepareEditNewDrawing() {
        if (this.initCanvas()) {
            // Creates a new page for the drawing.
            // Here we use a white background. You may use an image background instead.
            // If an image background is used, the color background will be ignored because transparent image background is not supported.
            this.service.appendPage(Constants.CANVAS_BACKGROUND_COLOR, null, PenService.MODE_FIT);

            // Updates the thumbnail after the page is changed.
            this.updateThumbnail();

            return true;
        }

        return false;
    }

    /**
     * Initializes PenService for an existing drawing.
     * @return <code>true</code> if PenService is initialized successfully; otherwise, <code>false</code>.
     */
    private boolean prepareEditOldDrawing() {
        if (this.initCanvas()) {
            try {
                // Loads an existing .spd file and make it editable.
                this.service.load(this.spdPath, true);

                // Updates the thumbnail after the page is changed.
                this.updateThumbnail();

                return true;
            } catch (final SpenInvalidPasswordException e) { // If the .spd file is password-protected, we must use this.service.load(this.spdPath, password, true) instead.
                Toast.makeText(this, R.string.message_spd_locked, Toast.LENGTH_SHORT).show();

                this.finish();
            } catch (final SpenUnsupportedTypeException e) { // The .spd file is not supported for whatever reason
                Toast.makeText(this, R.string.message_spd_unsupported, Toast.LENGTH_SHORT).show();

                this.finish();
            } catch (final SpenUnsupportedVersionException e) {
                Toast.makeText(this, R.string.message_spen_library_update_required, Toast.LENGTH_SHORT).show();

                this.finish();
            } catch (final IOException e) {
                Toast.makeText(this, R.string.message_spd_not_found, Toast.LENGTH_SHORT).show();

                this.finish();
            }
        }

        return false;
    }

    /**
     * Initializes the canvas and sets its dimensions the same as the screen size.
     * @return <code>true</code> if the PenService is initialized successfully; otherwise, <code>false</code>.
     */
    private boolean initCanvas() {
        final DisplayMetrics metrics = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(metrics);

        if (this.service.init(metrics.widthPixels, metrics.heightPixels, Constants.CANVAS_BACKGROUND_COLOR, null, PenService.MODE_FIT, Constants.DEFAULT_PEN_COLOR, Constants.DEFAULT_PEN_SIZE, Constants.DEFAULT_ERASER_SIZE)) {
            // (Optional) If the canvas is larger than the screen, we can enable "smart scrolling". It will automatically scroll with the specified speed when the pen is near the specified edge pixels.
            this.service.setScrollEnabled(false, (int)(Math.max(metrics.widthPixels, metrics.heightPixels) * Constants.CANVAS_SCROLL_EDGE), PenService.SPEED_NORMAL);

            // (Optional) Enables zooming for double-tap (by finger or pen)
            this.service.setZoomEnabled(true, Constants.CANVAS_ZOOM_SCALE);

            this.service.setButtonVisibility(PenService.BUTTON_ZOOM, View.GONE);

            // Attaches OnPageUpdatedListener after everything is initialized
            this.service.setOnPageUpdatedListener(this);

            this.updateThumbnail();

            return true;
        }

        return false;
    }

    /**
     * Updates the thumbnail.
     */
    private void updateThumbnail() {
        // Generates a Bitmap by scaling the canvas by a specific ratio
        this.imageView.setImageBitmap(this.service.generateThumbnail(Constants.CANVAS_PREVIEW_SCALE));
    }

    /**
     * Saves the drawing to a .spd file.
     * <p>It is better to save asynchronously because it takes time.</p>
     */
    private void save() {
        final ProgressDialog progressDialog = ProgressDialog.show(this, this.getString(R.string.save), this.getString(R.string.wait), true, false);

        final Handler handler = new Handler() {
            @Override
            public void handleMessage(final Message message) {
                progressDialog.dismiss();

                if (((Boolean)message.obj).booleanValue()) {
                    Toast.makeText(DrawingActivity.this, R.string.message_saved, Toast.LENGTH_SHORT).show();

                    // Tells MainActivity that we have created a new drawing so it refreshes the dashboard
                    DrawingActivity.this.setResult(Activity.RESULT_OK, DrawingActivity.this.getIntent().putExtra(Constants.EXTRA_SPD_PATH, DrawingActivity.this.spdPath));

                    DrawingActivity.this.finish();
                } else {
                    Toast.makeText(DrawingActivity.this, R.string.message_save_error, Toast.LENGTH_SHORT).show();
                }
            }
        };

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(final Void... params) {
                return Boolean.valueOf(DrawingActivity.this.onSave());
            }

            @Override
            protected void onPostExecute(final Boolean result) {
                handler.sendMessage(handler.obtainMessage(0, result));
            }
        }.execute();
    }

    private boolean onSave() {
        // Here we create a random file name to save the drawing. Of course you can save it to a different path.
        if (this.spdPath == null) {
            new File(Constants.SPD_PATH).mkdirs();

            this.spdPath = new File(Constants.SPD_PATH, UUID.randomUUID().toString() + Constants.SPD_EXTENSION).getAbsolutePath();
        }

        try {
            // Saves the drawing to a .spd file
            this.service.save(this.spdPath);

            final String spdName = new File(this.spdPath).getName();

            new File(Constants.THUMB_PATH).mkdirs();

            // It is better to save the thumbnail to a separate file because we cannot get the thumbnail from a .spd file without first initializing PenService (and this takes time).
            if (!this.saveThumbnail(new File(Constants.THUMB_PATH, spdName.substring(0, spdName.lastIndexOf(".")) + Constants.JPG_EXTENSION).getAbsolutePath())) { //$NON-NLS-1$
                return false;
            }

            return true;
        } catch (final IOException e) {
            Log.e(this.getClass().getName(), e.getMessage(), e);
        }

        return false;
    }

    /**
     * Saves a thumbnail of the drawing to a file.
     * @return <code>true</code> if the thumbnail is saved successfully; otherwise, <code>false</code>.
     */
    private boolean saveThumbnail(final String thumbnailPath) {
        // Generates a thumbnail by the specified scale and saves it
        return IOUtils.write(thumbnailPath, this.service.generateThumbnail(Constants.THUMBNAIL_SCALE));
    }
}
