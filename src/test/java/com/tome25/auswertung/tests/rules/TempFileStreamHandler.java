package com.tome25.auswertung.tests.rules;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.rules.TemporaryFolder;

import com.tome25.auswertung.LogHandler;
import com.tome25.auswertung.stream.FileInputStreamHandler;
import com.tome25.auswertung.stream.FileOutputStreamHandler;
import com.tome25.auswertung.utils.Pair;
import com.tome25.auswertung.utils.StringUtils;

/**
 * A {@link TestRule} used to create a temporary file with a {@link PrintStream}
 * to write to it, and a {@link FileInputStreamHandler} to read it.<br/>
 * Or a {@link FileOutputStreamHandler} to write and a {@link BufferedReader} to
 * read it.<br/>
 * Closes both and deletes the file after.
 * 
 * @author theodor
 */
public class TempFileStreamHandler extends TemporaryFolder {

	/**
	 * A list containing all the {@link PrintStream PrintStreams} and
	 * {@link FileInputStreamHandler FileInputStreamHandlers} to close after the
	 * method call.
	 */
	private List<Closeable> tempHandlers = new ArrayList<Closeable>();

	/**
	 * Creates a temporary file with a {@link FileInputStreamHandler} and a
	 * {@link PrintStream} pointing to it.<br/>
	 * This allows to write temporary data to a file and read it for parsing.<br/>
	 * Closes both after the test method fails or finishes.<br/>
	 * Uses a random file name.
	 * 
	 * @return A {@link Pair} containing the {@link FileInputStreamHandler} and
	 *         {@link PrintStream}.
	 * @throws IOException If creating the file fails, for example because it
	 *                     already exists.
	 */
	public Pair<FileInputStreamHandler, PrintStream> newTempInputFile() throws IOException {
		return newTempInputFile(null);
	}

	/**
	 * Creates a temporary file with a {@link FileInputStreamHandler} and a
	 * {@link PrintStream} pointing to it.<br/>
	 * This allows to write temporary data to a file and read it for parsing.<br/>
	 * Closes both after the test method fails or finishes.
	 * 
	 * @param name The name of the new file to create. {@code null} for random.
	 * @return A {@link Pair} containing the {@link FileInputStreamHandler} and
	 *         {@link PrintStream}.
	 * @throws IOException       If creating the file fails, for example because it
	 *                           already exists.<br/>
	 *                           Or if the file cannot be opened for reading or
	 *                           writing.
	 * @throws SecurityException If a {@link java.lang.SecurityManager
	 *                           SecurityManager} exists and denies
	 *                           reading/writing/creating the file.
	 */
	public Pair<FileInputStreamHandler, PrintStream> newTempInputFile(String name)
			throws IOException, SecurityException {
		File tempFile = name == null ? newFile() : newFile(name);

		PrintStream pout = new PrintStream(tempFile);
		tempHandlers.add(pout);
		FileInputStreamHandler fin = new FileInputStreamHandler(tempFile);
		tempHandlers.add(fin);

		return new Pair<FileInputStreamHandler, PrintStream>(fin, pout);
	}

	/**
	 * Creates a temporary file with a {@link FileOutputStreamHandler} and a
	 * {@link BufferedReader} pointing to it.<br/>
	 * This allows to parse output data written by a {@link FileOutputStreamHandler}
	 * in validate it.<br/>
	 * The {@link FileOutputStreamHandler} does not handle temporary data, but does
	 * auto flush.
	 * 
	 * @return A {@link Pair} containing the two objects.
	 * @throws IOException If creating the file fails, for example because it
	 *                     already exists.
	 */
	public Pair<FileOutputStreamHandler, BufferedReader> newTempOutputFile() throws IOException {
		return newTempOutputFile(null);
	}

	/**
	 * Creates a temporary file with a {@link FileOutputStreamHandler} and a
	 * {@link BufferedReader} pointing to it.<br/>
	 * This allows to parse output data written by a {@link FileOutputStreamHandler}
	 * in validate it.<br/>
	 * The {@link FileOutputStreamHandler} does not handle temporary data, but does
	 * auto flush.
	 * 
	 * @param name The name of the new file to create. {@code null} for random.
	 * @return A {@link Pair} containing the two objects.
	 * @throws IOException If creating the file fails, for example because it
	 *                     already exists.
	 */
	public Pair<FileOutputStreamHandler, BufferedReader> newTempOutputFile(String name) throws IOException {
		File tempFile = name == null ? newFile() : newFile(name);

		BufferedReader bin = new BufferedReader(new FileReader(tempFile));
		tempHandlers.add(bin);
		FileOutputStreamHandler fout = new FileOutputStreamHandler(tempFile, false, true);
		tempHandlers.add(fout);

		return new Pair<FileOutputStreamHandler, BufferedReader>(fout, bin);
	}

	@Override
	protected void after() {
		for (Closeable c : tempHandlers) {
			try {
				c.close();
			} catch (IOException e) {
				LogHandler.print_exception(e, "close temp file handlers", "Handler: %s, Temp Handlers: [%s]", c,
						StringUtils.collectionToString(", ", tempHandlers));
			}
		}
		super.after();
	}

}
