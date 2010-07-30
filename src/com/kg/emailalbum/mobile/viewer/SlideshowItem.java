package com.kg.emailalbum.mobile.viewer;

import java.util.Set;
import java.util.TreeSet;

import android.graphics.Bitmap;
import android.net.Uri;

import com.kg.emailalbum.mobile.tags.Tag;
import com.kg.emailalbum.mobile.tags.Tag.TagType;

public class SlideshowItem {
    public String name;
    public Uri uri;
    public String caption;
    public Bitmap bitmap;
    public Set<Tag> tags = new TreeSet<Tag>();
    
    public String getShortName() {
        if(name != null) {
            return name.substring(name.lastIndexOf('/') + 1);
        } else return "";
    }
    
    public boolean isDateTakenSet() {
        for(Tag tag : tags) {
            if(tag.type == TagType.MONTH || tag.type == TagType.YEAR) {
                return true;
            }
        }
        return false;
    }
}
