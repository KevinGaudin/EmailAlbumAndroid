package com.kg.emailalbum.mobile.viewer;

import java.util.AbstractList;

import com.kg.emailalbum.mobile.tags.TagFilter;

import android.net.Uri;

public abstract class SlideshowList extends AbstractList<SlideshowItem> {
    public abstract Uri getAlbumUri();

    public abstract void setFilters(TagFilter[] array);
}
