package com.kg.emailalbum.mobile.gallery;

import java.util.HashMap;
import java.util.Map;

import com.kg.emailalbum.mobile.gallery.Tag.TagType;

public class TagProvider {
    private static Map<Long, Tag> mTagsCache = new HashMap<Long, Tag>();
    
    public static Tag getTag(Long id, String label, String type) {
        Tag result = mTagsCache.get(id);
        if(result == null) {
            result = new Tag(id, label, TagType.valueOf(type));
            mTagsCache.put(id, result);
        }
        return result;
    }
}
