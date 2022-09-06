package com.tome25.auswertung;

import java.io.PrintStream;
import java.util.IllegalFormatException;

/**
 * The class responsible for handling the printing of status and debug info.
 * 
 * @author theodor
 */
public class LogHandler {

	/**
	 * Whether debug information should be written to the system output.
	 */
	private static boolean debug = false;

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
	 * Set to {@code null} to use {@link System#out}.
	 * 
	 * @param output the new standard output stream.
	 */
	public static void setOutput(PrintStream out) {
		output = out;
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
	 * Set to {@code null} to use {@link System#err}.
	 * 
	 * @param output the new standard error stream.
	 */
	public static void setError(PrintStream err) {
		error = err;
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
}
