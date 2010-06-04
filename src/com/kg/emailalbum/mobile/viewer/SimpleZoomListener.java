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

import android.view.MotionEvent;
import android.view.View;

public class SimpleZoomListener implements View.OnTouchListener {

    public enum ControlType {
        PAN, ZOOM
    }


    private ControlType mControlType = ControlType.ZOOM;

    private float mX;
    private float mY;

    /** Zoom control to manipulate */
    private BasicZoomControl mZoomControl;

    /** X-coordinate of latest down event */
    private float mDownX;

    /** Y-coordinate of latest down event */
    private float mDownY;

    /**
     * Sets the zoom control to manipulate
     * 
     * @param control
     *            Zoom control
     */
    public void setZoomControl(BasicZoomControl control) {
        mZoomControl = control;
    }

    public void setControlType(ControlType controlType) {
        mControlType = controlType;
    }

    public ControlType getControlType() {
        return mControlType;
    }

    public boolean onTouch(View v, MotionEvent event) {
        final int action = event.getAction();
        final float x = event.getX();
        final float y = event.getY();

        switch (action) {
        case MotionEvent.ACTION_DOWN:
            mDownX = x;
            mDownY = y;
            mX = x;
            mY = y;
            return false;

        case MotionEvent.ACTION_MOVE: {
            final float dx = (x - mX) / v.getWidth();
            final float dy = (y - mY) / v.getHeight();

            if (mControlType == ControlType.ZOOM) {
                mZoomControl.zoom((float) Math.pow(20, -dy), mDownX
                        / v.getWidth(), mDownY / v.getHeight());
            } else {
                mZoomControl.pan(-dx, -dy);
            }

            mX = x;
            mY = y;
            break;
        }

        }

        return true;
    }



}
