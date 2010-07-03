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
package com.kg.emailalbum.mobile.creator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.kg.emailalbum.mobile.R;
import com.kg.emailalbum.mobile.util.BitmapLoader;
import com.kg.emailalbum.mobile.util.CacheMap;

/**
 * Allows the user to select multiple pictures from the Media Library. This
 * Activity can only display pictures which have already been scanned by the
 * device, be it the android Gallery application (before 2.0) or the Media
 * Scanner (after 2.0).
 * 
 * The selection of pictures is returned to the caller activity as an Array of
 * {@link Uri} objects. You will have to cast them from {@link Parcelable} to
 * {@link Uri}.
 * 
 * @author Kevin Gaudin
 */
public class SelectPictures extends Activity {

    /**
     * A specific adapter for this list based activity.
     */
    public static class MultiSelectImageAdapter extends BaseAdapter {
        /**
         * Asynchronous process for retrieving thumbnails from Uris.
         */
        private class ThumbnailGetter extends AsyncTask<Uri, Integer, Uri> {
            private final String LOG_TAG = ThumbnailGetter.class
                    .getSimpleName();

            /** The Uri beeing processed */
            private Uri mUri = null;

            /*
             * (non-Javadoc)
             * 
             * @see android.os.AsyncTask#doInBackground(Params[])
             */
            @Override
            protected Uri doInBackground(Uri... uris) {
                // We process Uris one after another... so the Array contains
                // only one Uri.
                mUri = uris[0];
                // Let the ThumbnailLoader do the job.
                Uri result = ItemsLoader.getThumbnail(mContext, mUri);
                return result;
            }

            /*
             * (non-Javadoc)
             * 
             * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
             */
            @Override
            protected void onPostExecute(Uri result) {
                super.onPostExecute(result);
                // Give the retrieved thumbnail to the adapter...
                mImageAdapter.updateCache(mUri, result);
                // then process any other pending thumbnail request.
                if (!mPendingThumbnailRequests.isEmpty()) {
                    mThmGetter = new ThumbnailGetter();
                    mThmGetter.execute(mPendingThumbnailRequests.poll());

                }

            }
        }

        /**
         * Keeps references to UI elements to avoid looking for them. This
         * should be attached to a grid item with View.setTag().
         */
        private class ViewHolder {
            ImageView checkableImage = null;
            CheckBox imageCheck = null;
        }

        /** Number of Thumbnails to wipe from cache when cache is full */
        private static final int THUMBS_CACHE_REVOC_SIZE = 6;

        /** Number of Thumbnails to keep in cache */
        private static final int THUMBS_CACHE_SIZE = 32;;

        private final String LOG_TAG = MultiSelectImageAdapter.class
                .getSimpleName();

        /** This adapter needs a reference to a context. */
        private Context mContext = null;

        /**
         * This is a reference to a UI handler, necessary to manage clicks on
         * grid items sub-views This handler has to be carefully released when
         * the activity is destroyed !
         * */
        private Handler mHandler = null;

        /** The main list of images form the current Media Library bucket */
        private List<Uri> mImagesUris = new ArrayList<Uri>(100);

        /**
         * One generic Listener instance for every checbox that will be added to
         * the grid.
         */
        private OnCheckedChangeListener mOnImageCheckedListener = new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                // We stored the image Uri in the CheckBox tag
                Uri imageUri = (Uri) buttonView.getTag();
                setItemSelected(imageUri, isChecked);
            }

        };

        /** Contains all the pending requests for thumbnails. */
        private LinkedList<Uri> mPendingThumbnailRequests = new LinkedList<Uri>();
        /** Stores Uris selected by the user */
        private Set<Uri> mSelectedUris = new LinkedHashSet<Uri>(20);

        private ThumbnailGetter mThmGetter = null;

        /**
         * One generic Listener instance for every thumbnail that will be added
         * to the grid.
         */
        private OnClickListener mThumbnailClickListener = new OnClickListener() {

            @Override
            public void onClick(View v) {
                // A click on the thumbnail allows the user to display a
                // larger view of the image
                mSelectedImage = (Uri) v.getTag();
                mHandler.sendEmptyMessage(MSG_DISPLAY_IMAGE_PREVIEW);
            }

        };

        /**
         * We store Bitmaps of the Thumbnails in this cache.
         * 
         * TODO: check that this is still necessary now that
         * {@link BitmapLoader} manages its own cache...
         */
        private CacheMap<Uri, Uri> mThumbsUris = new CacheMap<Uri, Uri>(
                THUMBS_CACHE_SIZE, THUMBS_CACHE_REVOC_SIZE);

        /**
         * Create a new adapter with the given context and UI handler. The owner
         * of the handler should take care of asking the adapter to release it
         * when being destroyed.
         * 
         * @param context
         *            The context which will be used to load files and create UI
         *            components.
         * @param handler
         *            A handler from the UI thread. See
         *            {@link #releaseHandler()} and {@link #setHandler(Handler)}
         *            to avoid leak references to a destroyed activity.
         */
        public MultiSelectImageAdapter(Context context, Handler handler) {
            mContext = context;
            mHandler = handler;
        }

        /**
         * Add an item to the list being viewed with its Uri and associated
         * thumbnail.
         * 
         * @param imageUri
         *            The Uri of the picture.
         * @param thumb
         *            A thumbnail representation of the picture.
         */
        protected void addItem(String imageUri, Uri thumbUri) {
            if (imageUri != null) {
                Uri uri = Uri.parse(imageUri);
                if (!mImagesUris.contains(uri)) {
                    // Store the Uri in the list
                    mImagesUris.add(uri);
                    if (thumbUri != null) {
                        // Store the thumbnail in cache
                        mThumbsUris.put(uri, thumbUri);
                    }
                }
            } else {
                Log.e(LOG_TAG, "Asked to add a null item... ignore it.");
            }
            notifyDataSetChanged();
        }

        /**
         * Cancel any pending thumbnail request.
         */
        public void clearPendingThumbnails() {
            mPendingThumbnailRequests.clear();
        }

        /**
         * Empty list data : image list and selected items.
         */
        public void empty() {
            mImagesUris.clear();
            mSelectedUris.clear();
            notifyDataSetChanged();
        }

        /**
         * Provides items selected by the user.
         * 
         * @return A Set containing the Uris of the images which have been
         *         selected by the user.
         */
        public Set<Uri> getCheckedItems() {
            return mSelectedUris;
        }

        /*
         * (non-Javadoc)
         * 
         * @see android.widget.Adapter#getCount()
         */
        @Override
        public int getCount() {
            return mImagesUris.size();
        }

        /*
         * (non-Javadoc)
         * 
         * @see android.widget.Adapter#getItem(int)
         */
        @Override
        public Object getItem(int position) {
            if(mImagesUris.size() > 0) {
                return mImagesUris.get(position);
            } else {
                return null;
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see android.widget.Adapter#getItemId(int)
         */
        @Override
        public long getItemId(int position) {
            if (mImagesUris.size() > 0) {
                return Long.parseLong(mImagesUris.get(position)
                        .getLastPathSegment());
            } else {
                return 0;
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see android.widget.Adapter#getView(int, android.view.View,
         * android.view.ViewGroup)
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder vh = null;
            View resultView;
            if (convertView == null) {
                // No view to recycle, create a new one
                resultView = ((LayoutInflater) mContext
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                        .inflate(R.layout.selectable_image, null);

                // Initialize the ViewHolder with this view's references
                // to frequently used UI components
                vh = new ViewHolder();

                vh.checkableImage = (ImageView) resultView
                        .findViewById(R.id.CheckableImage);
                vh.checkableImage.setOnClickListener(mThumbnailClickListener);

                vh.imageCheck = (CheckBox) resultView
                        .findViewById(R.id.ImageCheck);

                vh.imageCheck
                        .setOnCheckedChangeListener(mOnImageCheckedListener);
                resultView.setTag(vh);
            } else {
                // We reuse a previously generated View
                resultView = convertView;
                vh = (ViewHolder) resultView.getTag();
                // Get rid of a possible pending thumbnail request
                // This cancels any request for a thumbnail which is now
                // out of the visible Views.
                if (vh.checkableImage.getTag() != null) {
                    mPendingThumbnailRequests
                            .remove(vh.checkableImage.getTag());
                }
            }

            Uri imageUri = mImagesUris.get(position);
            // Add a way to identify the logical item to which sub-views are
            // linked. That way, we can handle a specific click on each sub-view
            // and not only a click on the whole item.
            vh.imageCheck.setTag(imageUri);
            vh.checkableImage.setTag(imageUri);

            // Try to retrieve the thumbnail from cache
            Uri thumbUri = mThumbsUris.get(imageUri);
            if (thumbUri != null) {
                try {
                    Bitmap thumb = BitmapLoader.load(mContext, thumbUri, null,
                            null);
                    vh.checkableImage.setImageBitmap(thumb);
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error : ", e);
                }
            } else {
                // If not in cache, add this image uri to the list of
                // thumbnails to retrieve and display a replacement picture
                vh.checkableImage.setImageResource(R.drawable.robot);
                if (!mPendingThumbnailRequests.contains(imageUri)) {
                    mPendingThumbnailRequests.offer(imageUri);
                    if (mThmGetter == null
                            || mThmGetter.getStatus() == AsyncTask.Status.FINISHED) {
                        // If the previous instance of the thumbnail getter has
                        // finished, start a new one.
                        mHandler.sendEmptyMessage(MSG_SHOW_INDETERMINATE_PROGRESS);
                        mThmGetter = new ThumbnailGetter();
                        mThmGetter.execute(mPendingThumbnailRequests.poll());
                    }
                }
            }

            // Retrieve the user selection state
            vh.imageCheck.setChecked(isItemSelected(position));

            return resultView;
        }

        /**
         * Allows to know if an item has been selected by the user.
         * 
         * @param position
         *            The position of the item in the list.
         * @return true if the item has been selected by the user. false
         *         otherwise.
         */
        public boolean isItemSelected(int position) {
            Uri itemUri = mImagesUris.get(position);
            return mSelectedUris.contains(itemUri);
        }

        /**
         * Release the reference to the UI handler. A call to this method is
         * mandatory when the Activity owning the handler is being destroyed (on
         * orientation change for example).
         */
        public void releaseHandler() {
            mHandler = null;
        }

        /**
         * This should be called when the Activity is being restarted after
         * orientation change.
         * 
         * @param handler
         *            The new UI handler.
         */
        public void setHandler(Handler handler) {
            mHandler = handler;
        }

        /**
         * Store the selection state of an item.
         * 
         * @param itemUri
         *            The Uri of the item whose selection state has changed.
         * @param isSelected
         *            The new selection state of this item.
         */
        public void setItemSelected(Uri itemUri, boolean isSelected) {
            if (mSelectedUris.contains(itemUri)) {
                if (!isSelected) {
                    mSelectedUris.remove(itemUri);
                }
            } else if (isSelected) {
                mSelectedUris.add(itemUri);
            }
        }

        /**
         * Update the thumbnail for an item.
         * 
         * @param uri
         *            The Uri of the item.
         * @param thumbUri
         *            The Bitmap containing the thumbnail.
         */
        public void updateCache(Uri uri, Uri thumbUri) {
            if (uri != null && thumbUri != null) {
                mThumbsUris.put(uri, thumbUri);
                mPendingThumbnailRequests.remove(uri);
                notifyDataSetChanged();
            }
            if (mPendingThumbnailRequests.isEmpty()) {
                mHandler.sendEmptyMessage(MSG_HIDE_INDETERMINATE_PROGRESS);
            }
        }
    }

    /**
     * Object stored to keep state data when changing orientation.
     */
    private class StateHolder {
        MultiSelectImageAdapter adapter = null;
        int currentBucket = -1;
        Bitmap previewPic = null;
        Uri selectedImage = null;
    }

    // Dialogs Ids
    private static final int DIALOG_IMAGE_PREVIEW = 0;
    private static final int DIALOG_WAIT_PREVIEW = 1;

    private static final String LOG_TAG = SelectPictures.class.getSimpleName();

    /** Our list adapter */
    private static MultiSelectImageAdapter mImageAdapter;

    /**
     * A handler for managing list items population.
     */
    private static Handler mItemsHandler = new Handler() {

        /*
         * (non-Javadoc)
         * 
         * @see android.os.Handler#handleMessage(android.os.Message)
         */
        @Override
        public void handleMessage(final Message msg) {
            super.handleMessage(msg);
            post(new Runnable() {

                @Override
                public void run() {
                    // The ItemLoader is able to populate our list with both
                    // Uri and associated thumbnail. For UI performance reasons,
                    // thumbnails loading has been dissociated and deactivated
                    // in
                    // ItemLoader.
                    String strUri = msg.getData().getString("IMAGE_URI");
                    Uri thumbUri = msg.getData().getParcelable("THUMBURI");
                    mImageAdapter.addItem(strUri, thumbUri);
                }

            });
        }

    };

    /**
     * An instance of the ItemsLoader task. It is recreated on each bucket
     * change
     */
    private static ItemsLoader mItemsLoader;

    /** The Uri of the item on which the user clicked to get a preview */
    private static Uri mSelectedImage;

    /** Id given to the handler to identify the action to be done */
    private static final int MSG_DISPLAY_IMAGE_PREVIEW = 0;
    private static final int MSG_SHOW_INDETERMINATE_PROGRESS = 1;
    private static final int MSG_HIDE_INDETERMINATE_PROGRESS = 2;

    /**
     * Key for the result Array of this Activity when returning to caller
     * Activity
     */
    public static final String RESULT_URIS = "RESULT_URIS";

    /**
     * Used to indicate that even if the bucket selector state changed, we
     * actually did not change the bucket so we don't need to reload items.
     * */
    private boolean keepOldContent = false;

    /** The list of pictures buckets retrieved from the system. */
    private Set<String> mBuckets = new TreeSet<String>();

    /** The gridview used in this activity */
    private GridView mGrid;

    /** Used to store the bitmap loaded asynchronously for item preview */
    protected Bitmap mPreviewPic;

    /**
     * The spinner allowing the user to chose the bucket from which to pick
     * pictures.
     */
    private Spinner mSpinBuckets;

    /**
     * Populates mSpinBuckets with the list of buckets retrieved from the
     * MediaStore.
     * 
     * @param currentBucket
     *            When changing orientation, this parameter allows to provide
     *            the bucket that the user selected before the orientation
     *            change.
     */
    private void initBuckets(int currentBucket) {
        // Query the MediaStore for a list of pictures buckets.
        ContentResolver cr = getContentResolver();
        String[] projection = { MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME };
        Cursor cursor = MediaStore.Images.Media.query(cr,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection);

        if (cursor != null) {
            cursor.moveToFirst();
            while (!cursor.isLast() && !cursor.isAfterLast()) {
                // Store all the buckets labels in a Set
                cursor.moveToNext();
                mBuckets.add(cursor.getString(0));
            }
        }
        Log.d(getClass().getSimpleName(), "Buckets : " + mBuckets.toString());

        if (mBuckets.size() == 0) {
            Toast.makeText(getApplicationContext(), R.string.no_files_error,
                    Toast.LENGTH_LONG).show();
            this.finish();
        }
        // Prepare the adapter for the spinner
        String[] values = new String[mBuckets.size()];
        values = mBuckets.toArray(values);
        ArrayAdapter<String> bucketsAdapter = new ArrayAdapter<String>(
                getApplicationContext(), android.R.layout.simple_spinner_item,
                values);
        bucketsAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinBuckets.setAdapter(bucketsAdapter);

        // Handle spinner value change
        mSpinBuckets
                .setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(AdapterView<?> arg0, View arg1,
                            int pos, long arg3) {
                        if (!keepOldContent) {
                            // User changed images bucket
                            Log.d(getClass().getSimpleName(),
                                    "Selected item : "
                                            + mBuckets.toArray()[pos]);
                            mImageAdapter.empty();
                            if (mItemsLoader != null) {
                                mItemsLoader.stopJob();
                                mItemsLoader.removeLock();
                            }
                            mItemsLoader = new ItemsLoader(
                                    getApplicationContext(), mItemsHandler,
                                    (String) (mBuckets.toArray()[pos]));
                            mItemsLoader.start();
                        } else {
                            // User did not change bucket, this is the result
                            // of an orientation change. Set back the flag to
                            // false.
                            keepOldContent = false;
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {
                        // DO NOTHING
                    }

                });

        if (currentBucket >= 0) {
            // If an orientation changed occured, we return to the
            // previously selected bucket.
            mSpinBuckets.setSelection(currentBucket);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setTitle(R.string.btn_pick_pictures);
        setResult(RESULT_CANCELED);
        setContentView(R.layout.select_pictures);
        // Dither the background, to force dithering on pre 2.0 devices
        findViewById(R.id.select_pictures_root).getBackground().setDither(true);
        mSpinBuckets = (Spinner) findViewById(R.id.BucketSpinner);
        mSpinBuckets.getBackground().setDither(true);
        mGrid = (GridView) findViewById(R.id.gridview);

        // Handler for displaying dialogs on user click
        Handler dialogHandler = new Handler() {

            /*
             * (non-Javadoc)
             * 
             * @see android.os.Handler#handleMessage(android.os.Message)
             */
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                case MSG_DISPLAY_IMAGE_PREVIEW:
                    showPreviewDialog();
                    break;
                case MSG_SHOW_INDETERMINATE_PROGRESS:
                    setProgressBarIndeterminateVisibility(true);
                    break;
                case MSG_HIDE_INDETERMINATE_PROGRESS:
                    setProgressBarIndeterminateVisibility(false);
                    break;
                default:
                    break;
                }
            }

        };

        // Retrieve the state of an orientation change
        StateHolder state = (StateHolder) getLastNonConfigurationInstance();

        if (state != null) {
            // An orientation change occurred, restore previous state
            initBuckets(state.currentBucket);
            keepOldContent = true;
            mImageAdapter = state.adapter;
            mImageAdapter.setHandler(dialogHandler);
            mSelectedImage = state.selectedImage;
            mPreviewPic = state.previewPic;
        } else {
            initBuckets(-1);
            // or create a new one
            mImageAdapter = new MultiSelectImageAdapter(
                    getApplicationContext(), dialogHandler);
        }

        mGrid.setAdapter(mImageAdapter);

        // Init of the "Done" button
        Button btnSelectionDone = (Button) findViewById(R.id.btn_selection_done);
        btnSelectionDone.getBackground().setDither(true);
        btnSelectionDone.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                sendResult();
            }

        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreateDialog(int)
     */
    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;

        switch (id) {
        case DIALOG_IMAGE_PREVIEW:
            // Create the dialog for previewing pictures
            ImageView imgPrv = (ImageView) getLayoutInflater().inflate(
                    R.layout.image_preview, null);
            return new AlertDialog.Builder(this).setView(imgPrv).create();
        case DIALOG_WAIT_PREVIEW:
            // Create the progress dialog to be displayed while loading
            // the picture being previewd
            dialog = new ProgressDialog(this);
            ((ProgressDialog) dialog)
                    .setProgressStyle(ProgressDialog.STYLE_SPINNER);
            ((ProgressDialog) dialog)
                    .setMessage(getText(R.string.preparing_preview));
            ((ProgressDialog) dialog).setCancelable(false);
            return dialog;
        default:
            return dialog;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onDestroy()
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mItemsLoader != null) {
            mItemsLoader.stopJob();
            mItemsLoader.removeLock();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mItemsLoader != null && mItemsLoader.isAlive()) {
            mItemsLoader.stopJob();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
     */
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);
        switch (id) {
        case DIALOG_IMAGE_PREVIEW:
            ImageView imgPrv = (ImageView) dialog
                    .findViewById(R.id.image_preview);
            imgPrv.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    dismissDialog(DIALOG_IMAGE_PREVIEW);
                }

            });
            imgPrv.setImageBitmap(mPreviewPic);
            break;
        default:
            break;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onRetainNonConfigurationInstance()
     */
    @Override
    public Object onRetainNonConfigurationInstance() {
        StateHolder state = new StateHolder();
        mImageAdapter.releaseHandler();
        state.adapter = mImageAdapter;
        state.selectedImage = mSelectedImage;
        state.currentBucket = mSpinBuckets.getSelectedItemPosition();
        state.previewPic = mPreviewPic;
        return state;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mItemsLoader != null && mItemsLoader.isAlive()) {
            mItemsLoader.stopJob();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onStop()
     */
    @Override
    protected void onStop() {
        // End any parallel task which could be running
        mImageAdapter.clearPendingThumbnails();
        super.onStop();
        if (mItemsLoader != null) {
            mItemsLoader.stopJob();
            mItemsLoader.removeLock();
        }
        if (mImageAdapter != null && isFinishing()) {
            mImageAdapter.empty();
        }
    }

    /**
     * Prepare the result (selected images Uris) for the caller Activity.
     */
    protected void sendResult() {
        // Put the set of Uris in the result intent
        Intent resultIntent = new Intent();
        Set<Uri> resultUrisList = mImageAdapter.getCheckedItems();
        if (resultUrisList != null && !resultUrisList.isEmpty()) {
            Uri[] resultUrisArray = new Uri[resultUrisList.size()];
            int i = 0;
            for (Uri uri : resultUrisList) {
                resultUrisArray[i] = uri;
                i++;
            }
            resultIntent.putExtra(RESULT_URIS, resultUrisArray);
            setResult(RESULT_OK, resultIntent);
        } else {
            setResult(RESULT_CANCELED);
        }

        finish();
    }

    /**
     * Launches the process for displaying an image preview.
     */
    protected void showPreviewDialog() {

        // First, display aprogress dialog while the bitmap is loading
        showDialog(DIALOG_WAIT_PREVIEW);

        // The preview Bitmap will be loaded asynchronously. This is required
        // to avoid ANR with big image files.
        AsyncTask<Uri, Integer, Bitmap> previewLoader = new AsyncTask<Uri, Integer, Bitmap>() {

            @Override
            protected Bitmap doInBackground(Uri... uris) {
                try {
                    return BitmapLoader.load(getApplicationContext(), uris[0],
                            null, null);
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error : ", e);
                }
                return null;
            }

            /*
             * (non-Javadoc)
             * 
             * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
             */
            @Override
            protected void onPostExecute(Bitmap result) {
                mPreviewPic = result;
                removeDialog(DIALOG_WAIT_PREVIEW);
                showDialog(DIALOG_IMAGE_PREVIEW);
            }

        };
        previewLoader.execute(mSelectedImage);
    }

}