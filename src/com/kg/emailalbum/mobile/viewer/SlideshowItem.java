package com.kg.emailalbum.mobile.viewer;

import android.graphics.Bitmap;

public class SlideshowItem {
    public String name;
    public String caption;
    public Bitmap bitmap;
    
    public String getShortName() {
        if(name != null) {
            return name.substring(name.lastIndexOf('/') + 1);
        } else return "";
    }
}
