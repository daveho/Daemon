// Copyright (c) 2012-2014, David H. Hovemeyer <david.hovemeyer@gmail.com>
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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods.  Most of these are to perform system-y
 * type things like starting processes, checking the status
 * of a process, creating FIFOs, etc.  In theory these will
 * work on any Unix, but in practice they are probably only
 * tested on Linux.
 * 
 * <p> This class also has some general-purpose utility methods
 * such as {@link #loadPropertiesFromResource(ClassLoader, String)}
 * that might be useful for applications.
 * 
 * @author David Hovemeyer
 */
public class Util {
	/**
	 * Path to sh executable, or null if it can't be found.
	 */
	public static final String SH_PATH = findExe("sh", "/bin", "/usr/bin");

	/**
	 * Path to ps executable, or null if it can't be found.
	 */
	public static final String PS_PATH = findExe("ps", "/bin", "/usr/bin");
	
	/**
	 * Path to mkfifo executable, or null if it can't be found.
	 */
	public static final String MKFIFO_PATH = findExe("mkfifo", "/bin", "/usr/bin");
	
	/**
	 * Find the executable with the given name by checking the directories
	 * in the given path in order.
	 * 
	 * @param exeName  an executable name (e.g., "mkfifo")
	 * @param path     a path (e.g., "/bin", "/usr/bin")
	 * @return full path to the executable, or null if the executable can't be found 
	 * @throws DaemonException
	 */
	public static String findExe(String exeName, String...path) {
		for (String dir : path) {
			File f = new File(dir + "/" + exeName);
			if (f.exists()) { // XXX: should check if it is executable
				return f.getPath();
			}
		}
		return null;
	}
	
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
		File pidFile = new File(getPidFileName(instanceName));
		
		if (!pidFile.exists()) {
			return null;
		}
		
		BufferedReader reader = new BufferedReader(new FileReader(pidFile));
		
		try {
			String line = reader.readLine();
			if (line == null) {
				return null;
			}
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

	/**
	 * Write a pid file for instance.
	 * 
	 * @param instanceName name of the instance
	 * @param pid the pid
	 * @throws DaemonException 
	 */
	public static void writePid(String instanceName, Integer pid) throws DaemonException {
		try {
			doWritePid(instanceName, pid);
		} catch (IOException e) {
			throw new DaemonException("Could not write pid file", e);
		}
	}

	private static void doWritePid(String instanceName, Integer pid) throws IOException {
		FileWriter writer = new FileWriter(getPidFileName(instanceName));
		try {
			writer.write(pid.toString());
			writer.write("\n");
		} finally {
			IOUtil.closeQuietly(writer);
		}
	}

	/**
	 * Get the name of the pid file for given instance name.
	 * 
	 * @param instanceName  an instance name
	 * @return  the pid file name for that instance name
	 */
	public static String getPidFileName(String instanceName) {
		return instanceName + ".pid";
	}
	
	/**
	 * Get the name of the FIFO for given instance name and process id.
	 * 
	 * @param instanceName the instance name
	 * @param pid          the pid
	 * @return  the FIFO name for the isntance name and process id
	 */
	public static String getFifoName(String instanceName, Integer pid) {
		return instanceName + "-" + pid.toString() + ".fifo";
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
		
		boolean found = false;
		try {
			// Note that we need to run ps -e in order to see all processes
			reader = readProcess(PS_PATH, "-e");
			while (!found) {
				String line = reader.readLine();
				if (line == null) {
					break;
				}
				Matcher m = PID_FROM_PS.matcher(line);
				if (m.find()) {
					Integer otherPid = Integer.parseInt(m.group(1));
					if (pid.equals(otherPid)) {
						found = true;
					}
				}
			}
		} finally {
			IOUtil.closeQuietly(reader);
		}
		return found;
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

	/**
	 * Return the codebase from which the given class was loaded,
	 * in a form suitable for using as a component of a classpath.
	 * 
	 * @param cls  the class to determine the codebase of
	 * @return  the class's codebase, in a form suitable for use in a classpath
	 */
	public static String findCodeBase(Class<?> cls) {
		ClassLoader cl = cls.getClassLoader();
		String resourceName = cls.getName().replace('.', '/') + ".class";
		URL u = cl.getResource(resourceName);
		if (u == null) {
			throw new IllegalStateException("Can't find codebase for " + cls.getName());
		}
		String s = u.toExternalForm();
		if (!s.endsWith(resourceName)) {
			throw new IllegalStateException("Codebase resource URL " + s + " does not match class name " + cls.getName());
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
			if (!result.startsWith("file:")) {
				throw new IllegalStateException("Codebase is a jar not loaded from a file: URL");
			}
			result = getFilenameFromFileURL(result);
		} else {
			throw new IllegalStateException("Unknown URL type: " + raw);
		}
		
		return result;
	}

	/**
	 * Extract the filename from a file: URL.
	 * 
	 * @param fileURL  a file: URL
	 * @return  the filename in the file: URL
	 */
	private static String getFilenameFromFileURL(String fileURL) {
		String result;
		result = fileURL.substring("file:".length());
		if (result.startsWith("//")) {
			result = result.substring("//".length());
		}
		return result;
	}

	/**
	 * Delete a file.
	 * @param fileName name of the file to delete
	 */
	public static void deleteFile(String fileName) {
		File file = new File(fileName);
		file.delete();
	}

	/**
	 * Send a command to the daemon process.
	 * 
	 * @param instanceName   the instance name
	 * @param pid            the pid of the daemon process
	 * @param command        the command to send
	 * @throws DaemonException 
	 */
	public static void sendCommand(String instanceName, Integer pid, String command) throws DaemonException {
		try {
			doSendCommand(instanceName, pid, command);
		} catch (IOException e) {
			throw new DaemonException("Error sending command to daemon", e);
		}
	}

	private static void doSendCommand(String instanceName, Integer pid, String command) throws IOException {
		FileWriter writer = new FileWriter(getFifoName(instanceName, pid));
		try {
			writer.write(command);
			writer.write("\n");
			writer.flush();
		} finally {
			IOUtil.closeQuietly(writer);
		}
	}

	/**
	 * Execute a process.
	 * 
	 * @param cmd the command (program and arguments)
	 * @return the process's exit code
	 * @throws DaemonException
	 */
	public static int exec(String... cmd) throws DaemonException {
		try {
			return doExec(cmd);
		} catch (IOException e) {
			throw new DaemonException("Error executing command", e);
		}
	}

	private static int doExec(String... cmd) throws IOException {
		Process proc = Runtime.getRuntime().exec(cmd, getenvp());
		try {
			return proc.waitFor();
		} catch (InterruptedException e) {
			throw new IOException("Interrupted waiting for process to complete", e);
		}
	}

	/**
	 * Get this process's pid.
	 * 
	 * @return this process's pid
	 * @throws DaemonException 
	 * @see http://blog.igorminar.com/2007/03/how-java-application-can-discover-its.html
	 */
	public static Integer getPid() throws DaemonException {
		try {
			return doGetPid();
		} catch (IOException e) {
			throw new DaemonException("Could not get pid", e);
		}
	}

	private static Integer doGetPid() throws IOException {
		BufferedReader reader = Util.readProcess(SH_PATH, "-c", "echo $PPID");
		try {
			String line = reader.readLine();
			if (line == null) {
				throw new IOException("Could not read pid from child process");
			}
			try {
				return Integer.parseInt(line);
			} catch (NumberFormatException e) {
				throw new IOException("Invalid pid read from child process", e);
			}
		} finally {
			IOUtil.closeQuietly(reader);
		}
	}

	/**
	 * Wait for given process to exit.
	 * 
	 * @param pid  the pid of the process to wait for
	 * @param callback callback whose run() method is called periodically (once per second)
	 *                 if the process is still running
	 * @throws DaemonException
	 */
	public static void waitForExit(Integer pid, Runnable callback) throws DaemonException {
		try {
			doWaitForExit(pid, callback);
		} catch (IOException e) {
			throw new DaemonException("Error waiting for process to exit", e);
		}
	}

	private static void doWaitForExit(Integer pid, Runnable callback) throws DaemonException,
			IOException {
		while (Util.isRunning(pid)) {
			try { 
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				throw new IOException("Interrupted while waiting for process to exit", e);
			}
			if (callback != null) {
				callback.run();
			}
		}
		System.out.println("done");
	}

	/**
	 * Clean up the pid file and FIFO used by the given instance.
	 * The instance should have exited before this is called.
	 * 
	 * @param instanceName the instance name
	 * @param pid          the pid of the exited instance
	 */
	public static void cleanup(String instanceName, Integer pid) {
		Util.deleteFile(getPidFileName(instanceName));
		Util.deleteFile(getFifoName(instanceName, pid));
	}
	
	// XXX: any others that should be added?
	private static final String SHELL_META = "'\"\\$`";
	
	/**
	 * Determine whether or not given string contains any shell metacharacters.
	 * 
	 * @param s
	 * @return true if the string contains shell metacharacters,
	 *         false otherwise
	 */
	public static boolean hasShellMetaCharacters(String s) {
		for (int i = 0; i < s.length(); i++) {
			if (SHELL_META.indexOf(s.charAt(i)) >= 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Get the path of the JVM executable.
	 * Tries to identify the same JVM executable as the one
	 * that is executing currently.  Otherwise, just returns
	 * "java".  Note that the full JVM executable path will 
	 * (probably) only be found on Linux/Unix for JRE/JDK
	 * installations that use the standard directory layout.
	 * 
	 * @return path to JVM executable
	 */
	public static Object getJvmExecutablePath() {
		String javaHome = System.getProperty("java.home");
		File jvmExe = new File(javaHome + "/bin/java");
		if (jvmExe.exists()) {
			return jvmExe.getPath();
		}
		return "java";
	}

	/**
	 * Load properties from a resource embedded in the classpath.
	 * Note that an IllegalStateException is thrown if the properties
	 * cannot be loaded from the resource: therefore, this
	 * method should probably only be used to load properties that are
	 * guaranteed to be present.
	 * 
	 * @param clsLoader     the ClassLoader to load the resource from
	 * @param resourcePath  the resource from which the properties should be loaded
	 * @return the Properties loaded from the resource
	 * @throws IllegalStateException if the properties cannot be loaded
	 */
	public static Properties loadPropertiesFromResource(ClassLoader clsLoader, String resourcePath) {
		URL propURL = clsLoader.getResource(resourcePath);
		if (propURL == null) {
			throw new IllegalStateException("Couldn't find properties " + resourcePath);
		}
		Properties properties = new Properties();
		try {
			InputStream in = propURL.openStream();
			try {
				properties.load(in);
			} finally {
				in.close();
			}
		} catch (Exception e) {
			throw new IllegalStateException("Couldn't load properties " + resourcePath);
		}
		return properties;
	}
	
	private static Map<String, String> resourceNameToFileMap = new HashMap<String, String>();
	private static Object resourceNameToFileMapLock = new Object();
	
	/**
	 * Take a resource that is loadable from the given classloader
	 * and make it available as a file in the filesystem.
	 * If the app is running from a jarfile, loaded resources aren't
	 * directly available as files (the way they would be if the
	 * app is running out of a directory containing the app's class files.)
	 * This method will copy an internal resource to an external
	 * file in the system temporary directory if necessary.
	 * 
	 * @param clsLoader    the ClassLoader containing the resource
	 * @param resourceName the name of the resource to externalize
	 * @return the filename of the externalized resource
	 * @throws IOException if the resource can't be externalized
	 * @deprecated don't call this: call {@link #getExternalizedFileName(ClassLoader, String, File)} so that
	 *             the temporary directory where the externalized file will be placed is specified
	 *             explicitly
	 */
	public static String getExternalizedFileName(ClassLoader clsLoader, String resourceName) throws IOException {
		return getExternalizedFileName(clsLoader, resourceName, new File(System.getProperty("java.io.tmpdir")));
	}

	/**
	 * Take a resource that is loadable from the given classloader
	 * and make it available as a file in the filesystem.
	 * If the app is running from a jarfile, loaded resources aren't
	 * directly available as files (the way they would be if the
	 * app is running out of a directory containing the app's class files.)
	 * This method will copy an internal resource to an external
	 * file in the specified if necessary.
	 * 
	 * @param clsLoader    the ClassLoader containing the resource
	 * @param resourceName the name of the resource to externalize
	 * @return the filename of the externalized resource
	 * @throws IOException if the resource can't be externalized
	 */
	public static String getExternalizedFileName(ClassLoader clsLoader, String resourceName, File tempDir) throws IOException {
		String filePath = null;
		File tmpFile = null;
		
		// See if the resource has already been externalized.
		synchronized (resourceNameToFileMapLock) {
			filePath = resourceNameToFileMap.get(resourceName);
			if (filePath != null) {
				return filePath;
			}
		}
		
		// Get a URL to access the resource.
		URL url = clsLoader.getResource(resourceName);
		if (url == null) {
			throw new IllegalArgumentException("Resource " + resourceName + " does not exist");
		}
		String urlString = url.toExternalForm();
		
		// If the URL is a file: URL, then just compute the filename.
		if (urlString.startsWith("file:")) {
			if (urlString.startsWith("file://")) {
				filePath = urlString.substring("file://".length());
			} else {
				filePath = urlString.substring("file:".length());
			}
		} else {
			// This is some other kind of URL (probably a jar: URL).
			// Copy its data to a temporary file.
			InputStream in = null;
			OutputStream out = null;
			
			try {
				// Create a temp file that is marked for deletion when the program exits.
				File f = File.createTempFile("cctemp", null, tempDir);
				f.deleteOnExit();
				
				// Copy data from the resource into the temp file.
				in = url.openStream();
				out = new BufferedOutputStream(new FileOutputStream(f));
				IOUtil.copy(in, out);

				// Success!
				filePath = f.getAbsolutePath();
				tmpFile = f;
			} finally {
				IOUtil.closeQuietly(in);
				IOUtil.closeQuietly(out);
			}
		}
		
		if (filePath == null) {
			// This should be impossible
			throw new IllegalStateException("could not externalize resource?");
		}
		
		// Try to add the externalized filename to the map.
		// Note that we could be racing against another thread.
		// If we notice that an externalized filename has already been
		// added, then we will just use that one (and abandon the
		// one created by this method call.)
		synchronized (resourceNameToFileMapLock) {
			String existingFilePath = resourceNameToFileMap.get(resourceName);
			if (existingFilePath != null) {
				// Another thread already added an externalized file name.
				if (tmpFile != null) {
					// We created a temp file (that is not needed now), so delete it.
					tmpFile.delete();
				}
				return existingFilePath;
			}
			
			// Update the map
			resourceNameToFileMap.put(resourceName, filePath);
		}
		
		// Return the externalized file path.
		return filePath;
	}
}
