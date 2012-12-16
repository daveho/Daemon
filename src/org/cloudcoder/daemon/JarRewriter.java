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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.cloudcoder.daemon.IOUtil;

/**
 * Rewrite a jar file by replacing or adding specified files.
 * 
 * @author Jaime Spacco
 * @author David Hovemeyer
 */
public class JarRewriter {
	/**
	 * Data to use to replace an entry in the jar file.
	 */
	public interface EntryData {
		public InputStream getInputStream() throws IOException;
	}
	
	/**
	 * Entry data for {@link Properties}.
	 */
	public static class PropertiesEntryData implements EntryData {
		private Properties properties;
		
		/**
		 * Constructor.
		 * 
		 * @param properties the {@link Properties}
		 */
		public PropertiesEntryData(Properties properties) {
			this.properties = properties;
		}
		
		@Override
		public InputStream getInputStream() throws IOException {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			properties.store(bytes, "");
			return new ByteArrayInputStream(bytes.toByteArray());
		}
	}
	
	/**
	 * Entry data for a file.
	 */
	public static class FileEntryData implements EntryData {
		private String fileName;
		
		/**
		 * Constructor.
		 * 
		 * @param fileName name of the file
		 */
		public FileEntryData(String fileName) {
			this.fileName = fileName;
		}
		
		@Override
		public InputStream getInputStream() throws FileNotFoundException {
			return new BufferedInputStream(new FileInputStream(fileName));
		}
	}

	private String jarFileName;
	private Map<String, EntryData> replace;

	/**
	 * Constructor.
	 * 
	 * @param jarFileName filename of the jarfile to rewrite
	 */
	public JarRewriter(String jarFileName) {
		this.jarFileName = jarFileName;
		this.replace = new HashMap<String, JarRewriter.EntryData>();
	}

	/**
	 * Replace specified entry.
	 * If the original jar file does not contain an entry
	 * with the specified name, a new entry will be created.
	 * 
	 * @param entryName the name entry to replace or add
	 * @param data      the data to replace the entry with
	 */
	public void replaceEntry(String entryName, EntryData data) {
		replace.put(entryName, data);
	}

	/**
	 * copy input to output stream - available in several StreamUtils or Streams classes 
	 */    
	private void copy(InputStream input, OutputStream output) throws IOException {
		byte[] buffer = new byte[4096];
		int bytesRead;
		while ((bytesRead = input.read(buffer))!= -1) {
			output.write(buffer, 0, bytesRead);
		}
	}

	/**
	 * Rewrite the jarfile.
	 * 
	 * @throws IOException
	 */
	public void rewrite() throws IOException {
		File tempFile = null;
		
		// Outer try/finally ensures that the temp file is deleted
		try {
			// Read in jarfileName, and replace all entries specified with
			// previous calls to replaceEntry
			ZipFile jarfile = new ZipFile(jarFileName);
			ZipOutputStream newJarfileData = null;
			try {
				tempFile = File.createTempFile("ccRewriteJar", ".jar");
				tempFile.deleteOnExit();
				OutputStream tempOutputStream = new BufferedOutputStream(new FileOutputStream(tempFile));
				newJarfileData = new ZipOutputStream(tempOutputStream);

				// XXX Hack: zipfiles and jarfiles can apparently have multiple copies
				// of the SAME file.  The builder has many META-INF/LICENSE files
				// This should be fixed somehow, probably in the build.xml
				// by giving the licenses specific names or putting them into
				// other folders.
				Set<String> alreadySeen=new HashSet<String>();

				// first, copy contents from existing war
				Enumeration<? extends ZipEntry> entries = jarfile.entries();
				while (entries.hasMoreElements()) {
					ZipEntry e = entries.nextElement();
					if (alreadySeen.contains(e.getName())) {
						// skip filenames we've already added
						continue;
					}

					// Handle entry
					InputStream entryIn;
					if (replace.containsKey(e.getName())) {
						// Replace this entry
						entryIn = replace.get(e.getName()).getInputStream();
						ZipEntry newEntry = new ZipEntry(e.getName());
						newJarfileData.putNextEntry(newEntry);
					} else {
						// Keep this entry
						entryIn = jarfile.getInputStream(e);
						newJarfileData.putNextEntry(e);
					}
					if (!e.isDirectory()) {
						try {
							copy(entryIn, newJarfileData);
						} finally {
							entryIn.close();
						}
					}

					// Make a note that this entry has been handled
					alreadySeen.add(e.getName());

					newJarfileData.closeEntry();
				}
				
				// Special case: if any entries specified by replaceEntry weren't
				// written (because there were no matching entries in the original
				// jar file), then add them.
				for (Map.Entry<String, EntryData> e : replace.entrySet()) {
					if (!alreadySeen.contains(e.getKey())) {
						ZipEntry newEntry = new ZipEntry(e.getKey());
						InputStream entryIn = e.getValue().getInputStream();
						newJarfileData.putNextEntry(newEntry);
						try {
							copy(entryIn, newJarfileData);
						} finally {
							entryIn.close();
						}
					}
				}
			} finally {
				// Ensure open files are closed
				IOUtil.closeQuietly(newJarfileData);
				jarfile.close();
			}

			// Copy replacement file, overwriting original.
			OutputStream out = null;
			InputStream in = null;
			try {
				out = new BufferedOutputStream(new FileOutputStream(jarFileName));
				in = new BufferedInputStream(new FileInputStream(tempFile));
				copy(in, out);
			} finally {
				IOUtil.closeQuietly(out);
				IOUtil.closeQuietly(in);
			}

		} finally {
			// Ensure that temp file is deleted
			tempFile.delete();
		}
	}
}
