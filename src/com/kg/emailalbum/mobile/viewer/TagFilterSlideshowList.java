package com.kg.emailalbum.mobile.viewer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.net.Uri;

import com.kg.emailalbum.mobile.tags.Tag.TagType;
import com.kg.emailalbum.mobile.tags.TagFilter;
import com.kg.emailalbum.mobile.tags.TagsDbAdapter;

public class TagFilterSlideshowList extends SlideshowList {
    private static final String LOG_TAG = TagFilterSlideshowList.class
            .getSimpleName();
    private TagsDbAdapter mTagsDb;
    private TagFilter[] mTagFilters;

    private List<SlideshowItem> mItems = new ArrayList<SlideshowItem>();

    private Comparator<SlideshowItem> mComparator;

    public TagFilterSlideshowList(TagsDbAdapter tagsDb, TagFilter[] tagFilters) {
        mTagsDb = tagsDb;
        setFilters(tagFilters);
    }

    @Override
    public Uri getAlbumUri() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SlideshowItem get(int location) {
        // TODO : in order to allow sorting by different tag values, we have to
        // handle a cache of SlideshowItems, not only Uris. These SlideshowItems
        // will
        // be given their bitmap at the latest possible instant.
        SlideshowItem result = null;
        if (mItems.size() > 0) {
            result = mItems.get(location);
        }
        return result;
    }

    @Override
    public int size() {
        return mItems.size();
    }

    @Override
    public void setFilters(TagFilter[] tagFilters) {
        mTagFilters = tagFilters;
        List<Uri> uris = mTagsDb.getUrisFromAllTagFilters(mTagFilters);
        mItems.clear();
        SlideshowItem newItem;
        for (Uri uri : uris) {
            newItem = new SlideshowItem();
            newItem.uri = uri;
            if (mTagsDb != null) {
                newItem.tags = mTagsDb.getTags(uri);
            }
            // TODO: change .name to .getName() returning content of tag NAME
            newItem.name = uri.toString();
            // TODO: change .name to .getName() returning content of tag
            // DESCRIPTION
            newItem.caption = "";
            mItems.add(newItem);
        }
        setComparator(Collections.reverseOrder(SlideshowItem.getComparator(TagType.TIMESTAMP)));
    }

    public void setComparator(Comparator<SlideshowItem> comparator) {
        mComparator = comparator;
        Collections.sort(mItems, mComparator);
    }

}
