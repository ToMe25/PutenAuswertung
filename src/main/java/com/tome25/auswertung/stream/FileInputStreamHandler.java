package com.tome25.auswertung.stream;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import com.tome25.auswertung.log.LogHandler;

/**
 * This class handles reading the content of a file line by line.
 * 
 * @author Theodor Meyer zu Hörste
 */
public class FileInputStreamHandler implements IInputStreamHandler {

	/**
	 * The file from which which data is being read.
	 */
	private final File input_file;

	/**
	 * The {@link InputStream} reading using which the data is read.
	 */
	private final InputStream stream;

	/**
	 * Whether this Stream Handler is yet to be closed.
	 */
	private volatile boolean closed = false;

	/**
	 * Creates a new FileStreamHandler reading the content of the given file.
	 * 
	 * @param input The file to read.
	 * @throws FileNotFoundException if the given input file does not exist.
	 * @throws NullPointerException  if the given input file is {@code null}.
	 */
	public FileInputStreamHandler(File input) throws FileNotFoundException, NullPointerException {
		Objects.requireNonNull(input, "The file to be read, input, can't be null.");

		input_file = input;
		FileInputStream fin = new FileInputStream(input);
		stream = new BufferedInputStream(fin);
	}

	@Override
	public String readline() throws IOException {
		if (closed) {
			throw new IOException("stream handler closed");
		}

		ByteArrayOutputStream barr = new ByteArrayOutputStream();

		while (available()) {
			int read = (byte) stream.read();
			if (read == '\n') {
				break;
			} else if (read != '\r') {
				barr.write(read);
			}
		}

		return barr.toString("UTF-8");
	}

	@Override
	public boolean available() {
		if (closed) {
			return false;
		}

		try {
			return stream.available() > 0;
		} catch (IOException e) {
			LogHandler.err_println("An error occured while trying to check whether an input has more data to be read.");
			LogHandler.print_exception(e, "check whether there is more available data right now", "input file: \"%s\"",
					input_file.toString());
			return false;
		}
	}

	@Override
	public boolean done() {
		if (closed) {
			return true;
		}

		try {
			return stream.available() == 0;
		} catch (IOException e) {
			LogHandler.err_println("An error occured while trying to check whether an input has more data to be read.");
			LogHandler.print_exception(e, "check whether there is/will be more data to read", "input file: \"%s\"",
					input_file.toString());
			return true;
		}
	}

	@Override
	public void close() throws IOException {
		if (!closed) {
			closed = true;
			stream.close();
		} else {
			LogHandler.err_println("Trying to close an already closed FileInputStreamHandler.", true);
			LogHandler.print_debug_info("stream handler: %s", toString());
		}
	}

	@Override
	public String toString() {
		int bav = -1;
		if (closed) {
			bav = 0;
		} else {
			try {
				bav = stream.available();
			} catch (IOException e) {
				LogHandler.print_exception(e, "input handler to stream stream.available",
						"input file: \"%s\", closed: %s", input_file.toString(), closed ? "true" : "false");
			}
		}

		return String.format(
				getClass().getSimpleName()
						+ "[input_file=\"%s\", closed=%s, done=%s, available=%s, bytes_available=%s]",
				input_file.toString(), closed ? "true" : "false", done() ? "true" : "false",
				available() ? "true" : "false", bav == -1 ? "error" : Integer.toString(bav));
	}

	/**
	 * Gets the file this stream handle is reading from.
	 * 
	 * @return The file this stream handle is reading from.
	 */
	public File getInputFile() {
		return input_file;
	}

}
