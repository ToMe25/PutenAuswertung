package com.tome25.auswertung.stream;

import java.io.IOException;
import java.util.Collection;

import com.tome25.auswertung.CSVHandler;
import com.tome25.auswertung.LogHandler;
import com.tome25.auswertung.TurkeyInfo;
import com.tome25.auswertung.utils.StringUtils;

/**
 * A wrapper {@link IOutputStreamHandler} writing to multiple other
 * {@link IOutputStreamHandler IOutputStreamHandlers}.
 * 
 * @author theodor
 */
public class MultiOutputStreamHandler implements IOutputStreamHandler {

	/**
	 * Whether this stream handler has already been closed using the close method.
	 */
	private volatile boolean closed = false;

	/**
	 * An array containing all the IOutputStreamHandlers this handler writes to.
	 */
	private final IOutputStreamHandler handlers[];

	/**
	 * Creates a new multi output stream handler writing to the given output stream
	 * handlers.
	 * 
	 * @param handlers The output stream handlers to handle.
	 * @throws NullPointerException if the handlers array is {@code null} or empty.
	 */
	public MultiOutputStreamHandler(IOutputStreamHandler... handlers) throws NullPointerException {
		if (handlers == null || handlers.length == 0) {
			throw new NullPointerException(
					"A multi output stream handler needs at least one output handler to handle.");
		}

		this.handlers = handlers;
	}

	@Override
	public boolean println(String line) {
		boolean err = false;
		for (IOutputStreamHandler handler : handlers) {
			err = handler.println(line) || err;
		}
		return err;
	}

	@Override
	public boolean println(String line, boolean temporary) {
		boolean err = false;
		for (IOutputStreamHandler handler : handlers) {
			if (!temporary || handler.printsTemporary()) {
				err = handler.println(line, temporary) || err;
			}
		}
		return err;
	}

	@Override
	public boolean printDay(TurkeyInfo info, String date, Collection<String> zones) {
		String line = CSVHandler.turkeyToCsvLine(info, date, zones);
		return println(line, info.getCurrentDate().equals(date));
	}

	@Override
	public boolean printsTemporary() {
		for (IOutputStreamHandler handler : handlers) {
			if (handler.printsTemporary()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void flush() {
		for (IOutputStreamHandler handler : handlers) {
			try {
				handler.flush();
			} catch (IOException e) {
				LogHandler.err_println("An error occurred while flushing an output stream handler.");
				LogHandler.print_exception(e, "flushing sub output stream handler",
						"Multi Handler: %s, Errored Handler: %s", toString(), handler);
			}
		}
	}

	@Override
	public void close() {
		if (closed) {
			LogHandler.err_println("Trying to close an already closed MultiOutputStreamHandler.", true);
			LogHandler.err_println("Output Stream Handler: " + toString(), true);
		} else {
			closed = true;
			for (IOutputStreamHandler handler : handlers) {
				try {
					handler.close();
				} catch (IOException e) {
					LogHandler.err_println("An error occurred while closing an output stream handler.");
					LogHandler.print_exception(e, "closing sub output stream handler",
							"Multi Handler: %s, Errored Handler: %s", toString(), handler);
				}
			}
		}
	}

	@Override
	public String toString() {
		return String.format(getClass().getSimpleName() + "[open=%s, prints temporary=%s, sub handlers=[%s]]",
				closed ? "false" : "true", printsTemporary() ? "true" : "false",
				StringUtils.join(',', (Object[]) handlers));
	}

}
