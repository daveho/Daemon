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

/**
 * Launch a daemon process in the background and create a FIFO
 * used to send commands to the daemon.
 * 
 * @author David Hovemeyer
 */
public class DaemonLauncher {
	/**
	 * Launch the daemon as a background process (with a FIFO for communication).
	 * 
	 * @param instanceName    the instance name to use
	 * @param daemonClass     the daemon class
	 * @throws DaemonException
	 */
	public void launch(String instanceName, Class<?> daemonClass) throws DaemonException {
		// Check to see if the instance is already running
		Integer pid;
		pid = Util.readPid(instanceName);
		if (pid != null) {
			// Is the instance still running?
			if (Util.isRunning(pid)) {
				throw new DaemonException("Process " + pid + " is still running");
			}
		}
		
		// Start the process
		String codeBase = Util.findCodeBase(this.getClass());
		System.out.println("Codebase is " + codeBase);
	}

	/**
	 * This is the main method invoked by the shell command used
	 * to start the background process.  This should not be called
	 * directly.
	 * 
	 * @param args arguments
	 */
	public static void main(String[] args) {
		
	}
}
