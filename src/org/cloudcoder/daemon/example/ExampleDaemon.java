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

package org.cloudcoder.daemon.example;

import java.util.concurrent.LinkedBlockingQueue;

import org.cloudcoder.daemon.IDaemon;

/**
 * Example implementation of {@link IDaemon}.
 * 
 * @author David Hovemeyer
 */
public class ExampleDaemon implements IDaemon {
	/**
	 * This worker runnable represents the ongoing work to
	 * be done by the daemon process.
	 * In a real application, it would be doing something useful
	 * such as listening for client requests, doing periodic
	 * tasks, etc.  This implementation just waits for
	 * commands to arrive via a queue.
	 */
	private class Worker implements Runnable {
		@Override
		public void run() {
			while (!shutdown) {
				try {
					String command = commandQueue.take();
					System.out.println("Received a command: " + command);
					System.err.println("Same command, to stderr: " + command);
				} catch (InterruptedException e) {
					System.out.println("Worker thread interrupted, shutting down");
				}
			}
		}
	}
	
	private LinkedBlockingQueue<String> commandQueue;
	private volatile boolean shutdown;
	private Thread workerThread;
	
	/**
	 * Constructor.
	 */
	public ExampleDaemon() {
		commandQueue = new LinkedBlockingQueue<String>();
		shutdown = false;
		workerThread = new Thread(new Worker());
	}
	
	@Override
	public void start(String instanceName) {
		System.out.println("Daemon (instance name=" + instanceName + ") is starting!");
		
		// start the worker thread
		workerThread.start();
	}
	
	@Override
	public void handleCommand(String command) {
		try {
			// Send the command to the worker
			commandQueue.put(command);
		} catch (InterruptedException e) {
			System.out.println("This should not happen");
		}
	}

	@Override
	public void shutdown() {
		System.out.println("Daemon is shutting down!");
		
		// Let the worker thread know that it is time to shut down
		shutdown = true;
		workerThread.interrupt();
		
		// Wait for the worker thread to finish
		System.out.println("Waiting for worker thread to finish...");
		try {
			workerThread.join();
		} catch (InterruptedException e) {
			System.out.println("This should not happen");
		}
		System.out.println("Worker thread finished");
	}
}
