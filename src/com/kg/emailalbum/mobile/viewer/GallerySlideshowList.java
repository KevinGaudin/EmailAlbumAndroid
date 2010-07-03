package com.kg.emailalbum.mobile.viewer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.acra.ErrorReporter;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap.Config;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.Images.Media;
import android.util.Log;

import com.kg.emailalbum.mobile.util.BitmapLoader;

public class GallerySlideshowList extends SlideshowList {

    private static final String LOG_TAG = GallerySlideshowList.class
            .getSimpleName();

    private Context mContext = null;

    private List<Uri> mUris = new ArrayList<Uri>();

    private Integer mTargetSize;

    public GallerySlideshowList(Context context, int targetSize) {
        mContext = context;
        mTargetSize = targetSize;

        String[] projection = { ImageColumns.BUCKET_DISPLAY_NAME,
                ImageColumns.DATE_TAKEN, ImageColumns.TITLE, ImageColumns._ID,
                ImageColumns.DATA, ImageColumns.BUCKET_ID };

        Cursor cursor = mContext.getContentResolver().query(
                Media.EXTERNAL_CONTENT_URI, projection, null, null,
                MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC");
        if (cursor != null) {
            cursor.moveToFirst();

            // Iterate over all images
            while (!cursor.isAfterLast()) {
                Uri imageUri = Uri.withAppendedPath(Media.EXTERNAL_CONTENT_URI,
                        cursor.getString(cursor
                                .getColumnIndexOrThrow(ImageColumns._ID)));
                mUris.add(imageUri);

                cursor.moveToNext();
            }
        }

    }

    @Override
    public Uri getAlbumUri() {
        return Media.EXTERNAL_CONTENT_URI;
    }

    @Override
    public InputStream getOriginalInputStream(int position) throws IOException {
        mContext.getContentResolver().openInputStream(mUris.get(position));
        return null;
    }

    @Override
    public SlideshowItem get(int location) {
        SlideshowItem result = new SlideshowItem();
        result.name = mUris.get(location).toString();
        result.caption = "";
        if (mTargetSize > 0) {
            try {
                result.bitmap = BitmapLoader.load(mContext,
                        mUris.get(location), mTargetSize, mTargetSize,
                        Config.RGB_565, false);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                Log.e(LOG_TAG, "Error : ", e);
                ErrorReporter.getInstance().handleException(e);
            }
        }
        return result;
    }

    @Override
    public int size() {
        return mUris.size();
    }

}
