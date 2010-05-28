package com.kg.emailalbum.mobile.viewer;

import java.util.Observable;

public class AspectQuotient extends Observable {

    private float mAspectQuotient;

    public float get() {
        return mAspectQuotient;
    }

    public void updateAspectQuotient(float viewWidth, float viewHeight, float contentWidth,
            float contentHeight) {
        final float aspectQuotient = (contentWidth / contentHeight) / (viewWidth / viewHeight);

        if (aspectQuotient != mAspectQuotient) {
            mAspectQuotient = aspectQuotient;
            setChanged();
        }
    }
}
