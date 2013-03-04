// Copyright (c) 2012, Jaime Spacco <jspacco@knox.edu>
// Copyright (c) 2012, David H. Hovemeyer <david.hovemeyer@gmail.com>
// 
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
// 
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
// 
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package org.cloudcoder.daemon;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.cloudcoder.daemon.IOUtil;
import org.cloudcoder.daemon.Util;

/**
 * Upgrade a single-jarfile executable to the latest version,
 * copying any needed configuration files into the new jarfile.
 * Assumes that
 * <ul>
 * <li>the jarfile has a file called VERSION in its
 *     root directory that contains the version string of the current jarfile</li>
 * <li>the jarfile is named ${baseName}-${version}.jar, where ${version}
 *     is the version string in the VERSION file, and ${baseName} is some
 *     arbitrary base name (e.g., "cloudcoderApp")</li>
 * <li>there is a file ${downloadUrl}/LATEST that contains the latest
 *     application version string</li>
 * <li>that ${downloadUrl}/${baseName}-${latestVersion}.jar is the download
 *     for the latest version (where ${latestVersion} is the version string
 *     found in ${downloadUrl}/LATEST</li>
 * 
 * @author David Hovemeyer
 */
public class Upgrade {
	/**
	 * Callback interface for upgrade events.
	 * Can be used to provide visual indication of progress,
	 * error, etc.
	 */
	public interface Callback {
		/**
		 * Called if no upgrade is needed because app version is already the latest version.
		 */
		public void noUpgradeNeeded();
		
		/**
		 * Called to indicate that an error occurred.
		 * 
		 * @param errMsg error message
		 */
		public void onError(String errMsg);
		
		/**
		 * Called to indicate that download progress has been made.
		 */
		public void onDownloadTick();
		
		/**
		 * Called to indicate that the upgrade succeeded.
		 * 
		 * @param upgradedFileName the filename of the upgraded and configured jar file
		 */
		public void onSuccess(String upgradedFileName);
	}
	
	private String baseName;
	private String downloadUrl;
	private Class<?> appCls;
	private List<String> configFileList;
	
	/**
	 * Constructor.
	 * 
	 * @param baseName    the app jarfile's expected base name
	 * @param downloadUrl the download URL
	 * @param appCls      the main class of the application (which must be
	 *                    loaded from the jarfile that is being upgraded)
	 */
	public Upgrade(String baseName, String downloadUrl, Class<?> appCls) {
		this.baseName = baseName;
		this.downloadUrl = downloadUrl;
		this.appCls = appCls;
		this.configFileList = new ArrayList<String>();
	}
	
	/**
	 * Add the name of a configuration file that should be copied from
	 * the current jarfile to the upgraded jarfile.  Useful for configuration
	 * properties, keystores, etc.
	 * 
	 * @param configFile name of configuration file to copy from current jar file
	 *                   to upgraded jar file
	 */
	public void addConfigFile(String configFile) {
		configFileList.add(configFile);
	}
	
	/**
	 * Get a jarfile that is the latest version of the application,
	 * copying over all required configuration files to the upgraded jar file.
	 * 
	 * @param callback callback for status updates, error or success indication, etc.
	 * @throws IOException if the download or configuration fails
	 */
	public void upgradeJarFile(Callback callback) {
		try {
			doUpgradeJarFile(callback);
		} catch (IOException e) {
			callback.onError(e.getMessage());
		}
	}
	
	
	public void doUpgradeJarFile(final Callback callback) throws IOException {
		// Find the current application version
		String currentVersion = getVersion(appCls);
		
		// Find the current jar file name
		String codeBase = Util.findCodeBase(appCls);
		if (!codeBase.endsWith(".jar")) {
			throw new IOException("Can't upgrade: application codebase is not a jar file");
		}
		int lastSep = codeBase.lastIndexOf('/');
		String jarName = lastSep >= 0 ? codeBase.substring(lastSep+1) : codeBase;
		String expectedJarName = baseName + "-" + currentVersion + ".jar";
		if (!jarName.equals(expectedJarName)) {
			throw new IOException(
					"Current jar file name (" + jarName + ") does not match expected (" + expectedJarName + ")");
		}
		
		// Get the latest file
		String latestVersion;
		InputStream latestVersionIn = null;
		try {
			latestVersionIn = new URL(downloadUrl + "/LATEST").openStream();
			latestVersion = readOneLine(latestVersionIn);
		} finally {
			IOUtil.closeQuietly(latestVersionIn);
		}

		// If current version equals the latest version, then no
		// upgrade is needed.
		if (currentVersion.equals(latestVersion)) {
			callback.noUpgradeNeeded();
			return;
		}
		
		// Presumably, if latest version is different than current version,
		// then an upgrade is needed.
		
		// Determine full download URL
		String fullDownloadUrl = downloadUrl + "/" + baseName + "-" + latestVersion + ".jar";
		//System.out.println("Download " + fullDownloadUrl);
		
		// Download the latest jarfile
		InputStream latestAppIn = null;
		String latestJarFileName = baseName + "-" + latestVersion + ".jar";
		File latestJarFile = new File(latestJarFileName);
		if (latestJarFile.exists()) {
			throw new IOException("Cannot upgrade: file " + latestJarFileName + " already exists");
		}
		OutputStream latestJarOut = new BufferedOutputStream(new FileOutputStream(latestJarFile));
		try {
			latestAppIn = new URL(fullDownloadUrl).openStream();
			// Download progress tick once per megabyte
			callback.onDownloadTick(); // one tick to get started...
			IOUtil.copy(latestAppIn, latestJarOut, 1024*1024, new Runnable() {
				@Override
				public void run() {
					callback.onDownloadTick();
				}
			});
		} finally {
			IOUtil.closeQuietly(latestJarOut);
			IOUtil.closeQuietly(latestAppIn);
		}
		
		// TODO: configure
		
		callback.onSuccess(latestJarFileName);
	}
	
	private String getVersion(Class<?> appCls) throws IOException {
		InputStream in = appCls.getClassLoader().getResourceAsStream("VERSION");
		if (in == null) {
			throw new IOException("Cannot find version of this app");
		}
		try {
			return readOneLine(in);
		} finally {
			IOUtil.closeQuietly(in);
		}
	}

	private String readOneLine(InputStream in) throws IOException {
		BufferedReader r = new BufferedReader(new InputStreamReader(in));
		String line = r.readLine();
		if (line == null) {
			throw new IOException("VERSION file in this application is empty");
		}
		return line;
	}
}
