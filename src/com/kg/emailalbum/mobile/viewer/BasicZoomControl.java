/*
 * Copyright (c) 2010, Sony Ericsson Mobile Communication AB. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, 
 * are permitted provided that the following conditions are met:
 *
 *    * Redistributions of source code must retain the above copyright notice, this 
 *      list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above copyright notice,
 *      this list of conditions and the following disclaimer in the documentation
 *      and/or other materials provided with the distribution.
 *    * Neither the name of the Sony Ericsson Mobile Communication AB nor the names
 *      of its contributors may be used to endorse or promote products derived from
 *      this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.kg.emailalbum.mobile.viewer;

import java.util.Observable;
import java.util.Observer;

public class BasicZoomControl implements Observer {

    private static final float MAX_ZOOM = 16;

    private static final float MIN_ZOOM = 1;

    private AspectQuotient mAspectQuotient;

    private final ZoomState mState = new ZoomState();

    private float getMaxPanDelta(float zoom) {
        return Math.max(0f, .5f * ((zoom - 1) / zoom));
    }

    public ZoomState getZoomState() {
        return mState;
    }

    private void limitPan() {
        final float aspectQuotient = mAspectQuotient.get();

        final float zoomX = mState.getZoomX(aspectQuotient);
        final float zoomY = mState.getZoomY(aspectQuotient);

        final float panMinX = .5f - getMaxPanDelta(zoomX);
        final float panMaxX = .5f + getMaxPanDelta(zoomX);
        final float panMinY = .5f - getMaxPanDelta(zoomY);
        final float panMaxY = .5f + getMaxPanDelta(zoomY);

        if (mState.getPanX() > panMaxX) {
            mState.setPanX(panMaxX);
        }
        
        if (mState.getPanX() < panMinX) {
            mState.setPanX(panMinX);
        }
        
        if (mState.getPanY() > panMaxY) {
            mState.setPanY(panMaxY);
        }

        if (mState.getPanY() < panMinY) {
            mState.setPanY(panMinY);
        }
    }

    private void limitZoom() {
        if (mState.getZoom() > MAX_ZOOM) {
            mState.setZoom(MAX_ZOOM);
        }
        
        if (mState.getZoom() < MIN_ZOOM) {
            mState.setZoom(MIN_ZOOM);
        }
        
    }

    public void pan(float dx, float dy) {
        final float aspectQuotient = mAspectQuotient.get();

        mState.setPanX(mState.getPanX() + dx / mState.getZoomX(aspectQuotient));
        mState.setPanY(mState.getPanY() + dy / mState.getZoomY(aspectQuotient));

        limitPan();

        mState.notifyObservers();
    }

    public void setAspectQuotient(AspectQuotient aspectQuotient) {
        if (mAspectQuotient != null) {
            mAspectQuotient.deleteObserver(this);
        }

        mAspectQuotient = aspectQuotient;
        mAspectQuotient.addObserver(this);
    }

    public void update(Observable observable, Object data) {
        limitZoom();
        limitPan();
    }

    public void zoom(float f, float x, float y) {
        final float aspectQuotient = mAspectQuotient.get();

        final float prevZoomX = mState.getZoomX(aspectQuotient);
        final float prevZoomY = mState.getZoomY(aspectQuotient);

        mState.setZoom(mState.getZoom() * f);
        limitZoom();

        final float newZoomX = mState.getZoomX(aspectQuotient);
        final float newZoomY = mState.getZoomY(aspectQuotient);

        mState.setPanX(mState.getPanX() + (x - .5f)
                * (1f / prevZoomX - 1f / newZoomX));
        mState.setPanY(mState.getPanY() + (y - .5f)
                * (1f / prevZoomY - 1f / newZoomY));

        limitPan();

        mState.notifyObservers();
    }
    
    public boolean isZoomed() {
        return mState.isZoomed();
    }

}
