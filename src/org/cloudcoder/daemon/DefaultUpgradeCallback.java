// Copyright (c) 2013, Jaime Spacco <jspacco@knox.edu>
// Copyright (c) 2013, David H. Hovemeyer <david.hovemeyer@gmail.com>
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

/**
 * Convenience implementation of {@link Upgrade.Callback} that indicates
 * status/progress by printing to System.out.
 * 
 * @author David Hovemeyer
 */
public class DefaultUpgradeCallback implements Upgrade.Callback {
	private boolean startedDownload = false;
	
	@Override
	public void onSuccess(String upgradedFileName) {
		System.out.println("\nSuccess! Upgraded jar file is " + upgradedFileName);
	}
	
	@Override
	public void onError(String errMsg) {
		if (startedDownload) {
			System.out.println();
		}
		System.out.println("Error upgrading: " + errMsg);
	}
	
	@Override
	public void onDownloadTick() {
		if (!startedDownload) {
			startedDownload = true;
			System.out.print("Downloading...");
			System.out.flush();
		} else {
			System.out.print(".");
			System.out.flush();
		}
	}
	
	@Override
	public void upgradeNeeded(String latestVersion) {
		System.out.println("Upgrading to version " + latestVersion);
	}
	
	@Override
	public void noUpgradeNeeded() {
		System.out.println("This jar file is already at the latest version");
	}
	
	@Override
	public void onConfigure() {
		System.out.println("\nCopying configuration files...");
	}
}
