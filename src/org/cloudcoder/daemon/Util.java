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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods.  Most of these are to perform system-y
 * type things like starting processes, checking the status
 * of a process, creating FIFOs, etc.  In theory these will
 * work on any Unix, but in practice they are probably only
 * tested on Linux.
 * 
 * @author David Hovemeyer
 */
public class Util {
	/**
	 * Read the pid file for given instance, returning the pid.
	 * 
	 * @param instanceName  the instance name
	 * @return the contents of the pid file for this instance, or null if there is no pid file
	 *         (or an invalid pid file)
	 * @throws IOException
	 */
	public static Integer readPid(String instanceName) throws DaemonException {
		try {
			return doReadPid(instanceName);
		} catch (IOException e) {
			throw new DaemonException("Could not read pid file for instance", e);
		}
	}
	
	private static Integer doReadPid(String instanceName) throws IOException {
		File pidFile = new File(instanceName + ".pid");
		
		if (!pidFile.exists()) {
			return null;
		}
		
		BufferedReader reader = new BufferedReader(new FileReader(pidFile));
		
		try {
			String line = reader.readLine();
			line = line.trim();
			
			Integer pid = null;
			try {
				pid = Integer.parseInt(line);
			} catch (NumberFormatException e) {
				// ignore: existing pidfile is invalid
			}

			return pid;
		} finally {
			IOUtil.closeQuietly(reader);
		}
	}

	private static final Pattern PID_FROM_PS = Pattern.compile("^\\s*(\\d+)");

	/**
	 * Is there a process running with the given pid?
	 * 
	 * @param pid the pid
	 * @return true if a process is running with the given pid,
	 *         false otherwise
	 * @throws IOException 
	 */
	public static boolean isRunning(Integer pid) throws DaemonException {
		try {
			return doIsRunning(pid);
		} catch (IOException e) {
			throw new DaemonException("Couldn't read process list", e);
		}
	}

	private static boolean doIsRunning(Integer pid) throws IOException {
		BufferedReader reader = null; 
		
		try {
			reader = readProcess("ps");
			while (true) {
				String line = reader.readLine();
				if (line == null) {
					break;
				}
				Matcher m = PID_FROM_PS.matcher(line);
				if (m.find()) {
					Integer otherPid = Integer.parseInt(m.group(1));
					if (pid.equals(otherPid)) {
						return true;
					}
				}
			}
			return false;
		} finally {
			IOUtil.closeQuietly(reader);
		}
	}

	/**
	 * Read lines of text from a process.
	 * 
	 * @param cmd command (array of strings, first is program) to be used to start the process
	 * @return a {@link BufferedReader} reading from the process
	 * @throws IOException
	 */
	public static BufferedReader readProcess(String... cmd) throws IOException {
		BufferedReader reader;
		Process ps = Runtime.getRuntime().exec(cmd, Util.getenvp());
		reader = new BufferedReader(new InputStreamReader(ps.getInputStream()));
		return reader;
	}

	/**
	 * Get the current environment variables in a form that can
	 * be passed as an envp array to {@link Runtime#exec(String[], String[])}.
	 * 
	 * @return current environment variables
	 */
	public static String[] getenvp() {
		ArrayList<String> envp = new ArrayList<String>();
		Map<String, String> env = System.getenv();
		for (Map.Entry<String, String> entry : env.entrySet()) {
			envp.add(entry.getKey() + "=" + entry.getValue());
		}
		return envp.toArray(new String[envp.size()]);
	}

	public static String findCodeBase(Class<?> mainClass) {
		ClassLoader cl = mainClass.getClassLoader();
		String resourceName = mainClass.getName().replace('.', '/') + ".class";
		URL u = cl.getResource(resourceName);
		if (u == null) {
			throw new IllegalStateException("Can't find codebase for " + mainClass.getName());
		}
		String s = u.toExternalForm();
		if (!s.endsWith(resourceName)) {
			throw new IllegalStateException("Codebase resource URL " + s + " does not match class name " + mainClass.getName());
		}
		String raw = s.substring(0, s.length() - resourceName.length());
		
		String result;
		if (raw.startsWith("file:")) {
			// A file: URL, meaning that the codebase is a directory.
			result = getFilenameFromFileURL(raw);
		} else if (raw.startsWith("jar:")) {
			// A jar URL, meaning that the codebase is a jarfile.
			// Extract just the part that identifies the location of the jarfile.
			result = raw.substring("jar:".length());
			int bang = result.indexOf('!');
			if (bang < 0) {
				throw new IllegalStateException("jar: URL has no ! character?");
			}
			result = result.substring(0, bang);
			result = getFilenameFromFileURL(result);
		} else {
			throw new IllegalStateException("Unknown URL type: " + raw);
		}
		
		return result;
	}

	private static String getFilenameFromFileURL(String fileURL) {
		String result;
		result = fileURL.substring("file:".length());
		if (result.startsWith("//")) {
			result = result.substring("//".length());
		}
		return result;
	}

}
