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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
			obj.close();
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
}
