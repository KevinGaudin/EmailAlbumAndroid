package com.kg.emailalbum.mobile.tags;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.kg.emailalbum.mobile.tags.Tag.TagType;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

public class TagsDbAdapter {
    private static final String DATABASE_NAME = "EmailAlbum";

    private static final int DATABASE_VERSION = 1;

    private static final String TAGS_TABLE_NAME = "Tags";
    private static final String KEY_TAG_ID = "tagId";
    private static final String KEY_TAG_NAME = "tagName";
    private static final String KEY_TAG_TYPE = "tagType";
    private static final String TAGS_TABLE_CREATE = "CREATE TABLE "
            + TAGS_TABLE_NAME + " (" + KEY_TAG_ID + " INTEGER PRIMARY KEY, "
            + KEY_TAG_NAME + " TEXT NOT NULL," + KEY_TAG_TYPE
            + " TEXT NOT NULL, CONSTRAINT UNI_NAME_TYPE UNIQUE ("
            + KEY_TAG_NAME + ", " + KEY_TAG_TYPE + "));";

    private static final String TAG_URI_TABLE_NAME = "TagUri";
    private static final String KEY_URI = "uri";
    private static final String TAG_URI_TABLE_CREATE = "CREATE TABLE "
            + TAG_URI_TABLE_NAME + " (" + KEY_TAG_ID + " INTEGER NOT NULL, "
            + KEY_URI + " TEXT " + ");";

    private static final String LOG_TAG = TagsDbAdapter.class.getSimpleName();

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

    public Tag createTag(String tagName, TagType tagType) {
        Log.d(LOG_TAG, "Create Tag " + tagType.name() + "/" + tagName);
        Tag createdTag = getTag(tagName, tagType);
        if (createdTag == null) {
            Log.d(LOG_TAG, "Tag is really new, create it in db");
            ContentValues values = new ContentValues();
            values.put(KEY_TAG_NAME, tagName);
            values.put(KEY_TAG_TYPE, tagType.toString());
            long tagId = mDb.insert(TAGS_TABLE_NAME, null, values);
            createdTag = TagProvider.getTag(tagId, tagName, tagType);
        } else {
            Log.d(LOG_TAG, "Tag was already in db.");
        }
        return createdTag;
    }

    public Tag getTag(String tagName, TagType tagType) {
        String from = TAGS_TABLE_NAME;
        String[] columns = { KEY_TAG_ID, KEY_TAG_NAME, KEY_TAG_TYPE };
        String where = KEY_TAG_NAME + " = ? AND " + KEY_TAG_TYPE + " = ?";
        String[] whereArgs = { tagName, tagType.name() };

        SQLiteQueryBuilder qryBld = new SQLiteQueryBuilder();

        qryBld.setTables(from);
        Cursor tagsCursor = qryBld.query(mDb, columns, where, whereArgs, null,
                null, null);
        Tag result = null;
        int colIdId = tagsCursor.getColumnIndex(KEY_TAG_ID);
        int colNameId = tagsCursor.getColumnIndex(KEY_TAG_NAME);
        int colTypeId = tagsCursor.getColumnIndex(KEY_TAG_TYPE);
        if (tagsCursor.getCount() > 0) {
            tagsCursor.moveToNext();

            result = TagProvider.getTag(tagsCursor.getLong(colIdId),
                    tagsCursor.getString(colNameId),
                    TagType.valueOf(tagsCursor.getString(colTypeId)));

        }
        tagsCursor.close();
        return result;
    }

    public boolean renameTag(long tagId, String tagName) {
        ContentValues values = new ContentValues();
        values.put(KEY_TAG_NAME, tagName);
        boolean result = mDb.update(TAGS_TABLE_NAME, values, KEY_TAG_ID + "="
                + tagId, null) > 0;
        // TODO : apply modification to TagProvider
        return result;
    }

    public boolean deleteTag(long tagId) {
        boolean result = (mDb.delete(TAG_URI_TABLE_NAME, KEY_TAG_ID + "="
                + tagId, null) > 0)
                && (mDb.delete(TAGS_TABLE_NAME, KEY_TAG_ID + "=" + tagId, null) > 0);
        // TODO : apply modification to TagProvider
        return result;
    }

    public boolean setTag(Tag tag, Uri uri) {
        ContentValues values = new ContentValues();
        values.put(KEY_TAG_ID, tag.id);
        values.put(KEY_URI, uri.toString());
        boolean result = mDb.insert(TAG_URI_TABLE_NAME, null, values) != -1;
        return result;
    }

    public boolean unsetTag(Tag tag, Uri uri) {
        boolean result = mDb.delete(TAG_URI_TABLE_NAME, KEY_TAG_ID + "="
                + tag.id + " AND " + KEY_URI + "='" + uri.toString() + "'",
                null) > 0;
        return result;
    }

    public Set<Tag> getAllTags(TagType... types) {
        Set<Tag> allTagsSet = new TreeSet<Tag>();
        StringBuilder where = new StringBuilder();
        String[] values = new String[types.length];
        where.append(KEY_TAG_TYPE).append(" IN ( ");
        for (int i = 0; i < types.length; i++) {
            where.append('?');
            values[i] = types[i].name();
            if (i < types.length - 1) {
                where.append(", ");
            }
        }
        where.append(')');
        Cursor tagsCursor = mDb.query(TAGS_TABLE_NAME, null, where.toString(),
                values, null, null, KEY_TAG_NAME);

        int colNameId = tagsCursor.getColumnIndex(KEY_TAG_NAME);
        int colIdId = tagsCursor.getColumnIndex(KEY_TAG_ID);
        int colTypeId = tagsCursor.getColumnIndex(KEY_TAG_TYPE);
        if (tagsCursor.getCount() > 0) {
            while (tagsCursor.moveToNext()) {
                Tag tag = TagProvider.getTag(tagsCursor.getLong(colIdId),
                        tagsCursor.getString(colNameId),
                        TagType.valueOf(tagsCursor.getString(colTypeId)));
                allTagsSet.add(tag);
            }
        }
        tagsCursor.close();
        return allTagsSet;
    }

    public Set<Tag> getTags(Uri uri) {
        Log.d(LOG_TAG, "Retrieve tags for Uri " + uri.toString());
        String from = TAG_URI_TABLE_NAME + ", " + TAGS_TABLE_NAME;
        String[] columns = { TAGS_TABLE_NAME + "." + KEY_TAG_ID, KEY_TAG_NAME,
                KEY_TAG_TYPE };
        String where = TAGS_TABLE_NAME + "." + KEY_TAG_ID + "="
                + TAG_URI_TABLE_NAME + "." + KEY_TAG_ID + " AND " + KEY_URI
                + "= ?";
        String[] whereArgs = { uri.toString() };

        SQLiteQueryBuilder qryBld = new SQLiteQueryBuilder();

        qryBld.setTables(from);
        Cursor tagsCursor = qryBld.query(mDb, columns, where, whereArgs, null,
                null, null);
        Set<Tag> tags = new TreeSet<Tag>();
        int colIdId = tagsCursor.getColumnIndex(KEY_TAG_ID);
        int colNameId = tagsCursor.getColumnIndex(KEY_TAG_NAME);
        int colTypeId = tagsCursor.getColumnIndex(KEY_TAG_TYPE);
        if (tagsCursor.getCount() > 0) {
            Log.d(LOG_TAG, "Retrieved tags for Uri " + uri.toString() + ": "
                    + tagsCursor.getCount());
            while (tagsCursor.moveToNext()) {
                Log.d(LOG_TAG, "Retrieved tags for Uri " + uri.toString()
                        + ": adding " + tagsCursor.getLong(colIdId));

                tags.add(TagProvider.getTag(tagsCursor.getLong(colIdId),
                        tagsCursor.getString(colNameId),
                        TagType.valueOf(tagsCursor.getString(colTypeId))));
            }
        }
        tagsCursor.close();
        Log.d(LOG_TAG,
                "Retrieved tags for Uri " + uri.toString() + ": "
                        + tags.toString());
        return tags;
    }

    public List<Uri> getUrisFromAllTags(Tag... tags) {
        ArrayList<Uri> result = new ArrayList<Uri>();
        // SELECT b.*
        // FROM tagmap bt, bookmark b, tag t
        // WHERE bt.tag_id = t.tag_id
        // AND (t.name IN ('bookmark', 'webservice', 'semweb'))
        // AND b.id = bt.bookmark_id
        // GROUP BY b.id
        // HAVING COUNT( b.id )=3
        StringBuilder selArgsBldr = new StringBuilder();
        String[] selArgsValues = new String[tags.length];
        for (int i = 0; i < tags.length; i++) {
            selArgsValues[i] = Long.toString(tags[i].id);
            selArgsBldr.append('?');
            if (i < tags.length - 1) {
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

    // public long getTagId(String tagName) {
    // return getAllTags().get(tagName);
    // }
}
