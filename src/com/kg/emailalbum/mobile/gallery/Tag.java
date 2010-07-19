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

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return type + "/" + label;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
        result = prime * result + ((label == null) ? 0 : label.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Tag other = (Tag) obj;
        if (id != other.id)
            return false;
        if (label == null) {
            if (other.label != null)
                return false;
        } else if (!label.equals(other.label))
            return false;
        if (type != other.type)
            return false;
        return true;
    }

}
