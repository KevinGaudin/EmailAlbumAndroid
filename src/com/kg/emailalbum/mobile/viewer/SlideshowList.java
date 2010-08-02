package com.kg.emailalbum.mobile.viewer;

import java.util.AbstractList;

import android.net.Uri;

public abstract class SlideshowList extends AbstractList<SlideshowItem> {
    public abstract Uri getAlbumUri();
}
