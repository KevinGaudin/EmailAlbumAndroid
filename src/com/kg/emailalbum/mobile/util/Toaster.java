package com.kg.emailalbum.mobile.util;

import android.content.Context;
import android.os.Looper;
import android.widget.Toast;

import com.kg.emailalbum.mobile.R;
import com.kg.emailalbum.mobile.creator.EmailAlbumEditor;

public class Toaster extends Thread {
    
    private int mMessageId;
    private Context mContext;
    private int mLength;
    
    public Toaster(Context context, int msgId, int length) {
        mMessageId = msgId;
        mContext = context;
        mLength = length;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
        Looper.prepare();
        Toast.makeText(
                mContext,
                mMessageId,
                mLength).show();
        Looper.loop();
    }
}
