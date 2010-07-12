package com.kg.emailalbum.mobile.gallery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

public class TagsDbAdapter {
    private static final String DATABASE_NAME = "EmailAlbum";

    private static final int DATABASE_VERSION = 1;

    private static final String TAGS_TABLE_NAME = "Tags";
    private static final String KEY_TAG_ID = "tagId";
    private static final String KEY_TAG_NAME = "tagName";
    private static final String TAGS_TABLE_CREATE = "CREATE TABLE "
            + TAGS_TABLE_NAME + " (" + KEY_TAG_ID + " INTEGER PRIMARY KEY, "
            + KEY_TAG_NAME + " TEXT UNIQUE);";

    private static final String TAG_URI_TABLE_NAME = "TagUri";
    private static final String KEY_URI = "uri";
    private static final String TAG_URI_TABLE_CREATE = "CREATE TABLE "
            + TAG_URI_TABLE_NAME + " (" + KEY_TAG_ID + " INTEGER NOT NULL, "
            + KEY_URI + " TEXT " + ");";

    private static final String LOG_TAG = TagsDbAdapter.class.getSimpleName();

    private Map<String, Long> mTagsCache = null;

    Context mContext = null;
    TagDbOpenHelper mDbOpenHelper;
    SQLiteDatabase mDb;

    private class TagDbOpenHelper extends SQLiteOpenHelper {

        public TagDbOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(TAGS_TABLE_CREATE);
            db.execSQL(TAG_URI_TABLE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
            // TODO Auto-generated method stub

        }

    }

    public TagsDbAdapter(Context ctx) {
        mContext = ctx;
    }

    public TagsDbAdapter open() {
        mDbOpenHelper = new TagDbOpenHelper(mContext);
        mDb = mDbOpenHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        mDbOpenHelper.close();
    }

    public long createTag(String tagName) {
        ContentValues values = new ContentValues();
        values.put(KEY_TAG_NAME, tagName);
        long result = mDb.insert(TAGS_TABLE_NAME, null, values);
        mTagsCache.put(tagName, result);
        return result;
    }

    public boolean renameTag(long tagId, String tagName) {
        ContentValues values = new ContentValues();
        values.put(KEY_TAG_NAME, tagName);
        boolean result = mDb.update(TAGS_TABLE_NAME, values, KEY_TAG_ID + "="
                + tagId, null) > 0;
        mTagsCache = null;
        return result;
    }

    public boolean deleteTag(long tagId) {
        boolean result = (mDb.delete(TAG_URI_TABLE_NAME, KEY_TAG_ID + "="
                + tagId, null) > 0)
                && (mDb.delete(TAGS_TABLE_NAME, KEY_TAG_ID + "=" + tagId, null) > 0);
        mTagsCache = null;
        return result;
    }

    public boolean setTag(long tagId, Uri uri) {
        ContentValues values = new ContentValues();
        values.put(KEY_TAG_ID, tagId);
        values.put(KEY_URI, uri.toString());
        boolean result = mDb.insert(TAG_URI_TABLE_NAME, null, values) != -1;
        return result;
    }

    public boolean unsetTag(long tagId, Uri uri) {
        boolean result = mDb
                .delete(TAG_URI_TABLE_NAME, KEY_TAG_ID + "=" + tagId + " AND "
                        + KEY_URI + "='" + uri.toString() + "'", null) > 0;
        return result;
    }

    public Map<String, Long> getAllTags() {
        if (mTagsCache == null) {
            mTagsCache = new HashMap<String, Long>();

            Cursor tagsCursor = mDb.query(TAGS_TABLE_NAME, null, null, null,
                    null, null, KEY_TAG_NAME);

            int colNameId = tagsCursor.getColumnIndex(KEY_TAG_NAME);
            int colIdId = tagsCursor.getColumnIndex(KEY_TAG_ID);
            if (tagsCursor.getCount() > 0) {
                while (tagsCursor.moveToNext()) {
                    mTagsCache.put(tagsCursor.getString(colNameId),
                            tagsCursor.getLong(colIdId));
                }
            }
            tagsCursor.close();
        }
        return mTagsCache;

    }

    public Set<Long> getTags(Uri uri) {
        Log.d(LOG_TAG, "Retrieve tags for Uri " + uri.toString());
        String[] selArgs = { uri.toString() };
        String[] proj = { KEY_TAG_ID };
        Cursor tagsCursor = mDb.query(TAG_URI_TABLE_NAME, proj, KEY_URI + "=?",
                selArgs, null, null, null);
        TreeSet<Long> tags = new TreeSet<Long>();
        int colIdId = tagsCursor.getColumnIndex(KEY_TAG_ID);
        if (tagsCursor.getCount() > 0) {
            Log.d(LOG_TAG, "Retrieved tags for Uri " + uri.toString() + ": "
                    + tagsCursor.getCount());
            while (tagsCursor.moveToNext()) {
                Log.d(LOG_TAG, "Retrieved tags for Uri " + uri.toString()
                        + ": adding " + tagsCursor.getLong(colIdId));

                tags.add(tagsCursor.getLong(colIdId));
            }
        }
        tagsCursor.close();
        Log.d(LOG_TAG,
                "Retrieved tags for Uri " + uri.toString() + ": "
                        + tags.toString());
        return tags;
    }

    public String getTagName(long tagId) {
        String result = null;
        String[] selArgs = { Long.toString(tagId) };
        String[] proj = { KEY_TAG_NAME };
        Cursor tagsCursor = mDb.query(TAGS_TABLE_NAME, proj, KEY_TAG_ID + "=?",
                selArgs, null, null, null);
        int colNameId = tagsCursor.getColumnIndex(KEY_TAG_NAME);
        if (tagsCursor.getCount() > 0) {
            while (tagsCursor.moveToNext()) {
                result = tagsCursor.getString(colNameId);
            }
        }
        tagsCursor.close();
        return result;
    }

    public List<Uri> getUrisFromAllTagIds(Long... tagIds) {
        ArrayList<Uri> result = new ArrayList<Uri>();
        // SELECT b.*
        // FROM tagmap bt, bookmark b, tag t
        // WHERE bt.tag_id = t.tag_id
        // AND (t.name IN ('bookmark', 'webservice', 'semweb'))
        // AND b.id = bt.bookmark_id
        // GROUP BY b.id
        // HAVING COUNT( b.id )=3
        StringBuilder selArgsBldr = new StringBuilder();
        String[] selArgsValues = new String[tagIds.length];
        for (int i = 0; i < tagIds.length; i++) {
            selArgsValues[i] = tagIds[i].toString();
            selArgsBldr.append('?');
            if (i < tagIds.length - 1) {
                selArgsBldr.append(',');
            }
        }
        String[] proj = { KEY_URI };
        Cursor urisCursor = mDb.query(true, TAG_URI_TABLE_NAME, proj,
                KEY_TAG_ID + " IN ( " + selArgsBldr.toString() + ")",
                selArgsValues, null, null, null, null);
        int colNameId = urisCursor.getColumnIndex(KEY_URI);
        if (urisCursor.getCount() > 0) {
            while (urisCursor.moveToNext()) {
                result.add(Uri.parse(urisCursor.getString(colNameId)));
            }
        }
        urisCursor.close();

        return result;
    }

    public long getTagId(String tagName) {
        return getAllTags().get(tagName);
    }
}
