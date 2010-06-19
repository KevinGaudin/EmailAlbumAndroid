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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.acra.ErrorReporter;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.Media;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.kg.emailalbum.mobile.AboutDialog;
import com.kg.emailalbum.mobile.EmailAlbumPreferences;
import com.kg.emailalbum.mobile.R;
import com.kg.emailalbum.mobile.animation.Rotate3dAnimation;
import com.kg.emailalbum.mobile.util.CacheManager;
import com.kg.emailalbum.mobile.util.Compatibility;
import com.kg.emailalbum.mobile.util.ScaleGestureDetector;
import com.kg.emailalbum.mobile.util.ScaleGestureDetector.OnScaleGestureListener;
import com.kg.oifilemanager.filemanager.FileManagerProvider;
import com.kg.oifilemanager.intents.FileManagerIntents;

/**
 * A slideshow of pictures taken from an archive (zip or jar).
 * 
 * @author Kevin Gaudin
 * 
 */
public class ShowPics extends Activity implements OnGestureListener,
        OnSharedPreferenceChangeListener, OnDoubleTapListener,
        OnScaleGestureListener {
    private static final float DOUBLE_TAP_ZOOM_FACTOR = 2.0f;
    private static final double ACTIVATE_ZOOM_THRESHOLD = 10.0;
    private static final float Z_TRANSLATE_3D = 1000.0f;
    private final static int MENU_SET_AS_ID = 1;
    private static final int MENU_SAVE_ID = 2;
    private static final int MENU_SEND_ID = 3;
    private static final int MENU_OPEN_WITH_ID = 4;
    private static final int MENU_EDIT_ID = 5;
    private static final int MENU_ABOUT_ID = 6;
    private static final int MENU_SLIDESHOW_ID = 7;
    private static final int MENU_PREFS_ID = 8;

    private static final int BACKWARD = -1;
    private static final int FORWARD = 1;

    private static final int ACTIVITY_PICK_DIRECTORY_TO_SAVE = 0;
    private static final int DEFAULT_SLIDESHOW_TIME = 5;
    private static final String LOG_TAG = ShowPics.class.getSimpleName();

    /**
     * 3 ImageViews to handle the previous, current and next picture. The index
     * of each view is not constant, there is a rotation over the three views
     * roles during the slideshow.
     * */
    private ImageZoomView[] mImgViews = new ImageZoomView[3];
    private SlideshowItem[] mItems = new SlideshowItem[3];
    /**
     * These 3 variables will be updated each time we flip views. so,
     * mImgViews[curPic] will always be the current display pictures,
     * mImgViews[prevPic] will always be the previous picture and
     * mImgViews[nextPic] will always be the next picture.
     * */
    private int prevPic, curPic, nextPic;
    private ViewFlipper mFlipper;
    private SlideshowList mSlideshowList;
    private int mPosition;
    private int mOldPosition;
    private int mLatestMove = FORWARD;
    /** To catch gestures (mostly flings) */
    private GestureDetector mGestureScanner;
    private ScaleGestureDetector mScaleGestureScanner;
    private Animation mAnimNextIn;
    private Animation mAnimCurrentFwdOut;
    private Animation mAnimCurrentBwdOut;
    private Animation mAnimPreviousIn;
    private boolean mIsFlipping = false;
    // Not Enough Memory Mode (disables multiple pictures loading and
    // animations)
    private boolean mNEMMode = false;
    private boolean mSlideshow = false;
    private PowerManager.WakeLock mWakeLock;
    private SharedPreferences mPrefs;
    private long mSlideshowPeriod = DEFAULT_SLIDESHOW_TIME * 1000;
    private boolean mSlideshowLoop = false;
    private boolean mSlideshowRandomAnim = false;
    private Toast mLatestCaption = null;

    // Zoom handling
    private boolean mZoomMode = false;
    private boolean mMTZoomMode = false;
    private boolean mPanMode = false;
    private SimpleZoomListener mZoomListener = new SimpleZoomListener();
    private BasicZoomControl mZoomControl = new BasicZoomControl();
    Handler mHandler = new Handler();
    private PointF mDownPoint = new PointF(0, 0);
    private PointF mLatestPoint = new PointF(0, 0);

    /**
     * This will be used to trigger the automatic picture change while in
     * slideshow mode.
     */
    private Runnable slideshowCallback = new Runnable() {

        @Override
        public void run() {
            goForward();
        }

    };

    /**
     * Listen to the end of the animation to trigger pictures loading.
     */
    private Animation.AnimationListener animListener = new Animation.AnimationListener() {

        /*
         * (non-Javadoc)
         * 
         * @see
         * android.view.animation.Animation.AnimationListener#onAnimationEnd
         * (android.view.animation.Animation)
         */
        @Override
        public void onAnimationEnd(Animation animation) {
            mFlipper.postDelayed(new Runnable() {

                @Override
                public void run() {
                    resetViews();
                    mIsFlipping = false;
                }
            }, 10); // Added 10ms to work around an android bug... without this,
            // the animation is interrupted a few frames before its end.
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * android.view.animation.Animation.AnimationListener#onAnimationRepeat
         * (android.view.animation.Animation)
         */
        @Override
        public void onAnimationRepeat(Animation animation) {
            // NOT USED
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * android.view.animation.Animation.AnimationListener#onAnimationStart
         * (android.view.animation.Animation)
         */
        @Override
        public void onAnimationStart(Animation animation) {
            // NOT USED
        }
    };

    /**
     * Starts the pictures transition animation.
     * 
     * @param direction
     *            {@link #BACKWARD} or {@link #FORWARD}
     */
    private void applyTransition(int direction) {
        if (mSlideshow && mSlideshowRandomAnim) {
            initAnim();
        }
        switch (direction) {
        case FORWARD:
            // If the previous move was not in the same directon
            // we have to apply the correct animations for this move.
            if (mLatestMove != FORWARD) {
                mFlipper.setInAnimation(mAnimNextIn);
                mFlipper.setOutAnimation(mAnimCurrentFwdOut);
            }
            mFlipper.showNext();
            mLatestMove = FORWARD;

            break;
        case BACKWARD:
            // If the previous move was not in the same directon
            // we have to apply the correct animations for this move.
            if (mLatestMove != BACKWARD) {
                mFlipper.setInAnimation(mAnimPreviousIn);
                mFlipper.setOutAnimation(mAnimCurrentBwdOut);
            }
            mFlipper.showPrevious();
            mLatestMove = BACKWARD;
            break;
        }
    }

    /**
     * Display previous picture.
     */
    private void goBack() {
        if (!mIsFlipping) {
            mIsFlipping = true;
            mOldPosition = mPosition;
            mPosition = Math.max(0, mPosition - 1); // Don't go below 0
            if (mOldPosition != mPosition) {
                showPicture();
            } else {
                // We did not move so we were in first position.
                // Tell the user.
                toastFirstOrLast();
                mIsFlipping = false;
            }
        }
        // Going backwards stops any running slideshow
        mSlideshow = false;
        setWakeLock(mSlideshow);
        mHandler.removeCallbacks(slideshowCallback);
    }

    /**
     * Display next picture.
     */
    private void goForward() {
        if (!mIsFlipping) {
            mIsFlipping = true;
            mOldPosition = mPosition;
            int lastPos = mSlideshowList.size() - 1;
            if (mPosition == lastPos && mSlideshow && mSlideshowLoop) {
                // Reached the end in slideshow looping mode, restart from the
                // beginning
                mPosition = 0;
                mOldPosition = -2; // Why not -2 ???
            } else {
                // Don't go too far
                mPosition = Math.min(lastPos, mPosition + 1);
            }
            if (mOldPosition != mPosition) {
                showPicture();
            } else {
                // We did not move, so this was the last picture.
                toastFirstOrLast();
                mIsFlipping = false;
                stopSlideshow();
            }
        }
    }

    /**
     * Creates the Animation objects that will be used by the ViewFlipper to
     * animate pictures transitions.
     */
    private void initAnim() {
        String animType = mPrefs.getString("animationtype",
                getString(R.string.pref_def_anim));

        if (mSlideshow && mSlideshowRandomAnim) {
            // If in slideshow mode and with random transition selected by user,
            // let's pick a random animation.
            String[] values = getResources().getStringArray(
                    R.array.animationsvalues);
            animType = values[(int) (Math.random() * (values.length - 1))];
        }

        // Launch the corresponding init method
        if ("translate_both".equals(animType)) {
            initAnimTranslateBoth();
        } else if ("translate_new".equals(animType)) {
            initAnimTranslateNew();
        } else if ("fade".equals(animType)) {
            initAnimFade();
        } else if ("pivot".equals(animType)) {
            initAnimPivot();
        } else if ("spin".equals(animType)) {
            initAnimSpin();
        } else if ("carrousel".equals(animType)) {
            initAnimCarrousel();
        }

        // Reset the ViewFlipper with the new animations, depending on
        // what was the last move direction.
        if (mLatestMove == FORWARD) {
            mFlipper.setInAnimation(mAnimNextIn);
            mFlipper.setOutAnimation(mAnimCurrentFwdOut);
        } else {
            mFlipper.setInAnimation(mAnimPreviousIn);
            mFlipper.setOutAnimation(mAnimCurrentBwdOut);
        }
        // Might spare some memory ? Didn't see any difference with the
        // different modes.
        mFlipper.setPersistentDrawingCache(ViewGroup.PERSISTENT_NO_CACHE);
    }

    /**
     * Loads Animation objects.
     */
    private void initAnimCarrousel() {
        // Preload all animations and set listener on only one of each in/out
        // pair as they are run simultaneously
        mAnimNextIn = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.carrousel_right_in);
        mAnimNextIn.setAnimationListener(animListener);
        mAnimNextIn.setStartOffset(200);
        mAnimCurrentFwdOut = AnimationUtils.loadAnimation(
                getApplicationContext(), R.anim.carrousel_left_out);
        mAnimPreviousIn = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.carrousel_left_in);
        mAnimPreviousIn.setStartOffset(200);
        mAnimPreviousIn.setAnimationListener(animListener);
        mAnimCurrentBwdOut = AnimationUtils.loadAnimation(
                getApplicationContext(), R.anim.carrousel_right_out);
    }

    /**
     * Loads Animation objects.
     */
    private void initAnimFade() {
        // Preload all animations and set listener on only one of each in/out
        // pair as they are
        // run simultaneously
        mAnimNextIn = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.fade_in);
        mAnimNextIn.setAnimationListener(animListener);
        mAnimCurrentFwdOut = AnimationUtils.loadAnimation(
                getApplicationContext(), R.anim.fade_out);
        mAnimPreviousIn = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.fade_in);
        mAnimPreviousIn.setAnimationListener(animListener);
        mAnimCurrentBwdOut = mAnimCurrentFwdOut;
    }

    /**
     * Loads Animation objects.
     */
    private void initAnimPivot() {
        // Preload all animations and set listener on only one of each in/out
        // pair as they are
        // run simultaneously
        mAnimNextIn = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.rotate_left_in);
        mAnimNextIn.setAnimationListener(animListener);
        mAnimCurrentFwdOut = AnimationUtils.loadAnimation(
                getApplicationContext(), R.anim.rotate_left_out);
        mAnimPreviousIn = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.rotate_right_in);
        mAnimPreviousIn.setAnimationListener(animListener);
        mAnimCurrentBwdOut = AnimationUtils.loadAnimation(
                getApplicationContext(), R.anim.rotate_right_out);

    }

    /**
     * Loads Animation objects.
     */
    private void initAnimSpin() {
        // Preload all animations and set listener on only one of each in/out
        // pair as they are
        // run simultaneously
        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();

        float centerX = display.getWidth() / 2.0f;
        float centerY = display.getHeight() / 2.0f;
        mAnimNextIn = new Rotate3dAnimation(90, 0, centerX, centerY,
                Z_TRANSLATE_3D, false);
        mAnimNextIn.setDuration(500);
        mAnimNextIn.setFillAfter(false);
        mAnimNextIn.setFillBefore(true);
        mAnimNextIn.setStartOffset(500);
        mAnimNextIn.setInterpolator(new DecelerateInterpolator());
        mAnimNextIn.setAnimationListener(animListener);

        mAnimCurrentFwdOut = new Rotate3dAnimation(0, -90, centerX, centerY,
                Z_TRANSLATE_3D, true);
        mAnimCurrentFwdOut.setDuration(500);
        mAnimCurrentFwdOut.setFillAfter(false);
        mAnimCurrentFwdOut.setFillBefore(true);
        mAnimCurrentFwdOut.setInterpolator(new AccelerateInterpolator());

        mAnimPreviousIn = new Rotate3dAnimation(-90, 0, centerX, centerY,
                Z_TRANSLATE_3D, false);
        mAnimPreviousIn.setDuration(500);
        mAnimPreviousIn.setFillAfter(false);
        mAnimPreviousIn.setFillBefore(true);
        mAnimPreviousIn.setStartOffset(500);
        mAnimPreviousIn.setInterpolator(new DecelerateInterpolator());
        mAnimPreviousIn.setAnimationListener(animListener);

        mAnimCurrentBwdOut = new Rotate3dAnimation(0, 90, centerX, centerY,
                Z_TRANSLATE_3D, true);
        mAnimCurrentBwdOut.setDuration(500);
        mAnimCurrentBwdOut.setFillAfter(false);
        mAnimCurrentBwdOut.setFillBefore(true);
        mAnimCurrentBwdOut.setInterpolator(new AccelerateInterpolator());
    }

    /**
     * Loads Animation objects.
     */
    private void initAnimTranslateBoth() {
        // Preload all animations and set listener on only one of each in/out
        // pair as they are
        // run simultaneously
        mAnimNextIn = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.push_left_in);
        mAnimNextIn.setAnimationListener(animListener);
        mAnimCurrentFwdOut = AnimationUtils.loadAnimation(
                getApplicationContext(), R.anim.push_left_out);
        mAnimPreviousIn = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.push_right_in);
        mAnimPreviousIn.setAnimationListener(animListener);
        mAnimCurrentBwdOut = AnimationUtils.loadAnimation(
                getApplicationContext(), R.anim.push_right_out);
    }

    /**
     * Loads Animation objects.
     */
    private void initAnimTranslateNew() {
        // Preload all animations and set listener on only one of each in/out
        // pair as they are
        // run simultaneously
        mAnimNextIn = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.push_left_in);
        mAnimNextIn.setAnimationListener(animListener);
        mAnimCurrentFwdOut = AnimationUtils.loadAnimation(
                getApplicationContext(), R.anim.push_back);
        mAnimPreviousIn = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.pull_front);
        mAnimPreviousIn.setAnimationListener(animListener);
        mAnimCurrentBwdOut = AnimationUtils.loadAnimation(
                getApplicationContext(), R.anim.push_right_out);
    }

    /**
     * Loads Animation objects.
     */
    private void initSlideshow() {
        String prefPeriod = mPrefs.getString("slideshow_period", ""
                + DEFAULT_SLIDESHOW_TIME);
        if (prefPeriod == null || "".equals(prefPeriod.trim())) {
            prefPeriod = "" + DEFAULT_SLIDESHOW_TIME;
        }
        mSlideshowPeriod = Long.parseLong(prefPeriod) * 1000;
        mSlideshowLoop = mPrefs.getBoolean("slideshow_loop", mSlideshowLoop);
        mSlideshowRandomAnim = mPrefs.getBoolean("slideshow_random_animation",
                mSlideshowRandomAnim);
    }

    /**
     * Loads one picture.
     */
    private SlideshowItem loadPicture(int position) {
        SlideshowItem result = mSlideshowList.get(position);
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onActivityResult(int, int,
     * android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
        case ACTIVITY_PICK_DIRECTORY_TO_SAVE:
            if (resultCode == RESULT_OK && data != null) {
                // obtain the filename
                String dirname = data.getDataString();
                try {
                    savePicture(new File(Uri.parse(dirname).getEncodedPath()));
                } catch (IOException e) {
                    Log.e(this.getClass().getName(),
                            "onActivityResult() exception", e);
                    Toast.makeText(this, e.getLocalizedMessage(),
                            Toast.LENGTH_SHORT).show();
                }

            }
            break;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get a full-screen window
        final Window win = getWindow();
        win.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(Compatibility.getShowPicsLayout());

        // Prevent device from sleeping during slideshow
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                + PowerManager.ON_AFTER_RELEASE, "EmailAlbum.SlideShow");

        // Zoom handling
        mZoomListener.setZoomControl(mZoomControl);

        // Initialize our 3 ImageViews and positions.
        curPic = 0;
        mImgViews[curPic] = (ImageZoomView) findViewById(R.id.pic0);
        nextPic = 1;
        mImgViews[nextPic] = (ImageZoomView) findViewById(R.id.pic1);
        prevPic = 2;
        mImgViews[prevPic] = (ImageZoomView) findViewById(R.id.pic2);
        mFlipper = (ViewFlipper) findViewById(R.id.flipper);

        // Get notifications when preferences are updated
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mPrefs.registerOnSharedPreferenceChangeListener(this);

        initAnim();
        initSlideshow();

        // Handles flings (up / down : rotate picture, left : next, right :
        // previous
        mGestureScanner = new GestureDetector(this);
        mGestureScanner.setOnDoubleTapListener(this);
        mGestureScanner.setIsLongpressEnabled(true);
        // Handles multitouch zoom

        mScaleGestureScanner = Compatibility
                .getScaleGestureDetector(this, this);

        mOldPosition = -1;

        Uri albumUri = null;
        if (getIntent() != null) {
            // retrieve all data provided by EmailAlbumViewer
            albumUri = getIntent().getParcelableExtra("ALBUM");
            mPosition = getIntent().getIntExtra("POSITION", 0);
        }

        if (savedInstanceState != null) {
            albumUri = (Uri) savedInstanceState.getParcelable("ALBUM");
            mPosition = savedInstanceState.getInt("POSITION");
            mSlideshow = savedInstanceState.getBoolean("SLIDESHOW");
            setWakeLock(mSlideshow);
        }

        if (albumUri != null) {
            if (albumUri.equals(Media.EXTERNAL_CONTENT_URI)) {
                mSlideshowList = new GallerySlideshowList(getApplicationContext(), 900);
            } else {
                mSlideshowList = new ArchiveSlideshowList(
                        getApplicationContext(), albumUri, 900);
            }
        } else {
            ErrorReporter.getInstance().handleException(
                    new Exception("ShowPics invoked without data."));
            finish();
        }
        showPicture();
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        MenuItem item = menu.add(0, MENU_SLIDESHOW_ID, 0,
                R.string.start_slideshow);
        item.setIcon(android.R.drawable.ic_menu_slideshow);
        item = menu.add(0, MENU_SET_AS_ID, 0, R.string.menu_set_as);
        item.setIcon(android.R.drawable.ic_menu_set_as);
        item = menu.add(0, MENU_SEND_ID, 0, R.string.menu_share);
        item.setIcon(android.R.drawable.ic_menu_share);
        item = menu.add(0, MENU_SAVE_ID, 0, R.string.menu_save);
        item.setIcon(android.R.drawable.ic_menu_save);
        item = menu.add(0, MENU_OPEN_WITH_ID, 0, R.string.menu_open_with);
        item.setIcon(android.R.drawable.ic_menu_view);
        // Display an edit button only if an application is available
        Intent intent = new Intent(Intent.ACTION_EDIT);
        intent.setType("image/jpeg");
        intent.setData(Uri.withAppendedPath(Images.Media.EXTERNAL_CONTENT_URI,
                "1"));
        if (getPackageManager().resolveActivity(intent, 0) != null) {
            item = menu.add(0, MENU_EDIT_ID, 0, R.string.menu_edit);
            item.setIcon(android.R.drawable.ic_menu_edit);
        }
        item = menu.add(0, MENU_PREFS_ID, 0, R.string.menu_prefs);
        item.setIcon(android.R.drawable.ic_menu_preferences);
        item = menu.add(0, MENU_ABOUT_ID, 0, R.string.menu_about);
        item.setIcon(android.R.drawable.ic_menu_help);
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onDestroy()
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        setWakeLock(false);
        for (ImageZoomView view : mImgViews) {
            if (view != null) {
                if (isFinishing()) {
                    Drawable toRecycle = view.getDrawable();
                    if (toRecycle != null) {
                        Bitmap bmpToRecycle = ((BitmapDrawable) toRecycle)
                                .getBitmap();
                        if (bmpToRecycle != null) {
                            bmpToRecycle.recycle();
                        }
                    }
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @seeandroid.view.GestureDetector.OnGestureListener#onDown(android.view.
     * MotionEvent)
     */
    @Override
    public boolean onDown(MotionEvent arg0) {
        // NOT USED
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeandroid.view.GestureDetector.OnGestureListener#onFling(android.view.
     * MotionEvent, android.view.MotionEvent, float, float)
     */
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
            float velocityY) {
        if (Math.abs(velocityX) > Math.abs(velocityY)) {
            // This is an horizontal fling
            if (velocityX < 0) {
                // From left to right => go forward (and cancel a pending
                // automated move)
                mHandler.removeCallbacks(slideshowCallback);
                goForward();
                return true;
            } else if (velocityX > 0) {
                // From right to left => go backward
                goBack();
                return true;
            }
        } else {
            // This is a vertical fling
            float halfWidth = mImgViews[curPic].getWidth() / 2;
            if ((velocityY < 0 && e1.getX() > halfWidth)
                    || (velocityY > 0 && e1.getX() < halfWidth)) {
                // Rotate counter clockwise
                mHandler.removeCallbacks(slideshowCallback);
                rotate(-90);
                return true;
            } else if ((velocityY > 0 && e1.getX() > halfWidth)
                    || (velocityY < 0 && e1.getX() < halfWidth)) {
                // From up to down => rotate clockwise
                mHandler.removeCallbacks(slideshowCallback);
                rotate(90);
                return true;
            }
        }

        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Picture moves can also be triggered by D-Pad (and trackball)
        switch (keyCode) {
        case KeyEvent.KEYCODE_DPAD_LEFT:
            mHandler.removeCallbacks(slideshowCallback);
            goBack();
            return true;
        case KeyEvent.KEYCODE_DPAD_UP:
            mHandler.removeCallbacks(slideshowCallback);
            rotate(-90);
            return true;
        case KeyEvent.KEYCODE_DPAD_RIGHT:
            mHandler.removeCallbacks(slideshowCallback);
            goForward();
            return true;
        case KeyEvent.KEYCODE_DPAD_DOWN:
            mHandler.removeCallbacks(slideshowCallback);
            rotate(90);
            return true;
        case KeyEvent.KEYCODE_DPAD_CENTER:
            openOptionsMenu();
            return true;
        case KeyEvent.KEYCODE_BACK:
            if (mZoomControl.isZoomed()) {
                setZoomMode(false);
                setPanMode(false);
                mZoomControl.getZoomState().reset();
                return true;
            } else {
                return super.onKeyDown(keyCode, event);
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * android.view.GestureDetector.OnGestureListener#onLongPress(android.view
     * .MotionEvent)
     */
    @Override
    public void onLongPress(MotionEvent e) {
        if (!mMTZoomMode
                && (!mPanMode || (Math.abs(mDownPoint.x - mLatestPoint.x) < ACTIVATE_ZOOM_THRESHOLD && Math
                        .abs(mDownPoint.y - mLatestPoint.y) < ACTIVATE_ZOOM_THRESHOLD))) {
            mImgViews[curPic]
                    .performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            setZoomMode(true);
        }
    }

    private void setPanMode(boolean enable) {
        if (!mPanMode && enable) {
            mZoomListener.setControlType(SimpleZoomListener.ControlType.PAN);
            mPanMode = true;
            mZoomMode = false;
        } else if (mPanMode && !enable) {
            mPanMode = false;
        }
    }

    private void setZoomMode(boolean enable) {

        if (!mZoomMode && enable) {
            mZoomListener.setControlType(SimpleZoomListener.ControlType.ZOOM);
            mZoomMode = true;
            setPanMode(false);
        } else if (mZoomMode && !enable) {
            mZoomMode = false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = null;
        switch (item.getItemId()) {
        case MENU_SAVE_ID:
            intent = new Intent(FileManagerIntents.ACTION_PICK_DIRECTORY);
            intent.setData(Uri.fromFile(android.os.Environment
                    .getExternalStorageDirectory()));
            intent.putExtra(FileManagerIntents.EXTRA_TITLE,
                    getText(R.string.select_directory));
            intent.putExtra(FileManagerIntents.EXTRA_BUTTON_TEXT,
                    getText(R.string.btn_select_directory));
            try {
                startActivityForResult(intent, ACTIVITY_PICK_DIRECTORY_TO_SAVE);
            } catch (ActivityNotFoundException e) {
                Log.e(LOG_TAG, "Could not call File Manager !");
            }
            return true;
        case MENU_SEND_ID:
        case MENU_SET_AS_ID:
        case MENU_EDIT_ID:
            try {
                String title = getString(R.string.chooser_share);
                Uri fileUri = FileManagerProvider
                        .getContentUri(saveTmpPicture(new CacheManager(
                                getApplicationContext()).getCacheDir("viewer")));
                if (item.getItemId() == MENU_SEND_ID) {
                    intent = new Intent(Intent.ACTION_SEND, fileUri);
                    intent.putExtra(Intent.EXTRA_STREAM, fileUri);
                    intent.setType("image/jpeg");
                } else if (item.getItemId() == MENU_SET_AS_ID) {
                    intent = new Intent(Intent.ACTION_ATTACH_DATA, fileUri);
                    title = getString(R.string.menu_set_as);
                } else if (item.getItemId() == MENU_EDIT_ID) {
                    intent = new Intent(Intent.ACTION_EDIT);
                    title = getString(R.string.menu_edit);

                    Toast.makeText(getApplicationContext(),
                            R.string.alert_editor, Toast.LENGTH_LONG).show();
                    intent.setDataAndType(fileUri, "image/jpeg");
                }
                startActivity(Intent.createChooser(intent, title));

            } catch (IOException e) {
                Log.e(this.getClass().getName(), "other intents exception", e);
                Toast.makeText(
                        this,
                        getText(R.string.error_other_intents) + " : "
                                + e.getLocalizedMessage(), Toast.LENGTH_SHORT)
                        .show();
            }

            return true;

        case MENU_ABOUT_ID:
            intent = new Intent(this, AboutDialog.class);
            startActivity(intent);
            return true;
        case MENU_PREFS_ID:
            startPreferencesActivity();
            return true;
        case MENU_OPEN_WITH_ID:
            try {
                Uri fileUri = Uri.withAppendedPath(
                        FileManagerProvider.CONTENT_URI, saveTmpPicture(
                                new CacheManager(getApplicationContext())
                                        .getCacheDir("viewer"))
                                .getAbsolutePath());
                Log.d(this.getClass().getSimpleName(), "Open Uri : " + fileUri);
                CharSequence title = null;
                intent = new Intent(Intent.ACTION_VIEW);
                title = getText(R.string.menu_open_with);
                Toast.makeText(getApplicationContext(),
                        R.string.alert_different_viewer, Toast.LENGTH_LONG)
                        .show();
                intent.setDataAndType(fileUri, "image/jpeg");
                startActivity(Intent.createChooser(intent, title));
            } catch (IOException e) {
                Log.e(this.getClass().getSimpleName(),
                        "Error while creating temp file", e);
                Toast.makeText(getApplicationContext(),
                        "Error while creating temp file.", Toast.LENGTH_LONG)
                        .show();
            }
            return true;
        case MENU_SLIDESHOW_ID:
            if (mSlideshow) {
                stopSlideshow();
            } else {
                mSlideshow = true;
                slideshowCallback.run();
            }
            setWakeLock(mSlideshow);
        }
        return super.onOptionsItemSelected(item);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(MENU_SLIDESHOW_ID);
        if (mSlideshow) {
            item.setTitle(R.string.stop_slideshow);
        } else {
            item.setTitle(R.string.start_slideshow);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onRetainNonConfigurationInstance()
     */
    @Override
    public Object onRetainNonConfigurationInstance() {
        Bitmap[] bitmaps = new Bitmap[3];
        Drawable draw = mImgViews[curPic].getDrawable();
        if (draw != null)
            bitmaps[0] = ((BitmapDrawable) draw).getBitmap();
        draw = mImgViews[nextPic].getDrawable();
        if (draw != null)
            bitmaps[1] = ((BitmapDrawable) draw).getBitmap();
        draw = mImgViews[prevPic].getDrawable();
        if (draw != null)
            bitmaps[2] = ((BitmapDrawable) draw).getBitmap();
        return bitmaps;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("POSITION", mPosition);
        outState.putParcelable("ALBUM", mSlideshowList.getAlbumUri());
        outState.putBoolean("SLIDESHOW", mSlideshow);
        setWakeLock(false);
        super.onSaveInstanceState(outState);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * android.view.GestureDetector.OnGestureListener#onScroll(android.view.
     * MotionEvent, android.view.MotionEvent, float, float)
     */
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
            float distanceY) {
        // NOT USED
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeandroid.content.SharedPreferences.OnSharedPreferenceChangeListener#
     * onSharedPreferenceChanged(android.content.SharedPreferences,
     * java.lang.String)
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        if ("animationtype".equals(key)) {
            // User chose a different animaton for transitions
            initAnim();
        } else if ("slideshow_period".equals(key)
                || "slideshow_loop".equals(key)
                || "slideshow_random_animation".equals(key)) {
            // User changed a slideshow parameter
            initSlideshow();
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * android.view.GestureDetector.OnGestureListener#onShowPress(android.view
     * .MotionEvent)
     */
    @Override
    public void onShowPress(MotionEvent e) {
        // Not used
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * android.view.GestureDetector.OnGestureListener#onSingleTapUp(android.
     * view.MotionEvent)
     */
    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onTouchEvent(android.view.MotionEvent)
     */
    @Override
    public boolean onTouchEvent(MotionEvent me) {
        if (mScaleGestureScanner != null) {
            mScaleGestureScanner.onTouchEvent(me);
        }

        // If zoom mode is enabled, the current image view handles touch
        // moves.
        if ((mZoomMode || mPanMode) && me.getAction() == MotionEvent.ACTION_UP) {
            // We disable zoom mode when the user stops touching
            // the screen
            setZoomMode(false);
            setPanMode(false);
            return mGestureScanner.onTouchEvent(me);
        }

        if (mZoomMode) {
            return mZoomListener.onTouch(mImgViews[curPic], me);
        }

        if (me.getAction() == MotionEvent.ACTION_DOWN) {
            mZoomListener.onTouch(mImgViews[curPic], me);
            // Log.d(LOG_TAG, "DownX = " + me.getX() + " / DownY = " +
            // me.getY());
            mDownPoint.x = me.getX();
            mDownPoint.y = me.getY();
            mLatestPoint.x = mDownPoint.x;
            mLatestPoint.y = mDownPoint.y;
            return mGestureScanner.onTouchEvent(me);
        }

        if (!mMTZoomMode && me.getAction() == MotionEvent.ACTION_MOVE) {
            // Log.d(LOG_TAG, "LatestX = " + me.getX() + " / LatestY = " +
            // me.getY());
            mLatestPoint.x = me.getX();
            mLatestPoint.y = me.getY();
            if (mZoomControl.isZoomed()) {
                setPanMode(true);
                return mZoomListener.onTouch(mImgViews[curPic], me);
            } else {
                setPanMode(false);
            }
        }

        // Let the GestureScanner handle touch events
        if (!mMTZoomMode) {
            return mGestureScanner.onTouchEvent(me);
        } else {
            return true;
        }
    }

    /**
     * Called after picture moves, reaffects prev/cur/next positions and
     * preloads the next picture in the direction the user is going, so we
     * always have in memory the previous, current and next picture relative to
     * the current picture position.
     */
    private void resetViews() {
        try {
            if (mOldPosition < mPosition) {
                // Gone forward => rotate positions
                prevPic = (prevPic + 1) % 3;
                curPic = (curPic + 1) % 3;
                nextPic = (nextPic + 1) % 3;
                if (mPosition + 1 < mSlideshowList.size()) {
//                    if (mImgViews[nextPic].getDrawable() != null) {
//                        if (((BitmapDrawable) mImgViews[nextPic].getDrawable())
//                                .getBitmap() != null) {
//                            // nextPic currently contains the picture previous
//                            // to the previous picture. Discard it.
//                            ((BitmapDrawable) mImgViews[nextPic].getDrawable())
//                                    .getBitmap().recycle();
//                        }
//                    }
                    // Preload next picture
                    mItems[nextPic] = loadPicture(mPosition + 1);
                    mImgViews[nextPic].setImageBitmap(mItems[nextPic].bitmap);
                } else if (mSlideshowLoop) {
                    // prepare for looping
//                    if (mImgViews[nextPic].getDrawable() != null) {
//                        if (((BitmapDrawable) mImgViews[nextPic].getDrawable())
//                                .getBitmap() != null) {
//                            ((BitmapDrawable) mImgViews[nextPic].getDrawable())
//                                    .getBitmap().recycle();
//                        }
//                    }
                    // We are at the end of the slideshow, just before looping.
                    // Preload the first picture to prepare looping.
                    mItems[nextPic] = loadPicture(0);
                    mImgViews[nextPic].setImageBitmap(mItems[nextPic].bitmap);
                }
            } else if (mOldPosition > mPosition) {
                // Gone backward
                prevPic = (prevPic == 0) ? 2 : prevPic - 1;
                curPic = (curPic == 0) ? 2 : curPic - 1;
                nextPic = (nextPic == 0) ? 2 : nextPic - 1;
                if (mPosition - 1 >= 0) {
//                    if (mImgViews[prevPic].getDrawable() != null) {
//                        if (((BitmapDrawable) mImgViews[prevPic].getDrawable())
//                                .getBitmap() != null) {
//                            // prevPic now contains the picture next to the next
//                            // picture.
//                            // Discard it.
//                            ((BitmapDrawable) mImgViews[prevPic].getDrawable())
//                                    .getBitmap().recycle();
//                        }
//                    }
                    // Preload previous picture.
                    mItems[prevPic] = loadPicture(mPosition - 1);
                    mImgViews[prevPic].setImageBitmap(mItems[prevPic].bitmap);
                }
            }
        } catch (Exception e) {
            Log.e(this.getClass().getName(), "ResetViews() exception", e);
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT)
                    .show();
        } catch (OutOfMemoryError e) {
            Log.e(this.getClass().getName(), "showPicture() error", e);
            Toast.makeText(this, R.string.error_out_of_mem, Toast.LENGTH_SHORT)
                    .show();
        }

        for (ImageZoomView imgView : mImgViews) {
            if (imgView.getDrawable() != null) {
                imgView.getDrawable().setDither(true);
                imgView.getDrawable().setFilterBitmap(true);
                imgView.setDrawingCacheEnabled(true);
                imgView
                        .setDrawingCacheQuality(ImageView.DRAWING_CACHE_QUALITY_HIGH);
            }
        }
        mZoomControl.getZoomState().reset();
        mImgViews[curPic].setZoomState(mZoomControl.getZoomState());
        mZoomControl.setAspectQuotient(mImgViews[curPic].getAspectQuotient());
    }

    /**
     * Rotate current picture.
     * 
     * @param degrees
     *            Rotation angle in degrees.
     */
    private void rotate(float degrees) {
        try {
            Matrix rotMat = new Matrix();
            rotMat.postRotate(degrees);
            BitmapDrawable curDrawable = (BitmapDrawable) mImgViews[curPic]
                    .getDrawable();
            if (curDrawable != null) {
                Bitmap src = curDrawable.getBitmap();
                if (src != null) {
                    Bitmap dst = Bitmap.createBitmap(src, 0, 0, src.getWidth(),
                            src.getHeight(), rotMat, false);
                    mImgViews[curPic].setImageBitmap(dst);
                    //src.recycle();
                }
            }
        } catch (OutOfMemoryError e) {
            Toast.makeText(this, R.string.error_out_of_mem_rotate,
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Save current picture.
     * 
     * @param destDir
     *            The destination directory.
     * @return The created File.
     * @throws FileNotFoundException
     * @throws IOException
     */
    private File savePicture(File destDir) throws FileNotFoundException,
            IOException {
        File destFile;
        String fileName = mItems[curPic].name;
        int trailSlashIndex = fileName.lastIndexOf('/');
        if (trailSlashIndex >= 0) {
            fileName = fileName.substring(trailSlashIndex);
        }
        destFile = new File(destDir, fileName.toLowerCase());

        OutputStream destFileOS = new FileOutputStream(destFile);
        InputStream imageIS = mSlideshowList.getOriginalInputStream(mPosition);
        byte[] buffer = new byte[2048];
        int len = 0;
        while ((len = imageIS.read(buffer)) >= 0) {
            destFileOS.write(buffer, 0, len);
        }
        destFileOS.close();
        imageIS.close();
        return destFile;
    }

    /**
     * Save current picture in a temporary file, to send it to another
     * application in an Intent.
     * 
     * @param destDir
     *            The destination directory.
     * @return The created File.
     * @throws IOException
     */
    private File saveTmpPicture(File destDir) throws IOException {
        File destFile = null;
        if (destDir != null) {
            if (!destDir.exists()) {
                try {
                    destDir.mkdirs();
                } catch (Exception e) {
                    destDir = null;
                }
            } else if (!destDir.isDirectory()) {
                destDir = null;
            }
        }

        if (destDir == null) {
            destDir = getCacheDir();
        }

        try {
            destFile = savePicture(destDir);
        } catch (IOException e) {
            if (!destDir.equals(getCacheDir())) {
                destFile = savePicture(getCacheDir());
            }
        }
        return destFile;
    }

    /**
     * Toggle the wakeLock.
     * 
     * @param slideshow
     *            True : activate the wakelock. False : deactive the wakelock.
     */
    private void setWakeLock(boolean slideshow) {
        if (slideshow) {
            if (!mWakeLock.isHeld()) {
                mWakeLock.acquire();
            }
        } else {
            if (mWakeLock.isHeld()) {
                mWakeLock.release();
            }
        }
    }

    /**
     * Displays the caption of the current picture. TODO : replace Toast with
     * something more user friendly
     */
    private void showCaption() {
        String caption = mItems[curPic].caption;
        if (caption != null && !"".equals(caption.trim())) {
            if (mLatestCaption != null) {
                mLatestCaption.cancel();
            }

            mLatestCaption = Toast.makeText(getApplicationContext(), caption,
                    Toast.LENGTH_LONG);
            mLatestCaption.show();
        }
    }

    /**
     * Show the picture
     */
    private void showPicture() {
        try {
            if (mOldPosition == -1 || mNEMMode) {
                // First load, initialize all ImageViews from archive
                Object data = getLastNonConfigurationInstance();
                if (data != null) {
                    Bitmap[] bitmaps = (Bitmap[]) data;
                    mImgViews[curPic].setImageBitmap(bitmaps[0]);
                    mImgViews[nextPic].setImageBitmap(bitmaps[1]);
                    mImgViews[prevPic].setImageBitmap(bitmaps[2]);
                } else {
                    // Load first picture
                    if (mImgViews[curPic].getDrawable() != null) {
                        mImgViews[curPic].getDrawable().setCallback(null);
//                        Bitmap toRecycle = ((BitmapDrawable) mImgViews[curPic]
//                                .getDrawable()).getBitmap();
//                        if (toRecycle != null) {
//                            toRecycle.recycle();
//                        }

                    }
                    mItems[curPic] = loadPicture(mPosition);
                    mImgViews[curPic].setImageBitmap(mItems[curPic].bitmap);

                    // If memory is not a problem, preload pictures
                    if (!mNEMMode) {
                        // Load previous picture
                        if (mPosition > 0) {
                            mItems[prevPic] = loadPicture(mPosition - 1);
                            mImgViews[prevPic]
                                    .setImageBitmap(mItems[prevPic].bitmap);
                        }

                        // Load next picture
                        if (mPosition < mSlideshowList.size() - 1) {
                            mItems[nextPic] = loadPicture(mPosition + 1);
                            mImgViews[nextPic]
                                    .setImageBitmap(mItems[nextPic].bitmap);
                        }
                    }
                }
            } else if (mOldPosition < mPosition
                    || ((mOldPosition == mSlideshowList.size() - 1) && mPosition == 0)) {
                // Going forward
                applyTransition(FORWARD);
            } else if (mOldPosition > mPosition) {
                // Going backward
                applyTransition(BACKWARD);
            }

            mImgViews[curPic].setZoomState(mZoomControl.getZoomState().reset());
            mZoomControl.setAspectQuotient(mImgViews[curPic]
                    .getAspectQuotient());
            showCaption();
        } catch (Exception e) {
            Log.e(this.getClass().getName(), "showPicture() exception", e);
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT)
                    .show();
        } catch (OutOfMemoryError e) {
            Log.e(this.getClass().getName(), "showPicture() error", e);
            Toast.makeText(this, R.string.error_out_of_mem, Toast.LENGTH_SHORT)
                    .show();
            if (!mNEMMode
                    && ((BitmapDrawable) mImgViews[curPic].getDrawable())
                            .getBitmap() != null) {
                mNEMMode = true;
                Toast
                        .makeText(this, R.string.enter_nemmode,
                                Toast.LENGTH_SHORT).show();
            } else {
                finish();
            }
        }
        // In slideshow mode, post the next slide action
        if (mSlideshow) {
            mHandler.postDelayed(slideshowCallback, mSlideshowPeriod);
        }
    }

    /**
     * Launch the preferences screen.
     */
    private void startPreferencesActivity() {
        Intent i = new Intent(getApplicationContext(),
                EmailAlbumPreferences.class);
        i.putExtra(EmailAlbumPreferences.EXTRA_SCREEN,
                EmailAlbumPreferences.SCREEN_VIEWER);

        startActivity(i);
    }

    /**
     * Stop the slideshow mode.
     */
    private void stopSlideshow() {
        mSlideshow = false;
        mHandler.removeCallbacks(slideshowCallback);
        initAnim();
        setWakeLock(false);
    }

    /**
     * Displays a message telling the user that he has reached the beginning or
     * the end of the list of pictures.
     */
    private void toastFirstOrLast() {
        if (mPosition == 0) {
            Toast.makeText(this, R.string.first_pic, Toast.LENGTH_SHORT).show();
        } else if (mPosition == mSlideshowList.size() - 1) {
            Toast.makeText(this, R.string.last_pic, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onDoubleTap(MotionEvent me) {
        if (mZoomControl.isZoomed()) {
            mZoomControl.getZoomState().reset();
        } else {
            mZoomControl.zoom(DOUBLE_TAP_ZOOM_FACTOR, me.getX()
                    / mImgViews[curPic].getWidth(), me.getY()
                    / mImgViews[curPic].getHeight());
        }
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        // openOptionsMenu();
        return false;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        if (detector.isInProgress()) {
            setPanMode(false);
            float scale = (float) (Math.round(detector.getScaleFactor() * 100) / 100.0);
            if (Math.abs(scale - 1.0f) >= 0.01f) {
                // limit the scale change per step
                if (scale > 1.0f) {
                    scale = Math.min(scale, 1.25f);
                } else {
                    scale = Math.max(scale, 0.8f);
                }
                mZoomControl.zoom(scale, detector.getFocusX()
                        / mImgViews[curPic].getWidth(), detector.getFocusY()
                        / mImgViews[curPic].getHeight());
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        // If you don't return true, onScale is never called !
        mMTZoomMode = true;
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        mMTZoomMode = false;
    }

}
