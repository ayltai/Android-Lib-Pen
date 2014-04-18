package android.lib.pen.demo;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

public final class MainActivity extends Activity implements View.OnClickListener, AdapterView.OnItemClickListener {
    private GalleryAdapter adapter;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.activity_main);

        final GridView gallery = (GridView)this.findViewById(R.id.gallery);
        gallery.setAdapter(this.adapter = new GalleryAdapter(this));
        gallery.setOnItemClickListener(this);

        this.findViewById(R.id.create).setOnClickListener(this);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            this.adapter.refresh();
        }
    }

    @Override
    public void onClick(final View view) {
        this.startActivityForResult(new Intent(this, DrawingActivity.class), 1);
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
        final String thumbnailName = new File((String)this.adapter.getItem(position)).getName();

        this.startActivityForResult(new Intent(this, DrawingActivity.class).putExtra(Constants.EXTRA_SPD_PATH, new File(Constants.SPD_PATH, thumbnailName.substring(0, thumbnailName.lastIndexOf(".")) + Constants.SPD_EXTENSION).getAbsolutePath()), 1); //$NON-NLS-1$
    }
}
