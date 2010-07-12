package com.kg.emailalbum.mobile.viewer;

import java.util.Set;

import android.graphics.Bitmap;

public class SlideshowItem {
    public String name;
    public String caption;
    public Bitmap bitmap;
    public Set<Long> tags;
    
    public String getShortName() {
        if(name != null) {
            return name.substring(name.lastIndexOf('/') + 1);
        } else return "";
    }
}
