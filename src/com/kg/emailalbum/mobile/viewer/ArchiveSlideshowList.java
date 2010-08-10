package com.kg.emailalbum.mobile.viewer;

import java.io.File;
import java.util.ArrayList;
import java.util.zip.ZipFile;

import org.acra.ErrorReporter;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.kg.emailalbum.mobile.tags.TagFilter;
import com.kg.emailalbum.mobile.tags.TagsDbAdapter;
import com.kg.emailalbum.mobile.util.ZipUtil;
import com.kg.oifilemanager.filemanager.FileManagerProvider;

public class ArchiveSlideshowList extends SlideshowList {

    private static final String LOG_TAG = ArchiveSlideshowList.class
            .getSimpleName();
    private ZipFile mArchive;
    private Bundle mCaptions;
    private TagsDbAdapter mTagsDb;
    private ArrayList<String> mItemNames;

    public ArchiveSlideshowList(TagsDbAdapter tagsDb, Uri archiveUri) {
        try {
            File albumFile = new File(archiveUri.getPath());
            mArchive = new ZipFile(albumFile);
            mItemNames = ZipUtil.getPicturesFilesList(mArchive);
            mCaptions = ZipUtil.loadCaptions(mArchive);
            mTagsDb = tagsDb;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error while opening archive file.", e);
            ErrorReporter.getInstance().handleException(e);
        }
    }

    @Override
    public SlideshowItem get(int position) {
        Log.d(LOG_TAG, "Get SlideshowItem " + mItemNames.get(position));
        SlideshowItem result = new SlideshowItem();
        Uri imageUri = FileManagerProvider.getContentUri(mArchive.getName(),
                mItemNames.get(position));
        if (mTagsDb != null) {
            result.tags = mTagsDb.getTags(imageUri);
        }
        String shortName = mItemNames.get(position).substring(
                mItemNames.get(position).lastIndexOf('/') + 1);
        if (mCaptions != null) {
            result.caption = mCaptions.getString(shortName);
        }
        result.uri = imageUri;
        result.name = result.uri.toString();
        return result;
    }

    @Override
    public int size() {
        if (mArchive != null) {
            return mItemNames.size();
        }
        return 0;
    }

    @Override
    public Uri getAlbumUri() {
        return Uri.parse(mArchive.getName());
    }

    @Override
    public void setFilters(TagFilter[] array) {
        // TODO Auto-generated method stub

    }
}
