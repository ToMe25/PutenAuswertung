package com.tome25.auswertung.stream;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import com.tome25.auswertung.LogHandler;

/**
 * This class handles reading the content of a file line by line.
 * 
 * @author theodor
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
	private boolean open = true;

	/**
	 * Creates a new FileStreamHandler reading the content of the given file.
	 * 
	 * @param input The file to read.
	 * @throws FileNotFoundException if the given input file does not exist.
	 * @throws SecurityException     if a security manager exists and its
	 *                               {@code checkRead} method denies read access to
	 *                               the file.
	 * @throws NullPointerException  if the given input file is {@code null}.
	 */
	public FileInputStreamHandler(File input) throws FileNotFoundException, SecurityException, NullPointerException {
		Objects.requireNonNull(input, "The file to be read, input, can't be null.");

		input_file = input;
		FileInputStream fin = new FileInputStream(input);
		stream = new BufferedInputStream(fin);
	}

	@Override
	public String readline() throws IOException {
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
		if (open) {
			open = false;
			stream.close();
		} else {
			LogHandler.err_println("Trying to close an already closed FileInputStreamHandler.", true);
			LogHandler.err_println("Input Stream Handler: " + toString(), true);
		}
	}

	@Override
	public String toString() {
		int bav = -1;
		if (!open) {
			bav = 0;
		} else {
			try {
				bav = stream.available();
			} catch (IOException e) {
				LogHandler.print_exception(e, "input handler to stream stream.available",
						"input file: \"%s\", open: %s", input_file.toString(), open ? "true" : "false");
			}
		}

		return String.format(getClass().getSimpleName() + "[input_file=\"%s\", open=%s, done=%s, bytes_available=%s]",
				input_file.toString(), open ? "true" : "false", done() ? "true" : "false",
				bav == -1 ? "error" : Integer.toString(bav));
	}

}
