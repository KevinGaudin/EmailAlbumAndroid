package com.kg.emailalbum.mobile.viewer;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
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
    public Long mTimestamp = new Long(-1);
    public final static Map<TagType, Comparator<SlideshowItem>> mComparators = new HashMap<TagType, Comparator<SlideshowItem>>(); 

    public String getShortName() {
        if (name != null) {
            return name.substring(name.lastIndexOf('/') + 1);
        } else
            return "";
    }

    public boolean isDateTakenSet() {
        for (Tag tag : tags) {
            if (tag.type == TagType.TIMESTAMP) {
                return true;
            }
        }
        return false;
    }

    public Long getTimestamp() {
        if (mTimestamp < 0) {
            for (Tag tag : tags) {
                if (tag.type == TagType.TIMESTAMP) {
                    mTimestamp = Long.parseLong(tag.label);
                }
            }
        }
        return mTimestamp;
    }

    public static Comparator<SlideshowItem> getComparator(TagType tagType) {
        Comparator<SlideshowItem> result = mComparators.get(tagType);
        if(result == null) {
            result = new SlideshowItemComparator(tagType);
            mComparators.put(tagType, result);
        }
        return result;
    }


}
