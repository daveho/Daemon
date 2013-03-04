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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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
 *     arbitrary base name (e.g., "cloudcoderApp").</li>
 * <li>there is a file ${downloadUrl}/LATEST that contains the latest
 *     application version string</li>
 * <li>that ${downloadUrl}/${baseName}-${latestVersion}.jar is the download
 *     for the latest version (where ${latestVersion} is the version string
 *     found in ${downloadUrl}/LATEST</li>
 * 
 * @author David Hovemeyer
 */
public class Upgrade {
	private String baseName;
	private String downloadUrl;
	private Class<?> appCls;
	
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
	}
	
	/**
	 * Get a jarfile that is the latest version of the application,
	 * copying over all required configuration files to the upgraded jar file.
	 * 
	 * @throws IOException if the download or configuration fails
	 */
	public void upgradeJarFile() throws IOException {
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
		
		System.out.println("OK?");
	}
	
	private String getVersion(Class<?> appCls) throws IOException {
		InputStream in = appCls.getClassLoader().getResourceAsStream("VERSION");
		if (in == null) {
			throw new IOException("Cannot find version of this app");
		}
		try {
			BufferedReader r = new BufferedReader(new InputStreamReader(in));
			String line = r.readLine();
			if (line == null) {
				throw new IOException("VERSION file in this application is empty");
			}
			return line;
		} finally {
			IOUtil.closeQuietly(in);
		}
	}
}
