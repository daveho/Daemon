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

public abstract class DaemonController {
	
	private static class Options {
		private String command;
		private String instanceName;
		
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
	}
	
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
			launcher.launch(instanceName, getDaemonClass());
		}
	}
	
	public abstract String getDefaultInstanceName();
	
	public abstract Class<? extends IDaemon> getDaemonClass();
}
