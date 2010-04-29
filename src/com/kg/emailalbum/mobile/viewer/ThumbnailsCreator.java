/**
 * Copyright 2009, 2010 Kevin Gaudin
 *
 * This file is part of EmailAlbum.
 *
 * EmailAlbum is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * EmailAlbum is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with EmailAlbum.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.kg.emailalbum.mobile.viewer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.kg.emailalbum.mobile.util.BitmapLoader;
import com.kg.emailalbum.mobile.util.CacheManager;

/**
 * Creates Thumbnails from pictures contained in a zip archive.
 * 
 * @author Kevin Gaudin
 * 
 */
public class ThumbnailsCreator extends Thread {
    private int mThumbWidth = 80;
    private static final String THUMBS_PREFIX = "thm_";
    private ArrayList<String> mPictures;
    private HashMap<String, String> mThumbnails = new HashMap<String, String>();
    private Handler mHandler;
    private ZipFile mArchive;
    private Context mContext;
    private boolean mClearThumbnails;
    private boolean mContinueCreation = true;
    private File mCacheDir;

    /**
     * Instanciate the thumbnails creation process.
     * 
     * @param c
     *            Application context.
     * @param archive
     *            The archive file containing the pictures.
     * @param pictures
     *            The list of pictures entry names in the archive.
     * @param handler
     *            A UI handler for receving the created thumbnails.
     * @param clearThumbnails
     *            True to clear all existing thumbnails before starting.
     */
    public ThumbnailsCreator(Context c, ZipFile archive,
            ArrayList<String> pictures, Handler handler, boolean clearThumbnails) {
        mContext = c;
        mArchive = archive;
        mPictures = pictures;
        mHandler = handler;
        mClearThumbnails = clearThumbnails;
        Display display = ((WindowManager) mContext
                .getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int screenWidth = display.getWidth();
        int screenHeight = display.getHeight();
        mThumbWidth = Math.min(screenWidth, screenHeight) / 3;
        mCacheDir = new CacheManager(mContext).getCacheDir("viewer");
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {

        if (mArchive == null) {
            Log.e(this.getClass().getSimpleName(), "Archive is null");
            sendError(new FileNotFoundException("Archive not found."));
        } else {
            try {
                ZipEntry entry = null;
                String entryName = null;
                if (mClearThumbnails) {
                    clearFiles();
                }
                Iterator<String> iPictures = mPictures.iterator();
                int pos = 0;

                List<String> files = Arrays.asList(mCacheDir.list());
                Bitmap thumb = null;
                OutputStream thumbOS = null;
                File thumbFile = null;
                while (mContinueCreation && iPictures.hasNext()) {
                    entryName = iPictures.next();
                    entry = mArchive.getEntry(entryName);
                    // Build the thumbnail file name
                    String thumbName = THUMBS_PREFIX
                            + entryName
                                    .substring(entryName.lastIndexOf('/') + 1);
                    thumbFile = new File(mCacheDir, thumbName);
                    // Create the file only if it doesn't already exist
                    if (!files.contains(thumbName)) {
                        thumb = BitmapLoader.load(mContext, mArchive, entry,
                                mThumbWidth, null);

                        if (thumb != null) {
                            thumbOS = new FileOutputStream(thumbFile);

                            thumb.compress(CompressFormat.JPEG, 75, thumbOS);

                            thumbOS.flush();
                            thumbOS.close();
                            thumb.recycle();
                        } else {
                            sendError(new IOException(
                                    "This archive is corrupt or contains bad character encoding."));
                            stopCreation();
                        }
                    }

                    // Provide the UI with the filename of the generated
                    // thumbnail
                    mThumbnails.put(entryName, thumbFile.getAbsolutePath());
                    sendThumbnail(pos);
                    pos++;
                }
            } catch (Exception e) {
                Log.e(this.getClass().getSimpleName(),
                        "Error while creating thumbnail.", e);
                sendError(e);
            } catch (OutOfMemoryError e) {
                Log.e(this.getClass().getSimpleName(), "mem error", e);
                sendError(e);
            }
        }
    }

    /**
     * Clear generated thumbnails.
     */
    private void clearFiles() {
        for (File file : mCacheDir.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".jpg") || filename.endsWith(".JPG");
            }
            
        })) {
            file.delete();
        }
    }

    /**
     * Send the thumbnail generated at a specific position to the UI handler.
     * 
     * @param pos
     *            The position of the generated thumbnail to send.
     */
    private void sendThumbnail(int pos) {
        Message msg = new Message();
        Bundle data = new Bundle();
        msg.arg1 = 0;
        data.putInt(EmailAlbumViewer.KEY_THMBCREAT_ENTRY_POSITION, pos);
        data.putString(EmailAlbumViewer.KEY_THMBCREAT_THUMB_NAME, mThumbnails
                .get(mPictures.get(pos)));
        msg.setData(data);
        mHandler.sendMessage(msg);
    }

    /**
     * Send an error to the UI handler.
     * 
     * @param e
     *            The Throwable describing the error that occurred.
     */
    private void sendError(Throwable e) {
        Message msg = new Message();
        Bundle data = new Bundle();
        msg.arg1 = -1;
        data.putString("EXCEPTION", e.getMessage());
        data.putString("EXCEPTION_CLASS", e.getClass().getSimpleName());
        msg.setData(data);
        mHandler.sendMessage(msg);
    }

    /**
     * Stops the thumbnails creation process.
     */
    public void stopCreation() {
        mContinueCreation = false;
    }

}
