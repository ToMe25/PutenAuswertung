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
 * File output stream handlers do not write temporary data by default.
 * 
 * @author theodor
 */
// TODO unify with SysOut impl into generalized Stream/File handler
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
	 * Whether this output stream handler handles temporary data.
	 */
	private final boolean writeTemp;

	/**
	 * Whether this output stream handler calls {@link OutputStream#flush()} after
	 * every println call.
	 */
	private final boolean autoFlush;

	/**
	 * Whether this output stream handler has been explicitly closed.
	 */
	private volatile boolean closed = false;

	/**
	 * Creates a new file output stream handler, writing to the given file.<br/>
	 * File output stream handlers do not write temporary data by default.
	 * 
	 * @param output The file to write the data to.
	 * @throws FileNotFoundException if the file exists but is a directory rather
	 *                               than a regular file, does not exist but cannot
	 *                               be created, or cannot be opened for any other
	 *                               reason
	 * @throws NullPointerException  if the given output file is {@code null}.
	 */
	public FileOutputStreamHandler(File output) throws FileNotFoundException, NullPointerException {
		this(output, false);
	}

	/**
	 * Creates a new file output stream handler, writing to the given file.
	 * 
	 * @param output    The file to write the data to.
	 * @param writeTemp Whether this output stream handler should handle temporary
	 *                  data, or ignore it.
	 * @throws FileNotFoundException if the file exists but is a directory rather
	 *                               than a regular file, does not exist but cannot
	 *                               be created, or cannot be opened for any other
	 *                               reason
	 * @throws NullPointerException  if the given output file is {@code null}.
	 */
	public FileOutputStreamHandler(File output, boolean writeTemp) throws FileNotFoundException, NullPointerException {
		this(output, writeTemp, false);
	}

	/**
	 * Creates a new file output stream handler, writing to the given file.<br/>
	 * File output stream handlers do not write temporary data.
	 * 
	 * @param output    The file to write the data to.
	 * @param writeTemp Whether this output stream handler should handle temporary
	 *                  data, or ignore it.
	 * @param autoFlush Whether this output stream handler should call
	 *                  {@link OutputStream#flush()} every time something was
	 *                  written to it.
	 * @throws FileNotFoundException if the file exists but is a directory rather
	 *                               than a regular file, does not exist but cannot
	 *                               be created, or cannot be opened for any other
	 *                               reason
	 * @throws NullPointerException  if the given output file is {@code null}.
	 */
	public FileOutputStreamHandler(File output, boolean writeTemp, boolean autoFlush)
			throws FileNotFoundException, NullPointerException {
		Objects.requireNonNull(output, "The file to write to can't be null.");

		output_file = output;
		FileOutputStream fout = new FileOutputStream(output);
		stream = new BufferedOutputStream(fout);

		this.writeTemp = writeTemp;
		this.autoFlush = autoFlush;
	}

	@Override
	public boolean println(String line) {
		if (closed) {
			LogHandler.err_println(
					String.format("Tried to write line \"%s\" to an already closed FileOutputStreamHandler.", line),
					true);
			LogHandler.print_debug_info("stream handler: %s, line: \"%s\"", toString(), line);
			return false;
		}

		try {
			if (line == null || line.isEmpty()) {
				stream.write(System.lineSeparator().getBytes("UTF-8"));
			} else {
				stream.write((line + System.lineSeparator()).getBytes("UTF-8"));
			}

			if (autoFlush) {
				stream.flush();
			}
			return true;
		} catch (IOException e) {
			LogHandler.err_println(String.format("Writing line \"%s\" to the data output file failed.", line));
			LogHandler.print_exception(e, "print data to output file", "stream handler: %s, line: %s", toString(),
					line);
			return false;
		}
	}

	@Override
	public boolean println(String line, boolean temporary) {
		if (temporary && !printsTemporary()) {
			LogHandler.err_println(String.format(
					"Trying to write line \"%s\" to file output that does not handle temporary data.", line), true);
			LogHandler.print_debug_info("stream handler: %s, line: \"%s\", temporary: %s", toString(), line,
					temporary ? "true" : "false");
			return false;
		} else {
			return println(line);
		}
	}

	@Override
	public boolean printDay(TurkeyInfo info, String date, Collection<String> zones) {
		String line = CSVHandler.turkeyToCsvLine(info, date, zones);
		return println(line, info.getCurrentDate().equals(date) && info.getCurrentTime() != TurkeyInfo.DAY_END);
	}

	/**
	 * Gets the output file this stream handler writes to.
	 * 
	 * @return the output file this stream handler writes to.
	 */
	public File getOutputFile() {
		return output_file;
	}

	@Override
	public boolean printsTemporary() {
		return writeTemp;
	}

	@Override
	public void flush() throws IOException {
		stream.flush();
	}

	@Override
	public void close() throws IOException {
		if (!closed) {
			closed = true;
			stream.close();
		} else {
			LogHandler.err_println("Trying to close an already closed FileOutputStreamHandler.", true);
			LogHandler.print_debug_info("stream handler: %s", toString());
		}
	}

	@Override
	public String toString() {
		return String.format(
				getClass().getSimpleName() + "[file=\"%s\", closed=%s, writes temporary=%s, auto flush=%s]",
				output_file.toString(), closed ? "true" : "false", writeTemp ? "true" : "false",
				autoFlush ? "true" : "false");
	}

}
