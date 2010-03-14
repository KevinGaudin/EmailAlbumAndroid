package com.kg.emailalbum.mobile.creator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.Images.Media;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import com.kg.emailalbum.mobile.util.BitmapLoader;
import com.kg.emailalbum.mobile.util.CacheManager;

/**
 * Helper Thread to retrieve the content of MediaStore pictures buckets.
 * 
 * @author Kevin Gaudin
 */
public class ItemsLoader extends Thread {

    /**
     * Cache time to leave - for the moment only used to invalidate existing
     * thumbnails without deleting them
     */
    private static final long CACHE_TTL = 7 * 24 * 60 * 60 * 1000;
    /**
     * The name of a lock file used to prevent from running 2 instances of this
     * thread
     */
    private static final String LOCK_FILE = ItemsLoader.class.getSimpleName()
            + ".lock";
    private static final String LOG_TAG = ItemsLoader.class.getSimpleName();

    /** Max width and height of thumbnails (dip) */
    private static final int THUMBNAIL_WIDTH_HEIGHT_DIP = 90;

    /** Quality for compressed thumbnail */
    private static final int THUMBNAILS_QUALITY = 70;

    /**
     * This static method allows to create and store a thumbnail.
     * 
     * @param context
     *            Any context, might be the application context.
     * @param imageUri
     *            The Uri of the picture.
     * @return The Bitmap containing a scaled-down version of the requested
     *         picture.
     */
    public static Bitmap getThumbnail(Context context, Uri imageUri) {
        Bitmap bmp = null;
        if (context != null) {
            File storageDir = new CacheManager(context).getCacheDir("creator");

            try {
                // Thumbnails files names is made of the final part of the Uri
                // (the rowid for MediaScanner Uris. This could be a problem for
                // Uris which are not content:// Uris.
                File tmpFile = new File(storageDir, imageUri
                        .getLastPathSegment()
                        + ".jpg");
                if (tmpFile.exists()
                        && System.currentTimeMillis() - tmpFile.lastModified() < CACHE_TTL) {
                    // a thumbnail has already been generated and is not too old
                    // let's use it
                    bmp = BitmapFactory.decodeFile(tmpFile.getPath());
                } else {
                    DisplayMetrics metrics = new DisplayMetrics();
                    ((WindowManager) context
                            .getSystemService(Context.WINDOW_SERVICE))
                            .getDefaultDisplay().getMetrics(metrics);
                    // to allow thumbnails size to be adapted to the screen
                    // density, we calculate it's dip size in pixels
                    // TODO: add the dip to pixels calculation to the
                    // BitmapLoader
                    int size = (int) (THUMBNAIL_WIDTH_HEIGHT_DIP * metrics.density);
                    bmp = BitmapLoader.load(context, imageUri, size, size);
                    if (bmp != null) {
                        // we were able to load the image, let's store the
                        // thumbnail
                        FileOutputStream out = new FileOutputStream(tmpFile);
                        bmp.compress(CompressFormat.JPEG, THUMBNAILS_QUALITY,
                                out);
                        out.close();
                    }
                }
            } catch (FileNotFoundException e) {
                Log.e(LOG_TAG, "Error : ", e);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error : ", e);
            }
        } else {
            Log.e(LOG_TAG, "Context is null !");
        }
        return bmp;
    }

    /** A boolean for the running state of the process */
    public boolean isRunning = false;

    /** the bucket we have to look into */
    private String mBucketName;

    /** the application context */
    private Context mContext;

    /**
     * A UI handler to send results. TODO: check that we don't have leaks when
     * changing orientation
     */
    private Handler mHandler;

    /**
     * Builds a new ItemsLoader, caller has to provide a Context (can be the
     * application context) and a UI Handler to handle results.
     * 
     * @param context
     *            Can be the application context
     * @param handler
     *            A handler to receive results.
     * @param bucketName
     *            The name of the bucket to fetch.
     */
    public ItemsLoader(Context context, Handler handler, String bucketName) {
        mContext = context;
        mHandler = handler;
        mBucketName = bucketName;
    }

    private void createLock() {
        File lock = new File(mContext.getFilesDir(), LOCK_FILE);
        OutputStream os;
        try {
            os = new FileOutputStream(lock);
            os.write(1);
            os.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            Log.e(LOG_TAG, "Error : ", e);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            Log.e(LOG_TAG, "Error : ", e);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Thread#destroy()
     */
    @Override
    public void destroy() {
        // TODO Auto-generated method stub
        super.destroy();
        removeLock();
    }

    private boolean isLocked() {
        File lock = new File(mContext.getFilesDir(), LOCK_FILE);
        return lock.exists();
    }

    /**
     * Queries the MediaStore to retrieve the list of pictures contained in a
     * bucket.
     * 
     * @param withBitmap
     *            If true, the process also creates each Thumbnail and sends
     *            them to the UI. This is not used anymore.
     * 
     */
    private void loadAllItems(boolean withBitmap) {
        String[] projection = { ImageColumns.BUCKET_DISPLAY_NAME,
                ImageColumns.DATE_TAKEN, ImageColumns.TITLE,
                ImageColumns.MINI_THUMB_MAGIC, ImageColumns._ID,
                ImageColumns.DATA, ImageColumns.BUCKET_ID };

        String selection = ImageColumns.BUCKET_DISPLAY_NAME + " = "
            + DatabaseUtils.sqlEscapeString(mBucketName);

        Cursor cursor = mContext.getContentResolver().query(
                Media.EXTERNAL_CONTENT_URI, projection, selection, null,
                MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC");
        cursor.moveToFirst();

        // Iterate over all images
        while (isRunning && !cursor.isAfterLast()) {
            Uri imageUri = Uri.withAppendedPath(Media.EXTERNAL_CONTENT_URI,
                    cursor.getString(cursor
                            .getColumnIndexOrThrow(ImageColumns._ID)));
            Message msg = new Message();
            Bundle data = new Bundle();

            data.putString("IMAGE_URI", imageUri.toString());

            Bitmap thumb = null;

            // If the image has to be sent during list initialisation, send it
            // too.
            if (withBitmap) {
                thumb = getThumbnail(mContext, imageUri);
                if (thumb != null) {
                    data.putParcelable("THUMB", thumb);
                }
            }

            msg.setData(data);
            mHandler.handleMessage(msg);
            cursor.moveToNext();
        }
    }

    public void removeLock() {
        File lock = new File(mContext.getFilesDir(), LOCK_FILE);
        if (lock.exists()) {
            lock.delete();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {

        if (!isRunning && !isLocked()) {
            // The lock mechanism + isRunning boolean were useful when the
            // bitmaps loading was done
            // at the same time as the items listing.
            // TODO : remove these and code a better singleton implementation
            createLock();
            isRunning = true;

            loadAllItems(false);

            isRunning = false;
            removeLock();
        } else {
            // throw new IllegalStateException("There is already a " +
            // this.getClass().getName() + " job running !");
        }

    }

    public void stopJob() {
        isRunning = false;
    }

}
