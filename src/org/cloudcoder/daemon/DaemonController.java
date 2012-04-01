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
 * This class is responsible for taking commands issued from
 * the command line and processing them to start the daemon,
 * send administrative commands to the daemon, or shut down the
 * daemon.  You should extend it and override the abstract methods.
 * Your deployable main method should create an instance of
 * your controller subclass and invoke the {@link #exec(String[])}
 * method on it, passing the command line arguments.
 * 
 * @author David Hovemeyer
 */
public abstract class DaemonController {
	
	private static class Options {
		private String command;
		private String instanceName;
		private String stdoutLogFileName;
		
		public Options() {
			
		}
		
		public void parse(String[] args) {
			int i;
			for (i = 0; i < args.length; i++) {
				String arg = args[i];
				
				if (!arg.startsWith("--")) {
					break;
				}

				if (arg.startsWith("--instance=")) {
					instanceName = arg.substring("--instance=".length());
				} else if (arg.startsWith("--stdoutLog=")) {
					stdoutLogFileName = arg.substring("--stdoutLog=".length());
				} else {
					throw new IllegalArgumentException("Unknown option: " + arg);
				}
			}
			
			if (i >= args.length) {
				throw new IllegalArgumentException("no command");
			}
			
			command = args[i];
			
			if (i != args.length - 1) {
				throw new IllegalArgumentException("Extra arguments");
			}
		}
		
		public String getCommand() {
			return command;
		}

		public String getInstanceName() {
			return instanceName;
		}
		
		public String getStdoutLogFileName() {
			return stdoutLogFileName;
		}
	}
	
	/**
	 * Carry out the command described by the command-line arguments.
	 * 
	 * @param args the command-line arguments
	 */
	public void exec(String[] args) {
		try {
			doExec(args);
		} catch (DaemonException e) {
			System.err.println("Error: " + e.getMessage());
		} catch (IllegalArgumentException e) {
			System.err.println("Error: " + e.getMessage());
		}
	}

	private void doExec(String[] args) throws DaemonException {
		Options opts = new Options();
		opts.parse(args);
		
		String command = opts.getCommand();
		
		String instanceName = opts.getInstanceName();
		if (instanceName == null) {
			instanceName = getDefaultInstanceName();
		}
		
		if (command.equals("start")) {
			DaemonLauncher launcher = new DaemonLauncher();
			
			// If a stdout log file name was provided, use it
			if (opts.getStdoutLogFileName() != null) {
				launcher.setStdoutLogFile(opts.getStdoutLogFileName());
			}
			
			launcher.launch(instanceName, getDaemonClass());
		} else if (command.equals("shutdown")) {
			// shutdown command
			Integer pid = getPidOfInstance(instanceName);
			Util.sendCommand(instanceName, pid, "shutdown");
			Util.waitForExit(pid);
			Util.cleanup(instanceName, pid);
		} else {
			// some other command
			Integer pid = getPidOfInstance(instanceName);
			Util.sendCommand(instanceName, pid, command);
		}
	}

	private Integer getPidOfInstance(String instanceName) throws DaemonException {
		Integer pid = Util.readPid(instanceName);
		if (pid == null) {
			throw new DaemonException("No pid found for instance " + instanceName);
		}
		if (!Util.isRunning(pid)) {
			throw new DaemonException("Instance " + instanceName + " is not running (old pid=" + pid + ")");
		}
		return pid;
	}
	
	/**
	 * Get the default instance name.
	 * This will be used if the user does not pass an explicit
	 * instance name.
	 * 
	 * @return the default instance name
	 */
	public abstract String getDefaultInstanceName();
	
	/**
	 * Get the daemon class (representing the daemon task to
	 * be started or stopped).
	 * 
	 * @return the daemon class
	 */
	public abstract Class<? extends IDaemon> getDaemonClass();
}
