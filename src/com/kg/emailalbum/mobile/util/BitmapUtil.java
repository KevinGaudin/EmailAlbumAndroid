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
package com.kg.emailalbum.mobile.util;

import java.io.File;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.MediaStore.Images;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

/**
 * Provides common tools for manipulating Bitmap objects.
 * 
 * @author Normal
 * 
 */
public class BitmapUtil {
    private static DisplayMetrics mMetrics = null;
    private static final String LOG_TAG = BitmapUtil.class.getSimpleName();
    private static Uri sStorageURI = Images.Media.EXTERNAL_CONTENT_URI;

    /**
     * Rotate a bitmap.
     * 
     * @param bmp
     *            A Bitmap of the picture.
     * @param degrees
     *            Angle of the rotation, in degrees.
     * @return The rotated bitmap, constrained in the source bitmap dimensions.
     */
    public static Bitmap rotate(Bitmap bmp, float degrees) {
        if (degrees % 360 != 0) {
            Log.d(LOG_TAG, "Rotating bitmap " + degrees + "°");
            Matrix rotMat = new Matrix();
            rotMat.postRotate(degrees);

            if (bmp != null) {
                Bitmap dst = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp
                        .getHeight(), rotMat, false);

                return dst;
            }
        } else {
            return bmp;
        }
        return null;
    }

    /**
     * Store a picture that has just been saved to disk in the MediaStore.
     * 
     * @param imageFile
     *            The File of the picture
     * @return The Uri provided by the MediaStore.
     */
    public static Uri storePicture(Context ctx, File imageFile, String imageName) {
        ContentResolver cr = ctx.getContentResolver();
        imageName = imageName.substring(imageName.lastIndexOf('/') + 1);
        ContentValues values = new ContentValues(7);
        values.put(Images.Media.TITLE, imageName);
        values.put(Images.Media.DISPLAY_NAME, imageName);
        values.put(Images.Media.DESCRIPTION, "");
        values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(Images.Media.MIME_TYPE, "image/jpeg");
        values.put(Images.Media.ORIENTATION, 0);
        File parentFile = imageFile.getParentFile();
        String path = parentFile.toString().toLowerCase();
        String name = parentFile.getName().toLowerCase();
        values.put(Images.ImageColumns.BUCKET_ID, path.hashCode());
        values.put(Images.ImageColumns.BUCKET_DISPLAY_NAME, name);
        values.put("_data", imageFile.toString());

        Uri uri = cr.insert(sStorageURI, values);

        return uri;
    }

    public static Uri getContentUriFromFile(Context ctx, File imageFile) {
        Uri uri = null;
        ContentResolver cr = ctx.getContentResolver();
        // Columns to return
        String[] projection = { Images.Media._ID, Images.Media.DATA };
        // Look for a picture which matches with the requested path
        // (MediaStore stores the path in column Images.Media.DATA) 
        String selection = Images.Media.DATA + " = ?";
        String[] selArgs = { imageFile.toString() };

        Cursor cursor = cr.query(sStorageURI, projection, selection, selArgs,
                null);

        if (cursor.moveToFirst()) {

            String id;
            int idColumn = cursor.getColumnIndex(Images.Media._ID);
            id = cursor.getString(idColumn);

            uri = Uri.withAppendedPath(sStorageURI, id);
        }
        cursor.close();
        if (uri != null) {
            Log.d(LOG_TAG, "Found picture in MediaStore : "
                    + imageFile.toString() + " is " + uri.toString());
        } else {
            Log.d(LOG_TAG, "Did not find picture in MediaStore : "
                    + imageFile.toString());
        }
        return uri;
    }

    /**
     * @param ctx
     */
    public static float getDensity(Context ctx) {
        if (mMetrics == null) {
            mMetrics = new DisplayMetrics();
            ((WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay().getMetrics(mMetrics);
        }
        return mMetrics.density;
    }
}
