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
public class SysOutStreamHandler implements IOutputStreamHandler {

	/**
	 * Whether this stream handler has been explicitly closed.
	 */
	private boolean open = true;

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
		if (!open) {
			LogHandler.err_println(
					String.format("Tried to write line \"%s\" to an already closed SysOutStreamHandler."), true);
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
			return false;
		}
		return println(line);
	}

	@Override
	public boolean printDay(TurkeyInfo info, String date, Collection<String> zones) {
		String line = CSVHandler.turkeyToCsvLine(info, date, zones);
		return println(line, info.getCurrentDate().equals(date));
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
		if (open) {
			open = false;
		} else {
			LogHandler.err_println("Trying to close an already closed SysOutStreamHandler.", true);
			LogHandler.err_println("Output Stream Handler: " + toString(), true);
		}
	}

	@Override
	public String toString() {
		return String.format(getClass().getSimpleName() + "[prints temporary=%s, open=%s]", temp ? "true" : "false",
				open ? "true" : "false");
	}

}
