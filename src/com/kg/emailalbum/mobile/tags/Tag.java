package com.kg.emailalbum.mobile.tags;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.kg.emailalbum.mobile.R;

public class Tag implements Comparable<Object> {
    public long id;
    public String label;
    public TagType type;

    public enum TagType {
        BUCKET {
            @Override
            public Drawable getDrawable(Context ctx) {
                initMetrics(ctx);
                Drawable bucketDrawable = ctx.getResources().getDrawable(
                        R.drawable.ic_folder_camera);
                bucketDrawable.setBounds(0, 0,
                        (int) (TAG_ICON_SIZE * mMetrics.density),
                        (int) (TAG_ICON_SIZE * mMetrics.density));
                return bucketDrawable;
            }

            @Override
            public Integer getSortOrder() {
                return 0;
            }
        },
        USER {
            @Override
            public Drawable getDrawable(Context ctx) {
                initMetrics(ctx);
                Drawable userDrawable = ctx.getResources().getDrawable(
                        R.drawable.ic_tag);
                userDrawable.setBounds(0, 0,
                        (int) (TAG_ICON_SIZE * mMetrics.density),
                        (int) (TAG_ICON_SIZE * mMetrics.density));
                return userDrawable;
            }

            @Override
            public Integer getSortOrder() {
                return 1;
            }
        },
        PEOPLE {
            @Override
            public Drawable getDrawable(Context ctx) {
                initMetrics(ctx);
                Drawable peopleDrawable = ctx.getResources().getDrawable(
                        R.drawable.ic_people);
                peopleDrawable.setBounds(0, 0,
                        (int) (TAG_ICON_SIZE * mMetrics.density),
                        (int) (TAG_ICON_SIZE * mMetrics.density));
                return peopleDrawable;
            }

            @Override
            public Integer getSortOrder() {
                return 2;
            }
        },
        MONTH {
            @Override
            public Drawable getDrawable(Context ctx) {
                initMetrics(ctx);
                Drawable monthDrawable = ctx.getResources().getDrawable(
                        R.drawable.ic_calendar_view_month);
                monthDrawable.setBounds(0, 0,
                        (int) (TAG_ICON_SIZE * mMetrics.density),
                        (int) (TAG_ICON_SIZE * mMetrics.density));
                return monthDrawable;
            }

            @Override
            public Integer getSortOrder() {
                return 3;
            }
        },
        YEAR {
            @Override
            public Drawable getDrawable(Context ctx) {
                initMetrics(ctx);
                Drawable yearDrawable = ctx.getResources().getDrawable(
                        R.drawable.ic_calendar_view_month);
                yearDrawable.setBounds(0, 0,
                        (int) (TAG_ICON_SIZE * mMetrics.density),
                        (int) (TAG_ICON_SIZE * mMetrics.density));
                return yearDrawable;
            }

            @Override
            public Integer getSortOrder() {
                return 4;
            }
        };

        private static final int TAG_ICON_SIZE = 16;
        private static DisplayMetrics mMetrics;

        public abstract Drawable getDrawable(Context ctx);

        public abstract Integer getSortOrder();

        /**
         * @param ctx
         */
        private static void initMetrics(Context ctx) {
            if (mMetrics == null) {
                mMetrics = new DisplayMetrics();
                ((WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE))
                        .getDefaultDisplay().getMetrics(mMetrics);
            }
        }
    }

    public Tag(long id, String label, TagType type) {
        this.id = id;
        this.label = label;
        this.type = type;
    }

    @Override
    public int compareTo(Object another) {
        Tag anotherTag = (Tag) another;
        if(this.type.getSortOrder() != anotherTag.type.getSortOrder()) {
            return this.type.getSortOrder().compareTo(anotherTag.type.getSortOrder());
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
