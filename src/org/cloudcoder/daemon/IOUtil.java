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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

/**
 * IO utility methods.
 * In theory we could use commons-io, but I didn't want
 * to have any external dependencies.
 * 
 * @author David Hovemeyer
 */
public class IOUtil {
	/**
	 * Close a {@link Closeable} object, ignoring any
	 * {@link IOException} that might be thrown.
	 * 
	 * @param obj a Closeable object to close
	 */
	public static void closeQuietly(Closeable obj) {
		try {
			if (obj != null) {
				obj.close();
			}
		} catch (IOException e) {
			// ignore
		}
	}
	
	/**
	 * Copy as much data as possible from given input stream to given output stream.
	 * 
	 * @param in  an InputStream
	 * @param out an OutputStream
	 * @throws IOException
	 */
	public static void copy(InputStream in, OutputStream out) throws IOException {
		byte[] buf = new byte[4096];

		boolean done = false;
		while (!done) {
			int numRead = in.read(buf);
			if (numRead < 0) {
				done = true;
			} else {
				out.write(buf, 0, numRead);
			}
		}
	}

	/**
	 * Copy as much data as possible from given input stream to given output stream,
	 * invoking a callback periodically (which could drive a progress bar or
	 * other indication of progress.)
	 * 
	 * @param in  an InputStream
	 * @param out an OutputStream
	 * @param sizeIncrement how often to invoke the callback (number of bytes)
	 * @param callback callback to invoke periodically to indicate progress
	 * @throws IOException 
	 */
	public static void copy(InputStream in, OutputStream out, int sizeIncrement, Runnable callback) throws IOException {
		byte[] buf = new byte[4096];
		int total = 0;
		int nextTick = sizeIncrement;

		boolean done = false;
		while (!done) {
			int numRead = in.read(buf);
			if (numRead < 0) {
				done = true;
			} else {
				total += numRead;
				out.write(buf, 0, numRead);
				if (total >= nextTick) {
					callback.run();
					nextTick = total + sizeIncrement;
				}
			}
		}
	}
	
	/**
	 * Copy as much data as possible from given reader to given writer.
	 * 
	 * @param in  a Reader
	 * @param out a Writer
	 * @throws IOException
	 */
	public static void copy(Reader in, Writer out) throws IOException {
		char[] buf = new char[4096];

		boolean done = false;
		while (!done) {
			int numRead = in.read(buf);
			if (numRead < 0) {
				done = true;
			} else {
				out.write(buf, 0, numRead);
			}
		}
	}
}
