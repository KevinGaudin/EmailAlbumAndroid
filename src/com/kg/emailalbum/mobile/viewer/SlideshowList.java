package com.kg.emailalbum.mobile.viewer;

import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractList;

import android.net.Uri;

public abstract class SlideshowList extends AbstractList<SlideshowItem> {
    public abstract InputStream getOriginalInputStream(int position) throws IOException;
    public abstract Uri getAlbumUri();
}
