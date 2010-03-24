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

package com.kg.emailalbum.mobile;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.widget.Toast;

import com.kg.emailalbum.mobile.R;
import com.kg.emailalbum.mobile.util.CacheManager;

/**
 * Standard preferences activity.
 */
public class EmailAlbumPreferences extends PreferenceActivity {
    public static final String EXTRA_SCREEN = "SCREEN";
    public static final String SCREEN_VIEWER = "viewerscreen";
    public static final String SCREEN_CREATOR = "creatorscreen";

    /*
     * (non-Javadoc)
     * 
     * @see android.preference.PreferenceActivity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.viewer_preferences);
        String screen = getIntent().getStringExtra(EXTRA_SCREEN);
        if (screen != null) {
            setPreferenceScreen((PreferenceScreen) findPreference(screen));
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @seeandroid.preference.PreferenceActivity#onPreferenceTreeClick(android.
     * preference.PreferenceScreen, android.preference.Preference)
     */
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference.getKey().equals("clearcache")) {
            CacheManager cm = new CacheManager(getApplicationContext());
            int nbDeleted = CacheManager.deleteDirectory(cm.getCacheDir());
            
            Toast.makeText(
                    getApplicationContext(),
                    String.format(getString(R.string.number_files_deleted),
                            nbDeleted), Toast.LENGTH_LONG).show();
        }
        return true;
    }

}
