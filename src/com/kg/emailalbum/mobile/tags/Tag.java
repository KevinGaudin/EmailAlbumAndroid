package com.kg.emailalbum.mobile.tags;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.kg.emailalbum.mobile.R;
import com.kg.emailalbum.mobile.util.BitmapUtil;

public class Tag implements Comparable<Tag> {
    public long id;
    public String label;
    public TagType type;

    public enum TagType {
        BUCKET {
            @Override
            public Drawable getDrawable(Context ctx) {
                return initDrawable(ctx, this, R.drawable.ic_folder_camera);
            }

            @Override
            public Integer getSortOrder() {
                return 0;
            }
        },
        USER {
            @Override
            public Drawable getDrawable(Context ctx) {
                return initDrawable(ctx, this, R.drawable.ic_tag);
            }

            @Override
            public Integer getSortOrder() {
                return 1;
            }
        },
        PEOPLE {
            @Override
            public Drawable getDrawable(Context ctx) {
                return initDrawable(ctx, this, R.drawable.ic_people);
            }

            @Override
            public Integer getSortOrder() {
                return 2;
            }
        },
        MONTH {
            @Override
            public Drawable getDrawable(Context ctx) {
                return initDrawable(ctx, this,
                        R.drawable.ic_calendar_view_month);
            }

            @Override
            public Integer getSortOrder() {
                return 3;
            }
        },
        YEAR {
            @Override
            public Drawable getDrawable(Context ctx) {
                return initDrawable(ctx, this,
                        R.drawable.ic_calendar_view_month);
            }

            @Override
            public Integer getSortOrder() {
                return 4;
            }
        },
        TIMESTAMP {
            @Override
            public Drawable getDrawable(Context ctx) {
                return null;
            }

            @Override
            public Integer getSortOrder() {
                return 99;
            }
            
            @Override
            public boolean showToUser() {
                return false;
            }
        },
        NAME {
            @Override
            public Drawable getDrawable(Context ctx) {
                return null;
            }

            @Override
            public Integer getSortOrder() {
                return 99;
            }

            @Override
            public boolean showToUser() {
                return false;
            }
        };

        private static final int TAG_ICON_SIZE = 16;
        private static final Map<TagType, Drawable> mDrawables = new HashMap<TagType, Drawable>();

        public abstract Drawable getDrawable(Context ctx);

        public abstract Integer getSortOrder();

        /**
         * @param ctx
         * @return
         */
        private static Drawable initDrawable(Context ctx, TagType tagType,
                int drawableId) {
            Drawable drawable = mDrawables.get(tagType);
            if (drawable == null) {
                drawable = ctx.getResources().getDrawable(drawableId);
                drawable.setBounds(0, 0,
                        (int) (TAG_ICON_SIZE * BitmapUtil.getDensity(ctx)),
                        (int) (TAG_ICON_SIZE * BitmapUtil.getDensity(ctx)));
                mDrawables.put(tagType, drawable);
            }
            return drawable;
        }

        public boolean showToUser() {
            return true;
        }

    }

    public Tag(long id, String label, TagType type) {
        this.id = id;
        this.label = label;
        this.type = type;
    }

    @Override
    public int compareTo(Tag anotherTag) {
        if (this.type.getSortOrder() != anotherTag.type.getSortOrder()) {
            return this.type.getSortOrder().compareTo(
                    anotherTag.type.getSortOrder());
        }
        return this.label.compareTo(anotherTag.label);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return type + "/" + label;
    }

    /*
     * (non-Javadoc)
     * 
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

    /*
     * (non-Javadoc)
     * 
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
