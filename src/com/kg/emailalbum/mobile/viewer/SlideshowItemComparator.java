package com.kg.emailalbum.mobile.viewer;

import java.util.Comparator;

import com.kg.emailalbum.mobile.tags.Tag;
import com.kg.emailalbum.mobile.tags.Tag.TagType;

class SlideshowItemComparator implements Comparator<SlideshowItem> {

    private TagType mTagType = null;

    public SlideshowItemComparator(TagType tagType) {
        mTagType = tagType;
    }

    @Override
    public int compare(SlideshowItem item1, SlideshowItem item2) {
        switch (mTagType) {
        case NAME:
            String i1Name = item1.name;
            String i2Name = item2.name;

            if (i1Name == null && i2Name == null) {
                return 0;
            } else if (i1Name == null && i2Name != null) {
                return 1;
            } else if (i2Name == null && i1Name != null) {
                return -1;
            } else if(!i1Name.equals(i2Name)){
                return i1Name.compareTo(i2Name);
            } else return item1.getTimestamp().compareTo(item2.getTimestamp());

        case BUCKET:
            Tag i1Bucket = null;
            for (Tag i1Tag : item1.tags) {
                if (i1Tag.type == TagType.BUCKET) {
                    i1Bucket = i1Tag;
                }
            }

            Tag i2Bucket = null;
            for (Tag i2Tag : item2.tags) {
                if (i2Tag.type == TagType.BUCKET) {
                    i2Bucket = i2Tag;
                }
            }

            if (i1Bucket == null && i2Bucket == null) {
                return 0;
            } else if (i1Bucket == null && i2Bucket != null) {
                return 1;
            } else if (i2Bucket == null && i1Bucket != null) {
                return -1;
            } else if(!i1Bucket.equals(i2Bucket)){
                return i1Bucket.compareTo(i2Bucket);
            } else return item1.getTimestamp().compareTo(item2.getTimestamp());

        case TIMESTAMP:
        default:
            return item1.getTimestamp().compareTo(item2.getTimestamp());
        }
    }

}
