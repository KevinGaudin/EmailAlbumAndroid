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

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory.Options;
import android.net.Uri;
import android.view.Display;
import android.view.WindowManager;

/**
 * Helper class for enhanced picture loading with downscaling within a specified
 * dimension. Picture loading is split in 2 passes :
 * <ul>
 * <li>1st pass don't load the picture, just find it's real width and height and
 * calculate the optimal size and downsampling for next pass.</li>
 * <li>2nd pass read the picture with the best downsampling option to load only
 * the strictly necessary pixels.</li>
 * </ul>
 * 
 * @author Kevin Gaudin
 * 
 */
public class BitmapLoader {

    /**
     * The results from the first pass : final dimension of the picture fitting
     * the requested dimension, preserving the picture aspect ratio.
     * 
     * @author Normal
     * 
     */
    private class FirstPassResult {
        /** The picture width calculated to fit in the requested dimensions. */
        public int finalHeight = 0;
        /** The picture height calculated to fit in the requested dimensions. */
        public int finalWidth = 0;
        /**
         * Options containing the best downsampling factor to avoid loading the
         * full picture.
         */
        public Options options = new Options();

        @Override
        public String toString() {
            return "{finalWidth=" + finalWidth + ", finalHeight=" + finalHeight
                    + ", options={inSampleSize=" + options.inSampleSize + "}}";
        }
    }

    /**
     * A cache for storing the latest accessed bitmaps. If another call asks for
     * a larger resolution, we reload it and keep the latest.
     */
    private static CacheMap<Uri, Bitmap> bmpCache = new CacheMap<Uri, Bitmap>(
            20, 3);
    /**
     * A cache for storing real dimensions of all accessed bitmaps. With this
     * the cost of the first pass is reduced when loading a previously accessed
     * picture.
     */
    private static ConcurrentHashMap<Uri, int[]> dimensionCache = new ConcurrentHashMap<Uri, int[]>();

    /**
     * Singleton... just for allowing the instanciation of inner classes...
     */
    private static BitmapLoader instance = new BitmapLoader();

    private static final String LOG_TAG = BitmapLoader.class.getSimpleName();

    /**
     * First pass: read the picture real size and calculate what will be the
     * final size and downsampling option considering the dimensions asked by
     * the caller.
     * 
     * @param context
     *            The application context.
     * @param width
     *            The maximum width of the final picture.
     * @param height
     *            The maximum height of the final picture.
     * @param input
     *            The input stream of the original picture.
     * @param cachedDimension
     *            If the BitmapLoader has already
     * @return
     * @throws IOException
     */
    private static FirstPassResult firstPass(Context context, Integer width,
            Integer height, InputStream input, int[] cachedDimension)
            throws IOException {
        FirstPassResult fpResult = instance.new FirstPassResult();

        // First, get image size
        fpResult.options.inJustDecodeBounds = true;
        if (cachedDimension != null) {
            fpResult.options.outWidth = cachedDimension[0];
            fpResult.options.outHeight = cachedDimension[1];
            // Log.d(LOG_TAG, "Cached size : " + cachedDimension[0] + " x "
            // + cachedDimension[1]);
        } else if (input != null) {
            // Log.d(LOG_TAG, "Fetching size...");
            BitmapFactory.decodeStream(input, null, fpResult.options);
            // Log.d(LOG_TAG, "... size fetched.");
            input.close();
        }
        int srcWidth = fpResult.options.outWidth;
        int srcHeight = fpResult.options.outHeight;
        // Log.d(LOG_TAG, "Source picture has dimension " + srcWidth + " x "
        // + srcHeight);

        float srcImageFactor = (float) srcWidth / (float) srcHeight;

        // If no resolution given, use the device screen resolution
        if (width == null && height == null) {
            Display display = ((WindowManager) context
                    .getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay();
            fpResult.finalWidth = display.getWidth();
            fpResult.finalHeight = display.getHeight();
            // Log.d(LOG_TAG, "Display is : " + fpResult.finalWidth + " x "
            // + fpResult.finalHeight);
        } else if (width == null) {
            // If only one dimension is given, keep source proportions
            fpResult.finalWidth = (int) (height * srcImageFactor);
            fpResult.finalHeight = height;
        } else if (height == null) {
            // If only one dimension is given, keep source proportions
            fpResult.finalHeight = (int) (width / srcImageFactor);
            fpResult.finalWidth = width;
        } else {
            fpResult.finalWidth = width;
            fpResult.finalHeight = height;
        }

        float requestedImageFactor = (float) fpResult.finalWidth
                / (float) fpResult.finalHeight;

        // Switch requested orientation to allow best quality without
        // reloading if device orientation changes
        if ((srcImageFactor > 1 && requestedImageFactor < 1)
                || (srcImageFactor < 1 && requestedImageFactor > 1)) {
            int oldValue = fpResult.finalWidth;
            fpResult.finalWidth = fpResult.finalHeight;
            fpResult.finalHeight = oldValue;
            requestedImageFactor = 1 / requestedImageFactor;
        }

        // 2 final requested dimensions are now given, adjust for best fit
        // with aspect ratio preserved

        // Calculates which dimension should be used to preserve aspect
        // ratio
        if (requestedImageFactor <= srcImageFactor) {
            fpResult.finalHeight = (int) (fpResult.finalWidth / srcImageFactor);
        } else if (requestedImageFactor > srcImageFactor) {
            fpResult.finalWidth = (int) (fpResult.finalHeight * srcImageFactor);
        }

        // Calculate the sample size needed to load image with minimum
        // required memory consumption.
        // We eventually load a larger bitmap if orientation is different so
        // that if device orientation changes, we don't have to reload a
        // finer sampled bitmap
        if (srcWidth > fpResult.finalWidth) {
            fpResult.options.inSampleSize = srcWidth / fpResult.finalWidth;
        }
        return fpResult;
    }

    /**
     * Load a picture from the given Uri.
     * 
     * @param context
     *            The application context.
     * @param uri
     *            The Uri where the picture is located.
     * @param width
     *            The maximum width of the result bitmap. The original bitmap
     *            will be scaled down with aspect ratio preserved to fit both
     *            width/height. If null, the default value is the device screen
     *            size.
     * @param height
     *            The maximum height of the result bitmap. The original bitmap
     *            will be scaled down with aspect ratio preserved to fit both
     *            width/height. If null, the default value is the device screen
     *            size.
     * @return A Bitmap loaded from the Uri, scaled down to fit the given width
     *         and height.
     * @throws IOException
     */
    public static Bitmap load(Context context, Uri uri, Integer width,
            Integer height) throws IOException {
        return load(context, uri, width, height, Bitmap.Config.RGB_565, true);
    }

    /**
     * Load a picture from the given Uri.
     * 
     * @param context
     *            The application context.
     * @param uri
     *            The Uri where the picture is located.
     * @param width
     *            The maximum width of the result bitmap. The original bitmap
     *            will be scaled down with aspect ratio preserved to fit both
     *            width/height. If null, the default value is the device screen
     *            size.
     * @param height
     *            The maximum height of the result bitmap. The original bitmap
     *            will be scaled down with aspect ratio preserved to fit both
     *            width/height. If null, the default value is the device screen
     *            size.
     * @param colorConfig
     *            The desired result color configuration, as defined in
     *            {@link Config}. If null, will use {@link Config#RGB_565} which
     *            is enough for screen display but might reduce color depth of
     *            the original picture.
     * @param cacheResult
     *            Wether we should store the result bitmap in cache or not. If
     *            you know that the result will be a big Bitmap, you should set
     *            this parameter to false (do not cache) and take care of
     *            recycling the result Bitmap as soon as it is not necessary
     *            anymore.
     * @return A Bitmap loaded from the Uri, scaled down to fit the given width
     *         and height.
     * @throws IOException
     */
    public static Bitmap load(Context context, Uri uri, Integer width,
            Integer height, Bitmap.Config colorConfig, boolean cacheResult)
            throws IOException {
        // The resulting Bitmap.
        Bitmap result = null;

        if (colorConfig == null) {
            // Default color configuration. Is good enough for screen display
            // and reduce memory usage, but might decrease the color depth of
            // the original picture.
            colorConfig = Bitmap.Config.RGB_565;
        }

        InputStream input = context.getContentResolver().openInputStream(uri);

        if (input == null)
            return null;

        InputStream fpInput = null;
        int[] cachedDimension = null;
        if (!dimensionCache.containsKey(uri)) {
            fpInput = input;
        } else {
            // We already have the result of the first pass. We should not
            // preload anything more.
            cachedDimension = dimensionCache.get(uri);
            fpInput = null;
        }

        FirstPassResult fpResult = firstPass(context, width, height, fpInput,
                cachedDimension);
        int[] dimensionToCache = { fpResult.options.outWidth,
                fpResult.options.outHeight };
        // Store the dimension in cache so we don't have to get it again
        dimensionCache.put(uri, dimensionToCache);

        if (fpInput == null) {
            // We first pass input is null, so we can reuse the original input
            // stream as it has not been used for the first pass.
            fpInput = input;
        } else {
            // The original input stream has been consumed for the first pass.
            // Get a new one.
            fpInput = context.getContentResolver().openInputStream(uri);
        }

        Bitmap cachedBitmap = null;
        if (bmpCache.containsKey(uri)) {
            cachedBitmap = bmpCache.get(uri);
            // We have a Bitmap in cache, but we have to check if its resolution
            // is large enough.
            if (cachedBitmap.getWidth() < fpResult.finalWidth
                    || cachedBitmap.getHeight() < fpResult.finalHeight) {
                // invalidate the existing entry
                bmpCache.remove(uri);
                cachedBitmap = null;
            }

        }
        result = secondPass(context, fpInput, fpResult, colorConfig,
                cachedBitmap);

        // Store the result in cache
        if (cacheResult && result != null && !bmpCache.containsKey(uri)) {
            bmpCache.put(uri, result);
        }

        return result;
    }

    /**
     * Load a picture from the given Zip Archive. TODO: get rid of the temporary
     * file as we don't need it anymore since the Uri is provided instead of an
     * InputStream. TODO: add caching of dimensions and bitmaps.
     * 
     * @param context
     *            The application context.
     * @param archive
     *            The archive where the picture is located.
     * @param entry
     *            The picture entry in this archive.
     * @param width
     *            The maximum width of the result bitmap. The original bitmap
     *            will be scaled down with aspect ratio preserved to fit both
     *            width/height. If null, the default value is the device screen
     *            size.
     * @param height
     *            The maximum height of the result bitmap. The original bitmap
     *            will be scaled down with aspect ratio preserved to fit both
     *            width/height. If null, the default value is the device screen
     *            size.
     * @return
     * @throws IOException
     */
    public static Bitmap load(Context context, ZipFile archive, ZipEntry entry,
            Integer width, Integer height) throws IOException {
        InputStream input = ZipUtil.getInputStream(archive, entry);

        Bitmap result = null;
        if (input == null)
            return null;

        FirstPassResult fpResult = firstPass(context, width, height, input,
                null);

        // Reload the input stream for the second pass
        input.close();
        input = ZipUtil.getInputStream(archive, entry);

        result = secondPass(context, input, fpResult, Bitmap.Config.RGB_565,
                null);
        return result;
    }

    /**
     * Let's free some heavy bitmaps...
     */
    public static void onLowMemory() {
        bmpCache.clear();
    }

    /**
     * Create the new Bitmap fitting in the requested size.
     * 
     * @param context
     *            The application context.
     * @param input
     *            An InputStream providing the picture data.
     * @param fpResult
     *            The calculations obtained in the first pass (final dimension
     *            and optimized sample size).
     * @param colorConfig
     *            The color {@link Config} we should use to deliver the final
     *            Bitmap.
     * @param cachedBitmap
     *            If the picture has been previously loaded and if we can reuse
     *            the previous result, the cached Bitmap should be provided
     *            here.
     * @return The final Bitmap.
     * @throws IOException
     */
    private static Bitmap secondPass(Context context, InputStream input,
            FirstPassResult fpResult, Bitmap.Config colorConfig,
            Bitmap cachedBitmap) throws IOException {
        Bitmap result = null;
        if (input != null) {

            fpResult.options.inJustDecodeBounds = false;
            fpResult.options.inPreferredConfig = colorConfig;
            if (!colorConfig.equals(Config.ARGB_8888)) {
                // Other Configs reduce the color depth. Enforcing dithering
                // could help get nicer pictures.
                fpResult.options.inDither = true;
            }
            // Log.d(LOG_TAG, "fpResult =" + fpResult);

            Bitmap source = cachedBitmap;
            if (source == null) {
                // Log.d(LOG_TAG,
                // "No cached bitmap to use, loading from stream");
                // Log.d(LOG_TAG, "Decoding picture..." + fpResult);
                source = BitmapFactory.decodeStream(input, null,
                        fpResult.options);
                // Log.d(LOG_TAG, "Picture decoded.");
                input.close();
            }

            if (source != null) {
                // Log.d(LOG_TAG, "Loaded picture with dimension "
                // + source.getWidth() + " x " + source.getHeight());

                if (fpResult.finalWidth != source.getWidth()
                        || fpResult.finalHeight != source.getHeight()) {
                    // Resize the picture to the caller specs.
                    result = Bitmap.createScaledBitmap(source,
                            fpResult.finalWidth, fpResult.finalHeight, true);
                } else {
                    result = source;
                }
                    
                // Log.d(LOG_TAG, "Resized picture to dimension "
                // + fpResult.finalWidth + " x " + fpResult.finalHeight);
            } else {
                result = null;
            }
        }
        return result;
    }
}
