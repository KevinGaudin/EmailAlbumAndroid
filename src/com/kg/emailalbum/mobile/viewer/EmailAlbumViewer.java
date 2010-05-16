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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.kg.emailalbum.mobile.AboutDialog;
import com.kg.emailalbum.mobile.EmailAlbumPreferences;
import com.kg.emailalbum.mobile.R;
import com.kg.emailalbum.mobile.creator.EmailAlbumEditor;
import com.kg.emailalbum.mobile.util.CacheManager;
import com.kg.emailalbum.mobile.util.Compatibility;
import com.kg.emailalbum.mobile.util.HumanReadableProperties;
import com.kg.emailalbum.mobile.util.IntentHelper;
import com.kg.emailalbum.mobile.util.ZipUtil;
import com.kg.oifilemanager.intents.FileManagerIntents;

/**
 * Loads an album and displays its content as a list.
 * 
 * @author Kevin Gaudin
 */
public class EmailAlbumViewer extends ListActivity {

    /**
     * Loads an archive provided by an Intent (from Gmail for example). TODO:
     * use AsyncTask
     * 
     * @author Normal
     * 
     */
    private class ArchiveRetriever implements Runnable {

        private static final int MSG_ARCHIVE_RETRIEVED = 0;

        @Override
        public void run() {
            try {
                // Copy the stream from the intent to a temporary file for later
                // random access
                Random generator = new Random();
                int random = generator.nextInt(99999);
                InputStream intentInputStream = getContentResolver()
                        .openInputStream(mAlbumFileUri);
                File tempArchive = new File(new CacheManager(
                        getApplicationContext()).getCacheDir("viewer"),
                        "emailalbum" + random + ".zip");
                OutputStream tempOS = new FileOutputStream(tempArchive);
                Log.d(LOG_TAG, "Write retrieved archive : "
                        + tempArchive.getAbsolutePath());

                byte[] buffer = new byte[256];
                int readBytes = -1;
                while ((readBytes = intentInputStream.read(buffer)) != -1) {
                    tempOS.write(buffer, 0, readBytes);
                }
                mAlbumFileUri = Uri.fromFile(tempArchive);
                tempOS.close();
                intentInputStream.close();
                archiveCopyHandler.sendEmptyMessage(MSG_ARCHIVE_RETRIEVED);

            } catch (IOException e) {
                Message msg = new Message();
                Bundle data = new Bundle();
                msg.arg1 = -1;
                data.putString("EXCEPTION", e.getLocalizedMessage());
                msg.setData(data);
                archiveCopyHandler.sendMessage(msg);
            }
        }

    }

    /**
     * An adapter handling the content of the archive.
     */
    public class PhotoAdapter extends BaseAdapter {

        /**
         * Keeps references to UI elements to avoid looking for them. This
         * should be attached to a row item with View.setTag().
         */
        private class ViewHolder {
            public ImageView image = null;
            public TextView text = null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see android.widget.Adapter#getCount()
         */
        @Override
        public int getCount() {
            if (mContentModel == null)
                return 0;
            return mContentModel.size();
        }

        /*
         * (non-Javadoc)
         * 
         * @see android.widget.Adapter#getItem(int)
         */
        @Override
        public Object getItem(int position) {
            return mContentModel.get(position);
        }

        /*
         * (non-Javadoc)
         * 
         * @see android.widget.Adapter#getItemId(int)
         */
        @Override
        public long getItemId(int position) {
            return position;
        }

        /*
         * (non-Javadoc)
         * 
         * @see android.widget.Adapter#getView(int, android.view.View,
         * android.view.ViewGroup)
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;

            if (convertView == null) {
                // No view to reuse, create a new one
                convertView = getLayoutInflater().inflate(R.layout.content_row,
                        null, false);
                holder = new ViewHolder();
                // Retrieve and store components references
                holder.image = (ImageView) convertView
                        .findViewById(R.id.thumbnail);
                holder.text = (TextView) convertView
                        .findViewById(R.id.image_name);
                convertView.setTag(holder);
            } else {
                // Reuse a previously created view
                holder = (ViewHolder) convertView.getTag();
            }

            Map<String, String> currentMetaData = mContentModel.get(position);
            String thumbName = currentMetaData.get(KEY_THUMBNAIL);
            if (thumbName != null) {
                holder.image.setImageDrawable(new BitmapDrawable(thumbName));
            } else {
                holder.image.setImageResource(R.drawable.robot);
            }

            String shortName = currentMetaData.get(KEY_SHORTNAME);
            StringBuilder text = new StringBuilder(shortName);

            if (mCaptions != null && !mCaptions.isEmpty()) {
                String caption = (String) mCaptions.get(shortName);
                if (caption != null && !"".equals(caption.trim())) {
                    text.append("\n\n").append(caption);
                }
            }
            holder.text.setText(text);
            return convertView;
        }

        /**
         * Update the Thumbnail of an item.
         * 
         * @param position
         *            The position of the item in the list.
         * @param thumbName
         *            The thumbnail file path.
         */
        public void updateThumbnail(int position, String thumbName) {
            mContentModel.get(position).put(KEY_THUMBNAIL, thumbName);
            notifyDataSetChanged();
        }

    }

    /**
     * Called Activities IDs
     */
    private static final int ACTIVITY_PICK_DIRECTORY_TO_SAVE_ALL = 2;
    private static final int ACTIVITY_PICK_DIRECTORY_TO_SAVE_SELECTED = 3;
    private static final int ACTIVITY_PICK_FILE = 1;

    private static final String KEY_FULLNAME = "imageName";
    private static final String KEY_SHORTNAME = "imageShortName";
    public static final String KEY_THMBCREAT_ENTRY_POSITION = "entryPosition";
    public static final String KEY_THMBCREAT_THUMB_NAME = "thumbName";
    private static final String KEY_THUMBNAIL = "thumbnail";

    private static final String LOG_TAG = EmailAlbumViewer.class
            .getSimpleName();

    // Options menu items
    private static final int MENU_LOAD_ALBUM_ID = 1;
    private static final int MENU_SAVE_ALL_ID = 2;
    private static final int MENU_ABOUT_ID = 3;
    private static final int MENU_PREFS_ID = 4;
    private static final int MENU_SEND_ALL_ID = 5;

    // Context menu items
    private static final int MENUCTX_SAVE_SELECTED_ID = 4;

    protected static final Uri URI_DEMO = Uri
            .parse("http://www.gaudin.tv/storage/android/curious-creature.jar");

    /**
     * Handler for receiving the archive when it is asynchronously loaded.
     */
    private Handler archiveCopyHandler = new Handler() {

        /*
         * (non-Javadoc)
         * 
         * @see android.os.Handler#handleMessage(android.os.Message)
         */
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            // Hide the progress dialog
            mProgress.dismiss();
            if (msg.arg1 < 0) {
                // If an error occurred, tell it.
                Toast.makeText(context, R.string.error_saving,
                        Toast.LENGTH_SHORT).show();
            }
            // Read the archive content
            fillData(true);
        }

    };

    private Context context;
    private PhotoAdapter mAdapter;
    private Uri mAlbumFileUri = null;

    private ZipFile mArchive = null;

    private Bundle mCaptions;

    private ArrayList<Map<String, String>> mContentModel;
    private ProgressDialog mProgress;

    private ThumbnailsCreator mThmbCreator;
    private int posPictureToSave;
    private Handler saveAllHandler = new Handler() {

        /*
         * (non-Javadoc)
         * 
         * @see android.os.Handler#handleMessage(android.os.Message)
         */
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.arg1 < 0) {
                Toast.makeText(context, msg.getData().getString("EXCEPTION"),
                        Toast.LENGTH_SHORT).show();
                setProgressBarVisibility(false);
            } else {
                int position = msg.what;
                setProgress((position + 1) * 10000 / mContentModel.size());
            }
        }

    };

    /**
     * Receive created thumbnails and give them to the adapter.
     */
    private Handler thumbnailsCreationHandler = new Handler() {
        /*
         * (non-Javadoc)
         * 
         * @see android.os.Handler#handleMessage(android.os.Message)
         */
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.arg1 < 0) {
                if (msg.getData().getString("EXCEPTION_CLASS").equals(
                        OutOfMemoryError.class.getSimpleName())) {
                    Toast.makeText(context, R.string.error_out_of_mem,
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context,
                            msg.getData().getString("EXCEPTION"),
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                int position = msg.getData().getInt(
                        KEY_THMBCREAT_ENTRY_POSITION);
                String thumbName = msg.getData().getString(
                        KEY_THMBCREAT_THUMB_NAME);
                setProgress((position + 1) * 10000 / mContentModel.size());
                mAdapter.updateThumbnail(position, thumbName);
            }
        }

    };

    /**
     * Load the content of the archive.
     * 
     * @param clearThumbnails
     *            Set this to true if you want to discard existing thumbnails.
     */
    private void fillData(boolean clearThumbnails) {
        if (mAlbumFileUri != null) {
            try {
                File selectedFile = new File(new URI(mAlbumFileUri.toString()
                        .replace(" ", "%20")));
                mArchive = new ZipFile(selectedFile);
            } catch (Exception e) {
                Toast.makeText(this,
                        R.string.open_archive_error + e.getLocalizedMessage(),
                        Toast.LENGTH_SHORT).show();
            }

            if (mArchive == null) {
                Toast.makeText(this, R.string.open_archive_error,
                        Toast.LENGTH_SHORT).show();
            } else {
                try {
                    // Each picture is represented as a Map containing metadata
                    mContentModel = new ArrayList<Map<String, String>>();
                    setTitle(mAlbumFileUri.getLastPathSegment());

                    // Test if an EmailAlbum description file is present
                    ZipEntry captions = mArchive
                            .getEntry(EmailAlbumEditor.ALBUM_CONTENT_FILE);
                    if (captions != null) {
                        Log.d(this.getClass().getSimpleName(),
                                "content file found !");
                        loadCaptions(captions, false);
                    } else {
                        captions = mArchive
                                .getEntry(EmailAlbumEditor.ZIP_CONTENT_FILE);
                        if (captions != null) {
                            loadCaptions(captions, true);

                        } else {
                            Log.d(this.getClass().getSimpleName(),
                                    "no content file !");
                        }
                    }

                    ZipEntry entry = null;
                    // Read all archive entries
                    Enumeration<? extends ZipEntry> entries = mArchive
                            .entries();
                    while (entries.hasMoreElements()) {
                        entry = entries.nextElement();
                        if (entry.getName().endsWith(".jpg")
                                || entry.getName().endsWith(".JPG")
                                || entry.getName().endsWith(".gif")
                                || entry.getName().endsWith(".GIF")
                                || entry.getName().endsWith(".png")
                                || entry.getName().endsWith(".PNG")) {
                            Map<String, String> contentLine = new HashMap<String, String>();
                            contentLine.put(KEY_FULLNAME, entry.getName());
                            contentLine
                                    .put(
                                            KEY_SHORTNAME,
                                            entry
                                                    .getName()
                                                    .substring(
                                                            entry
                                                                    .getName()
                                                                    .lastIndexOf(
                                                                            '/') + 1));
                            mContentModel.add(contentLine);
                        }
                    }
                    registerForContextMenu(getListView());
                    getListView().setSelection(
                            getListView().getFirstVisiblePosition());
                } catch (Exception e) {
                    Toast.makeText(this, e.getLocalizedMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
        // We have some pictures to display, start creating thumbnails
        if (mContentModel != null) {
            setProgressBarVisibility(true);
            startThumbnailsCreation(clearThumbnails);
        }
    }

    /**
     * Load the captions set in the EmailAlbum content file.
     * 
     * @param captions
     *            The ZipEntry containing the captions.
     * @throws IOException
     */
    private void loadCaptions(ZipEntry captions,
            boolean isHumanReadableProperties) throws IOException {
        mCaptions = new Bundle();
        HumanReadableProperties props = new HumanReadableProperties();
        if (isHumanReadableProperties) {
            props.loadHumanReadable(ZipUtil.getInputStream(mArchive, captions));
        } else {
            props.load(ZipUtil.getInputStream(mArchive, captions));
        }
        for (Object key : props.keySet()) {
            mCaptions.putString((String) key, props.getProperty((String) key));
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onActivityResult(int, int,
     * android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
        case ACTIVITY_PICK_FILE:
            // An album file has been chosen
            if (resultCode == RESULT_OK && data != null) {
                // obtain the filename
                Uri filename = data.getData();
                if (filename != null) {
                    mAlbumFileUri = filename;
                    // load the content
                    fillData(true);
                } else {
                    finish();
                }
            } else {
                finish();
            }
            break;
        case ACTIVITY_PICK_DIRECTORY_TO_SAVE_SELECTED:
        case ACTIVITY_PICK_DIRECTORY_TO_SAVE_ALL:
            // A directory has been selected by the user to save one or all
            // pictures.
            if (resultCode == RESULT_OK && data != null) {
                // obtain the filename
                String dirname = data.getDataString();
                try {
                    if (requestCode == ACTIVITY_PICK_DIRECTORY_TO_SAVE_SELECTED) {
                        savePicture(posPictureToSave, new File(Uri.parse(
                                dirname).getPath()));
                    } else {
                        saveAllPictures(new File(Uri.parse(dirname).getPath()));
                    }
                } catch (IOException e) {
                    Log.e(this.getClass().getName(),
                            "onActivityResult() exception", e);
                    Toast.makeText(this, e.getLocalizedMessage(),
                            Toast.LENGTH_SHORT).show();
                }

            }
            break;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_SAVE_ALL_ID:
            // Let the user chose a directory where he wants to save all
            // pictures.
            pickDirectory(ACTIVITY_PICK_DIRECTORY_TO_SAVE_ALL);
            return true;
        case MENUCTX_SAVE_SELECTED_ID:
            // Let the user chose a directory where he wants to save the
            // selected picture.
            pickDirectory(ACTIVITY_PICK_DIRECTORY_TO_SAVE_SELECTED);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.content_list);

        // Dither the background for pre 2.0 devices
        findViewById(R.id.album_viewer_root).getBackground().setDither(true);

        mAdapter = new PhotoAdapter();
        setListAdapter(mAdapter);

        context = this;
        // if (getIntent() != null) {
        // Toast.makeText(this, "Intent action : " + getIntent().getAction(),
        // Toast.LENGTH_SHORT).show();
        // }

        if (savedInstanceState != null) {
            // Retrieve a previous session
            if (savedInstanceState.getString("albumFileUri") != null) {
                mAlbumFileUri = Uri.parse(savedInstanceState
                        .getString("albumFileUri"));
                fillData(false);
                if (savedInstanceState.getBoolean("thumbCreatorInterrupted")) {
                    startThumbnailsCreation(false);
                }
            }
        } else if (getIntent() != null
                && Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            // An album is provided by another app
            mAlbumFileUri = getIntent().getData();
            if (mAlbumFileUri.getScheme()
                    .equals(ContentResolver.SCHEME_CONTENT)) {
                // In this case, the Intent certainly comes from GMail
                // This is not a file:// Uri, we have to store a temporary
                // copy of the album.
                mProgress = ProgressDialog.show(this,
                        getText(R.string.title_prog_retrieve_archive),
                        getText(R.string.msg_prog_retrieve_archive), true,
                        false);
                new Thread(new ArchiveRetriever()).start();

            } else {
                fillData(true);
            }
        }

        if (mAlbumFileUri == null || "".equals(mAlbumFileUri)) {
            // Pas d'album en entrée, demander à l'utilisateur dans choisir un .
            Log.d(LOG_TAG, "No album");
            openAlbum();
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu,
     * android.view.View, android.view.ContextMenu.ContextMenuInfo)
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        posPictureToSave = ((AdapterView.AdapterContextMenuInfo) menuInfo).position;
        menu.setHeaderTitle(R.string.menu_save);
        menu.add(0, MENUCTX_SAVE_SELECTED_ID, 0, R.string.menu_save_selected);
        menu.add(0, MENU_SAVE_ALL_ID, 0, R.string.menu_save_all);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // TODO Auto-generated method stub
        boolean result = super.onCreateOptionsMenu(menu);
        MenuItem item = menu.add(0, MENU_LOAD_ALBUM_ID, 0,
                R.string.menu_load_album);
        item.setIcon(android.R.drawable.ic_menu_gallery);
        item = menu.add(0, MENU_SAVE_ALL_ID, 0, R.string.menu_save_all);
        item.setIcon(android.R.drawable.ic_menu_save);
        if (Compatibility.isSendMultipleAppAvailable(getApplicationContext())) {
            item = menu.add(0, MENU_SEND_ALL_ID, 0, R.string.menu_share_all);
            item.setIcon(android.R.drawable.ic_menu_share);
        }
        item = menu.add(0, MENU_PREFS_ID, 0, R.string.menu_prefs);
        item.setIcon(android.R.drawable.ic_menu_preferences);
        item = menu.add(0, MENU_ABOUT_ID, 0, R.string.menu_about);
        item.setIcon(android.R.drawable.ic_menu_help);
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onDestroy()
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            // Delete cached album files and thumbnails
            new CacheManager(getApplicationContext()).clearCache("viewer");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.ListActivity#onListItemClick(android.widget.ListView,
     * android.view.View, int, long)
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent i = new Intent(this, ShowPics.class);

        ArrayList<String> imageNames = new ArrayList<String>();
        for (Map<String, String> contentItem : mContentModel) {
            imageNames.add(contentItem.get(KEY_FULLNAME));
        }

        // Send to the pictures viewer all the needed data:
        // The list of pictures names in the archive
        i.putExtra("PICS", imageNames);
        // The captions
        i.putExtra("CAPTIONS", mCaptions);
        // The Uri of the archive
        i.putExtra("ALBUM", mAlbumFileUri.toString());
        // The position selected by the user to start the slideshow
        i.putExtra("POSITION", position);
        startActivity(i);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_LOAD_ALBUM_ID:
            openAlbum();
            return true;
        case MENU_SAVE_ALL_ID:
            pickDirectory(ACTIVITY_PICK_DIRECTORY_TO_SAVE_ALL);
            return true;
        case MENU_SEND_ALL_ID:
            File dir = new CacheManager(getApplicationContext())
                    .getCacheDir("temp");
            IntentHelper.sendAllPicturesInFolder(getApplicationContext(), dir,
                    "", "");
            return true;
        case MENU_ABOUT_ID:
            Intent intent = new Intent(this, AboutDialog.class);
            startActivity(intent);
            return true;
        case MENU_PREFS_ID:
            startPreferencesActivity();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mThmbCreator != null && mThmbCreator.isAlive()) {
            mThmbCreator.stopCreation();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mContentModel == null) {
            menu.findItem(MENU_SAVE_ALL_ID).setEnabled(false);
        } else {
            menu.findItem(MENU_SAVE_ALL_ID).setEnabled(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (mThmbCreator == null
                || (mThmbCreator != null && !mThmbCreator.isAlive())) {
            startThumbnailsCreation(false);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAlbumFileUri != null) {
            outState.putString("albumFileUri", mAlbumFileUri.toString());
        }
        if (mThmbCreator != null && mThmbCreator.isAlive()) {
            mThmbCreator.stopCreation();
            outState.putBoolean("thumbCreatorInterrupted", true);
        }
    }

    /**
     * Start the album opening process : start a file chooser activity.
     */
    private void openAlbum() {
        Intent intent = new Intent(FileManagerIntents.ACTION_PICK_FILE);
        if (android.os.Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            intent.setData(Uri.fromFile(android.os.Environment
                    .getExternalStorageDirectory()));
        } else {
            intent.setData(Uri.parse("file:///"));
        }

        intent.putExtra(FileManagerIntents.EXTRA_TITLE,
                getText(R.string.select_file));
        intent.putExtra(FileManagerIntents.EXTRA_BUTTON_TEXT,
                getText(R.string.btn_select_file));
        try {
            startActivityForResult(intent, ACTIVITY_PICK_FILE);
        } catch (ActivityNotFoundException e) {
            Log.e(LOG_TAG, "Error before picking file", e);
        }
    }

    /**
     * Let the user choose a directory.
     * 
     * @param requestCode
     *            The code which will be returned to allow to select which
     *            action has to be performed on the directory.
     */
    private void pickDirectory(int requestCode) {
        Intent intent = new Intent(FileManagerIntents.ACTION_PICK_DIRECTORY);

        intent.setData(Uri.fromFile(android.os.Environment
                .getExternalStorageDirectory()));
        intent.putExtra(FileManagerIntents.EXTRA_TITLE,
                getText(R.string.select_directory));
        intent.putExtra(FileManagerIntents.EXTRA_BUTTON_TEXT,
                getText(R.string.btn_select_directory));
        try {
            startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            Log.e(LOG_TAG, "Error before picking directory", e);
        }
    }

    /**
     * Save all the archive pictures. TODO: use AsyncTask
     * 
     * @param file
     *            The destination directory.
     * @throws IOException
     */
    private void saveAllPictures(final File file) throws IOException {
        setProgress(0);
        setProgressBarVisibility(true);
        new Thread() {

            /*
             * (non-Javadoc)
             * 
             * @see java.lang.Thread#run()
             */
            @Override
            public void run() {
                for (int i = 0; i < mContentModel.size(); i++) {
                    try {
                        savePicture(i, file);
                        // call the handler to display progress
                        saveAllHandler.sendEmptyMessage(i);
                    } catch (FileNotFoundException e) {
                        Log.e(this.getClass().getSimpleName(),
                                "Error while saving pictures", e);
                        Message msg = new Message();
                        Bundle data = new Bundle();
                        msg.arg1 = -1;
                        data.putString("EXCEPTION", e.getMessage());
                        msg.setData(data);
                        saveAllHandler.sendMessage(msg);
                    } catch (IOException e) {
                    }
                }
            }

        }.start();

    }

    /**
     * Save one picture from the archive
     * 
     * @param position
     * @param destDir
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    private File savePicture(int position, File destDir)
            throws FileNotFoundException, IOException {
        File destFile;
        Map<String, String> imgModel = mContentModel.get(position);
        destFile = new File(destDir, imgModel.get(KEY_SHORTNAME).toLowerCase());

        // Raw copy of the file
        OutputStream destFileOS = new FileOutputStream(destFile);
        InputStream imageIS = ZipUtil.getInputStream(mArchive, mArchive
                .getEntry(imgModel.get(KEY_FULLNAME)));
        byte[] buffer = new byte[2048];
        int len = 0;
        while ((len = imageIS.read(buffer)) >= 0) {
            destFileOS.write(buffer, 0, len);
        }
        destFileOS.close();
        imageIS.close();
        return destFile;
    }

    /**
     * Start the settings activity.
     */
    private void startPreferencesActivity() {
        Intent i = new Intent(getApplicationContext(),
                EmailAlbumPreferences.class);
        i.putExtra(EmailAlbumPreferences.EXTRA_SCREEN,
                EmailAlbumPreferences.SCREEN_VIEWER);
        startActivity(i);
    }

    /**
     * Start the asynchronous thumbnails creation process.
     * 
     * @param clearThumbnails
     */
    private void startThumbnailsCreation(boolean clearThumbnails) {
        if (mContentModel != null) {
            ArrayList<String> pictureNames = new ArrayList<String>();
            Iterator<Map<String, String>> i = mContentModel.iterator();
            while (i.hasNext()) {
                Map<String, String> entry = i.next();
                pictureNames.add(entry.get(KEY_FULLNAME));
            }
            mThmbCreator = new ThumbnailsCreator(this, mArchive, pictureNames,
                    thumbnailsCreationHandler, clearThumbnails);
            mThmbCreator.start();
        }
    }

}
