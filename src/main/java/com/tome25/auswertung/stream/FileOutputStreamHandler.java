package com.tome25.auswertung.stream;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Objects;

import com.tome25.auswertung.CSVHandler;
import com.tome25.auswertung.LogHandler;
import com.tome25.auswertung.TurkeyInfo;

/**
 * The class responsible for handling data output to a
 * {@link FileOutputStream}.<br/>
 * File output stream handlers do not write temporary data.
 * 
 * @author theodor
 */
public class FileOutputStreamHandler implements IOutputStreamHandler {

	/**
	 * The file this Output Stream Handler writes to.
	 */
	private final File output_file;

	/**
	 * The OutputStream this Handler writes its data to.
	 */
	private final OutputStream stream;

	/**
	 * Whether this output stream handler is still open.<br/>
	 * {@code false} if it has been closed.
	 */
	private volatile boolean open = true;

	/**
	 * Creates a new file output stream handler, writing to the given file.<br/>
	 * File output stream handlers do not write temporary data.
	 * 
	 * @param output The file to write the data to.
	 * @throws FileNotFoundException if the file exists but is a directory rather
	 *                               than a regular file, does not exist but cannot
	 *                               be created, or cannot be opened for any other
	 *                               reason
	 * @throws SecurityException     if a security manager exists and its
	 *                               {@code checkWrite} method denies write access
	 *                               to the file.
	 * @throws NullPointerException  if the given output file is {@code null}.
	 */
	public FileOutputStreamHandler(File output) throws FileNotFoundException, SecurityException, NullPointerException {
		Objects.requireNonNull(output, "The file to write to can't be null.");

		output_file = output;
		FileOutputStream fout = new FileOutputStream(output);
		stream = new BufferedOutputStream(fout);
	}

	@Override
	public boolean println(String line) {
		try {
			stream.write((line + System.lineSeparator()).getBytes("UTF-8"));
			return true;
		} catch (IOException e) {
			LogHandler.err_println(String.format("Writing line \"%s\" to the data output file failed.", line));
			LogHandler.print_exception(e, "print data to output file", "FileOutputStreamHandler: %s, Line: %s",
					toString(), line);
			return false;
		}
	}

	@Override
	public boolean println(String line, boolean temporary) {
		if (temporary && !printsTemporary()) {
			LogHandler.err_println(String.format(
					"Trying to write line \"%s\" to file output that does not handle temporary data.", line), true);
			return false;
		} else {
			return println(line);
		}
	}

	@Override
	public boolean printDay(TurkeyInfo info, String date, Collection<String> zones) {
		String line = CSVHandler.turkeyToCsvLine(info, date, zones);
		return println(line, info.getCurrentDate().equals(date));
	}

	@Override
	public boolean printsTemporary() {
		return false;
	}

	@Override
	public void flush() throws IOException {
		stream.flush();
	}

	@Override
	public void close() throws IOException {
		if (open) {
			open = false;
			stream.close();
		} else {
			LogHandler.err_println("Trying to close an already closed FileOutputStreamHandler.", true);
			LogHandler.err_println("Output Stream Handler: " + toString(), true);
		}
	}

	@Override
	public String toString() {
		return String.format(getClass().getSimpleName() + "[file=\"%s\", open=%s]", output_file.toString(),
				open ? "true" : "false");
	}

}
