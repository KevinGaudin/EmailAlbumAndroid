package com.kg.emailalbum.mobile;

import org.acra.CrashReportingApplication;

import android.os.Bundle;

public class EmailAlbumApplication extends CrashReportingApplication {

    @Override
    public String getFormId() {
        return "dGpqbndMSW96S3FDZ1h3REVoaGdmTWc6MQ";
    }

    @Override
    public Bundle getCrashResources() {
        Bundle result = new Bundle();
        result.putInt(RES_TOAST_TEXT, R.string.crash_toast_text);
        return result;
    }

}
