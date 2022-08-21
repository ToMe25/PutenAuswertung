package com.tome25.auswertung;

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
	private static boolean debug = true;

	/**
	 * Whether anything, besides potentially the output data, should be written to
	 * the error and output stream.
	 */
	private static boolean silent = false;

	/**
	 * Writes the given string to the system output if not in silent mode.<br/>
	 * Changes to the next line after writing.<br/>
	 * The given text is not considered debug info, however it is not printed in
	 * silent mode.
	 * 
	 * @param output The string to write to the system output.
	 */
	public static void out_println(String output) {
		out_println(output, false);
	}

	/**
	 * Writes the given string to the system output if not in silent mode.<br/>
	 * Changes to the next line after writing.
	 * 
	 * @param output The string to write to the system output.
	 * @param debug  Whether the string should only be printed in debug mode.
	 */
	public static void out_println(String output, boolean debug) {
		if (debug && !LogHandler.debug) {
			return;
		}

		if (silent) {
			return;
		}

		System.out.println(output);
	}

	/**
	 * Writes the given string to the system error stream if not in silent
	 * mode.<br/>
	 * Changes to the next line after writing.<br/>
	 * The given text is not considered debug info.
	 * 
	 * @param output The string to write to the system output.
	 */
	public static void err_println(String output) {
		err_println(output, false);
	}

	/**
	 * Writes the given string to the system error stream if not in silent
	 * mode.<br/>
	 * Changes to the next line after writing.
	 * 
	 * @param output The string to write to the system output.
	 * @param debug  Whether the string should only be printed in debug mode.
	 */
	public static void err_println(String output, boolean debug) {
		if (debug && !LogHandler.debug) {
			return;
		}

		if (silent) {
			return;
		}

		System.err.println(output);
	}
	
	/**
	 * Prints the given debug information to the system error stream.<br/>
	 * Prints an error message if given an empty or <code>null</code> info arg.
	 * 
	 * @param info		The debug info string to print. Can contain formatting args.
	 * @param info_arg	The formatting args from info.
	 */
	public static void print_debug_info(String info, Object... info_args) {
		if (info == null || info.isEmpty()) {
			LogHandler.err_println("print_debug_info: Received empty debug info string.", true);
			return;
		}
		
		String info_str = String.format(info, info_args);
		System.err.println("Additional debug Information: " + info_str);
	}
	
	/**
	 * Prints the given exception and debug info to the system error stream.<br/>
	 * Does not print if not in debug mode.
	 * 
	 * @param ex		The exception to print.
	 * @param task		What was currently being done when the exception occurred.
	 * @param info		Additional debug information. Not printed at all if <code>null</code>.
	 * @param info_args	String formatting args for info.
	 * @throws	IllegalFormatException if the types or number of formatting args does not match those used in the info string.
	 */
	public static void print_exception(Exception ex, String task, String info, Object... info_args) throws IllegalFormatException {
		if (ex == null || !debug || silent) {
			return;
		}
		
		System.out.flush();
		System.err.printf("An Exception occurred while trying to %s.%n", task);
		
		if (info != null) {
			print_debug_info(info, info_args);
		}
		
		System.err.println("Exception stack trace:");
		ex.printStackTrace();
		System.err.flush();
	}
}
