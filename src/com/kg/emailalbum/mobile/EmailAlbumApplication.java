package com.kg.emailalbum.mobile;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;

@ReportsCrashes(formKey="dGpqbndMSW96S3FDZ1h3REVoaGdmTWc6MQ",
        mode=ReportingInteractionMode.TOAST,
        resToastText=R.string.crash_toast_text)
public class EmailAlbumApplication extends Application {

    /* (non-Javadoc)
     * @see android.app.Application#onCreate()
     */
    @Override
    public void onCreate() {
        ACRA.init(this);
        super.onCreate();
    }


}
