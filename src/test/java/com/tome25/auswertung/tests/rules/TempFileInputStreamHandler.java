package com.tome25.auswertung.tests.rules;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.rules.TemporaryFolder;

import com.tome25.auswertung.LogHandler;
import com.tome25.auswertung.stream.FileInputStreamHandler;
import com.tome25.auswertung.utils.Pair;
import com.tome25.auswertung.utils.StringUtils;

/**
 * A {@link TestRule} used to create a temporary file with a {@link PrintStream}
 * to write to it, and a {@link FileInputStreamHandler} to read it.<br/>
 * Closes both and deletes the file after.
 * 
 * @author theodor
 */
public class TempFileInputStreamHandler extends TemporaryFolder {

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
	public Pair<FileInputStreamHandler, PrintStream> newTempFileHandler() throws IOException {
		return newTempFileHandler(null);
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
	 * @throws IOException If creating the file fails, for example because it
	 *                     already exists.
	 */
	public Pair<FileInputStreamHandler, PrintStream> newTempFileHandler(String name) throws IOException {
		File tempFile = name == null ? newFile() : newFile(name);

		PrintStream pout = new PrintStream(tempFile);
		tempHandlers.add(pout);
		FileInputStreamHandler fin = new FileInputStreamHandler(tempFile);
		tempHandlers.add(fin);

		return new Pair<FileInputStreamHandler, PrintStream>(fin, pout);
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
