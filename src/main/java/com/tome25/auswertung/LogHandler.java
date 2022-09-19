package com.tome25.auswertung;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.IllegalFormatException;
import java.util.Objects;

import com.tome25.auswertung.stream.MultiOutputStream;

/**
 * The class responsible for handling the printing of status and debug info.
 * 
 * @author theodor
 */
public class LogHandler {

	/**
	 * Whether debug information should be written to the system output.
	 */
	private static boolean debug = true;

	/**
	 * Whether anything, besides potentially the output data, should be written to
	 * the error and output stream.
	 */
	private static boolean silent = false;

	/**
	 * The {@link PrintStream} to write system output messages to.<br/>
	 * Set to {@code null} to use {@link System#out}.
	 */
	private static PrintStream output = null;

	/**
	 * The {@link PrintStream} to write system error messages to.<br/>
	 * Set to {@code null} to use {@link System#err}.
	 */
	private static PrintStream error = null;

	/**
	 * The underlying {@link MultiOutputStream} writing the the primary output
	 * stream, and the log files.
	 */
	private static MultiOutputStream out;

	/**
	 * The underlying {@link MultiOutputStream} writing the the primary error
	 * stream, and the log files.
	 */
	private static MultiOutputStream err;

	/**
	 * The system output stream from before the LogHandler overrode it.
	 */
	private static PrintStream oldOut = null;

	/**
	 * The system error stream from before the LogHandler overrode it.
	 */
	private static PrintStream oldErr = null;

	/**
	 * Writes the given string to the system output if not in silent mode.<br/>
	 * Changes to the next line after writing.<br/>
	 * The given text is not considered debug info, however it is not printed in
	 * silent mode.
	 * 
	 * @param line The string to write to the system output.
	 */
	public static void out_println(String line) {
		out_println(line, false);
	}

	/**
	 * Writes the given string to the system output if not in silent mode.<br/>
	 * Changes to the next line after writing.
	 * 
	 * @param line  The string to write to the system output.
	 * @param debug Whether the string should only be printed in debug mode.
	 */
	public static void out_println(String line, boolean debug) {
		if (debug && !LogHandler.debug) {
			return;
		}

		if (silent) {
			return;
		}

		if (output != null) {
			output.println(line);
		} else {
			System.out.println(line);
		}
	}

	/**
	 * Writes the given string to the system error stream if not in silent
	 * mode.<br/>
	 * Changes to the next line after writing.<br/>
	 * The given text is not considered debug info.
	 * 
	 * @param line The string to write to the system output.
	 */
	public static void err_println(String line) {
		err_println(line, false);
	}

	/**
	 * Writes the given string to the system error stream if not in silent
	 * mode.<br/>
	 * Changes to the next line after writing.
	 * 
	 * @param line  The string to write to the system output.
	 * @param debug Whether the string should only be printed in debug mode.
	 */
	public static void err_println(String line, boolean debug) {
		if (debug && !LogHandler.debug) {
			return;
		}

		if (silent) {
			return;
		}

		if (error != null) {
			error.println(line);
		} else {
			System.err.println(line);
		}
	}

	/**
	 * Prints the given debug information to the system error stream.<br/>
	 * Prints an error message if given an empty or {@code null} info arg.
	 * 
	 * @param info     The debug info string to print. Can contain formatting args.
	 * @param info_arg The formatting args from info.
	 */
	public static void print_debug_info(String info, Object... info_args) {
		if (!debug || silent) {
			return;
		}

		if (info == null || info.isEmpty()) {
			LogHandler.err_println("print_debug_info: Received empty debug info string.", true);
			return;
		}

		String info_str = "Additional debug Information: " + String.format(info, info_args);
		err_println(info_str, true);
	}

	/**
	 * Prints the given exception and debug info to the system error stream.<br/>
	 * Does not print if not in debug mode.
	 * 
	 * @param ex        The exception to print.
	 * @param task      What was currently being done when the exception occurred.
	 * @param info      Additional debug information. Not printed at all if
	 *                  {@code null}.
	 * @param info_args String formatting args for info.
	 * @throws IllegalFormatException if the types or number of formatting args does
	 *                                not match those used in the info string.
	 */
	public static void print_exception(Exception ex, String task, String info, Object... info_args)
			throws IllegalFormatException {
		if (ex == null || !debug || silent) {
			return;
		}

		if (output != null) {
			output.flush();
		} else {
			System.out.flush();
		}

		if (task != null && !task.isEmpty()) {
			err_println("An Exception occurred while trying to " + task + '.');
		}

		if (info != null) {
			print_debug_info(info, info_args);
		}

		err_println("Exception stack trace:");
		if (error != null) {
			ex.printStackTrace(error);
			error.flush();
		} else {
			ex.printStackTrace(System.err);
			System.err.flush();
		}
	}

	/**
	 * Gets the current output stream the {@code LogHandler} writes log messages to.
	 * 
	 * @return The current standard output stream.
	 */
	public static PrintStream getOutput() {
		return output;
	}

	/**
	 * Sets the new output stream to write log messages to.<br/>
	 * Set to {@code null} to use {@link System#out}.<br/>
	 * Completely replaces the output stream, potentially removing all log files
	 * added using {@link #addLogFile}.
	 * 
	 * @param output the new standard output stream.
	 */
	public static void setOutput(PrintStream out) {
		output = out;
		LogHandler.out = null;
	}

	/**
	 * Gets the current output stream the {@code LogHandler} writes error messages
	 * to.
	 * 
	 * @return The current standard error stream.
	 */
	public static PrintStream getError() {
		return error;
	}

	/**
	 * Sets the new output stream to write error messages to.<br/>
	 * Set to {@code null} to use {@link System#err}.<br/>
	 * Completely replaces the error stream, potentially removing all log files
	 * added using {@link #addLogFile}.
	 * 
	 * @param output the new standard error stream.
	 */
	public static void setError(PrintStream err) {
		error = err;
		LogHandler.err = null;
	}

	/**
	 * Checks whether the log handler is currently printing debug messages.
	 * 
	 * @return {@code true} if debug messages are printed.
	 */
	public static boolean isDebug() {
		return debug;
	}

	/**
	 * Sets whether the log handler should print debug messages.
	 * 
	 * @param debug Whether the log handler should print debug messages.
	 */
	public static void setDebug(boolean debug) {
		LogHandler.debug = debug;
	}

	/**
	 * Checks whether the log handler should print any messages at all.
	 * 
	 * @return {@code true} if the log handler does not print anything at all.
	 */
	public static boolean isSilent() {
		return silent;
	}

	/**
	 * Sets whether the log handler should be silent, meaning it doesn't print any
	 * messages at all.
	 * 
	 * @param silent Whether the log handler should be silent.
	 */
	public static void setSilent(boolean silent) {
		LogHandler.silent = silent;
	}

	/**
	 * Adds the given file as a log file to write the system log to.
	 * 
	 * @param log The file to write to.
	 * @param out If the file should be added as a log file for the output stream.
	 * @param err If the file should be added as a log file for the error stream.
	 * @throws FileNotFoundException if the file exists but is a directory rather
	 *                               than a regular file, does not exist but cannot
	 *                               be created, or cannot be opened for any other
	 *                               reason
	 * @throws SecurityException     if a security manager exists and its
	 *                               {@code checkWrite} method denies write access
	 *                               to the file.
	 * @throws NullPointerException  If {@code log} is {@code null}.
	 */
	public static void addLogFile(File log, boolean out, boolean err)
			throws FileNotFoundException, SecurityException, NullPointerException {
		Objects.requireNonNull(log, "The log file to add can't be null.");

		if (!out && !err) {
			return;
		}

		FileOutputStream fiout = new FileOutputStream(log);

		if (err) {
			if (LogHandler.err == null) {
				LogHandler.err = new MultiOutputStream(error == null ? System.err : error, fiout);
				error = new PrintStream(LogHandler.err);
			} else {
				LogHandler.err.addStream(fiout);
			}
		}

		if (out) {
			if (LogHandler.out == null) {
				LogHandler.out = new MultiOutputStream(output == null ? System.out : output, fiout);
				output = new PrintStream(LogHandler.out);
			} else {
				LogHandler.out.addStream(fiout);
			}
		}
	}

	/**
	 * Overrides the system standard output with the output stream used by this log
	 * handler.<br/>
	 * Doesn't do anything if this log handler is already writing to
	 * {@link System#out}.
	 * 
	 * @see #overrideSysErr()
	 * @see #resetSysOut()
	 */
	public static void overrideSysOut() {
		if (output == null) {
			return;
		}

		if (oldOut == null) {
			oldOut = System.out;
		}

		System.setOut(output);
	}

	/**
	 * Overrides the system standard error with the output stream used by this log
	 * handler.<br/>
	 * Doesn't do anything if this log handler is already writing errors to
	 * {@link System#err}.
	 * 
	 * @see #overrideSysOut()
	 * @see #resetSysErr()
	 */
	public static void overrideSysErr() {
		if (error == null) {
			return;
		}

		if (oldErr == null) {
			oldErr = System.err;
		}

		System.setErr(error);
	}

	/**
	 * Resets the system output stream to the one it was before it was changed by
	 * the log handler.<br/>
	 * If {@link #overrideSysOut()} was called twice, it resets to the one before
	 * the first call, unless this method was called between those two calls.
	 * 
	 * Does not do anything if {@link #overrideSysOut()} wasn't called since this
	 * method was last called.
	 * 
	 * @see #overrideSysOut()
	 * @see #resetSysErr()
	 */
	public static void resetSysOut() {
		if (oldOut != null) {
			System.setOut(oldOut);

			oldOut = null;
		}
	}

	/**
	 * Resets the system error stream to the one it was before it was changed by the
	 * log handler.<br/>
	 * If {@link #overrideSysErr()} was called twice, it resets to the one before
	 * the first call, unless this method was called between those two calls.
	 * 
	 * Does not do anything if {@link #overrideSysErr()} wasn't called since this
	 * method was last called.
	 * 
	 * @see #overrideSysErr()
	 * @see #resetSysOut()
	 */
	public static void resetSysErr() {
		if (oldErr != null) {
			System.setErr(oldErr);

			oldErr = null;
		}
	}
}
