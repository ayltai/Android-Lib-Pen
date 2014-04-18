package android.lib.pen.demo;

import java.io.File;

import android.graphics.Color;
import android.os.Environment;

final class Constants {
    public static final String SPD_PATH   = new File(Environment.getExternalStorageDirectory(), "spd_files").getAbsolutePath(); //$NON-NLS-1$
    public static final String THUMB_PATH = new File(Environment.getExternalStorageDirectory(), "thumbs").getAbsolutePath();    //$NON-NLS-1$

    public static final String SPD_EXTENSION = ".spd"; //$NON-NLS-1$
    public static final String JPG_EXTENSION = ".jpg"; //$NON-NLS-1$

    public static final String EXTRA_SPD_PATH = "SPD_PATH"; //$NON-NLS-1$

    public static final int CANVAS_BACKGROUND_COLOR = Color.WHITE;
    public static final int DEFAULT_PEN_COLOR       = Color.BLUE;
    public static final int DEFAULT_PEN_SIZE        = 24;
    public static final int DEFAULT_ERASER_SIZE     = 36;

    public static final float CANVAS_ZOOM_SCALE    = 1.5f;
    public static final float CANVAS_SCROLL_EDGE   = 0.1f;
    public static final float CANVAS_PREVIEW_SCALE = 0.1f;

    public static final float THUMBNAIL_SCALE   = 0.2f;
    public static final int   THUMBNAIL_QUALITY = 80;

    private Constants() {
    }
}
