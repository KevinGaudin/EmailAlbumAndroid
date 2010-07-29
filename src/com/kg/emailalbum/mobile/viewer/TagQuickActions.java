package com.kg.emailalbum.mobile.viewer;


import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.kg.emailalbum.mobile.R;
import com.kg.emailalbum.mobile.ui.BetterPopupWindow;

public class TagQuickActions extends BetterPopupWindow {
    Context mContext = null;
    

    public TagQuickActions(View anchor, int[] labels, Runnable[] tasks) {
        super(anchor, labels, tasks);
    }

    @Override
    protected void onCreate() {
        mContext = anchor.getContext();
        // inflate layout
        LayoutInflater inflater =
                (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.quick_actions, null);

        LinearLayout rootLayout = (LinearLayout) root;

        for(int i=0; i < mTasks.length; i++) {
            // setup button events
            Button b = new Button(mContext);
            b.setText(mContext.getText(mLabels[i]));
            final Runnable task = mTasks[i];
            b.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View v) {
                    task.run();
                    TagQuickActions.this.dismiss();
                    
                }
            });
            rootLayout.addView(b);
        }
        
        
        // set the inflated view as what we want to display
        this.setContentView(rootLayout);
    }
}
