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
        // should return an uri pointing to a Slideshow configured with the
        // current filtering/sorting parameters. Could be like :
        // content://com.kg.emailalbum.filtered/?tags=tag1+!tag2+-tag3&orderby=TIMESTAMP&reverse=true
        return null;
    }

    @Override
    public SlideshowItem get(int location) {
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
            // TODO: change .caption to .getCaption() returning content of tag
            // DESCRIPTION
            newItem.caption = "";
            mItems.add(newItem);
        }
        // TODO: implement user defined comparator
        setComparator(Collections.reverseOrder(SlideshowItem
                .getComparator(TagType.TIMESTAMP)));
    }

    public void setComparator(Comparator<SlideshowItem> comparator) {
        mComparator = comparator;
        Collections.sort(mItems, mComparator);
    }

}
