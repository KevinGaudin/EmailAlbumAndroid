package com.kg.emailalbum.mobile.gallery;

public class Tag implements Comparable<Object>{
    public long id;
    public String label;
    public TagType type;

    public enum TagType {
        BUCKET, USER, PEOPLE, MONTH, YEAR;
    }

    public Tag(long id, String label, TagType type) {
        this.id = id;
        this.label = label;
        this.type = type;
    }

    @Override
    public int compareTo(Object another) {
        return this.label.compareTo(((Tag)another).label);
    }

}
