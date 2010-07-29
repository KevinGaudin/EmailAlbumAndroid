package com.kg.emailalbum.mobile.gallery;

import java.util.HashMap;
import java.util.Map;

import com.kg.emailalbum.mobile.gallery.Tag.TagType;

public class TagProvider {
    private static Map<TagType, Map<Long, Tag>> mTagsCache = new HashMap<TagType, Map<Long, Tag>>();

    static {
        for(TagType type : TagType.values()) {
            mTagsCache.put(type, new HashMap<Long, Tag>());
        }
    }
    
    public static Tag getTag(Long id, String label, TagType type) {
        if(type == null) {
            type = TagType.USER;
        }
        Map<Long, Tag> cache = mTagsCache.get(type);
        Tag result = cache.get(id);
        if(result == null) {
            result = new Tag(id, label, type);
            cache.put(id, result);
        }
        return result;
    }
    
    public static Tag getTag(String label, TagType type) {
        if(type == null) {
            type = TagType.USER;
        }
        Map<Long, Tag> cache = mTagsCache.get(type);
        for(Tag tag : cache.values()) {
            if(tag.label.equalsIgnoreCase(label)) {
                return tag;
            }
        }
        return null;
    }    
}
