package com.kg.emailalbum.mobile.viewer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.zip.ZipFile;

import org.acra.ErrorReporter;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.kg.emailalbum.mobile.util.BitmapLoader;
import com.kg.emailalbum.mobile.util.ZipUtil;

public class ArchiveSlideshowList extends AbstractList<SlideshowItem> implements
        SlideshowList {

    private static final String LOG_TAG = ArchiveSlideshowList.class
            .getSimpleName();
    private ZipFile mArchive;
    private Bundle mCaptions;
    private Context mContext;
    private ArrayList<String> mItemNames;
    private int mTargetSize = 900;

    public ArchiveSlideshowList(Context context, String archiveName, int targetSize) {
        try {
            File albumFile = new File(new URI(archiveName.replace(" ", "%20")));
            mArchive = new ZipFile(albumFile);
            mItemNames = ZipUtil.getPicturesFilesList(mArchive);
            mCaptions = ZipUtil.loadCaptions(mArchive);
            mContext = context;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error while opening archive file.", e);
            ErrorReporter.getInstance().handleException(e);
        }
    }
    
    @Override
    public SlideshowItem get(int position) {
        SlideshowItem result = new SlideshowItem();
        Log.d(this.getClass().getName(), "Load image "
                + mItemNames.get(position));
        try {
            result.bitmap = BitmapLoader.load(mContext, mArchive, mArchive
                    .getEntry(mItemNames.get(position)), mTargetSize, mTargetSize);
            String shortName = mItemNames.get(position).substring(
                    mItemNames.get(position).lastIndexOf('/') + 1);
            result.caption = mCaptions.getString(shortName);
            result.name = mItemNames.get(position);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error : ", e);
            ErrorReporter.getInstance().handleSilentException(e);
        }
        return result;
    }

    /**
     * @return the mArchive
     */
    public String getArchiveName() {
        return mArchive.getName();
    }

    @Override
    public int size() {
        if (mArchive != null) {
            return mItemNames.size();
        }
        return 0;
    }

    @Override
    public InputStream getOriginalInputStream(int position) throws IOException {
        if (mArchive != null) {
            return ZipUtil.getInputStream(mArchive, mArchive
                    .getEntry(mItemNames.get(position)));
        }
        return null;
    }
}
