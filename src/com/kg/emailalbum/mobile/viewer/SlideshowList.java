package com.kg.emailalbum.mobile.viewer;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import android.net.Uri;

public interface SlideshowList extends List<SlideshowItem> {
    public InputStream getOriginalInputStream(int position) throws IOException;
    public Uri getAlbumUri();
}
