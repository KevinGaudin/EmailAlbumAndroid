/**
 * Copyright 2009, 2010 Kevin Gaudin
 *
 * This file is part of EmailAlbum.
 *
 * EmailAlbum is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * EmailAlbum is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with EmailAlbum.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.kg.emailalbum.mobile.viewer;

import java.util.Observable;
import java.util.Observer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class ImageZoomView extends ImageView implements Observer {

    private ZoomState mZoomState;
    private final Paint mPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Rect mRectSrc = new Rect();
    private final Rect mRectDst = new Rect();
    private AspectQuotient mAspectQuotient = new AspectQuotient();

    public ImageZoomView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setZoomState(new ZoomState());
    }

    public void setZoomState(ZoomState state) {
        if (mZoomState != null) {
            mZoomState.deleteObserver(this);
        }

        mZoomState = state;
        if (mZoomState != null) {
            mZoomState.addObserver(this);
        }

        invalidate();
    }

    public ZoomState getZoomState() {
        return mZoomState;
    }

    public void update(Observable observable, Object data) {
        if (isShown()) {
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getDrawable() != null && getDrawable() instanceof BitmapDrawable) {
            if (mZoomState != null) {
                Bitmap bitmap = ((BitmapDrawable) getDrawable()).getBitmap();
                if (bitmap != null) {
                    final int viewWidth = getWidth();
                    final int viewHeight = getHeight();
                    final int bitmapWidth = bitmap.getWidth();
                    final int bitmapHeight = bitmap.getHeight();

                    final float panX = mZoomState.getPanX();
                    final float panY = mZoomState.getPanY();
                    final float zoomX = mZoomState.getZoomX(mAspectQuotient
                            .get()) * viewWidth / bitmapWidth;
                    final float zoomY = mZoomState.getZoomY(mAspectQuotient
                            .get()) * viewHeight / bitmapHeight;

                    // Setup source and destination rectangles
                    mRectSrc.left = (int) (panX * bitmapWidth - viewWidth
                            / (zoomX * 2));
                    mRectSrc.top = (int) (panY * bitmapHeight - viewHeight
                            / (zoomY * 2));
                    mRectSrc.right = (int) (mRectSrc.left + viewWidth / zoomX);
                    mRectSrc.bottom = (int) (mRectSrc.top + viewHeight / zoomY);
                    mRectDst.left = getLeft();
                    mRectDst.top = getTop();
                    mRectDst.right = getRight();
                    mRectDst.bottom = getBottom();

                    // Adjust source rectangle so that it fits within the source
                    // image.
                    if (mRectSrc.left < 0) {
                        mRectDst.left += -mRectSrc.left * zoomX;
                        mRectSrc.left = 0;
                    }
                    if (mRectSrc.right > bitmapWidth) {
                        mRectDst.right -= (mRectSrc.right - bitmapWidth)
                                * zoomX;
                        mRectSrc.right = bitmapWidth;
                    }
                    if (mRectSrc.top < 0) {
                        mRectDst.top += -mRectSrc.top * zoomY;
                        mRectSrc.top = 0;
                    }
                    if (mRectSrc.bottom > bitmapHeight) {
                        mRectDst.bottom -= (mRectSrc.bottom - bitmapHeight)
                                * zoomY;
                        mRectSrc.bottom = bitmapHeight;
                    }

                    canvas.drawBitmap(bitmap, mRectSrc, mRectDst, mPaint);
                }
            }
        } else {
            super.draw(canvas);
        }
    }

    private void calculateAspectQuotient() {
        BitmapDrawable drawable = (BitmapDrawable) getDrawable();
        if (drawable != null) {
            Bitmap bitmap = ((BitmapDrawable) getDrawable()).getBitmap();
            if (bitmap != null) {
                mAspectQuotient.updateAspectQuotient(getWidth(), getHeight(),
                        bitmap.getWidth(), bitmap.getHeight());
                mAspectQuotient.notifyObservers();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.widget.ImageView#setImageBitmap(android.graphics.Bitmap)
     */
    @Override
    public void setImageBitmap(Bitmap bm) {
        // TODO Auto-generated method stub
        super.setImageBitmap(bm);
        calculateAspectQuotient();
        invalidate();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
            int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        calculateAspectQuotient();
    }

    public AspectQuotient getAspectQuotient() {
        return mAspectQuotient;
    }

}
