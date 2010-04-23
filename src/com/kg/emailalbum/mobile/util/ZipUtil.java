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
package com.kg.emailalbum.mobile.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import android.util.Log;

/**
 * Tools for working around implementation bugs of java.util.zip in android.
 * @author Normal
 *
 */
public class ZipUtil {
	private static final String LOG_TAG = ZipUtil.class.getSimpleName();

    public static InputStream getInputStream(ZipFile archive, ZipEntry entry)
			throws IOException {
		if (archive.getEntry("META-INF/MANIFEST.MF") != null
		        || Integer.parseInt(android.os.Build.VERSION.SDK) >= 5) {
			return archive.getInputStream(entry);
		}

		File zipFile = new File(archive.getName());
		ZipInputStream zio = new ZipInputStream(new FileInputStream(zipFile));
		boolean found = false;
		String currentEntryName = null;
		while ((zio.available() > 0) && !found) {

			try {
				currentEntryName = zio.getNextEntry().getName();
				Log.d(LOG_TAG, "Examining entry : " + currentEntryName);
			} catch (Exception e) {
				// Might be a wrong character encoding... file won't be read
				// anyway.
				Log.e(LOG_TAG,
						"Read a bad UTF-8 entry name", e);
				return null;
			}
			if (currentEntryName != null
					&& currentEntryName.equals(entry.getName())) {
				found = true;
				Log.d(LOG_TAG, "Entry found");
			}
		}
		if (found) {
			return zio;
		} else {
			return null;
		}
	}
}
