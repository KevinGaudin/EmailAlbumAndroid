package com.kg.emailalbum.mobile.viewer;

import java.util.Observable;

public class ZoomState extends Observable {

    private float mZoom;
    private float mPanX;
    private float mPanY;

    public ZoomState() {
        super();
        reset();
    }

    public float getPanX() {
        return mPanX;
    }

    public float getPanY() {
        return mPanY;
    }

    public float getZoom() {
        return mZoom;
    }

    public void setPanX(float panX) {
        if (panX != mPanX) {
            mPanX = panX;
            setChanged();
        }
    }

    public void setPanY(float panY) {
        if (panY != mPanY) {
            mPanY = panY;
            setChanged();
        }
    }

    public void setZoom(float zoom) {
        if (zoom != mZoom) {
            mZoom = zoom;
            setChanged();
        }
    }

    public float getZoomX(float aspectQuotient) {
        return Math.min(mZoom, mZoom * aspectQuotient);
    }

    public float getZoomY(float aspectQuotient) {
        return Math.min(mZoom, mZoom / aspectQuotient);
    }
    
    public void reset() {
        setPanX(0.5f);
        setPanY(0.5f);
        setZoom(1f);
        notifyObservers();
    }
}
