package com.kg.emailalbum.mobile.viewer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.acra.ErrorReporter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap.Config;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.Images.Media;
import android.text.format.Time;
import android.util.Log;

import com.kg.emailalbum.mobile.tags.Tag;
import com.kg.emailalbum.mobile.tags.TagFilter;
import com.kg.emailalbum.mobile.tags.TagProvider;
import com.kg.emailalbum.mobile.tags.TagsDbAdapter;
import com.kg.emailalbum.mobile.tags.Tag.TagType;
import com.kg.emailalbum.mobile.util.BitmapLoader;

public class GallerySlideshowList extends SlideshowList {

    private static final String LOG_TAG = GallerySlideshowList.class
            .getSimpleName();

    private Context mContext = null;
    private TagsDbAdapter mTagsDb = null;

    private List<Uri> mUris = new ArrayList<Uri>();
    private Map<Uri, Bundle> mMetaData = new HashMap<Uri, Bundle>();

    private Integer mTargetSize;

    public GallerySlideshowList(Context context, TagsDbAdapter tagsDb,
            int targetSize) {
        mContext = context;
        mTagsDb = tagsDb;
        mTargetSize = targetSize;

        String[] projection = { ImageColumns.BUCKET_DISPLAY_NAME,
                ImageColumns.DATE_TAKEN, ImageColumns.TITLE, ImageColumns._ID,
                ImageColumns.DATA, ImageColumns.BUCKET_ID,
                ImageColumns.BUCKET_DISPLAY_NAME };

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
                Bundle metadata = new Bundle();
                metadata.putString(
                        ImageColumns.BUCKET_DISPLAY_NAME,
                        cursor.getString(cursor
                                .getColumnIndexOrThrow(ImageColumns.BUCKET_DISPLAY_NAME)));
                metadata.putString(ImageColumns.TITLE, cursor.getString(cursor
                        .getColumnIndexOrThrow(ImageColumns.TITLE)));
                metadata.putLong(ImageColumns.DATE_TAKEN, cursor.getLong(cursor
                        .getColumnIndexOrThrow(ImageColumns.DATE_TAKEN)));
                metadata.putLong(
                        ImageColumns.BUCKET_ID,
                        Long.parseLong(cursor.getString(cursor
                                .getColumnIndexOrThrow(ImageColumns.BUCKET_ID))));
                mMetaData.put(imageUri, metadata);
                cursor.moveToNext();
            }
        }

    }

    @Override
    public Uri getAlbumUri() {
        return Media.EXTERNAL_CONTENT_URI;
    }


    @Override
    public SlideshowItem get(int location) {
        SlideshowItem result = new SlideshowItem();
        result.uri = mUris.get(location);
        result.name = result.uri.toString();
        result.caption = "";
        if (mTargetSize > 0) {
            try {
                Uri imageUri = mUris.get(location);
                result.bitmap = BitmapLoader.load(mContext, imageUri,
                        mTargetSize, mTargetSize, Config.RGB_565, false);

                Bundle metadata = mMetaData.get(imageUri);

                Log.d(LOG_TAG, "METADATA : " + metadata.toString());

                // Add the BUCKET tag
                Tag bucketTag = mTagsDb.createTag(
                        metadata.getString(ImageColumns.BUCKET_DISPLAY_NAME),
                        TagType.BUCKET);
                mTagsDb.setTag(bucketTag, imageUri);

                if (!result.isDateTakenSet()) {
                    long dateTakenInMillis = metadata
                            .getLong(ImageColumns.DATE_TAKEN);
                    Time dateTaken = new Time();
                    dateTaken.set(dateTakenInMillis);
                    Tag tag = mTagsDb.createTag(dateTaken.format("%B"),
                            TagType.MONTH);
                    mTagsDb.setTag(tag, imageUri);
                    tag = mTagsDb.createTag(dateTaken.format("%Y"),
                            TagType.YEAR);
                    mTagsDb.setTag(tag, imageUri);
                    tag = mTagsDb.createTag(Long.toString(dateTakenInMillis), TagType.TIMESTAMP);
                    mTagsDb.setTag(tag, imageUri);
                }

                result.tags = mTagsDb.getTags(imageUri);
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

    @Override
    public void setFilters(TagFilter[] array) {
        // TODO Auto-generated method stub
        
    }

}
