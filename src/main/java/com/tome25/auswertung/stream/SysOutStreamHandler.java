package com.tome25.auswertung.stream;

import java.util.Collection;

import com.tome25.auswertung.CSVHandler;
import com.tome25.auswertung.LogHandler;
import com.tome25.auswertung.TurkeyInfo;

/**
 * Handles writing generated data to the system output stream.<br/>
 * 
 * Note: This handler always prints to the current system output, if the system
 * output stream is changed this handler will write to the new one.<br/>
 * 
 * Note: closing this will NOT close the system output stream!
 * 
 * @author theodor
 */
// TODO unify base methods, or all methods, with File Out impl
public class SysOutStreamHandler implements IOutputStreamHandler {

	/**
	 * Whether this stream handler has been explicitly closed.
	 */
	private volatile boolean closed = false;

	/**
	 * Whether this stream handler handles temporary output.
	 */
	private boolean temp;

	/**
	 * Creates a new SysOutStreamHandler.<br/>
	 * The newly create stream handler handles temporary data.<br/>
	 * 
	 * Note: This handler always prints to the current system output, if the system
	 * output stream is changed this handler will write to the new one.<br/>
	 * 
	 * Note: closing this will NOT close the system output stream!
	 */
	public SysOutStreamHandler() {
		this(true);
	}

	/**
	 * Creates a new SysOutStreamHandler.<br/>
	 * 
	 * Note: This handler always prints to the current system output, if the system
	 * output stream is changed this handler will write to the new one.<br/>
	 * 
	 * Note: closing this will NOT close the system output stream!
	 * 
	 * @param handleTemporary Whether this stream handler should write temporary
	 *                        data.
	 */
	public SysOutStreamHandler(boolean handleTemporary) {
		temp = handleTemporary;
	}

	@Override
	public boolean println(String line) {
		if (closed) {
			LogHandler.err_println(
					String.format("Tried to write line \"%s\" to an already closed SysOutStreamHandler.", line), true);
			LogHandler.print_debug_info("stream handler: %s, line: \"%s\"", toString(), line);
			return false;
		}

		boolean err = false;
		if (System.out.checkError()) {
			LogHandler.err_println(
					String.format("Trying to write line \"%s\" to a PrintStream with an error set.", line), true);
			LogHandler.err_println(
					"This is not recommended, and likely wont work. Also it cannot be verified whether printing worked.",
					true);
			LogHandler.print_debug_info("stream handler: %s, print stream: %s", toString(), System.out);
			err = true;
		}

		System.out.println(line);

		if (!err && System.out.checkError()) {
			LogHandler.err_println(
					String.format("An error occured while writing line \"%s\" to the system output stream.", line));
			LogHandler.print_debug_info("stream handler: %s, print stream: %s", toString(), System.out);
			return false;
		} else {
			return true;
		}
	}

	@Override
	public boolean println(String line, boolean temporary) {
		if (temporary && !printsTemporary()) {
			LogHandler.err_println(String.format(
					"Trying to write line \"%s\" to system output that does not handle temporary data.", line), true);
			LogHandler.print_debug_info("stream handler: %s, line: \"%s\", temporary: %s", toString(), line,
					temporary ? "true" : "false");
			return false;
		}
		return println(line);
	}

	@Override
	public boolean printDay(TurkeyInfo info, String date, Collection<String> zones) {
		String line = CSVHandler.turkeyToCsvLine(info, date, zones);
		return println(line, info.getCurrentDate().equals(date) && info.getCurrentTime() != TurkeyInfo.DAY_END);
	}

	/**
	 * A utility wrapper for calling {@link java.io.PrintStream#checkError() System.out.checkError}.
	 * 
	 * @return {@code true} if and only if this stream has encountered an
	 *         {@code IOException} other than {@code InterruptedIOException}, or the
	 *         {@code setError} method has been invoked.
	 */
	public boolean checkError() {
		return System.out.checkError();
	}

	@Override
	public boolean printsTemporary() {
		return temp;
	}

	@Override
	public void flush() {
		System.out.flush();
	}

	@Override
	public void close() {
		if (!closed) {
			closed = true;
		} else {
			LogHandler.err_println("Trying to close an already closed SysOutStreamHandler.", true);
			LogHandler.print_debug_info("stream handler: %s", toString());
		}
	}

	@Override
	public String toString() {
		return String.format(getClass().getSimpleName() + "[prints temporary=%s, closed=%s]", temp ? "true" : "false",
				closed ? "true" : "false");
	}

}
