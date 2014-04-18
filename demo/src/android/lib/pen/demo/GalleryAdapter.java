package android.lib.pen.demo;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

final class GalleryAdapter extends BaseAdapter {
    private static final FilenameFilter FILTER = new FilenameFilter() {
        @Override
        public boolean accept(final File dir, final String filename) {
            return filename.endsWith(Constants.JPG_EXTENSION) && filename.length() > Constants.JPG_EXTENSION.length();
        }
    };

    private final Context      context;
    private final List<String> items = new ArrayList<String>();

    public GalleryAdapter(final Context context) {
        this.context = context;

        this.refresh();
    }

    @Override
    public int getCount() {
        return this.items.size();
    }

    @Override
    public Object getItem(final int position) {
        return this.items.get(position);
    }

    @Override
    public long getItemId(final int position) {
        return position;
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        final ImageView view = convertView == null ? new ImageView(this.context) : (ImageView)convertView;

        // We should load the bitmap asynchronously and use weak references for the Bitmap objects, but we don't bother in this example.
        view.setImageBitmap(BitmapFactory.decodeFile(this.items.get(position)));

        return view;
    }

    public void refresh() {
        final File folder = new File(Constants.THUMB_PATH);

        if (!folder.exists()) {
            folder.mkdirs();
        }

        this.items.clear();

        for (final File file : folder.listFiles(GalleryAdapter.FILTER)) {
            this.items.add(file.getAbsolutePath());
        }

        this.notifyDataSetChanged();
    }
}
