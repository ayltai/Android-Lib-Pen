package android.lib.pen.demo;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.util.Log;

final class IOUtils {
    private IOUtils() {
    }

    /**
     * Saves the specified bitmap to a JPG file.
     */
    public static boolean write(final String path, final Bitmap bitmap) {
        OutputStream outputStream = null;

        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(path));

            bitmap.compress(CompressFormat.JPEG, Constants.THUMBNAIL_QUALITY, outputStream);

            return true;
        } catch (final FileNotFoundException e) {
            Log.e(IOUtils.class.getClass().getName(), e.getMessage(), e);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (final IOException e) {
                    Log.w(IOUtils.class.getClass().getName(), e.getMessage(), e);
                }
            }

            bitmap.recycle();
        }

        return false;
    }
}
