package com.kg.emailalbum.mobile.viewer;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.List;

import org.acra.ErrorReporter;

import android.content.Context;
import android.graphics.Bitmap.Config;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Images.ImageColumns;
import android.text.format.Time;
import android.util.Log;

import com.kg.emailalbum.mobile.tags.Tag;
import com.kg.emailalbum.mobile.tags.Tag.TagType;
import com.kg.emailalbum.mobile.tags.TagProvider;
import com.kg.emailalbum.mobile.tags.TagsDbAdapter;
import com.kg.emailalbum.mobile.util.BitmapLoader;

public class TagFilterSlideshowList extends SlideshowList {
    private static final String LOG_TAG = TagFilterSlideshowList.class.getSimpleName();
    private Context mContext;
    private TagsDbAdapter mTagsDb;
    private int mTargetSize;
    
    private List<Uri> mUris;

    public TagFilterSlideshowList(Context context, TagsDbAdapter tagsDb,
            int targetSize, Tag[] tags) {
        mContext = context;
        mTagsDb = tagsDb;
        mTargetSize = targetSize;

        
        mUris = mTagsDb.getUrisFromAllTags(tags);
        

    }

    
    
    @Override
    public InputStream getOriginalInputStream(int position) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri getAlbumUri() {
        // TODO Auto-generated method stub
        return null;
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
                if (mTagsDb != null) {
                    result.tags = mTagsDb.getTags(imageUri);
                }

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
